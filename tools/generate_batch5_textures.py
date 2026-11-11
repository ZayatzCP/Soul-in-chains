#!/usr/bin/env python3
"""Placeholder sprites for the Crystal Ball, the Aura and Fear glyphs and the Fear effect.
Only missing files are written unless --force is given."""
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, glyph, ROOT  # noqa: E402

FORCE = '--force' in sys.argv


def save(img, *path):
    p = os.path.join(ROOT, *path)
    if os.path.exists(p) and not FORCE:
        print('kept', os.path.relpath(p))
        return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', os.path.relpath(p))


BALL = [
    '................',
    '.....dddddd.....',
    '...ddCCCCCCdd...',
    '..dCCWWCCCCCCd..',
    '..dCWWCCCvCCCd..',
    '.dCCWCCCvvvCCCd.',
    '.dCCCCCvvVvvCCd.',
    '.dCCCCCCvvvCCCd.',
    '.dCCCCCCCvCCCCd.',
    '..dCCCCCCCCCCd..',
    '..dCCCCCCCCCCd..',
    '...ddCCCCCCdd...',
    '....gddddddg....',
    '...gHHHHHHHHg...',
    '..gHhhhhhhhhhHg.',
    '..gggggggggggg..',
]
BALL_PAL = {'d': '#5B3E8A', 'C': '#B9A4F0', 'W': '#FFFFFF', 'v': '#7A2FC0', 'V': '#F4D8FF',
            'g': '#8E4B3B', 'H': '#E7A08B', 'h': '#C77B66'}

AURA = [
    '............',
    '...t.tt.t...',
    '..t......t..',
    '.t..vvvv..t.',
    '...v####v...',
    '.t.v#gg#v.t.',
    '.t.v#gg#v.t.',
    '...v####v...',
    '.t..vvvv..t.',
    '..t......t..',
    '...t.tt.t...',
    '............',
]
FEAR = [
    '............',
    '...######...',
    '..#gggggg#..',
    '.#gg#gg#gg#.',
    '.#g#t##t#g#.',
    '.#gggggggg#.',
    '.#ggg##ggg#.',
    '.#gg#vv#gg#.',
    '..#g#vv#g#..',
    '..#gggggg#..',
    '...######...',
    '............',
]
FEAR_ICON = ['.' * 18] * 3 + [
    '......######......',
    '.....#gggggg#.....',
    '....#gg#gg#gg#....',
    '....#g#t##t#g#....',
    '....#gggggggg#....',
    '....#ggg##ggg#....',
    '....#gg#vv#gg#....',
    '.....#g#vv#g#.....',
    '.....#gggggg#.....',
    '......######......',
] + ['.' * 18] * 5


def main():
    save(sprite(BALL, BALL_PAL), 'item', 'crystal_ball.png')
    save(glyph(AURA, {'t': '#1E9C8A', 'v': '#7A2FC0', 'g': '#F4D8FF'}), 'item', 'glyph_aura.png')
    save(glyph(FEAR, {'g': '#D9D2C2', 't': '#E0466E', 'v': '#2A1B12'}), 'item', 'glyph_fear.png')
    save(sprite(FEAR_ICON, {'#': '#2A1B12', 'g': '#D9D2C2', 't': '#E0466E', 'v': '#2A1B12'}), 'mob_effect', 'fear.png')


if __name__ == '__main__':
    main()
