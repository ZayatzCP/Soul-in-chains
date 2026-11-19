#!/usr/bin/env python3
"""Textures of the Shackled class, 1.27: set pieces, Ritual Dagger, Soul Vessel, Phantom Step, graveyard soils.

Run from the repository root:  python3 tools/generate_necro_127.py
"""
import os
import random
import sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image  # noqa: E402
from generate_textures import sprite, save, glyph, hexc  # noqa: E402

GLOVES = [
    '................',
    '................',
    '..c..........c..',
    '...c........c...',
    '....cc....cc....',
    '...LLLL..LLLL...',
    '..LLLLLLLLLLLL..',
    '..LlLLL..LLLlL..',
    '..LLLLL..LLLLL..',
    '..LLLLL..LLLLL..',
    '..iiiii..iiiii..',
    '..ImImI..ImImI..',
    '..iiiii..iiiii..',
    '................',
    '................',
    '................',
]
SHROUD = [
    '................',
    '.....dddddd.....',
    '....dDDDDDDd....',
    '...dDdddddDDd...',
    '...dDd....dDd...',
    '...dDd....dDd...',
    '..dDDd....dDDd..',
    '..dDDDddddDDDd..',
    '..dDDDDDDDDDDd..',
    '.dDDDDsDDsDDDDd.',
    '.dDDDDDDDDDDDDd.',
    '.dDDDDDDDDDDDDd.',
    'dDDDDDDDDDDDDDDd',
    'dDDDdDDDDDDdDDDd',
    'dd..d.dddd.d..dd',
    '................',
]
CROWN = [
    '................',
    '................',
    '................',
    '..b...b..b...b..',
    '..bb.bbbbbb.bb..',
    '..bbbbbbbbbbbb..',
    '..BbbbbGGbbbbB..',
    '..BbbbGggGbbbB..',
    '..BbbbbGGbbbbB..',
    '..BBBBBBBBBBBB..',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]
DAGGER = [
    '................',
    '.............ss.',
    '............sSs.',
    '...........sSs..',
    '..........sSs...',
    '.........sSs....',
    '........sSs.....',
    '.......sSs......',
    '....g.sSs.......',
    '.....gSs........',
    '.....bg.........',
    '....b..g........',
    '...b............',
    '..B.............',
    '................',
    '................',
]
VESSEL = [
    '................',
    '......kkkk......',
    '......kkkk......',
    '.......gg.......',
    '.......gg.......',
    '......g..g......',
    '.....g....g.....',
    '....g..ss..g....',
    '....g.sSSs.g....',
    '....g.sSSs.g....',
    '....g..ss..g....',
    '....g......g....',
    '.....gggggg.....',
    '................',
    '................',
    '................',
]
PHANTOM_GLYPH = [
    '............',
    '....tttt....',
    '...t....t...',
    '..t.#..#.t..',
    '..t......t..',
    '..t..##..t..',
    '..t......t..',
    '..t.t..t.t..',
    '..tt.tt.tt..',
    '............',
    '............',
    '............',
]
PHANTOM_ICON = [
    '..................',
    '......cccccc......',
    '.....c......c.....',
    '....c........c....',
    '....c.dd..dd.c....',
    '....c........c....',
    '....c...dd...c....',
    '....c........c....',
    '....c........c....',
    '....c.c..c.c.c....',
    '....cc.cc.cc.cc...',
    '..................',
    '..................',
    '..................',
    '..................',
    '..................',
    '..................',
    '..................',
]


def soil(base, specks, seed, glow=None):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = base[rnd.randrange(len(base))]
            if rnd.random() < 0.12:
                c = specks[rnd.randrange(len(specks))]
            img.putpixel((x, y), hexc(c))
    if glow:
        for _ in range(5):
            img.putpixel((rnd.randrange(16), rnd.randrange(16)), hexc(glow))
    return img


def soil_side(top, seed):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    dirt = ['#4A3626', '#3E2D20', '#56402C']
    for y in range(16):
        for x in range(16):
            if y < 4 + (1 if rnd.random() < 0.3 else 0):
                img.putpixel((x, y), top.getpixel((x, y)))
            else:
                img.putpixel((x, y), hexc(dirt[rnd.randrange(len(dirt))]))
    return img


if __name__ == '__main__':
    save(sprite(GLOVES, {'c': '#6E7280', 'L': '#3B2A20', 'l': '#5A4232', 'i': '#8C929E', 'I': '#C8CED8', 'm': '#5E626E'}), 'item', 'shackle_gloves.png')
    save(sprite(SHROUD, {'d': '#14121A', 'D': '#2A2634', 's': '#2EC4D6'}), 'item', 'shroud.png')
    save(sprite(CROWN, {'b': '#E8E2CF', 'B': '#B8B09A', 'G': '#2EA8BE', 'g': '#8BF3FF'}), 'item', 'bone_crown.png')
    save(sprite(DAGGER, {'s': '#9AA0AA', 'S': '#E2E6EE', 'g': '#5E626E', 'b': '#E8E2CF', 'B': '#B8B09A'}), 'item', 'ritual_dagger.png')
    save(sprite(VESSEL, {'k': '#6B4A2B', 'g': '#C8E8F0', 's': '#2EC4D6', 'S': '#8BF3FF'}), 'item', 'soul_vessel.png')
    save(glyph(PHANTOM_GLYPH, {'t': '#2EC4D6'}), 'item', 'glyph_phantom_step.png')
    save(sprite(PHANTOM_ICON, {'c': '#9FE8FF', 'd': '#2A6B78'}), 'mob_effect', 'phantom.png')
    holy = soil(['#8A7A5C', '#958566', '#7E6F52'], ['#D8CFA8', '#BFB48C'], 7, '#FFF2B0')
    save(holy, 'block', 'consecrated_soil_top.png')
    save(soil_side(holy, 8), 'block', 'consecrated_soil_side.png')
    unholy = soil(['#2A2130', '#241C2A', '#30263A'], ['#4A3A56', '#E8E2CF'], 9, '#5ADB5A')
    save(unholy, 'block', 'desecrated_soil_top.png')
    save(soil_side(unholy, 10), 'block', 'desecrated_soil_side.png')
