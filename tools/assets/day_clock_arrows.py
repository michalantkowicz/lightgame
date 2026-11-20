"""
Draws the day clock's two steepest arrows, nearly straight up and nearly straight down (80 degrees from the
horizontal), which the Sprout Lands UI pack doesn't have: the arrows next to its weather frame
(emojis/emoji style ui/weather/Weather_UI.png) go from about 50 degrees up to 50 degrees down. Its straight-right arrow
is turned to point up (or down) and then sheared: each row of pixels moves a little to the right, more towards the
tip (3 pixels over its 16 rows), which tilts it about 10 degrees and keeps its pixels crisp (a rotation by 80 degrees
would blur them). They are saved in the UI pack folder (git-ignored, like the pack) as "Day clock arrow up.png" and
"Day clock arrow down.png" next to the weather frame, which ui.json reads as the pieces day_clock_arrow_up and
day_clock_arrow_down (UiImage.DAY_CLOCK_ARROW_UP, DAY_CLOCK_ARROW_DOWN). Run it once after copying the UI pack, before
build_assets.py:

  python3 tools/assets/day_clock_arrows.py ["/path/to/Sprout Lands - UI Pack - Premium pack"]
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
PACK = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "Sprout Lands - UI Pack - Premium pack"
FOLDER = "emojis/emoji style ui/weather"
SHEET = FOLDER + "/Weather_UI.png"
# the straight-right arrow on the sheet (its column of arrows, the middle one)
RIGHT = (336, 34, 352, 46)
# how far its tip moves sideways: 3 pixels over 16 rows, about 10 degrees
SHEAR = 3


def sheared(arrow, tip_at_top):
    """Each row moves right, the more the nearer it is to the tip."""
    rows = arrow.height
    out = Image.new("RGBA", (arrow.width + SHEAR, rows), (0, 0, 0, 0))
    for y in range(rows):
        from_tip = y if tip_at_top else rows - 1 - y
        shift = round((rows - 1 - from_tip) * SHEAR / (rows - 1))
        out.paste(arrow.crop((0, y, arrow.width, y + 1)), (shift, y))
    return out


def main():
    right = Image.open(PACK / SHEET).convert("RGBA").crop(RIGHT)
    up = sheared(right.rotate(90, expand=True), tip_at_top=True)
    down = sheared(right.rotate(270, expand=True), tip_at_top=False)
    for name, image in (("Day clock arrow up.png", up), ("Day clock arrow down.png", down)):
        path = PACK / FOLDER / name
        image.save(path)
        print(f"{path}: an arrow of the day clock")


if __name__ == "__main__":
    main()
