#!/usr/bin/env python3
"""Placeholder textures of the Archive of Unkept Promises (1.37). Run from the repository root:  python3 tools/generate_archive.py"""
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save, hexc  # noqa: E402
from generate_cult import stone, CHARCOAL, BONE, CINNABAR, BORDEAUX, MILK  # noqa: E402


def lock():
    img = stone(71, ['#1E1A1D', '#231E22', '#2A2428'], ['#3A3438'])
    for x in range(5, 11):
        for y in range(4, 12):
            if x in (5, 10) or y in (4, 11):
                img.putpixel((x, y), hexc(BORDEAUX))
    for p in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9)):
        img.putpixel(p, hexc(CINNABAR if p[1] < 8 else BORDEAUX))
    return img


def plate():
    img = stone(72, ['#E8DCCB', '#F0E7DE', '#DCCFBD'], ['#C8B9A3'])
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), hexc('#B8A88F'))
    for x in range(4, 12):
        img.putpixel((x, 7), hexc(CINNABAR))
        img.putpixel((x, 8), hexc(CINNABAR))
    return img


def holding():
    img = stone(73, ['#2A2428', '#231E22', '#302A2E'], ['#3A3438'])
    ring = [(x, y) for x in range(16) for y in range(16) if 3.5 <= ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 <= 6.2]
    for p in ring:
        img.putpixel(p, hexc(CINNABAR if (p[0] + p[1]) % 3 else BONE))
    for p in ((7, 7), (8, 8), (7, 8), (8, 7)):
        img.putpixel(p, hexc(MILK))
    return img


def candle():
    img = stone(74, ['#8E2E28', '#B9463D', '#A23A33'], ['#6F2927'])
    for y in range(4):
        for x in range(16):
            img.putpixel((x, y), hexc(BONE if y < 2 else '#C8B9A3'))
    return img


SEAL_ITEM = [
    '................',
    '.....bbbbbb.....',
    '...bbrrrrrrbb...',
    '..brrRRRRRRrrb..',
    '..brRRmmmmRRrb..',
    '.brRRmrrrrmRRrb.',
    '.brRmrrRRrrmRrb.',
    '.brRmrRmmRrmRrb.',
    '.brRmrRmmRrmRrb.',
    '.brRmrrRRrrmRrb.',
    '.brRRmrrrrmRRrb.',
    '..brRRmmmmRRrb..',
    '..brrRRRRRRrrb..',
    '...bbrrrrrrbb...',
    '.....bbbbbb.....',
    '................',
]

if __name__ == '__main__':
    save(lock(), 'block', 'archive_lock.png')
    save(plate(), 'block', 'vow_plate.png')
    save(holding(), 'block', 'holding_seal.png')
    save(candle(), 'block', 'vow_candle.png')
    save(sprite(SEAL_ITEM, {'b': BONE, 'r': BORDEAUX, 'R': CINNABAR, 'm': MILK}), 'item', 'archdeacon_seal.png')
