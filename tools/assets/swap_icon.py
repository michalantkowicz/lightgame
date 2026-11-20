"""
Draws a swap icon, two arrows (the upper one to the right, the lower one to the left), which the Sprout Lands UI pack
doesn't have (its icons have a single arrow): 16x16, white with the light brown outline of the pack's icons
(UI Sprites/Icons/All Icons.png, e.g. the question mark). It goes in the middle of the HUD's tool button when it offers
to swap a backpack item with the one the rabbit stands on. It is saved in the UI pack folder (git-ignored, like the
pack) as "UI Sprites/Icons/Swap icon.png", which ui.json reads as the piece icons_swap (UiImage.SWAP_ICON). Run it
once after copying the UI pack, before build_assets.py:

  python3 tools/assets/swap_icon.py ["/path/to/Sprout Lands - UI Pack - Premium pack"]
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
PACK = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "Sprout Lands - UI Pack - Premium pack"
ICON = "UI Sprites/Icons/Swap icon.png"

# the icons' palette (All Icons.png, the white ones with an outline): the outline and the white
OUTLINE = (196, 154, 108, 255)
WHITE = (251, 251, 246, 255)

# the white of the two arrows, row by row from the top; the outline goes all around it (diagonals too)
ARROWS = [
    "................",
    "..........W.....",
    "..........WW....",
    "..WWWWWWWWWWW...",
    "..WWWWWWWWWWW...",
    "..........WW....",
    "..........W.....",
    "................",
    ".....W..........",
    "....WW..........",
    "...WWWWWWWWWWW..",
    "...WWWWWWWWWWW..",
    "....WW..........",
    ".....W..........",
    "................",
    "................",
]


def swap_icon():
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = icon.load()
    white = {(x, y) for y, row in enumerate(ARROWS) for x, c in enumerate(row) if c == "W"}
    for x, y in white:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in white:
                    px[nx, ny] = OUTLINE
    for x, y in white:
        px[x, y] = WHITE
    return icon


def main():
    path = PACK / ICON
    swap_icon().save(path)
    print(f"{path}: the swap icon")


if __name__ == "__main__":
    main()
