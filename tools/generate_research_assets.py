#!/usr/bin/env python3
"""Generates the Research Table GUI (an open book with arcane circles) and block textures.

Run from the repository root: python3 tools/generate_research_assets.py
The GUI texture is 512x256 with the 352x236 book at (0, 0); slot positions match ResearchTableMenu.
"""
import math
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'arcane_artifacts', 'textures')
W, H = 352, 236
INK = (91, 26, 142)
SAMPLE = (82, 46)       # ResearchTableMenu.SAMPLE_X/Y
INV = (8, 148)          # ResearchTableMenu.INVENTORY_X/Y


def parchment(img, box, seed):
    rnd = random.Random(seed)
    x0, y0, x1, y1 = box
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            # warm parchment, darker towards the edges and the spine
            dx = min(x - x0, x1 - 1 - x) / ((x1 - x0) / 2)
            dy = min(y - y0, y1 - 1 - y) / ((y1 - y0) / 2)
            edge = min(dx, dy)
            shade = 0.80 + 0.20 * min(1.0, edge * 3.2)
            n = rnd.randint(-6, 6)
            r = int((236 + n) * shade)
            g = int((222 + n) * shade)
            b = int((189 + n) * shade)
            px[x, y] = (r, g, b, 255)


def circle(draw, cx, cy, r, width=1, alpha=110):
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=INK + (alpha,), width=width)


def polygon_star(draw, cx, cy, r, points, step, alpha=90, rot=0.0):
    pts = [(cx + r * math.cos(rot + 2 * math.pi * i / points), cy + r * math.sin(rot + 2 * math.pi * i / points)) for i in range(points)]
    for i in range(points):
        a, b = pts[i], pts[(i + step) % points]
        draw.line([a, b], fill=INK + (alpha,), width=1)


def ornament_circle(layer, cx, cy, r, seed):
    d = ImageDraw.Draw(layer)
    circle(d, cx, cy, r, 2, 120)
    circle(d, cx, cy, r - 5, 1, 90)
    circle(d, cx, cy, r * 0.55, 1, 90)
    polygon_star(d, cx, cy, r - 5, 6, 2, 80)
    polygon_star(d, cx, cy, r * 0.55, 5, 2, 70, rot=math.pi / 2)
    rnd = random.Random(seed)
    for i in range(24):  # tick runes on the rim
        a = 2 * math.pi * i / 24
        x0, y0 = cx + (r - 4) * math.cos(a), cy + (r - 4) * math.sin(a)
        x1, y1 = cx + (r - 1) * math.cos(a), cy + (r - 1) * math.sin(a)
        d.line([(x0, y0), (x1, y1)], fill=INK + (rnd.randint(60, 120),), width=1)


def slot(draw, x, y):
    draw.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(170, 145, 110, 255))
    draw.rectangle([x - 1, y - 1, x + 15, y - 1], fill=(120, 96, 70, 255))
    draw.rectangle([x - 1, y - 1, x - 1, y + 15], fill=(120, 96, 70, 255))
    draw.rectangle([x, y, x + 15, y + 15], fill=(196, 174, 140, 255))


def gui():
    img = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # leather cover
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=8, fill=(74, 36, 46, 255), outline=(40, 18, 24, 255), width=2)
    # pages
    parchment(img, (6, 5, W // 2 - 1, H - 5), 1)
    parchment(img, (W // 2 + 1, 5, W - 6, H - 5), 2)
    d.line([(W // 2, 4), (W // 2, H - 4)], fill=(120, 80, 60, 255), width=2)
    # faint arcane circles drawn in ink, like spell diagrams on the pages
    layer = Image.new('RGBA', img.size, (0, 0, 0, 0))
    ornament_circle(layer, SAMPLE[0] + 8, SAMPLE[1] + 8, 30, 3)
    ornament_circle(layer, W // 2 + 88, 118, 70, 4)
    lay = ImageDraw.Draw(layer)
    circle(lay, W // 2 + 40, 60, 26, 1, 45)
    circle(lay, 40, 120, 14, 1, 45)
    # dim the big right-page circle so text stays readable
    faded = layer.copy()
    alpha = faded.split()[3].point(lambda a: int(a * 0.45))
    faded.putalpha(alpha)
    img.alpha_composite(faded)
    # sample slot in its circle
    slot(d, *SAMPLE)
    # player inventory
    for row in range(3):
        for col in range(9):
            slot(d, INV[0] + col * 18, INV[1] + row * 18)
    for col in range(9):
        slot(d, INV[0] + col * 18, INV[1] + 58)
    # corner flourishes
    for (x, y, sx, sy) in [(10, 8, 1, 1), (W - 11, 8, -1, 1), (10, H - 9, 1, -1), (W - 11, H - 9, -1, -1)]:
        for i in range(6):
            d.point((x + sx * i, y), fill=INK + (160,))
            d.point((x, y + sy * i), fill=INK + (160,))
    return img


def block_textures():
    rnd = random.Random(7)

    def tex(fn):
        img = Image.new('RGBA', (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                px[x, y] = fn(x, y)
        return img

    def wood(x, y):
        base = (92, 54, 70) if (y // 4) % 2 == 0 else (82, 46, 62)
        n = rnd.randint(-8, 8)
        if x in (0, 15) or y % 4 == 3:
            base = (58, 30, 42)
        return tuple(max(0, min(255, c + n)) for c in base) + (255,)

    def top(x, y):
        if x in (0, 15) or y in (0, 15):
            return (58, 30, 42, 255)
        if x in (1, 14) or y in (1, 14):
            return (201, 150, 43, 255)  # rose-gold trim
        n = rnd.randint(-6, 6)
        return (104 + n, 62 + n, 80 + n, 255)

    def book(x, y):
        if x in (0, 15) or y in (0, 15):
            return (74, 36, 46, 255)
        if x in (7, 8):
            return (150, 110, 85, 255)
        n = rnd.randint(-5, 5)
        c = (232 + n, 216 + n, 182 + n, 255)
        # ink circles on both pages
        for cx in (4, 11):
            if abs(math.hypot(x - cx, y - 7.5) - 3) < 0.6:
                return (120, 50, 170, 255)
        if y in (12, 13) and x not in (7, 8) and x % 2 == 0:
            return (90, 60, 50, 255)
        return c

    return {'research_table_wood': tex(wood), 'research_table_top': tex(top), 'research_table_book': tex(book)}


def main():
    out = os.path.join(ROOT, 'gui', 'research_table.png')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    gui().save(out)
    print('wrote', os.path.relpath(out))
    for name, img in block_textures().items():
        p = os.path.join(ROOT, 'block', name + '.png')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img.save(p)
        print('wrote', os.path.relpath(p))


if __name__ == '__main__':
    main()
