"""
Draws the quest row's two icons the Sprout Lands UI pack doesn't have (ui.hud.QuestPanel):

- a red X (the quest's cancel button): the pack's X ("UI Sprites/Icons/All Icons.png", icon 197: white with a beige
  outline) recoloured, red with a dark red outline; saved as "UI Sprites/Icons/Red close.png" (ui.json: close_red);
- an arrow pointing right (who asks -> what for): the pack's arrows have a gap between shaft and head, this one is in
  one piece, white with the pack's beige outline, 15x13; saved as "UI Sprites/Icons/Quest arrow.png" (quest_arrow).

Both are saved in the UI pack folder (git-ignored, like the pack). Run it once after copying the UI pack, before
build_assets.py:

  python3 tools/assets/quest_icons.py ["/path/to/Sprout Lands - UI Pack - Premium pack"]
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
PACK = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "Sprout Lands - UI Pack - Premium pack"
ICONS = "UI Sprites/Icons/All Icons.png"
CLOSE = 197  # the white X with a beige outline, in 16x16 cells, 16 a row

WHITE, BEIGE = (251, 251, 246, 255), (196, 154, 108, 255)
RED, DARK_RED = (222, 78, 64, 255), (120, 36, 40, 255)

ARROW = ["......#......",
         "......##.....",
         "......###....",
         "##########...",
         "###########..",
         "############.",
         "###########..",
         "##########...",
         "......###....",
         "......##.....",
         "......#......"]


def red_close(icons):
    x, y = CLOSE % 16 * 16, CLOSE // 16 * 16
    close = icons.crop((x, y, x + 16, y + 16))
    pixels = close.load()
    for py in range(16):
        for px in range(16):
            colour = pixels[px, py]
            if colour[3]:
                pixels[px, py] = RED if colour[:3] == WHITE[:3] else DARK_RED
    return close.crop(close.getbbox())


def arrow():
    height, width = len(ARROW) + 2, len(ARROW[0]) + 2
    filled = {(x + 1, y + 1) for y, row in enumerate(ARROW) for x, c in enumerate(row) if c == "#"}
    image = Image.new("RGBA", (width, height))
    for py in range(height):
        for px in range(width):
            if (px, py) in filled:
                image.putpixel((px, py), WHITE)
            elif any((px + dx, py + dy) in filled for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                image.putpixel((px, py), BEIGE)
    return image


def main():
    icons = Image.open(PACK / ICONS).convert("RGBA")
    red_close(icons).save(PACK / "UI Sprites/Icons/Red close.png")
    arrow().save(PACK / "UI Sprites/Icons/Quest arrow.png")
    print("saved Red close.png and Quest arrow.png in", PACK / "UI Sprites/Icons")


if __name__ == "__main__":
    main()
