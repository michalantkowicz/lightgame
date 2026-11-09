#!/usr/bin/env python3
"""
Turns a sprite pack (Sprout Lands) into everything the project needs:

  assets-src/tilesets/*.tsx           Tiled tilesets with terrains (Wang sets) and tile animations  (committed)
  assets-src/tilesets/images/*.png    copies of the pack's sheets, used by Tiled                      (git-ignored)
  assets-src/maps/*.tmx               your levels, edited in Tiled (a starter level is created once)  (committed)

  android/assets/maps/*.tmx           game copies of the levels, pointing at the tiles atlas          (generated)
  android/assets/maps/tiles.atlas     ONLY the tiles the levels use, in one texture                   (generated)
  android/assets/sprites/sprites.atlas  the sprite sheets listed in the game's Sprite enum, in one texture (generated)
  android/assets/sprites/animations.json  animation names, frame counts, durations                     (generated)
  core/src/.../resources/Action.java  enum of the animation actions named in pack.json                 (generated)
  assets-src/lightgame.tiled-project  Tiled project, with the objectClass dropdown (TileObjectType's constants)

  build/assets-preview/               animated GIF of every animation, terrain overlays, report        (git-ignored)

What is in the pack is described in tools/assets/pack.json (which sheet is a tileset, which one has terrains,
how animation sheets are cut). Terrains are detected from the tiles' transparency, empty animation frames are skipped,
so a changed pack usually needs no change in pack.json. Files that pack.json doesn't mention are reported.

Usage:
  python3 tools/assets/build_assets.py "/path/to/Sprout Lands - Sprites - premium pack"
  python3 tools/assets/build_assets.py            # same pack as last time

Needs Pillow (pip3 install pillow).
"""
import argparse
import base64
import difflib
import gzip
import hashlib
import json
import math
import os
import re
import shutil
import sys
import xml.etree.ElementTree as ET
import zlib
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow is missing: pip3 install pillow")

ROOT = Path(__file__).resolve().parents[2]
TOOL_DIR = Path(__file__).resolve().parent
CONFIG_FILE = TOOL_DIR / "pack.json"
PACK_PATH_FILE = TOOL_DIR / ".pack-path"  # remembers the last pack directory (git-ignored)

SRC_DIR = ROOT / "assets-src"
TILESETS_DIR = SRC_DIR / "tilesets"
TILESET_IMAGES_DIR = TILESETS_DIR / "images"
SRC_MAPS_DIR = SRC_DIR / "maps"

GAME_ASSETS = ROOT / "android" / "assets"
GAME_MAPS_DIR = GAME_ASSETS / "maps"
GAME_SPRITES_DIR = GAME_ASSETS / "sprites"
GAME_RESOURCES_SRC = ROOT / "core" / "src" / "com" / "mantkowicz" / "light" / "resources"
SPRITE_ENUM = GAME_RESOURCES_SRC / "Sprite.java"  # written by hand: the sheets the game uses
ACTION_ENUM = GAME_RESOURCES_SRC / "Action.java"  # generated from the row names in pack.json
OBJECT_TYPE_ENUM = ROOT / "core" / "src" / "com" / "mantkowicz" / "light" / "map" / "TileObjectType.java"
OBJECT_CLASS = "objectClass"  # the map object property picked from a dropdown in Tiled (enum ObjectClass)
DIRECTIONS = ("down", "up", "left", "right")  # the game's Direction enum
TILES_ATLAS = "tiles"
SPRITES_ATLAS = "sprites"

PREVIEW_DIR = ROOT / "build" / "assets-preview"

FLIP_FLAGS = 0xF0000000
MAX_PAGE = 4096
TILED_VERSION = "1.10.2"

warnings = []


def warn(message):
    warnings.append(message)
    print("  ! " + message)


# ----------------------------------------------------------------------------------------------------------------------
# names and images
# ----------------------------------------------------------------------------------------------------------------------

def slug(text):
    """'Grass_tiles_v2.png' -> 'grass_tiles_v2'; used for tileset names, atlas regions and file names."""
    text = re.sub(r"\.[a-zA-Z]+$", "", Path(text).name)
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")


def load_image(pack, relative):
    path = pack / relative
    if not path.exists():
        raise SystemExit(f"Missing in the pack: {relative} (fix tools/assets/pack.json)")
    try:
        return Image.open(path).convert("RGBA")
    except PermissionError:
        raise SystemExit(f"Not allowed to read {path}. macOS protects Downloads, Desktop and Documents: give this "
                         "terminal access in System Settings → Privacy & Security → Files and Folders, or move the pack "
                         "elsewhere (e.g. next to the repo).")


def is_empty(image):
    return image.getchannel("A").getbbox() is None


def source_hash(path):
    """
    SHA-1 of a source file's text with \\n line ends. Generated files record the hash of what they were made from, so
    e2e's AssetsUpToDateTest can tell when a source changed and the tool wasn't run (git checkouts don't keep times).
    """
    text = path.read_bytes().decode("utf-8").replace("\r\n", "\n")
    return hashlib.sha1(text.encode("utf-8")).hexdigest()


def save_if_changed(path, data):
    """Writes text or bytes only when they differ, so unchanged outputs keep their timestamps (and git stays quiet)."""
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, str):
        data = data.encode("utf-8")
    if path.exists() and path.read_bytes() == data:
        return False
    path.write_bytes(data)
    return True


def png_bytes(image):
    from io import BytesIO
    out = BytesIO()
    image.save(out, "PNG", optimize=True)
    return out.getvalue()


def save_png_if_changed(path, image):
    if path.exists():
        try:
            old = Image.open(path).convert("RGBA")
            if old.size == image.size and old.tobytes() == image.tobytes():
                return False
        except OSError:
            pass
    return save_if_changed(path, png_bytes(image))


# ----------------------------------------------------------------------------------------------------------------------
# terrain detection (Tiled "mixed" Wang sets: 4 edges + 4 corners)
# ----------------------------------------------------------------------------------------------------------------------

# Wang id order used by Tiled: top, top-right, right, bottom-right, bottom, bottom-left, left, top-left
def detect_wang_id(tile):
    """
    Which of the 8 border zones of a terrain tile are covered by the terrain, judged by opacity right at the border.
    An edge zone counts when most of the middle of that edge is opaque; a corner counts only when the corner pixels are
    opaque and both neighbouring edges count (that is how blob terrains are drawn).
    """
    alpha = tile.getchannel("A")
    w, h = tile.size

    def opaque_fraction(box):
        region = alpha.crop(box)
        pixels = region.getdata()
        return sum(1 for a in pixels if a > 0) / max(1, len(pixels))

    m0, m1 = w // 2 - 3, w // 2 + 3
    n0, n1 = h // 2 - 3, h // 2 + 3
    top = opaque_fraction((m0, 0, m1, 1)) > 0.5
    bottom = opaque_fraction((m0, h - 1, m1, h)) > 0.5
    left = opaque_fraction((0, n0, 1, n1)) > 0.5
    right = opaque_fraction((w - 1, n0, w, n1)) > 0.5

    def corner(box, edge_a, edge_b):
        return edge_a and edge_b and opaque_fraction(box) > 0.5

    top_left = corner((0, 0, 2, 2), top, left)
    top_right = corner((w - 2, 0, w, 2), top, right)
    bottom_right = corner((w - 2, h - 2, w, h), bottom, right)
    bottom_left = corner((0, h - 2, 2, h), bottom, left)
    return [int(v) for v in (top, top_right, right, bottom_right, bottom, bottom_left, left, top_left)]


def is_opaque(image):
    return image.getchannel("A").getextrema()[0] > 0


def terrain_regions(entry, rows):
    """
    pack.json 'terrain' is a name (whole sheet) or a list of {name, rows: [first, last]}. 'layoutFrom', 'wang' and
    'terrainExclude' next to a name apply to the whole sheet; inside a list each region has its own.
    """
    terrain = entry.get("terrain")
    if not terrain:
        return []
    if isinstance(terrain, str):
        return [{"name": terrain, "rows": [0, rows - 1], "exclude": entry.get("terrainExclude", []),
                 "layoutFrom": entry.get("layoutFrom"), "wang": entry.get("wang", {})}]
    return [dict(region, exclude=region.get("exclude", [])) for region in terrain]


_layouts = {}


def region_layout(pack, tile_size, reference):
    """
    Wang ids detected on a reference sheet, by (row, column). Sheets that draw outlines into their inner corners can't be
    detected reliably, but they share the layout of a clean cut-out sheet - which is detected instead.
    """
    if not reference:
        return None
    if reference not in _layouts:
        image = load_image(pack, reference)
        layout = {}
        for row in range(image.height // tile_size):
            for column in range(image.width // tile_size):
                x, y = column * tile_size, row * tile_size
                tile = image.crop((x, y, x + tile_size, y + tile_size))
                if not is_empty(tile):
                    wang = detect_wang_id(tile)
                    if any(wang):
                        layout[(row, column)] = wang
        _layouts[reference] = layout
    return _layouts[reference]


# ----------------------------------------------------------------------------------------------------------------------
# tilesets
# ----------------------------------------------------------------------------------------------------------------------

class Tileset:
    def __init__(self, entry, pack, tile_size):
        self.file = entry["file"]
        self.name = entry.get("name") or slug(self.file)
        self.image = load_image(pack, self.file)
        self.tile = tile_size
        self.columns = self.image.width // tile_size
        self.rows = self.image.height // tile_size
        if self.image.width % tile_size or self.image.height % tile_size:
            warn(f"{self.file}: {self.image.size} is not a multiple of {tile_size}px, the remainder is ignored")
        self.count = self.columns * self.rows
        self.animations = {}  # tile id -> [(frame tile id, duration ms)]
        for animation in entry.get("animations", []):
            frames = animation["frames"]
            self.animations[animation["tile"]] = [(f, animation["duration"]) for f in frames]
        self.wangsets = []  # (name, [(tile id, wang id)], fill tiles)
        for region in terrain_regions(entry, self.rows):
            first, last = region["rows"]
            layout = region_layout(pack, tile_size, region.get("layoutFrom"))
            overrides = {int(k): [int(v) for v in w.split(",")] for k, w in region.get("wang", {}).items()}
            tiles, fills = [], []
            for tile_id in range(first * self.columns, (last + 1) * self.columns):
                if tile_id in region["exclude"]:
                    continue
                tile = self.tile_image(tile_id)
                if is_empty(tile):
                    continue
                if tile_id in overrides:
                    wang = overrides[tile_id]
                elif layout is not None:
                    # same layout as a clean reference sheet: take its piece at the same place
                    wang = layout.get((tile_id // self.columns - first, tile_id % self.columns))
                    if wang is None or (all(wang) and not is_opaque(tile)):
                        continue  # not a terrain piece here (e.g. an overlay where the reference has a fill tile)
                else:
                    wang = detect_wang_id(tile)
                if not any(wang):
                    continue  # a loose decoration inside the sheet, not part of the terrain
                tiles.append((tile_id, wang))
                if all(wang):
                    fills.append(tile_id)
            if not fills:
                warn(f"{self.file}: terrain '{region['name']}' has no fully covered tile, check its rows in pack.json")
                continue
            pieces = [tuple(w) for _, w in tiles if not all(w)]
            if layout is None and len(pieces) - len(set(pieces)) > 4:
                warn(f"{self.file}: terrain '{region['name']}' detection looks unreliable ({len(set(pieces))} different "
                     f"pieces for {len(pieces)} tiles) - give it a 'layoutFrom' sheet with the same layout in pack.json")
            self.wangsets.append((region["name"], tiles, fills))

    def tile_image(self, tile_id):
        x, y = (tile_id % self.columns) * self.tile, (tile_id // self.columns) * self.tile
        return self.image.crop((x, y, x + self.tile, y + self.tile))

    def image_name(self):
        return self.name + ".png"

    def tsx(self):
        root = ET.Element("tileset", {
            "version": "1.10", "tiledversion": TILED_VERSION, "name": self.name,
            "tilewidth": str(self.tile), "tileheight": str(self.tile),
            "tilecount": str(self.count), "columns": str(self.columns),
        })
        ET.SubElement(root, "image", {"source": "images/" + self.image_name(),
                                      "width": str(self.columns * self.tile), "height": str(self.rows * self.tile)})
        # fill tiles: the first one is the plain one, the others are variations Tiled sprinkles in now and then
        probabilities = {}
        for _, _, fills in self.wangsets:
            for index, tile_id in enumerate(fills):
                if index > 0:
                    probabilities[tile_id] = "0.1"
        for tile_id in sorted(set(self.animations) | set(probabilities)):
            attributes = {"id": str(tile_id)}
            if tile_id in probabilities:
                attributes["probability"] = probabilities[tile_id]
            tile = ET.SubElement(root, "tile", attributes)
            if tile_id in self.animations:
                animation = ET.SubElement(tile, "animation")
                for frame, duration in self.animations[tile_id]:
                    ET.SubElement(animation, "frame", {"tileid": str(frame), "duration": str(duration)})
        if self.wangsets:
            wangsets = ET.SubElement(root, "wangsets")
            for index, (name, tiles, fills) in enumerate(self.wangsets):
                wangset = ET.SubElement(wangsets, "wangset", {"name": name, "type": "mixed", "tile": str(fills[0])})
                ET.SubElement(wangset, "wangcolor", {"name": name, "color": COLORS[index % len(COLORS)],
                                                     "tile": str(fills[0]), "probability": "1"})
                for tile_id, wang in tiles:
                    ET.SubElement(wangset, "wangtile", {"tileid": str(tile_id), "wangid": ",".join(map(str, wang))})
        return xml_text(root)

    def terrain_preview(self, scale=4):
        """The sheet with the detected terrain corners/edges drawn over it, to check the detection by eye."""
        from PIL import ImageDraw
        big = self.image.resize((self.image.width * scale, self.image.height * scale), Image.NEAREST)
        canvas = Image.new("RGBA", big.size, (40, 40, 50, 255))
        canvas.alpha_composite(big)
        draw = ImageDraw.Draw(canvas)
        size = self.tile * scale
        spots = [(0.5, 0.08), (0.92, 0.08), (0.92, 0.5), (0.92, 0.92), (0.5, 0.92), (0.08, 0.92), (0.08, 0.5), (0.08, 0.08)]
        for _, tiles, fills in self.wangsets:
            for tile_id, wang in tiles:
                x0, y0 = (tile_id % self.columns) * size, (tile_id // self.columns) * size
                draw.rectangle([x0, y0, x0 + size - 1, y0 + size - 1], outline=(255, 255, 255, 60))
                for (fx, fy), bit in zip(spots, wang):
                    cx, cy, r = x0 + fx * size, y0 + fy * size, max(2, scale // 2 + 1)
                    color = (60, 220, 90, 255) if bit else (230, 60, 60, 255)
                    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
                if tile_id in fills:
                    draw.text((x0 + 3, y0 + size // 2 - 5), "fill", fill=(255, 255, 0, 255))
        return canvas


COLORS = ["#ff0000", "#00ff00", "#0000ff", "#ff7700", "#00e9ff", "#ff00d8"]


def xml_text(root):
    ET.indent(root, space=" ")
    return '<?xml version="1.0" encoding="UTF-8"?>\n' + ET.tostring(root, encoding="unicode") + "\n"


def build_tilesets(config, pack):
    print("Tilesets")
    tilesets = [Tileset(entry, pack, config["tileSize"]) for entry in config["tilesets"]]
    names = [t.name for t in tilesets]
    duplicates = {n for n in names if names.count(n) > 1}
    if duplicates:
        raise SystemExit(f"Two tilesets share a name: {sorted(duplicates)} (give one a 'name' in pack.json)")
    written = 0
    for tileset in tilesets:
        written += save_png_if_changed(TILESET_IMAGES_DIR / tileset.image_name(), tileset.image)
        written += save_if_changed(TILESETS_DIR / (tileset.name + ".tsx"), tileset.tsx())
        if tileset.wangsets:
            save_png_if_changed(PREVIEW_DIR / "terrains" / (tileset.name + ".png"), tileset.terrain_preview())
    terrains = sum(len(t.wangsets) for t in tilesets)
    print(f"  {len(tilesets)} tilesets ({terrains} terrains), {written} files updated")
    return {t.name: t for t in tilesets}


# ----------------------------------------------------------------------------------------------------------------------
# texture packing (one page; shelf packing is plenty for pixel-art sheets)
# ----------------------------------------------------------------------------------------------------------------------

def pack_regions(regions, padding=2, extrude=0):
    """
    regions: list of dicts with 'image' (and anything else); sets 'x', 'y'. Returns the page image.
    extrude copies the border pixels into the padding so scaled tiles never show seams.
    """
    order = sorted(regions, key=lambda r: (-r["image"].height, -r["image"].width))
    area = sum((r["image"].width + padding) * (r["image"].height + padding) for r in regions)
    width = 64
    while width * width < area and width < MAX_PAGE:
        width *= 2
    while True:
        x = y = shelf = 0
        fits = True
        for region in order:
            w, h = region["image"].size
            if x + w + 2 * padding > width:  # the region, its padding and its extruded border must fit
                x, y, shelf = 0, y + shelf, 0
            if w + 2 * padding > width:
                fits = False
                break
            region["x"], region["y"] = x + padding, y + padding
            x += w + padding
            shelf = max(shelf, h + padding)
        height = 1
        while height < y + shelf + padding:
            height *= 2
        if fits and height <= width:
            break
        if width >= MAX_PAGE:
            raise SystemExit("Everything doesn't fit into one 4096x4096 page")
        width *= 2

    page = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for region in regions:
        image, px, py = region["image"], region["x"], region["y"]
        if extrude:
            w, h = image.size
            for e in range(1, extrude + 1):
                page.paste(image.crop((0, 0, w, 1)), (px, py - e))
                page.paste(image.crop((0, h - 1, w, h)), (px, py + h - 1 + e))
                page.paste(image.crop((0, 0, 1, h)), (px - e, py))
                page.paste(image.crop((w - 1, 0, w, h)), (px + w - 1 + e, py))
                for cx, cy, sx, sy in ((0, 0, -e, -e), (w - 1, 0, e, -e), (0, h - 1, -e, e), (w - 1, h - 1, e, e)):
                    page.putpixel((px + cx + sx, py + cy + sy), image.getpixel((cx, cy)))
        page.paste(image, (px, py))
    return page


def atlas_text(page_name, page, regions):
    """libGDX TextureAtlas format. Regions sharing a name are told apart by 'index' (atlas.findRegions(name))."""
    lines = [page_name, f"size:{page.width},{page.height}", "filter:Nearest,Nearest", "repeat:none"]
    for region in sorted(regions, key=lambda r: (r["name"], r["index"])):
        w, h = region["image"].size
        lines.append(region["name"])
        lines.append(f"  bounds:{region['x']},{region['y']},{w},{h}")
        lines.append(f"  index:{region['index']}")
    return "\n".join(lines) + "\n"


# ----------------------------------------------------------------------------------------------------------------------
# sprites / animations
# ----------------------------------------------------------------------------------------------------------------------

def sprite_sources(entry):
    """One entry may cover several colour variants that share a layout: {"variants": {"chicken_blue": "...png"}}."""
    if "variants" in entry:
        return list(entry["variants"].items())
    return [(entry["name"], entry["file"])]


def row_names(entry, rows):
    names = entry.get("rows", "auto")
    if names == "auto":
        return {r: f"row_{r:02d}" for r in range(rows)}
    if isinstance(names, list):
        return {r: n for r, n in enumerate(names) if n}
    return {int(r): n for r, n in names.items()}


def cut_frames(image, entry):
    """{row name: [frame images]} for one sheet. Frames that are completely transparent are skipped."""
    fw, fh = entry["frame"]
    ox, oy = entry.get("offset", [0, 0])
    columns = entry.get("columns") or max(1, math.ceil((image.width - ox) / fw))
    rows = max(1, math.ceil((image.height - oy) / fh))
    rows = min(rows, entry.get("rowCount", rows))  # a sheet may switch to another grid further down
    result = {}
    for row, name in row_names(entry, rows).items():
        frames = []
        for column in range(columns):
            x, y = ox + column * fw, oy + row * fh
            if x >= image.width or y >= image.height:
                continue
            frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
            frame.paste(image.crop((x, y, min(x + fw, image.width), min(y + fh, image.height))), (0, 0))
            if not is_empty(frame):
                frames.append(frame)
        if frames:
            result[name] = frames
        elif entry.get("rows", "auto") != "auto":
            warn(f"{entry.get('file') or entry['name']}: row {row} ('{name}') is empty")
    return result


def used_sprites(all_sheets):
    """
    {sheet name: enum constant} from the game's Sprite enum, e.g. {"rabbit": "RABBIT"}. Only these sheets are packed,
    so the committed atlas holds what the game uses and not the whole pack.
    """
    if not SPRITE_ENUM.exists():
        raise SystemExit(f"Missing {SPRITE_ENUM.relative_to(ROOT)}: the enum of the sprite sheets the game uses")
    code = re.sub(r"//[^\n]*|/\*.*?\*/", "", SPRITE_ENUM.read_text(encoding="utf-8"), flags=re.S)
    used = {}
    for constant, sheet in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*\(\s*\"([^\"]*)\"\s*\)", code):
        if sheet not in all_sheets:
            close = difflib.get_close_matches(sheet, all_sheets, n=1)
            hint = f", did you mean '{close[0]}'?" if close else " (see build/assets-preview/index.html)"
            raise SystemExit(f"{SPRITE_ENUM.name}: {constant} names sheet '{sheet}', which isn't in pack.json{hint}")
        if sheet in used:
            raise SystemExit(f"{SPRITE_ENUM.name}: sheet '{sheet}' is listed twice ({used[sheet]} and {constant})")
        used[sheet] = constant
    return used


def split_row(row):
    """'walk_left' -> ('walk', 'left'), 'open' -> ('open', None); unnamed rows ('row_03') -> (None, None)."""
    if re.fullmatch(r"row_\d+", row):
        return None, None
    for direction in DIRECTIONS:
        if row.endswith("_" + direction):
            return row[:-len(direction) - 1], direction
    return row, None


def write_action_enum(animations):
    """core/.../Action.java: one constant per action named in pack.json, so the game never types animation names."""
    actions = set()
    for name in animations:
        action, _ = split_row(name.split("/", 1)[1])
        if action is None:
            continue
        if not re.fullmatch(r"[a-z][a-z0-9_]*", action):
            warn(f"pack.json: row '{name}' can't become an Action (use lower_case letters, digits and _)")
            continue
        actions.add(action)
    constants = ",\n".join(f'    {action.upper()}("{action}")' for action in sorted(actions))
    code = f"""// Generated by tools/assets/build_assets.py from the row names in tools/assets/pack.json: don't edit, rename rows there.
package com.mantkowicz.light.resources;

/**
 * What an animation shows. A sheet's row in pack.json is named "action" or "action_direction" (e.g. "open",
 * "walk_left"); rows still called row_00, row_01... aren't actions yet. Which sheet has which action: see
 * build/assets-preview/index.html.
 */
public enum Action {{
{constants};

    private final String row;

    Action(String row) {{
        this.row = row;
    }}

    /**
     * The row name in pack.json without the direction, e.g. "walk".
     */
    public String getRow() {{
        return row;
    }}
}}
"""
    return save_if_changed(ACTION_ENUM, code)


def build_sprites(config, pack):
    """
    Cuts every sheet in pack.json (all of them get a preview), packs the ones in the Sprite enum into sprites.atlas and
    writes the Action enum. Returns (all animations, {sheet: Sprite constant}) for the preview page.
    """
    print("Sprites")
    regions, animations, sheets = [], {}, {}
    for entry in config["sprites"]:
        duration = entry.get("duration", 100) / 1000
        play_mode = entry.get("playMode", "LOOP")
        for sheet_name, file in sprite_sources(entry):
            rows = cut_frames(load_image(pack, file), entry)
            # trim all frames of the sheet to one common box: smaller atlas, and every animation keeps the same anchor
            box = None
            for frames in rows.values():
                for frame in frames:
                    b = frame.getchannel("A").getbbox()
                    box = b if box is None else (min(box[0], b[0]), min(box[1], b[1]), max(box[2], b[2]), max(box[3], b[3]))
            if box is None:
                warn(f"{file}: no visible frames for sheet '{sheet_name}' (check frame, offset and rowCount in pack.json)")
                continue
            fw, fh = entry["frame"]
            if sheet_name in sheets:
                raise SystemExit(f"Sheet '{sheet_name}' is defined twice in pack.json")
            sheets[sheet_name] = {"frameWidth": fw, "frameHeight": fh,
                                  "trimX": box[0], "trimY": fh - box[3],  # from the bottom-left, like libGDX
                                  "width": box[2] - box[0], "height": box[3] - box[1]}
            for row, frames in rows.items():
                name = f"{sheet_name}/{row}"
                if name in animations:
                    raise SystemExit(f"Animation '{name}' is defined twice in pack.json")
                for index, frame in enumerate(frames):
                    regions.append({"name": name, "index": index, "image": frame.crop(box)})
                animations[name] = {"sheet": sheet_name, "frames": len(frames),
                                    "frameDuration": duration, "playMode": play_mode}

    used = used_sprites(sheets)
    packed_regions = [r for r in regions if animations[r["name"]]["sheet"] in used]
    packed = {name: a for name, a in animations.items() if a["sheet"] in used}
    page = pack_regions(packed_regions)
    changed = save_png_if_changed(GAME_SPRITES_DIR / (SPRITES_ATLAS + ".png"), page)
    changed += save_if_changed(GAME_SPRITES_DIR / (SPRITES_ATLAS + ".atlas"),
                               atlas_text(SPRITES_ATLAS + ".png", page, packed_regions))
    description = {"atlas": SPRITES_ATLAS + ".atlas", "packJsonSha1": source_hash(CONFIG_FILE),
                   "sheets": {name: sheets[name] for name in sorted(used)},
                   "animations": dict(sorted(packed.items()))}
    changed += save_if_changed(GAME_SPRITES_DIR / "animations.json", json.dumps(description, indent=2) + "\n")
    changed += write_action_enum(animations)
    print(f"  {len(sheets)} sheets, {len(animations)} animations in pack.json")
    print(f"  packed for the game (Sprite enum): {', '.join(sorted(used)) or 'nothing'} -> {len(packed)} animations, "
          f"{len(packed_regions)} frames, page {page.width}x{page.height}{'' if changed else ' (unchanged)'}")
    write_animation_previews(regions, animations, sheets)
    return animations, used


def write_preview_index(animations, used):
    """
    build/assets-preview/index.html: every animation of the pack playing (the ones in the game marked), the levels,
    the terrain overlays and the warnings.
    """
    from html import escape
    by_sheet = {}
    for name, animation in animations.items():
        by_sheet.setdefault(animation["sheet"], []).append((name, animation))

    def images(folder):
        return sorted((PREVIEW_DIR / folder).glob("*.png")) if (PREVIEW_DIR / folder).exists() else []

    parts = ["<!doctype html><meta charset='utf-8'><title>Asset preview</title><style>",
             "body{font:14px system-ui,sans-serif;margin:16px;background:#b4b4b4;color:#111}",
             "a{color:#123c7a}code{background:#d6d6d6;padding:0 3px;border-radius:3px}",
             "h2{margin-top:32px}h3{margin:20px 0 6px;font-size:15px}",
             "img{image-rendering:pixelated;display:block}",
             ".grid{display:flex;flex-wrap:wrap;gap:10px}",
             ".card{background:#8fb07f;border-radius:6px;padding:6px;font-size:12px}",
             ".card span{display:block;color:#fff;margin-top:4px}",
             "nav a{margin-right:12px}pre{background:#d6d6d6;padding:8px;white-space:pre-wrap}",
             "</style><h1>Asset preview</h1>",
             "<nav><a href='#animations'>Animations</a><a href='#maps'>Levels</a>",
             "<a href='#terrains'>Terrains</a><a href='#warnings'>Warnings</a></nav>",
             f"<h2 id='animations'>Animations ({len(animations)})</h2>",
             "<p>Every sheet in the pack. Only the ones listed in the game's <code>Sprite</code> enum "
             "(<code>core/.../resources/Sprite.java</code>) are packed for the game; add a line there to use another "
             "one. In the game: <code>resourcesService.getAnimation(Sprite.RABBIT, Action.WALK, Direction.LEFT)</code> "
             "for the row <code>walk_left</code>. Name rows (<code>row_03</code> → <code>eat</code>) in "
             "<code>tools/assets/pack.json</code>.</p>"]
    for sheet in sorted(by_sheet, key=lambda name: (name not in used, name)):
        status = (f" <small>in the game: <code>Sprite.{used[sheet]}</code></small>" if sheet in used
                  else " <small>(not in the game)</small>")
        parts.append(f"<h3>{escape(sheet)}{status}</h3><div class='grid'>")
        for name, animation in sorted(by_sheet[sheet]):
            gif = "animations/" + name.replace("/", "__") + ".gif"
            info = f"{animation['frames']} × {int(animation['frameDuration'] * 1000)} ms, {animation['playMode'].lower()}"
            parts.append(f"<div class='card'><img src='{gif}' alt=''><span>{escape(name)}<br>{info}</span></div>")
        parts.append("</div>")
    parts.append("<h2 id='maps'>Levels</h2>")
    for image in images("maps"):
        parts.append(f"<h3>{escape(image.stem)}</h3><img src='maps/{image.name}' alt=''>")
    parts.append("<h2 id='terrains'>Terrains</h2><p>Green dot: that edge/corner is covered by the terrain, red: it "
                 "isn't. 'fill' marks the plain tiles.</p>")
    for image in images("terrains"):
        parts.append(f"<h3>{escape(image.stem)}</h3><img src='terrains/{image.name}' alt=''>")
    parts.append(f"<h2 id='warnings'>Warnings ({len(warnings)})</h2>"
                 f"<pre>{escape(chr(10).join(warnings)) or 'none'}</pre>")
    (PREVIEW_DIR / "index.html").write_text("\n".join(parts) + "\n", encoding="utf-8")


def write_animation_previews(regions, animations, sheets, scale=4):
    by_name = {}
    for region in regions:
        by_name.setdefault(region["name"], []).append(region)
    folder = PREVIEW_DIR / "animations"
    if folder.exists():
        shutil.rmtree(folder)
    folder.mkdir(parents=True)
    for name, animation in animations.items():
        frames = [r["image"] for r in sorted(by_name[name], key=lambda r: r["index"])]
        w, h = frames[0].size
        images = []
        for frame in frames:
            background = Image.new("RGBA", (w, h), (120, 170, 110, 255))
            background.alpha_composite(frame)
            images.append(background.resize((w * scale, h * scale), Image.NEAREST).convert("P", palette=Image.ADAPTIVE))
        target = folder / (name.replace("/", "__") + ".gif")
        images[0].save(target, save_all=True, append_images=images[1:], loop=0,
                       duration=int(animation["frameDuration"] * 1000), disposal=2)


# ----------------------------------------------------------------------------------------------------------------------
# maps
# ----------------------------------------------------------------------------------------------------------------------

def layer_gids(data):
    """All gids of a tile layer's <data> (csv, base64, base64+zlib/gzip; also infinite maps' chunks)."""
    encoding, compression = data.get("encoding"), data.get("compression")
    parts = data.findall("chunk") or [data]
    gids = []
    for part in parts:
        if encoding == "csv":
            gids += [int(v) for v in part.text.replace("\n", "").split(",") if v.strip()]
        elif encoding == "base64":
            raw = base64.b64decode(part.text.strip())
            if compression == "zlib":
                raw = zlib.decompress(raw)
            elif compression == "gzip":
                raw = gzip.decompress(raw)
            elif compression:
                raise SystemExit(f"Layer compression '{compression}' isn't supported by libGDX: in Tiled use "
                                 f"Map > Map Properties > Tile Layer Format: CSV or Base64 (zlib)")
            gids += [int.from_bytes(raw[i:i + 4], "little") for i in range(0, len(raw), 4)]
        else:
            gids += [int(tile.get("gid", 0)) for tile in part.findall("tile")]
    return gids


def map_tilesets(map_path, root):
    """[(firstgid, tileset element from the .tsx)] in gid order."""
    result = []
    for element in root.findall("tileset"):
        firstgid = int(element.get("firstgid"))
        source = element.get("source")
        if source:
            tsx = (map_path.parent / source).resolve()
            if not tsx.exists():
                raise SystemExit(f"{map_path.name}: tileset {source} not found")
            result.append((firstgid, ET.parse(tsx).getroot()))
        else:
            raise SystemExit(f"{map_path.name}: tileset '{element.get('name')}' is embedded in the map. In Tiled, use the "
                             f"tilesets from assets-src/tilesets (Map → Add External Tileset) and remove this one.")
    return sorted(result, key=lambda t: t[0])


def used_tiles(map_path, root, tilesets):
    """{tileset name: set of local tile ids} used by the map, including all frames of animated tiles."""
    gids = []
    for layer in root.iter("layer"):
        gids += layer_gids(layer.find("data"))
    for obj in root.iter("object"):
        if obj.get("gid"):
            gids.append(int(obj.get("gid")))
    used = {}
    for gid in gids:
        gid &= ~FLIP_FLAGS
        if gid == 0:
            continue
        owner = None
        for firstgid, tileset in tilesets:
            if gid >= firstgid:
                owner = (firstgid, tileset)
        if owner is None:
            raise SystemExit(f"{map_path.name}: tile gid {gid} belongs to no tileset")
        firstgid, tileset = owner
        used.setdefault(tileset.get("name"), set()).add(gid - firstgid)
    for firstgid, tileset in tilesets:
        ids = used.get(tileset.get("name"))
        if not ids:
            continue
        for tile in tileset.findall("tile"):
            if int(tile.get("id")) in ids and tile.find("animation") is not None:
                ids.update(int(f.get("tileid")) for f in tile.find("animation").findall("frame"))
    return used


def game_map(map_path, root, tilesets, atlas_path):
    """
    A copy of the map for the game: tilesets embedded (no .tsx needed), without Wang sets, and with the 'atlas'
    property that libGDX's AtlasTmxMapLoader reads.
    """
    game_root = ET.fromstring(ET.tostring(root))
    properties = game_root.find("properties")
    if properties is None:
        properties = ET.Element("properties")
        game_root.insert(0, properties)
    for prop in properties.findall("property"):
        if prop.get("name") == "atlas":
            properties.remove(prop)
    ET.SubElement(properties, "property", {"name": "atlas", "value": atlas_path})
    for element in game_root.findall("tileset"):
        game_root.remove(element)
    position = list(game_root).index(properties) + 1
    for firstgid, tileset in tilesets:
        embedded = ET.Element("tileset", {k: v for k, v in tileset.attrib.items() if k not in ("version", "tiledversion")})
        embedded.set("firstgid", str(firstgid))
        for child in tileset:
            if child.tag == "image":
                # the game takes the pixels from the atlas; the path is for Tiled, if someone opens this copy
                image = ET.SubElement(embedded, "image", dict(child.attrib))
                sheet = TILESET_IMAGES_DIR / Path(child.get("source")).name
                image.set("source", os.path.relpath(sheet, GAME_MAPS_DIR).replace(os.sep, "/"))
            elif child.tag != "wangsets":
                embedded.append(child)
        game_root.insert(position, embedded)
        position += 1
    note = (f" Generated by tools/assets/build_assets.py from assets-src/maps/{map_path.name}: edit that one in Tiled."
            f" source-sha1: {source_hash(map_path)} ")
    return xml_text(game_root).replace("?>\n", "?>\n<!--" + note + "-->\n", 1)


def use_dropdown(map_path):
    """
    Marks objectClass properties typed as plain text (older maps, or added by hand) as ObjectClass, so Tiled shows the
    dropdown for them. Only adds the propertytype attribute; the rest of the file stays as it is.
    """
    text = map_path.read_text(encoding="utf-8")
    marked = re.sub(r'<property name="%s"(?![^>]*\bpropertytype=)' % OBJECT_CLASS,
                    lambda m: m.group(0) + ' propertytype="ObjectClass"', text)
    if marked != text:
        map_path.write_text(marked, encoding="utf-8")
        print(f"  {map_path.name}: objectClass is a dropdown now")


def render_map(root, tilesets, tile_images, scale=2):
    """The visible tile layers drawn in order (first frame of animated tiles), as a quick look without Tiled."""
    width, height = int(root.get("width")), int(root.get("height"))
    tile_w, tile_h = int(root.get("tilewidth")), int(root.get("tileheight"))
    canvas = Image.new("RGBA", (width * tile_w, height * tile_h), (30, 30, 40, 255))
    for layer in root.iter("layer"):
        if layer.get("visible") == "0":
            continue
        for index, gid in enumerate(layer_gids(layer.find("data"))[:width * height]):
            gid &= ~FLIP_FLAGS
            if not gid:
                continue
            firstgid, tileset = max((t for t in tilesets if t[0] <= gid), key=lambda t: t[0])
            image = tile_images.get((tileset.get("name"), gid - firstgid))
            if image is not None:
                canvas.alpha_composite(image, ((index % width) * tile_w, (index // width) * tile_h))
    return canvas.resize((canvas.width * scale, canvas.height * scale), Image.NEAREST)


def build_maps(tilesets_by_name, pack_available):
    print("Maps")
    maps = sorted(SRC_MAPS_DIR.glob("*.tmx"))
    if not maps:
        print("  no maps in assets-src/maps")
        return
    regions, used_total = [], {}
    parsed = []
    for map_path in maps:
        use_dropdown(map_path)
        root = ET.parse(map_path).getroot()
        if root.get("infinite") == "1":
            raise SystemExit(f"{map_path.name} is an infinite map; the game needs a fixed size. In Tiled: Map → Map "
                             f"Properties → untick Infinite.")
        tilesets = map_tilesets(map_path, root)
        for name, ids in used_tiles(map_path, root, tilesets).items():
            used_total.setdefault(name, set()).update(ids)
        parsed.append((map_path, root, tilesets))
    for name, ids in sorted(used_total.items()):
        image_path = TILESET_IMAGES_DIR / (name + ".png")
        if name in tilesets_by_name:
            tileset = tilesets_by_name[name]
            sheet, columns, size = tileset.image, tileset.columns, tileset.tile
        elif image_path.exists():
            tsx = ET.parse(TILESETS_DIR / (name + ".tsx")).getroot()
            sheet, columns, size = Image.open(image_path).convert("RGBA"), int(tsx.get("columns")), int(tsx.get("tilewidth"))
        else:
            raise SystemExit(f"The image of tileset '{name}' is missing: run the tool with the pack directory first")
        for tile_id in sorted(ids):
            x, y = (tile_id % columns) * size, (tile_id // columns) * size
            regions.append({"name": name, "index": tile_id, "image": sheet.crop((x, y, x + size, y + size))})
    page = pack_regions(regions, padding=2, extrude=1)
    save_png_if_changed(GAME_MAPS_DIR / (TILES_ATLAS + ".png"), page)
    save_if_changed(GAME_MAPS_DIR / (TILES_ATLAS + ".atlas"), atlas_text(TILES_ATLAS + ".png", page, regions))
    tile_images = {(r["name"], r["index"]): r["image"] for r in regions}
    for map_path, root, tilesets in parsed:
        save_if_changed(GAME_MAPS_DIR / map_path.name, game_map(map_path, root, tilesets, TILES_ATLAS + ".atlas"))
        save_png_if_changed(PREVIEW_DIR / "maps" / (map_path.stem + ".png"), render_map(root, tilesets, tile_images))
    stale = {p.name for p in GAME_MAPS_DIR.glob("*.tmx")} - {p.name for p in maps}
    for name in stale:
        (GAME_MAPS_DIR / name).unlink()
    print(f"  {len(maps)} maps, {len(regions)} different tiles used, page {page.width}x{page.height}"
          + (f", removed stale {sorted(stale)}" if stale else ""))


# ----------------------------------------------------------------------------------------------------------------------
# starter map
# ----------------------------------------------------------------------------------------------------------------------

def autotile(tileset, wangset_index, filled, width, height):
    """Picks, for every filled cell, the tile whose Wang id matches its 8 neighbours (what Tiled's terrain brush does)."""
    _, tiles, fills = tileset.wangsets[wangset_index]
    by_wang = {}
    for tile_id, wang in tiles:
        by_wang.setdefault(tuple(wang), tile_id)

    def at(x, y):
        return 0 <= x < width and 0 <= y < height and filled[y][x]

    grid = [[0] * width for _ in range(height)]
    for y in range(height):
        for x in range(width):
            if not filled[y][x]:
                continue
            t, b, l, r = at(x, y - 1), at(x, y + 1), at(x - 1, y), at(x + 1, y)
            wang = (t, t and r and at(x + 1, y - 1), r, b and r and at(x + 1, y + 1),
                    b, b and l and at(x - 1, y + 1), l, t and l and at(x - 1, y - 1))
            wang = tuple(int(v) for v in wang)
            grid[y][x] = by_wang.get(wang, fills[0]) + 1  # +1: local id -> gid with firstgid 1 (fixed below)
    return grid


def object_classes():
    """The constants of the game's TileObjectType enum (what an objectClass can be), e.g. ["SPRITE", "LIGHT", ...]."""
    code = re.sub(r"//[^\n]*|/\*.*?\*/", "", OBJECT_TYPE_ENUM.read_text(encoding="utf-8"), flags=re.S)
    body = re.search(r"\benum\s+\w+\s*\{(.*?)(;|\})", code, flags=re.S)
    if body is None:
        raise SystemExit(f"Can't read the constants of {OBJECT_TYPE_ENUM.relative_to(ROOT)}")
    # drop constructor arguments, e.g. COW(Cow::new) -> COW
    return re.findall(r"\b([A-Z][A-Z0-9_]*)\s*(?:\([^()]*\))?\s*(?:,|$)", body.group(1).strip())


def create_tiled_project():
    """
    assets-src/lightgame.tiled-project: open it in Tiled (File > Open Project) to see all maps and tilesets. It defines
    the enum ObjectClass, so an object's objectClass is picked from a dropdown instead of typed.
    """
    object_class = {"id": 1, "name": "ObjectClass", "storageType": "string", "type": "enum",
                    "values": sorted(object_classes()), "valuesAsFlags": False}
    project = {"automappingRulesFile": "", "commands": [], "compatibilityVersion": 1100, "extensionsPath": "extensions",
               "folders": ["maps", "tilesets"], "properties": [], "propertyTypes": [object_class]}
    save_if_changed(SRC_DIR / "lightgame.tiled-project", json.dumps(project, indent=4) + "\n")


def create_starter_map(tilesets_by_name, config):
    """level1.tmx: a grass island in animated water, with a collision layer and the player's start. Only if no map exists."""
    if list(SRC_MAPS_DIR.glob("*.tmx")):
        return
    starter = config.get("starterMap")
    if not starter:
        return
    print("Starter map")
    width, height = 24, 16
    water = tilesets_by_name[starter["water"]]
    grass = tilesets_by_name[starter["grass"]]
    # island: a rounded rectangle with a bay
    filled = [[0] * width for _ in range(height)]
    for y in range(2, height - 2):
        for x in range(3, width - 3):
            corner = (x in (3, width - 4)) and (y in (2, height - 3))
            bay = 14 <= x <= 17 and y >= height - 6
            filled[y][x] = 0 if corner or bay else 1
    grass_first = 1 + water.count
    grass_cells = autotile(grass, 0, filled, width, height)
    ground = [[(gid - 1 + grass_first) if gid else 0 for gid in row] for row in grass_cells]
    water_gid = 1 + next(iter(water.animations), 0)
    collision_gid = water_gid
    layers = [
        ("water", [[water_gid] * width for _ in range(height)], {}),
        ("ground", ground, {}),
        ("collision", [[0 if filled[y][x] else collision_gid for x in range(width)] for y in range(height)],
         {"visible": "0", "opacity": "0.4"}),
    ]
    root = ET.Element("map", {"version": "1.10", "tiledversion": TILED_VERSION, "orientation": "orthogonal",
                              "renderorder": "right-down", "width": str(width), "height": str(height),
                              "tilewidth": str(config["tileSize"]), "tileheight": str(config["tileSize"]),
                              "infinite": "0", "nextlayerid": str(len(layers) + 2), "nextobjectid": "2"})
    properties = ET.SubElement(root, "properties")
    ET.SubElement(properties, "property", {"name": "ambientLight", "type": "float", "value": "1"})  # daylight
    ET.SubElement(root, "tileset", {"firstgid": "1", "source": f"../tilesets/{water.name}.tsx"})
    ET.SubElement(root, "tileset", {"firstgid": str(grass_first), "source": f"../tilesets/{grass.name}.tsx"})
    for index, (name, cells, extra) in enumerate(layers):
        layer = ET.SubElement(root, "layer", dict({"id": str(index + 1), "name": name,
                                                   "width": str(width), "height": str(height)}, **extra))
        data = ET.SubElement(layer, "data", {"encoding": "csv"})
        data.text = "\n" + ",\n".join(",".join(str(v) for v in row) for row in cells) + "\n"
    objects = ET.SubElement(root, "objectgroup", {"id": str(len(layers) + 1), "name": "objects"})
    tile = config["tileSize"]
    player = ET.SubElement(objects, "object", {"id": "1", "name": "PLAYER", "x": str(6 * tile + 4),
                                               "y": str(10 * tile + 4), "width": "8", "height": "8"})
    ET.SubElement(player, "ellipse")
    props = ET.SubElement(player, "properties")
    ET.SubElement(props, "property", {"name": OBJECT_CLASS, "propertytype": "ObjectClass", "value": "PLAYER"})
    save_if_changed(SRC_MAPS_DIR / "level1.tmx", xml_text(root))
    print("  created assets-src/maps/level1.tmx")


# ----------------------------------------------------------------------------------------------------------------------
# pack check
# ----------------------------------------------------------------------------------------------------------------------

def check_pack_coverage(config, pack):
    """Every image in the pack should be a tileset, a sprite sheet, or ignored on purpose."""
    known = {e["file"] for e in config["tilesets"]}
    for entry in config["sprites"]:
        known.update(file for _, file in sprite_sources(entry))
    ignored = config.get("ignore", [])
    unknown = []
    for path in sorted(pack.rglob("*")):
        if path.suffix.lower() != ".png":
            continue
        relative = path.relative_to(pack).as_posix()
        if relative in known or any(Path(relative).match(pattern) or relative.startswith(pattern) for pattern in ignored):
            continue
        unknown.append(relative)
    for relative in unknown:
        warn(f"not in pack.json (add it as a tileset, a sprite sheet or to 'ignore'): {relative}")


# ----------------------------------------------------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pack", nargs="?", help="the pack's root directory (remembered for next time)")
    args = parser.parse_args()

    config = json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    usage = 'python3 tools/assets/build_assets.py "/path/to/pack"'
    pack = Path(args.pack) if args.pack else None
    if pack is None and PACK_PATH_FILE.exists():
        pack = Path(PACK_PATH_FILE.read_text(encoding="utf-8").strip())
        if not pack.is_dir():
            sys.exit(f"The pack isn't at {pack} any more (remembered from the last run). Give its directory: {usage}")
    if pack is None:
        sys.exit(f"Where is the Sprout Lands pack? Give its directory once (it's remembered): {usage}")
    if not pack.is_dir():
        sys.exit(f"Not a directory: {pack}. Give the pack's root directory: {usage}")
    PACK_PATH_FILE.write_text(str(pack.resolve()) + "\n", encoding="utf-8")
    print(f"Pack: {pack}")
    check_pack_coverage(config, pack)
    tilesets = build_tilesets(config, pack)
    animations, used = build_sprites(config, pack)
    create_tiled_project()
    create_starter_map(tilesets, config)
    build_maps(tilesets, pack_available=True)

    report = PREVIEW_DIR / "report.txt"
    report.write_text("\n".join(warnings) + "\n" if warnings else "no warnings\n", encoding="utf-8")
    write_preview_index(animations, used)
    print(f"Done. {len(warnings)} warnings. Open {(PREVIEW_DIR / 'index.html').relative_to(ROOT)} in a browser.")


if __name__ == "__main__":
    main()
