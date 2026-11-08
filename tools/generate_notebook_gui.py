#!/usr/bin/env python3
"""Generates textures/gui/notebook.png, the GUI of the Researcher's Secret Notebook (512x256).

Layout (must match ArcaneNotebookScreen):
  0,0     open book 288x180
  0,184   slot frame 18x18
  20,184  next button 18x10   (hovered variant at v=196)
  40,184  previous button     (hovered at v=196)
  60,184  back button         (hovered at v=196)
  80,184  recipe arrow 22x15
  104,184 down arrow 9x12
Run from the repository root:  python3 tools/generate_notebook_gui.py
Do not run it after the designer replaced the texture: it would overwrite it.
"""
import os
import random
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'arcane_artifacts',
                   'textures', 'gui', 'notebook.png')

LEATHER = (92, 54, 30, 255)
LEATHER_DARK = (58, 33, 18, 255)
LEATHER_LIGHT = (122, 76, 42, 255)
GOLD = (201, 150, 43, 255)
PAGE = (232, 214, 170, 255)
PAGE_SHADE = (214, 192, 145, 255)
PAGE_EDGE = (190, 165, 118, 255)
VIOLET = (91, 42, 134, 255)
VIOLET_LIGHT = (180, 76, 240, 255)
INK = (59, 42, 26, 255)


def main():
    random.seed(7)
    img = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Leather cover.
    d.rounded_rectangle((0, 0, 287, 179), radius=6, fill=LEATHER, outline=LEATHER_DARK, width=2)
    for _ in range(500):
        x, y = random.randint(3, 284), random.randint(3, 176)
        img.putpixel((x, y), LEATHER_LIGHT if random.random() < 0.5 else LEATHER_DARK)
    # Gold corners.
    for cx, cy in ((4, 4), (283, 4), (4, 175), (283, 175)):
        d.rectangle((cx - 2, cy - 2, cx + 2, cy + 2), fill=GOLD)
    # Pages.
    for x0, x1 in ((10, 143), (144, 277)):
        d.rectangle((x0, 8, x1, 171), fill=PAGE)
        for _ in range(260):
            img.putpixel((random.randint(x0 + 1, x1 - 1), random.randint(9, 170)), PAGE_SHADE)
    d.rectangle((10, 8, 277, 171), outline=PAGE_EDGE)
    # Spine shadow.
    for i in range(6):
        shade = (150 + i * 12, 128 + i * 12, 90 + i * 12, 255)
        d.line((143 - i, 9, 143 - i, 170), fill=shade)
        d.line((144 + i, 9, 144 + i, 170), fill=shade)
    # Bookmark ribbon.
    d.rectangle((250, 172, 255, 179), fill=(47, 214, 181, 255))
    d.polygon([(250, 179), (252, 177), (255, 179)], fill=LEATHER)

    # Slot frame.
    d.rectangle((0, 184, 17, 201), fill=(201, 182, 138, 255))
    d.line((0, 184, 17, 184), fill=PAGE_EDGE)
    d.line((0, 184, 0, 201), fill=PAGE_EDGE)
    d.line((0, 201, 17, 201), fill=(245, 233, 205, 255))
    d.line((17, 184, 17, 201), fill=(245, 233, 205, 255))

    # Buttons: next (right), previous (left), back (curved arrow), normal + hovered.
    for v, color in ((184, VIOLET), (196, VIOLET_LIGHT)):
        d.polygon([(22, v + 2), (32, v + 2), (32, v), (37, v + 5), (32, v + 9), (32, v + 7), (22, v + 7)], fill=color)
        d.polygon([(55, v + 2), (45, v + 2), (45, v), (40, v + 5), (45, v + 9), (45, v + 7), (55, v + 7)], fill=color)
        d.rectangle((66, v + 1, 76, v + 3), fill=color)
        d.rectangle((74, v + 1, 76, v + 8), fill=color)
        d.polygon([(66, v + 5), (70, v + 5), (68, v + 9)], fill=color)
        d.rectangle((67, v + 4, 69, v + 6), fill=color)

    # Recipe arrow 22x15 and down arrow 9x12.
    d.rectangle((80, 189, 94, 194), fill=INK)
    d.polygon([(94, 184), (101, 191), (94, 198)], fill=INK)
    d.rectangle((106, 184, 110, 191), fill=INK)
    d.polygon([(104, 191), (112, 191), (108, 195)], fill=INK)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print('wrote', os.path.relpath(OUT))


if __name__ == '__main__':
    main()
