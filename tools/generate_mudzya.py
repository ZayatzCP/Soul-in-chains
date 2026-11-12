#!/usr/bin/env python3
"""Mudzya texture (64x64): a translucent cyan jelly dome, a pink core, big dark eyes, violet fins."""
import os
import random
from PIL import Image

OUT = 'src/main/resources/assets/arcane_artifacts/textures/entity/'


def box_regions(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def fill(img, region, colour_fn):
    x0, y0, w, h = region
    for x in range(int(x0), int(x0 + w)):
        for y in range(int(y0), int(y0 + h)):
            img.putpixel((x, y), colour_fn(x - x0, y - y0, w, h))


def make(charged):
    rnd = random.Random(7)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    glow = 1.25 if charged else 1.0

    def c(r, g, b, a):
        return (min(255, int(r * glow)), min(255, int(g * glow)), min(255, int(b * glow)), a)

    def jelly(x, y, w, h):
        if rnd.random() < 0.06:
            return c(220, 255, 255, 200)
        edge = x == 0 or y == 0 or x == w - 1 or y == h - 1
        return c(110, 225, 245, 150 if edge else 95)
    for region in box_regions(0, 0, 8, 7, 8).values():
        fill(img, region, jelly)

    def core(x, y, w, h):
        pink = (235, 150, 225) if (x + y) % 3 else (205, 120, 215)
        if charged and rnd.random() < 0.2:
            pink = (255, 210, 250)
        return c(*pink, 235)
    for region in box_regions(0, 16, 6, 5, 6).values():
        fill(img, region, core)

    def skirt(x, y, w, h):
        return c(90, 210, 240, 170 if (x + y) % 2 else 130)
    for region in box_regions(0, 28, 9, 1, 9).values():
        fill(img, region, skirt)

    eye = box_regions(40, 0, 2, 2, 1)
    for name, region in eye.items():
        fill(img, region, lambda x, y, w, h: c(70, 210, 240, 230))
    x0, y0, _, _ = eye['front']
    img.putpixel((x0, y0), (235, 255, 255, 255))
    img.putpixel((x0 + 1, y0), (25, 20, 60, 255))
    img.putpixel((x0, y0 + 1), (25, 20, 60, 255))
    img.putpixel((x0 + 1, y0 + 1), (40, 30, 90, 255))

    def fin(x, y, w, h):
        t = y / max(1, h - 1)
        base = (190 - int(60 * t), 90 + int(40 * t), 210 + int(30 * t))
        ridge = int(x) % 2 == 0
        return c(*(tuple(int(v * 0.8) for v in base) if ridge else base), 215)
    for region in box_regions(40, 8, 0, 6, 5).values():
        if region[2] > 0 and region[3] > 0:
            fill(img, region, fin)

    def stem(x, y, w, h):
        return c(120, 230, 250, 200)
    for region in box_regions(52, 0, 1, 5, 1).values():
        fill(img, region, stem)

    def tentacle(x, y, w, h):
        return c(80 + 20 * (y % 2), 170, 235, 190)
    for region in box_regions(52, 8, 1, 4, 1).values():
        fill(img, region, tentacle)
    return img


make(False).save(OUT + 'mudzya.png')
make(True).save(OUT + 'mudzya_charged.png')
print('mudzya textures written')
