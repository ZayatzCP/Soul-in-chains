#!/usr/bin/env python3
"""Placeholder textures of the Cultist class (palette of the class design document; final art comes from the designer).

Run from the repository root:  python3 tools/generate_cult.py
"""
import os
import random
import sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image  # noqa: E402
from generate_textures import sprite, save, hexc  # noqa: E402

CHARCOAL, BONE, ROSE, CINNABAR, BORDEAUX, ASH, MILK = '#171416', '#F0E7DE', '#D6B2A5', '#B9463D', '#6F2927', '#929292', '#FAF7F3'

KNIFE = [
    '................',
    '.............mm.',
    '............mMm.',
    '...........mMm..',
    '..........mMm...',
    '.........mMm....',
    '........mMm.....',
    '.......mMm......',
    '....r.mMm.......',
    '.....rMm........',
    '.....br.........',
    '....bbr.........',
    '...bb...........',
    '..bb............',
    '..b.............',
    '................',
]
SHIELD = [
    '..cccccccccccc..',
    '.cCCCCCCCCCCCCc.',
    '.cCCCCrrrrCCCCc.',
    '.cCCCrbbbbrCCCc.',
    '.cCCrbbbbbbrCCc.',
    '.cCCrbbRRbbrCCc.',
    '.cCCrbbRRbbrCCc.',
    '.cCCrbbbbbbrCCc.',
    '.cCCCrbbbbrCCCc.',
    '.cCCCCrbbrCCCCc.',
    '..cCCCCrrCCCCc..',
    '..cCCCCCCCCCCc..',
    '...cCCCCCCCCc...',
    '....cCCCCCCc....',
    '.....cccccc.....',
    '................',
]
ASH_PILE = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '.......a........',
    '......aAa.......',
    '.....aAAAa..a...',
    '....aAAgAAa.....',
    '...aAAAAAAAa....',
    '..aAAgAAAAgAa...',
    '..aaaaaaaaaaa...',
    '................',
    '................',
    '................',
    '................',
]
SHARD = [
    '................',
    '................',
    '..........bb....',
    '.........bBb....',
    '........bBBb....',
    '.......bBBb.....',
    '......bBBb......',
    '.....bBBb.......',
    '....bBBb........',
    '....bBb.........',
    '....bb..........',
    '................',
    '................',
    '................',
    '................',
    '................',
]
DUST = [
    '................',
    '......cc........',
    '.....cRRc.......',
    '.....cRRc.......',
    '......cc........',
    '.......bb.......',
    '......bBBb......',
    '.....bBrrBb.....',
    '.....bBrrBb.....',
    '.....bBBBBb.....',
    '......bbbb......',
    '................',
    '................',
    '................',
    '................',
    '................',
]

HAMMER = [
    '................',
    '....bbbbbbbb....',
    '...bBBBBBBBBb...',
    '...bBrrBBrrBb...',
    '...bBBBBBBBBb...',
    '....bbbBBbbb....',
    '.......hh.......',
    '.......hh.......',
    '.......hh.......',
    '.......rr.......',
    '.......hh.......',
    '.......hh.......',
    '.......hh.......',
    '.......cc.......',
    '................',
    '................',
]
BOOK = [
    '................',
    '..cccccccccc....',
    '..cRRRRRRRRcb...',
    '..cRbbbbbbRcb...',
    '..cRbRRRRbRcb...',
    '..cRbRwwRbRcb...',
    '..cRbRwwRbRcb...',
    '..cRbRRRRbRcb...',
    '..cRbbbbbbRcb...',
    '..cRRRRRRRRcb...',
    '..cRRRRRRRRcb...',
    '..ccccccccccb...',
    '...bbbbbbbbbb...',
    '................',
    '................',
    '................',
]
WARD = [
    '..................',
    '.......rrrr.......',
    '.....rr....rr.....',
    '....r..bbbb..r....',
    '...r..b....b..r...',
    '...r.b..rr..b.r...',
    '..r..b.rRRr.b..r..',
    '..r..b.rRRr.b..r..',
    '..r..b..rr..b..r..',
    '...r..b....b..r...',
    '...r...bbbb...r...',
    '....r........r....',
    '.....rr....rr.....',
    '.......rrrr.......',
    '..................',
    '..................',
    '..................',
    '..................',
]


def stone(seed, base, specks, size=16):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            c = base[rnd.randrange(len(base))]
            if rnd.random() < 0.08:
                c = specks[rnd.randrange(len(specks))]
            img.putpixel((x, y), hexc(c))
    return img


def seal_top():
    img = stone(41, ['#2A2428', '#231E22', '#302A2E'], ['#3A3438'])
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), hexc(CHARCOAL))
    ring = [(x, y) for x in range(16) for y in range(16) if 4.5 <= ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5 <= 5.6]
    for p in ring:
        img.putpixel(p, hexc(CINNABAR))
    for p in ((7, 4), (8, 4), (7, 11), (8, 11), (4, 7), (4, 8), (11, 7), (11, 8), (7, 7), (8, 8), (7, 8), (8, 7)):
        img.putpixel(p, hexc(BORDEAUX if p[0] in (7, 8) and p[1] in (7, 8) else BONE))
    return img


def seal_side():
    img = stone(42, ['#2A2428', '#231E22', '#302A2E'], ['#3A3438'])
    for x in range(16):
        img.putpixel((x, 0), hexc(CHARCOAL))
        img.putpixel((x, 7), hexc(CINNABAR) if x % 4 != 3 else hexc(BORDEAUX))
    return img


def cap(lit):
    img = stone(43 + lit, ['#2A2428', '#231E22'], ['#3A3438'])
    for x in range(16):
        for y in range(16):
            if 5 <= x <= 10 and 5 <= y <= 10:
                img.putpixel((x, y), hexc(CINNABAR if lit else BORDEAUX))
    return img


if __name__ == '__main__':
    save(seal_top(), 'block', 'ritual_seal_top.png')
    save(seal_side(), 'block', 'ritual_seal_side.png')
    save(cap(0), 'block', 'ritual_anchor_cap.png')
    save(cap(1), 'block', 'ritual_anchor_cap_lit.png')
    save(sprite(KNIFE, {'m': '#8C929E', 'M': MILK, 'r': CINNABAR, 'b': BONE}), 'item', 'ritual_knife.png')
    save(sprite(SHIELD, {'c': CHARCOAL, 'C': '#2A2428', 'r': CINNABAR, 'b': BONE, 'R': BORDEAUX}), 'item', 'oath_shield.png')
    save(sprite(ASH_PILE, {'a': '#6E6E6E', 'A': ASH, 'g': '#C8C8C8'}), 'item', 'ritual_ash.png')
    save(sprite(SHARD, {'b': '#B8B09A', 'B': BONE}), 'item', 'bone_shard.png')
    save(sprite(HAMMER, {'b': CHARCOAL, 'B': ASH, 'r': CINNABAR, 'h': '#3B2A20', 'c': BONE}), 'item', 'seal_hammer.png')
    save(sprite(BOOK, {'c': CHARCOAL, 'R': BORDEAUX, 'b': BONE, 'w': CINNABAR}), 'item', 'formula_book.png')
    save(sprite(WARD, {'r': CINNABAR, 'b': BONE, 'R': MILK}), 'mob_effect', 'warded.png')
    censer = stone(51, ['#3A3438', '#2A2428', '#4A4448'], ['#929292'])
    for x in range(16):
        censer.putpixel((x, 3), hexc(CINNABAR))
        censer.putpixel((x, 12), hexc(BORDEAUX))
    save(censer, 'block', 'ritual_censer.png')
    save(sprite(DUST, {'c': BORDEAUX, 'R': CINNABAR, 'b': ROSE, 'B': BONE, 'r': CINNABAR}), 'item', 'vow_dust.png')
