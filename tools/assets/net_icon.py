"""
Draws the fishing net's icon, which the Sprout Lands packs don't have: a round hoop with a mesh in it on a short
handle, 16x16, in the palette and light outline of the pack's item icons (Objects/Items/tools and meterials.png, like
the scythe's icon from scythe_animation.py). It is saved in the pack folder (git-ignored, like the pack) as
Objects/Items/fishing net icon.png, which pack.json reads as the sheet 'fishing_net_icon'. Run it once after copying
the pack, before build_assets.py:

  python3 tools/assets/net_icon.py ["/path/to/Sprout Lands - Sprites - premium pack"]
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
PACK = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "Sprout Lands - Sprites - premium pack"
ICON = "Objects/Items/fishing net icon.png"

# the item icons' palette (as in scythe_animation.py): the light outline, the wood's shades, the mesh's grey
OUTLINE = (243, 244, 231, 255)
WOOD_DARK, WOOD, WOOD_LIGHT = (117, 76, 96, 255), (144, 98, 93, 255), (182, 137, 98, 255)
MESH = (193, 200, 185, 255)

# the picture, row by row from the top: a long handle from the bottom left (W light, w dark, d its end), the hoop at
# its top right seen a little from the side (H its near, light side, h its far, dark side), and the net's bag hanging
# from it to the bottom right (m its strings, o a hole: see-through, the button shows through it); the outline goes
# only around the outside
PICTURE = [
    "................",
    "........hhhhh...",
    "......hh.....hh.",
    ".....h.momomo.h.",
    ".....HmomomomoH.",
    "......HHmomoHHm.",
    ".....wWmHHHHomm.",
    "....wW.momomomm.",
    "...wW...momomm..",
    "..wW.....momm...",
    ".wW.......mm....",
    "dW..............",
    "dd..............",
    "................",
    "................",
    "................",
]
COLOURS = {"H": WOOD_LIGHT, "h": WOOD_DARK, "m": MESH, "W": WOOD_LIGHT, "w": WOOD, "d": WOOD_DARK}


def net_icon():
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = icon.load()
    rows = PICTURE[-2:] + PICTURE[:-2]  # two rows lower: in the middle of the icon
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c in COLOURS:
                px[x, y] = COLOURS[c]
    filled = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
    outside, todo = set(), [(0, 0)]  # the empty pixels reachable from the edge: not the mesh's holes
    while todo:
        x, y = todo.pop()
        if 0 <= x < 16 and 0 <= y < 16 and (x, y) not in outside and (x, y) not in filled:
            outside.add((x, y))
            todo += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    for x, y in outside:
        if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = OUTLINE
    return icon


def main():
    path = PACK / ICON
    net_icon().save(path)
    print(f"{path}: the fishing net's icon")


if __name__ == "__main__":
    main()
