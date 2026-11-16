"""
Makes the rabbit's scythe swing (facing down) from the Sprout Lands premium pack: a copy of the rabbit's sheet
(Characters/Premium Charakter Spritesheet.png) with one more row at the bottom, "scythe_down": the chop_down row with
the axe swapped for a scythe drawn here (a longer handle and a curved blade). The copy is saved next to the original in
the pack folder (git-ignored, like the pack), and pack.json reads the rabbit from it. Also the scythe's item icon
(Objects/Items/scythe icon.png), in the style of the pack's tools and meterials.png. Run it once after copying the pack,
before build_assets.py:

  python3 tools/assets/scythe_animation.py ["/path/to/Sprout Lands - Sprites - premium pack"]
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent
PACK = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "Sprout Lands - Sprites - premium pack"
SOURCE = "Characters/Premium Charakter Spritesheet.png"
TARGET = "Characters/Premium Charakter Spritesheet with scythe.png"
ICON = "Objects/Items/scythe icon.png"
FRAME = 48
CHOP_DOWN_ROW = 16

# the axe in the chop rows: its head (greys) and handle (browns); none of these is on the rabbit itself
AXE = {(129, 129, 129), (183, 183, 183), (170, 121, 89), (144, 98, 93)}
SWING_ARC = (243, 244, 231)  # the white arc of the swing frame, kept
HANDLE, HANDLE_DARK = (170, 121, 89, 255), (144, 98, 93, 255)
BLADE_EDGE, BLADE, BLADE_DARK = (226, 230, 230, 255), (183, 183, 183, 255), (90, 84, 92, 255)
HANDLE_LENGTH = 10
BLADE_LENGTH = 8

# where the blade hooks in each frame, in the picture: the rabbit holds the scythe low on its left (0), raises it up
# and left (1-3, the blade to the left as on a raised scythe), sweeps it down past itself (4, the pack's white arc) and
# ends low in front of its feet (5-7, the blade along the ground)
HOOKS = [(0, 1), (-1, 0), (-1, 0), (-1, 0), (1, 0), (1, 0), (1, 0), (1, 0)]


def scythe_frame(axe_frame, hook):
    """The frame with the axe taken out and a scythe drawn along where the axe was held."""
    frame = axe_frame.copy()
    pixels = frame.load()
    axe, body = [], []
    for y in range(FRAME):
        for x in range(FRAME):
            p = pixels[x, y]
            if p[3] == 0:
                continue
            if p[:3] in AXE:
                axe.append((x, y))
                pixels[x, y] = (0, 0, 0, 0)
            elif p[:3] != SWING_ARC:
                body.append((x, y))
    if not axe or not body:
        return frame
    body_x = sum(p[0] for p in body) / len(body)
    body_y = sum(p[1] for p in body) / len(body)
    hand = min(axe, key=lambda p: (p[0] - body_x) ** 2 + (p[1] - body_y) ** 2)
    tip = max(axe, key=lambda p: (p[0] - hand[0]) ** 2 + (p[1] - hand[1]) ** 2)
    length = math.hypot(tip[0] - hand[0], tip[1] - hand[1]) or 1
    ux, uy = (tip[0] - hand[0]) / length, (tip[1] - hand[1]) / length
    end = (round(hand[0] + ux * HANDLE_LENGTH), round(hand[1] + uy * HANDLE_LENGTH))

    draw = ImageDraw.Draw(frame)
    draw.line([hand, end], fill=HANDLE_DARK, width=2)
    draw.line([hand, (round(hand[0] + ux * (HANDLE_LENGTH - 1)), round(hand[1] + uy * (HANDLE_LENGTH - 1)))],
              fill=HANDLE, width=1)

    # the blade: from the handle's end along the hook, curving back along the handle towards the hands
    hx, hy = hook
    curve = []
    for i in range(BLADE_LENGTH + 1):
        t = i / BLADE_LENGTH
        back = (t ** 2) * 4
        curve.append((round(end[0] + hx * i - ux * back), round(end[1] + hy * i - uy * back)))
    draw.line(curve, fill=BLADE_DARK, width=3)
    draw.line(curve, fill=BLADE, width=2)
    edge = [(x - ux, y - uy) for x, y in curve[1:-1]]  # the cutting edge, on the side towards the hands
    draw.line([(round(x), round(y)) for x, y in edge], fill=BLADE_EDGE, width=1)
    return frame


# the item icons' palette (Objects/Items/tools and meterials.png): a light outline, the handle's and the head's shades
ICON_OUTLINE = (243, 244, 231, 255)
ICON_HANDLE_DARK, ICON_HANDLE, ICON_HANDLE_LIGHT = (117, 76, 96, 255), (144, 98, 93, 255), (182, 137, 98, 255)
ICON_BLADE_DARK, ICON_BLADE, ICON_BLADE_MID, ICON_BLADE_LIGHT = ((107, 116, 112, 255), (129, 139, 131, 255),
                                                               (157, 168, 154, 255), (193, 200, 185, 255))


def scythe_icon():
    """A 16x16 item icon in the pack's style, after the owner's picture: a straight handle standing on the right, a
    dark collar at its top, and from there a long blade sweeping to the left, thick at the collar and narrowing to a
    point that curves down; everything in the icons' 1 px light outline."""
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = icon.load()
    # the handle: two pixels wide, light on its left, from under the collar down to the bottom
    for y in range(4, 15):
        px[11, y] = ICON_HANDLE_LIGHT
        px[12, y] = ICON_HANDLE
    px[12, 14] = ICON_HANDLE_DARK
    # the collar holding the blade
    for x, y in ((11, 2), (12, 2), (11, 3), (12, 3)):
        px[x, y] = ICON_BLADE_DARK
    # the blade, column by column from the collar to its tip: its back (light), its body, its edge (dark)
    blade = {
        10: [(2, ICON_BLADE_LIGHT), (3, ICON_BLADE_MID), (4, ICON_BLADE_DARK)],
        9: [(2, ICON_BLADE_LIGHT), (3, ICON_BLADE_MID), (4, ICON_BLADE_DARK)],
        8: [(2, ICON_BLADE_LIGHT), (3, ICON_BLADE_MID), (4, ICON_BLADE_DARK)],
        7: [(2, ICON_BLADE_LIGHT), (3, ICON_BLADE_DARK)],
        6: [(2, ICON_BLADE_LIGHT), (3, ICON_BLADE_DARK)],
        5: [(2, ICON_BLADE_MID), (3, ICON_BLADE_DARK)],
        4: [(3, ICON_BLADE_MID), (4, ICON_BLADE_DARK)],
        3: [(4, ICON_BLADE_MID), (5, ICON_BLADE_DARK)],
        2: [(5, ICON_BLADE), (6, ICON_BLADE_DARK)],
        1: [(7, ICON_BLADE_DARK)],
    }
    for x, column in blade.items():
        for y, colour in column:
            px[x, y] = colour
    # the outline: every empty pixel next to the drawing
    filled = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
    for x in range(16):
        for y in range(16):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = ICON_OUTLINE
    return icon


def main():
    sheet = Image.open(PACK / SOURCE).convert("RGBA")
    rows = sheet.height // FRAME
    result = Image.new("RGBA", (sheet.width, (rows + 1) * FRAME), (0, 0, 0, 0))
    result.paste(sheet, (0, 0))
    for column, hook in enumerate(HOOKS):
        box = (column * FRAME, CHOP_DOWN_ROW * FRAME, (column + 1) * FRAME, (CHOP_DOWN_ROW + 1) * FRAME)
        result.alpha_composite(scythe_frame(sheet.crop(box), hook), (column * FRAME, rows * FRAME))
    result.save(PACK / TARGET)
    print(f"{PACK / TARGET}: row {rows} 'scythe_down', {len(HOOKS)} frames")
    scythe_icon().save(PACK / ICON)
    print(f"{PACK / ICON}: the scythe's item icon")


if __name__ == "__main__":
    main()
