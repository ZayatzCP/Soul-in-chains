#!/usr/bin/env python3
"""Glyph icons of the Cultist (1.36). Run from the repository root:  python3 tools/generate_cult_glyphs.py"""
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import glyph, save  # noqa: E402

RED, BONE, BORDEAUX, ASH, BLUE = '#B9463D', '#F0E7DE', '#6F2927', '#929292', '#3E7BD6'

ICONS = {
    # a sign drawn over an eye
    'glyph_inscribe': ({'r': RED}, [
        '............', '.....rr.....', '....r..r....', '.....rr.....', '......r.....', '...######...',
        '..#..vv..#..', '.#..v##v..#.', '..#..vv..#..', '...######...', '............', '............']),
    # a seal circle with a shield inside
    'glyph_warding_seal': ({'b': BONE, 'r': RED}, [
        '...######...', '..#......#..', '.#..rrrr..#.', '#..rbbbbr..#', '#..rbbbbr..#', '#..rbbbbr..#',
        '#...rbbr...#', '#....rr....#', '.#........#.', '..#......#..', '...######...', '............']),
    # a small ash figure rising from two hands
    'glyph_witness_call': ({'a': ASH, 'r': RED}, [
        '.....aa.....', '....aaaa....', '.....aa.....', '....aaaa....', '...a.aa.a...', '.....aa.....',
        '....a..a....', '.r........r.', '.rr......rr.', '..rr....rr..', '...rrrrrr...', '............']),
    # four anchor stones around a ring
    'glyph_hold_circle': ({'r': RED, 'b': BONE}, [
        '.....bb.....', '.....bb.....', '...######...', '..#......#..', '.#........#.', 'bb#......#bb',
        'bb#......#bb', '.#........#.', '..#......#..', '...######...', '.....bb.....', '.....bb.....']),
    # a pointing claw toward a marked target
    'glyph_retribution': ({'r': RED, 'd': BORDEAUX}, [
        '............', '.........rr.', '........r..r', '........r..r', '.........rr.', '.......d....',
        '......d.....', '..##.d......', '.####.......', '.###........', '..#.........', '............']),
    # a ledger column of tally marks
    'glyph_census': ({'r': RED}, [
        '.##########.', '.#........#.', '.#.r.r.r..#.', '.#.r.r.r..#.', '.#.r.r.r..#.', '.#........#.',
        '.#.r.r....#.', '.#.r.r....#.', '.#.r.r....#.', '.#........#.', '.##########.', '............']),
    # an overflowing bowl
    'glyph_rite_abundance': ({'g': '#C08A1E', 'r': RED}, [
        '....g..g....', '...g.gg.g...', '....gggg....', '..gggrrggg..', '.##########.', '.#rrrrrrrr#.',
        '..#rrrrrr#..', '...#rrrr#...', '....####....', '.....##.....', '....####....', '............']),
    # two mouths singing one note
    'glyph_second_voice': ({'b': BLUE, 'r': RED}, [
        '............', '.##......##.', '#..#....#..#', '#rr#.bb.#rr#', '#..#.bb.#..#', '.##..bb..##.',
        '.....bb.....', '....bbbb....', '...bb..bb...', '....bbbb....', '............', '............']),
}

for name, (extra, rows) in ICONS.items():
    save(glyph(rows, extra), 'item', name + '.png')
