#!/usr/bin/env python3
"""Placeholder sprites for batch 4 (foods, Ring of Calm, Hop-n-Skip Slippers, shields, Pity / Cupid /
Harmless glyphs, Pity and BONK! effects). Only missing files are written unless --force is given."""
import colorsys
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, glyph, ROOT  # noqa: E402
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


def hue_shift(path, degrees, sat=1.0):
    img = Image.open(os.path.join(ROOT, *path)).convert('RGBA')
    out = Image.new('RGBA', img.size)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hls_to_rgb((h + degrees / 360) % 1, l, min(1, s * sat))
            out.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
    return out


POTATO = [
    '................',
    '................',
    '......dddd......',
    '....ddHHHHdd....',
    '...dHHLHHHHHd...',
    '..dHLHHHgHHHHd..',
    '..dHHHHHHHHgHd..',
    '..dHHgHHHHHHHd..',
    '..dHHHHHHgHHHd..',
    '...dHHHHHHHHd...',
    '...dhHHgHHHhd...',
    '....dhhHHhhd....',
    '.....dddddd.....',
    '................',
    '................',
    '................',
]
POTATO_PAL = {'d': '#6E3B2B', 'H': '#E7A08B', 'h': '#C77B66', 'L': '#FFE1D6', 'g': '#7FBF3A'}

PUFFER = [
    '................',
    '.....s..s..s....',
    '....s.dddddd.s..',
    '...sdHHHHHHHd...',
    '..sdHHLLHHHHHd.s',
    '...dHHwkHHHHHHd.',
    '..sdHHwwHHHHHHd.',
    '...dHHHHHHHHHHds',
    '..sdhHHHHHHHHhd.',
    '...dhhHHHHHHhd..',
    '....dhhhhhhhd.s.',
    '...s.ddddddd....',
    '......s..s......',
    '................',
    '................',
    '................',
]
PUFFER_PAL = {'d': '#6E3B2B', 'H': '#E7A08B', 'h': '#C77B66', 'L': '#FFE1D6', 's': '#F4D06F', 'w': '#FFFFFF', 'k': '#1A1A1A'}

EYE = [
    '................',
    '................',
    '.....dddddd.....',
    '...ddHHHHHHdd...',
    '..dHHrrrrrrHHd..',
    '..dHrrRRRRrrHd..',
    '.dHrrRkkkkRrrHd.',
    '.dHrRRkkkkRRrHd.',
    '.dHrrRkkkkRrrHd.',
    '..dHrrRRRRrrHd..',
    '..dHHrrrrrrHHd..',
    '...ddHHHHHHdd...',
    '.....dddddd.....',
    '................',
    '................',
    '................',
]
EYE_PAL = {'d': '#6E3B2B', 'H': '#E7A08B', 'r': '#8E2A4A', 'R': '#C74A6A', 'k': '#2A0A1A'}

SLIPPERS = [
    '................',
    '..pw......pw....',
    '..pw......pw....',
    '..ww.ww...ww.ww.',
    '..ww.pw...ww.pw.',
    '..ww.pw...ww.pw.',
    '.wwwwwww.wwwwwww',
    'wwwwwwww.wwwwwww',
    'wkwwkwgw.wkwwkwg',
    'wwwppwww.wwwppww',
    'wwwwwwww.wwwwwww',
    '.gggggg...gggggg',
    '................',
    '................',
    '................',
    '................',
]
SLIPPERS_PAL = {'w': '#F4F1EA', 'p': '#F2A7B8', 'k': '#2A1B12', 'g': '#C9BFB0'}

SLIPPERS_WORN = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

MIRROR = [
    '................',
    '..dddddddddddd..',
    '..dSSSSSSSSSSd..',
    '..dSWWSSSSSSSd..',
    '..dSWSSSSvSSSd..',
    '..dSSSSSvvvSSd..',
    '..dSSSSvvvvvSd..',
    '..dSSSSSvvvSSd..',
    '..dSSSSSSvSSSd..',
    '..dSSSSSSSSSSd..',
    '...dSSSSSSSSd...',
    '....dSSSSSSd....',
    '.....dSSSSd.....',
    '......dddd......',
    '................',
    '................',
]
MIRROR_PAL = {'d': '#8E4B3B', 'S': '#C9D2E0', 'W': '#FFFFFF', 'v': '#B44CF0'}

FOCUS = [
    '................',
    '..dddddddddddd..',
    '..dHHHHHHHHHHd..',
    '..dHppppppppHd..',
    '..dHpppGGpppHd..',
    '..dHppGWWGppHd..',
    '..dHppGWWGppHd..',
    '..dHpppGGpppHd..',
    '..dHppppppppHd..',
    '..dHppppppppHd..',
    '...dHppppppHd...',
    '....dHppppHd....',
    '.....dHHHHd.....',
    '......dddd......',
    '................',
    '................',
]
FOCUS_PAL = {'d': '#6E3B2B', 'H': '#E7A08B', 'p': '#4A2A6E', 'G': '#B44CF0', 'W': '#F4D8FF'}

PITY = [
    '............',
    '....####....',
    '...#gggg#...',
    '..#g#gg#g#..',
    '..#gggggg#..',
    '..#gg##gg#..',
    '..#g#..#g#..',
    '...#gggg#...',
    '....####....',
    '.....tt.....',
    '....t..t....',
    '.....tt.....',
]
CUPID = [
    '............',
    '..vv....vv..',
    '.vvvv..vvvv.',
    '.vvvvvvvvvv.',
    '.vvvvvvvvvv.',
    '..vvvvvvvv..',
    '...vvvvvv...',
    '#...vvvv...#',
    '.#...vv...#.',
    '..#......#..',
    '...######...',
    '............',
]
HARMLESS = [
    '............',
    '.....##.....',
    '....#gg#....',
    '...#gggg#...',
    '..#gg##gg#..',
    '..#g#tt#g#..',
    '..#g#tt#g#..',
    '..#gg##gg#..',
    '...#gggg#...',
    '....#gg#....',
    '.....##.....',
    '............',
]

BONK_ICON = [
    '..................',
    '..................',
    '...y......y.......',
    '..yyy....yyy......',
    '...y......y.......',
    '......dddddddd....',
    '.....dIIIIIIIId...',
    '......dddddddd....',
    '........dIId......',
    '........dIId......',
    '.......dIIIId.....',
    '......dddddddd....',
    '..................',
    '....y.......y.....',
    '...yyy.....yyy....',
    '....y.......y.....',
    '..................',
    '..................',
]
PITY_ICON = [
    '..................',
    '..................',
    '.....########.....',
    '....#gggggggg#....',
    '...#gg#gggg#gg#...',
    '...#gggggggggg#...',
    '...#ggg####ggg#...',
    '...#gg#gggg#gg#...',
    '....#gggggggg#....',
    '.....########.....',
    '......t....t......',
    '.....t......t.....',
    '......t....t......',
    '..................',
    '..................',
    '..................',
    '..................',
    '..................',
]


def main():
    save(sprite(POTATO, POTATO_PAL), 'item', 'enchanted_poisonous_potato.png')
    save(sprite(PUFFER, PUFFER_PAL), 'item', 'gilded_pufferfish.png')
    save(sprite(EYE, EYE_PAL), 'item', 'gilded_fermented_spider_eye.png')
    save(sprite(SLIPPERS, SLIPPERS_PAL), 'item', 'hop_slippers.png')
    worn = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(8):
        for x in range(8):
            worn.putpixel((x, y), (244, 241, 234, 255) if (x + y) % 5 else (226, 220, 210, 255))
    for y in range(6):
        for x in range(8, 10):
            worn.putpixel((x, y), (242, 167, 184, 255) if x == 8 else (244, 241, 234, 255))
    for y in range(2):
        for x in range(10, 12):
            worn.putpixel((x, y), (242, 167, 184, 255))
    save(worn, 'item', 'worn', 'hop_slippers.png')
    save(hue_shift(('item', 'ring_of_reversal.png'), 150), 'item', 'ring_of_calm.png')
    save(hue_shift(('item', 'worn', 'ring_of_reversal_3d.png'), 150), 'item', 'worn', 'ring_of_calm_3d.png')
    save(sprite(MIRROR, MIRROR_PAL), 'item', 'mirror_shield.png')
    save(sprite(FOCUS, FOCUS_PAL), 'item', 'focusing_shield.png')
    save(glyph(PITY, {'g': '#8C9A5B', 't': '#4A7AC0'}), 'item', 'glyph_pity.png')
    save(glyph(CUPID, {'v': '#E0466E'}), 'item', 'glyph_cupid.png')
    save(glyph(HARMLESS, {'g': '#F3F3F3', 't': '#7A2FC0'}), 'item', 'glyph_harmless.png')
    save(sprite(BONK_ICON, {'d': '#3A3A44', 'I': '#8A8A96', 'y': '#F4D06F'}), 'mob_effect', 'bonk.png')
    save(sprite(PITY_ICON, {'#': '#2A1B12', 'g': '#8C9A5B', 't': '#4A7AC0'}), 'mob_effect', 'pity.png')


if __name__ == '__main__':
    main()
