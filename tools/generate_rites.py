#!/usr/bin/env python3
"""Placeholder textures of the chalk rites (the Cultist, 1.38). Run from the repository root:  python3 tools/generate_rites.py"""
import os
import random
import sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image  # noqa: E402
from generate_textures import sprite, save, hexc  # noqa: E402

COLORS = {'white': ('#F1EEE6', '#CFCBC1'), 'cinnabar': ('#D0473B', '#9C2F27'), 'coal': ('#4A4650', '#2C2930'),
          'amethyst': ('#B48CF0', '#7E5BB8'), 'bone': ('#E6D9C3', '#BBA98C')}

CHALK = [
    '................',
    '................',
    '...........kk...',
    '..........kLLk..',
    '.........kLLLk..',
    '........kLLLk...',
    '.......kLLLk....',
    '......kLLLk.....',
    '.....kLLLk......',
    '....kDLLk.......',
    '...kDDLk........',
    '...kDDk.........',
    '....kk..........',
    '................',
    '................',
    '................',
]
DUST = [
    '................', '................', '................', '......l.........', '....l..L..l.....', '.....LLLL.......',
    '...lLLDDLLl.....', '..LLDDDDDLL.l...', '..LDDDDDDDDL....', '.lLDDDDDDDDLl...', '..LLLLLLLLLL....', '................',
    '................', '................', '................', '................',
]
BOOK = [
    '................', '..kkkkkkkkkk....', '..kPPPPPPPPkb...', '..kPPPPPPPPkb...', '..kPPaaaaPPkb...', '..kPaPPPPaPkb...',
    '..kPaPaaPaPkb...', '..kPaPaaPaPkb...', '..kPaPPPPaPkb...', '..kPPaaaaPPkb...', '..kPPPPPPPPkb...', '..kPPPPPPPPkb...',
    '..kkkkkkkkkkb...', '...bbbbbbbbbb...', '................', '................',
]
CAGE = [
    '................', '..kkkkkkkkkkkk..', '..k.g..g..g..k..', '..k.g..g..g..k..', '..kkkkkkkkkkkk..', '..k.g..g..g..k..',
    '..k.g..g..g..k..', '..k.g..g..g..k..', '..k.g..g..g..k..', '..k.g..g..g..k..', '..kkkkkkkkkkkk..', '..k.g..g..g..k..',
    '..k.g..g..g..k..', '..kkkkkkkkkkkk..', '................', '................',
]


def noise(seed, base, specks, chance=0.1):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = base[rnd.randrange(len(base))]
            if rnd.random() < chance:
                c = specks[rnd.randrange(len(specks))]
            img.putpixel((x, y), hexc(c))
    return img


def chalk_mark(name, light, dark):
    rnd = random.Random(sum(map(ord, name)))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.85:
                r, g, b = hexc(light if rnd.random() < 0.7 else dark)[:3]
                img.putpixel((x, y), (r, g, b, 200 + rnd.randrange(55)))
    return img


def heart_top():
    img = noise(81, ['#3B3A40', '#35343A', '#403E45'], ['#2A292E'])
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), hexc('#26252A'))
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d < 3.2:
                img.putpixel((x, y), hexc('#D8B8FF' if d < 1.6 else '#B48CF0'))
            elif d < 4.4:
                img.putpixel((x, y), hexc('#7E5BB8'))
    return img


def heart_side():
    img = noise(82, ['#3B3A40', '#35343A', '#403E45'], ['#2A292E'])
    for x in range(16):
        img.putpixel((x, 0), hexc('#26252A'))
        img.putpixel((x, 15), hexc('#26252A'))
    for x in (5, 6, 9, 10):
        for y in (6, 7, 8, 9):
            img.putpixel((x, y), hexc('#B48CF0' if (x + y) % 2 else '#7E5BB8'))
    return img


def soil_top():
    return noise(91, ['#3A2A3E', '#33243A', '#412F47'], ['#6D4F86', '#5A3D6E', '#8A6AA8'], 0.18)


def soil_side():
    img = noise(92, ['#5B4636', '#4F3D2F', '#664F3D'], ['#3E2F24'])
    top = soil_top()
    for x in range(16):
        for y in range(4 + (x * 7) % 3):
            img.putpixel((x, y), top.getpixel((x, y)))
    return img


if __name__ == '__main__':
    for name, (light, dark) in COLORS.items():
        save(sprite(CHALK, {'k': '#2A2428', 'L': light, 'D': dark}), 'item', name + '_chalk.png')
        save(chalk_mark(name, light, dark), 'block', 'chalk_mark_' + name + '.png')
    save(sprite(DUST, {'l': '#D8B8FF', 'L': '#B48CF0', 'D': '#7E5BB8'}), 'item', 'amethyst_dust.png')
    save(sprite(BOOK, {'k': '#171416', 'P': '#2E2A33', 'a': '#B48CF0', 'b': '#E6D9C3'}), 'item', 'sigil_book.png')
    save(sprite(CAGE, {'k': '#4A4650', 'g': '#8E8A96'}), 'item', 'rite_cage.png')
    save(heart_top(), 'block', 'rite_heart_top.png')
    save(heart_side(), 'block', 'rite_heart_side.png')
    save(soil_top(), 'block', 'corrupted_soil_top.png')
    save(soil_side(), 'block', 'corrupted_soil_side.png')

# ---------------------------------------------------------------- 1.39: the Treatise and the vestments
CODEX = [
    '................', '.kkkkkkkkkkkkk..', '.kRRRRRRRRRRRkb.', '.kRRaaRRRaaRRkb.', '.kRaRRaRaRRaRkb.', '.kRRaaRRRaaRRkb.',
    '.kRRRRRRRRRRRkb.', '.kRRaaaaaaaRRkb.', '.kRRaRRRRRaRRkb.', '.kRRaaaaaaaRRkb.', '.kRRRRRRRRRRRkb.', '.kkkkkkkkkkkkkb.',
    '..bbbbbbbbbbbbb.', '................', '................', '................',
]
MASK = [
    '................', '.....bbbbbb.....', '....bBBBBBBb....', '...bBBBBBBBBb...', '..bBBkkBBkkBBb..', '..bBBkkBBkkBBb..',
    '..bBBBBBBBBBBb..', '..bBBBBrrBBBBb..', '...bBBBrrBBBb...', '...bBBrrrrBBb...', '....bBBBBBBb....', '.....bBBBBb.....',
    '......bbbb......', '................', '................', '................',
]
ROBE = [
    '................', '.....kk..kk.....', '....kKKrrKKk....', '...kKKKrrKKKk...', '..kKKKKrrKKKKk..', '..kKKKKrrKKKKk..',
    '..kKKKrrrrKKKk..', '...kKKKrrKKKk...', '...kKKKrrKKKk...', '...kKKKrrKKKk...', '..kKKKKrrKKKKk..', '..kKKKKrrKKKKk..',
    '..kkkkkkkkkkkk..', '................', '................', '................',
]
BELT = [
    '................', '................', '................', '................', '................', 'kkkkkkkkkkkkkkkk',
    'rrrrrrbbbbrrrrrr', 'rrrrrrbaabrrrrrr', 'rrrrrrbbbbrrrrrr', 'kkkkkkkkkkkkkkkk', '..........k.....', '.........kak....',
    '..........k.....', '................', '................', '................',
]
GLOVES = [
    '................', '..k.k.k.........', '..kakak.k.......', '..kakakaka......', '..kKKKKKKk......', '..kKKKKKKk......',
    '..kKKKKKKKk.....', '..kKKKKKKKKk....', '...kKKKKKKk.....', '...kKKKKKk......', '...krrrrrk......', '...kkkkkkk......',
    '................', '................', '................', '................',
]
SANDALS = [
    '................', '................', '................', '................', '...kk.......kk..', '..kKKk.....kKKk.',
    '..kKKk.....kKKk.', '..kKKk.....kKKk.', '..kKKk.....kKKk.', '..kKKk.....kKKk.', '.krrrrk...krrrrk', '.kKKKKk...kKKKKk',
    '..kkkk.....kkkk.', '................', '................', '................',
]

if __name__ == '__main__':
    save(sprite(CODEX, {'k': '#171416', 'R': '#6F2927', 'a': '#B48CF0', 'b': '#E6D9C3'}), 'item', 'sigil_codex.png')
    save(sprite(MASK, {'b': '#8D8270', 'B': '#F0E7DE', 'k': '#171416', 'r': '#B9463D'}), 'item', 'cult_mask.png')
    save(sprite(ROBE, {'k': '#0E0C0F', 'K': '#2A2428', 'r': '#B9463D'}), 'item', 'cult_robe.png')
    save(sprite(BELT, {'k': '#3B2A20', 'r': '#B9463D', 'b': '#8D8270', 'a': '#B48CF0'}), 'item', 'offering_belt.png')
    save(sprite(GLOVES, {'k': '#171416', 'K': '#2A2428', 'a': '#4FD6D0', 'r': '#B9463D'}), 'item', 'inscriber_gloves.png')
    save(sprite(SANDALS, {'k': '#171416', 'K': '#3B3A40', 'r': '#B9463D'}), 'item', 'silent_sandals.png')
