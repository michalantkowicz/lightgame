"""
Draws the dream bubble (what a sleeping animal dreams about), which the Sprout Lands UI pack doesn't have: its speech
bubbles have a pointer, a dream is a cloud with little circles trailing down to the sleeper's head. 40x34, in the grey
speech bubble's palette (emojis/speech_bubble_grey.png): its dark outline, its light rim along the top, its fill and
its grey shadow along the bottom. The cloud is at the top right, the circles go down to the bottom left (the head). It
is saved in the UI pack folder (git-ignored, like the pack) as "UI Sprites/Dialouge UI/Dream bubble.png", which
ui.json reads as the piece dream_bubble (UiImage.DREAM_BUBBLE). Run it once after copying the UI pack, before
build_assets.py:

  python3 tools/assets/dream_bubble.py ["/path/to/Sprout Lands - UI Pack - Premium pack"]
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
PACK = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "Sprout Lands - UI Pack - Premium pack"
PICTURE = "UI Sprites/Dialouge UI/Dream bubble.png"

# the grey speech bubble's palette
OUTLINE = (107, 116, 112, 255)
RIM = (243, 244, 231, 255)
FILL = (220, 224, 210, 255)
SHADOW = (157, 168, 154, 255)

WIDTH, HEIGHT = 40, 34
# the cloud: overlapping circles (x, y, radius), and the circles trailing down to the head
CLOUD = [(15, 13, 7.5), (23, 8.5, 8.5), (31, 12, 7.5), (25, 16, 7), (18, 17, 6)]
TRAIL = [(7, 27, 2.6), (2.5, 31.5, 1.6)]


def inside(circles, x, y):
    return any((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r for cx, cy, r in circles)


def shape(circles):
    return {(x, y) for x in range(WIDTH) for y in range(HEIGHT) if inside(circles, x, y)}


def paint(image, area):
    px = image.load()
    for x, y in area:
        up, down = (x, y - 1) in area, (x, y + 1) in area
        left, right = (x - 1, y) in area, (x + 1, y) in area
        if not (up and down and left and right):
            px[x, y] = OUTLINE  # the edge
        elif (x, y - 2) not in area:
            px[x, y] = RIM  # just under the top edge
        elif (x, y + 2) not in area:
            px[x, y] = SHADOW  # just over the bottom edge
        else:
            px[x, y] = FILL


def dream_bubble():
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    paint(image, shape(CLOUD))
    for circle in TRAIL:
        paint(image, shape([circle]))
    return image


def main():
    path = PACK / PICTURE
    dream_bubble().save(path)
    print(f"{path}: the dream bubble")


if __name__ == "__main__":
    main()
