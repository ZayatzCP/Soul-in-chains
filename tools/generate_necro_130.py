#!/usr/bin/env python3
"""Textures of the Shackled class, 1.30: Throne of Chains blocks, Scythe of the Reaper, King's Crown, relics, notes, tablet.

Run from the repository root:  python3 tools/generate_necro_130.py
"""
import os
import random
import sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image  # noqa: E402
from generate_textures import sprite, save, hexc  # noqa: E402
from generate_necro import TABLET  # noqa: E402

SCYTHE = [
    '................',
    '..bbbbbb........',
    '.bBBBBBBbb......',
    '.....bbBBBb.....',
    '........bBBb....',
    '.........bBb....',
    '..........hb....',
    '.........h......',
    '........h.......',
    '.......c........',
    '......h.........',
    '.....h..........',
    '....h...........',
    '...h............',
    '..g.............',
    '................',
]
CROWN = [
    '................',
    '................',
    '................',
    '..g..g.gg.g..g..',
    '..gg.gggggg.gg..',
    '..gggggggggggg..',
    '..GggppggppggG..',
    '..GggpPggPpggG..',
    '..GggppggppggG..',
    '..GGGGGGGGGGGG..',
    '...cc.c..c.cc...',
    '................',
    '................',
    '................',
    '................',
    '................',
]
NECKLACE = [
    '................',
    '..ss........ss..',
    '...s........s...',
    '...s........s...',
    '....s......s....',
    '....s......s....',
    '.....s....s.....',
    '.....t.ss.t.....',
    '......tsst......',
    '......t..t......',
    '.......tt.......',
    '.......t........',
    '................',
    '................',
    '................',
    '................',
]
RING = [
    '................',
    '................',
    '................',
    '......rrrr......',
    '.....rRRRRr.....',
    '......rrrr......',
    '.....i....i.....',
    '....i......i....',
    '....i......i....',
    '....i......i....',
    '.....i....i.....',
    '......iiii......',
    '................',
    '................',
    '................',
    '................',
]
EYE = [
    '................',
    '................',
    '................',
    '....oooooooo....',
    '...o........o...',
    '..o..wwwwww..o..',
    '.o..wwppppww..o.',
    '.o..wpPkkPpw..o.',
    '.o..wpPkkPpw..o.',
    '.o..wwppppww..o.',
    '..o..wwwwww..o..',
    '...o........o...',
    '....oooooooo....',
    '................',
    '................',
    '................',
]
HEART = [
    '................',
    '.....i....i.....',
    '....iii..iii....',
    '...i.r.ii.r.i...',
    '..i.rRr..rRr.i..',
    '..irRRRrrRRRri..',
    '..i.rRRRRRRr.i..',
    '..i.rRRRRRRr.i..',
    '...i.rRRRRr.i...',
    '...i..rRRr..i...',
    '....i..rr..i....',
    '.....i....i.....',
    '......iiii......',
    '................',
    '................',
    '................',
]
NOTES = [
    '................',
    '..pppppppppp....',
    '..pPPPPPPPPpb...',
    '..pPllllllPpb...',
    '..pPPPPPPPPpb...',
    '..pPllllPPPpb...',
    '..pPPPPPPPPpb...',
    '..pPlcclllPpb...',
    '..pPPccPPPPpb...',
    '..pPllllllPpb...',
    '..pPPPPPPPPpb...',
    '..pPllllPPPpb...',
    '..pPPPPPPPPpb...',
    '..ppppppppppb...',
    '...bbbbbbbbbb...',
    '................',
]


def stone(seed, base, specks, chain=None):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = base[rnd.randrange(len(base))]
            if rnd.random() < 0.1:
                c = specks[rnd.randrange(len(specks))]
            if (y % 8 == 0) or (x % 8 == (0 if (y // 8) % 2 == 0 else 4)):
                c = '#14121A'
            img.putpixel((x, y), hexc(c))
    if chain:
        for y in range(16):
            for x in (7, 8):
                if (y % 4) != 3:
                    img.putpixel((x, y), hexc(chain[0] if x == 7 else chain[1]))
    return img


def column(seed, base, link, glow=None):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = base[rnd.randrange(len(base))]
            if x in (0, 15):
                c = '#14121A'
            img.putpixel((x, y), hexc(c))
    for y in range(16):
        for x in range(5, 11):
            ring = y % 6
            if (ring in (0, 4) and 6 <= x <= 9) or (ring in (1, 2, 3) and x in (5, 6, 9, 10)):
                img.putpixel((x, y), hexc(link))
    if glow:
        for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
            img.putpixel((x, y), hexc(glow))
    return img


if __name__ == '__main__':
    save(stone(31, ['#2A2634', '#24202C', '#302B3A'], ['#3E3A48', '#4A4458'], ('#8C929E', '#5E626E')), 'block', 'shackle_stone.png')
    save(column(32, ['#2E2A36', '#26222E', '#34303E'], '#8C929E'), 'block', 'chain_pillar_side.png')
    save(stone(33, ['#2E2A36', '#26222E'], ['#3E3A48']), 'block', 'chain_pillar_top.png')
    save(column(34, ['#3A2450', '#2E1C40', '#44305A'], '#C8CED8', '#B05ADB'), 'block', 'chain_lock.png')
    save(sprite(SCYTHE, {'b': '#3A3F4A', 'B': '#B8C0CC', 'h': '#2A2634', 'c': '#2EC4D6', 'g': '#D4A93A'}), 'item', 'scythe_of_the_reaper.png')
    save(sprite(CROWN, {'g': '#F2D16B', 'G': '#B8902A', 'p': '#7A3FB0', 'P': '#C98BFF', 'c': '#8C929E'}), 'item', 'kings_crown.png')
    save(sprite(NECKLACE, {'s': '#6B4A2B', 't': '#E8E2CF'}), 'item', 'tooth_necklace.png')
    save(sprite(RING, {'r': '#7A1F2A', 'R': '#C83A4A', 'i': '#3A3F4A'}), 'item', 'executioner_ring.png')
    save(sprite(EYE, {'o': '#6E7280', 'w': '#E8E2CF', 'p': '#2EA8BE', 'P': '#8BF3FF', 'k': '#14121A'}), 'item', 'jailer_eye.png')
    save(sprite(HEART, {'i': '#8C929E', 'r': '#7A1F2A', 'R': '#C83A4A'}), 'item', 'caged_heart.png')
    save(sprite(NOTES, {'p': '#8A7A5C', 'P': '#D8CFA8', 'l': '#6B5A40', 'c': '#2EA8BE', 'b': '#4A3A26'}), 'item', 'jailer_notes.png')
    save(sprite(TABLET, {'a': '#1E2A1E', 'b': '#3A4A3A', 'c': '#8C929E', 'd': '#3A9A3A', 'e': '#9AF09A'}), 'item', 'ritual_conversion.png')
