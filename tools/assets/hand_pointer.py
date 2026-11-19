"""
Draws a hand pointer, the usual "tap here" cursor (the index finger straight up, the other fingers folded, the thumb
out to the left), which the Sprout Lands UI pack doesn't have: 16x16, in the palette and dark outline of the pack's
mouse cursors (UI Sprites/Mouse sprites). It is saved there, in the UI pack folder (git-ignored, like the pack), as
"Hand pointing Mouse icon.png"; ui.json's mouse cursors entry picks it up as the piece mouse_hand_pointing_mouse_icon
(UiImage.HAND_POINTING). Run it once after copying the UI pack, before build_assets.py:

  python3 tools/assets/hand_pointer.py ["/path/to/Sprout Lands - UI Pack - Premium pack"]
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
PACK = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "Sprout Lands - UI Pack - Premium pack"
ICON = "UI Sprites/Mouse sprites/Hand pointing Mouse icon.png"

# the mouse cursors' palette: the outline, the hand's white, its light and darker shade
OUTLINE = (84, 89, 89, 255)
WHITE, LIGHT, SHADE = (243, 244, 231, 255), (212, 217, 203, 255), (167, 173, 165, 255)

# row by row from the top: O the outline, W white, L light shade, S shade (between the folded fingers)
PICTURE = [
    "....OOO.........",
    "...OWWLO........",
    "...OWWLO........",
    "...OWWLO........",
    "...OWWLOOO......",
    "...OWWLOWLOOO...",
    "...OWWLOWLOWLOO.",
    ".OOOWWLOWLOWLOLO",
    "OWWOWWWSWWSWWWLO",
    "OWWLOWWWWWWWWWLO",
    ".OWWLWWWWWWWWWLO",
    "..OWWWWWWWWWWWLO",
    "...OWWWWWWWWWLO.",
    "....OWWWWWWWWLO.",
    ".....OLLLLLLLO..",
    "......OOOOOOO...",
]
COLOURS = {"O": OUTLINE, "W": WHITE, "L": LIGHT, "S": SHADE}
# where the finger's tip is in the picture (x, from the left; the tip is the top row): the point it taps
TIP_X = 5


def hand_pointer():
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = icon.load()
    for y, row in enumerate(PICTURE):
        for x, c in enumerate(row):
            if c in COLOURS:
                px[x, y] = COLOURS[c]
    return icon


def main():
    path = PACK / ICON
    hand_pointer().save(path)
    print(f"{path}: the hand pointer")


if __name__ == "__main__":
    main()
