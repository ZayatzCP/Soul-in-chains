#!/usr/bin/env python3
"""Placeholder sprites for the rose gold nugget/block, Anvil Drop and Satiety glyphs, the Oath Scroll,
the four suit charms and the Well Fed effect. Run from the repository root; it only writes files
that do not exist yet (pass --force to overwrite), so designer textures are kept."""
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, glyph, hexc, ROOT, ROSE_GOLD_PAL  # noqa: E402
from PIL import Image  # noqa: E402

FORCE = '--force' in sys.argv


def save(img, *path):
    p = os.path.join(ROOT, *path)
    if os.path.exists(p) and not FORCE:
        print('kept', os.path.relpath(p))
        return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', os.path.relpath(p))


NUGGET = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '......dd........',
    '.....dHLd.......',
    '....dHHHhd......',
    '....dHhhhd.dd...',
    '.....dhhd.dHLd..',
    '......dd..dhhd..',
    '...........dd...',
    '................',
    '................',
    '................',
    '................',
]


def rose_block():
    img = Image.new('RGBA', (16, 16))
    pal = {k: hexc(v) for k, v in ROSE_GOLD_PAL.items()}
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                c = pal['d']
            elif x == 1 or y == 1:
                c = pal['L']
            elif x == 14 or y == 14:
                c = pal['h']
            else:
                c = pal['H'] if (x * 7 + y * 3) % 11 else pal['L']
            img.putpixel((x, y), c)
    return img


ANVIL = [
    '............',
    '............',
    '.##########.',
    '.#gggggggg#.',
    '..########..',
    '....#gg#....',
    '....#gg#....',
    '...#gggg#...',
    '..########..',
    '............',
    '..v..v..v...',
    '.v.v.v.v.v..',
]

SATIETY = [
    '............',
    '.....##.....',
    '....#gg#....',
    '...#gggg#...',
    '..#gggggg#..',
    '..#gtggtg#..',
    '..#gggggg#..',
    '..########..',
    '.#vvvvvvvv#.',
    '.#vvvvvvvv#.',
    '..########..',
    '............',
]

SCROLL = [
    '................',
    '..dddddddddddd..',
    '.dLLLLLLLLLLLLd.',
    '.dLPPPPPPPPPPLd.',
    '..dPiiiiiiiiPd..',
    '..dPPPPPPPPPPd..',
    '..dPiiiiiiPPPd..',
    '..dPPPPPPPPPPd..',
    '..dPiiiiiiiiPd..',
    '..dPPPPPPPPPPd..',
    '..dPiiiPPPrrPd..',
    '..dPPPPPPrRRrd..',
    '.dLLLLLLLLrRRr..',
    '.dLPPPPPPPPrrd..',
    '..dddddddddddd..',
    '................',
]
SCROLL_PAL = {'d': '#6B4A2A', 'L': '#D9BB7E', 'P': '#F3E2B8', 'i': '#5B3E2A', 'r': '#8E1F2A', 'R': '#D0384A'}

SUITS = {
    'heart_charm': ([
        '................',
        '....gggggggg....',
        '...gWWWWWWWWg...',
        '...gW.RR.RR.g...',
        '...gWRRRRRRRg...',
        '...gWRRRRRRRg...',
        '...gW.RRRRR.g...',
        '...gW..RRR..g...',
        '...gW...R...g...',
        '...gW.......g...',
        '...gWW.....Wg...',
        '....gWWWWWWg....',
        '.....gggggg.....',
        '................',
        '................',
        '................',
    ], {'g': '#C9962B', 'W': '#F3E2B8', 'R': '#D0384A'}),
    'diamond_charm': ([
        '................',
        '....gggggggg....',
        '...gWWWWWWWWg...',
        '...gW...R...g...',
        '...gW..RRR..g...',
        '...gW.RRRRR.g...',
        '...gWRRRRRRRg...',
        '...gW.RRRRR.g...',
        '...gW..RRR..g...',
        '...gW...R...g...',
        '...gWW.....Wg...',
        '....gWWWWWWg....',
        '.....gggggg.....',
        '................',
        '................',
        '................',
    ], {'g': '#C9962B', 'W': '#F3E2B8', 'R': '#E07A1F'}),
    'club_charm': ([
        '................',
        '....gggggggg....',
        '...gWWWWWWWWg...',
        '...gW..KKK..g...',
        '...gW..KKK..g...',
        '...gWKKKKKKKg...',
        '...gWKKKKKKKg...',
        '...gWKK.K.KKg...',
        '...gW...K...g...',
        '...gW..KKK..g...',
        '...gWW.....Wg...',
        '....gWWWWWWg....',
        '.....gggggg.....',
        '................',
        '................',
        '................',
    ], {'g': '#C9962B', 'W': '#F3E2B8', 'K': '#1F6B3A'}),
    'spade_charm': ([
        '................',
        '....gggggggg....',
        '...gWWWWWWWWg...',
        '...gW...K...g...',
        '...gW..KKK..g...',
        '...gW.KKKKK.g...',
        '...gWKKKKKKKg...',
        '...gWKKKKKKKg...',
        '...gW.K.K.K.g...',
        '...gW..KKK..g...',
        '...gWW.....Wg...',
        '....gWWWWWWg....',
        '.....gggggg.....',
        '................',
        '................',
        '................',
    ], {'g': '#C9962B', 'W': '#F3E2B8', 'K': '#1E2A44'}),
}

WELL_FED = [
    '..................',
    '.......##.........',
    '......#gg#........',
    '.....#gggg#.......',
    '....#gggggg#......',
    '....#gtggtg#......',
    '....#gggggg#......',
    '....########......',
    '...#vvvvvvvv#.....',
    '...#vvvvvvvv#.....',
    '....########......',
    '..................',
]


def main():
    save(sprite(NUGGET, ROSE_GOLD_PAL), 'item', 'rose_gold_nugget.png')
    save(rose_block(), 'block', 'rose_gold_block.png')
    save(glyph(ANVIL, {'g': '#6E6E78'}), 'item', 'glyph_anvil_drop.png')
    save(glyph(SATIETY, {'g': '#C9962B', 't': '#7A3B1A', 'v': '#B5652A'}), 'item', 'glyph_satiety.png')
    save(sprite(SCROLL, SCROLL_PAL), 'item', 'oath_scroll.png')
    for name, (rows, pal) in SUITS.items():
        save(sprite(rows, pal), 'item', name + '.png')
    save(sprite(['.' * 18] * 3 + WELL_FED + ['.' * 18] * 3, {'#': '#2A1B12', 'g': '#E8A23A', 't': '#7A3B1A', 'v': '#B5652A'}), 'mob_effect', 'well_fed.png')


if __name__ == '__main__':
    main()
