#!/usr/bin/env python3
"""Textures of the Shackled class, 1.29: Shackle Key, Executioner's Axe, Eternal Binding tablet, Chainlord glyphs, Cursed Chains icon.

Run from the repository root:  python3 tools/generate_necro_129.py
"""
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save, glyph  # noqa: E402
from generate_necro import TABLET  # noqa: E402

KEY = [
    '................',
    '................',
    '....ooo.........',
    '...o...o........',
    '...o.g.o........',
    '...o...o........',
    '....oOo.........',
    '.....O..........',
    '.....O..........',
    '.....OO.........',
    '.....O..........',
    '.....OOO........',
    '.....O..........',
    '................',
    '................',
    '................',
]
AXE = [
    '................',
    '.....bbbb.......',
    '....bBBBBb......',
    '...bBBBBBBb.....',
    '...bBBhBBBBb....',
    '...bBBBhBBBBb...',
    '....bBBBhhBBb...',
    '.....bbBBBhb....',
    '.......bb.h.....',
    '.........h......',
    '........h.......',
    '.......h........',
    '......h.........',
    '.....c..........',
    '....h...........',
    '................',
]
CURSE_GLYPH = [
    '............',
    '..oo....oo..',
    '.o..o..o..o.',
    '.o..o..o..o.',
    '..oo.pp.oo..',
    '....p..p....',
    '....p..p....',
    '..oo.pp.oo..',
    '.o..o..o..o.',
    '.o..o..o..o.',
    '..oo....oo..',
    '............',
]
EXEC_GLYPH = [
    '............',
    '....####....',
    '...#tttt#...',
    '..#tt##tt#..',
    '..#t#..#t#..',
    '...#....#...',
    '.....##.....',
    '.....##.....',
    '..o..##..o..',
    '...o.##.o...',
    '....o..o....',
    '............',
]
CURSED_ICON = [
    '..................',
    '..oo..........oo..',
    '.o..o........o..o.',
    '.o..o........o..o.',
    '..oo.oo....oo.oo..',
    '....o..o..o..o....',
    '....o..pppp..o....',
    '.....oopPPpoo.....',
    '.......pPPp.......',
    '.......pPPp.......',
    '.....oopPPpoo.....',
    '....o..pppp..o....',
    '....o..o..o..o....',
    '..oo.oo....oo.oo..',
    '.o..o........o..o.',
    '.o..o........o..o.',
    '..oo..........oo..',
    '..................',
]

if __name__ == '__main__':
    save(sprite(KEY, {'o': '#8C929E', 'O': '#C8CED8', 'g': '#2EC4D6'}), 'item', 'shackle_key.png')
    save(sprite(AXE, {'b': '#3A3F4A', 'B': '#6E7280', 'h': '#3B2A20', 'c': '#2EC4D6'}), 'item', 'executioner_axe.png')
    save(sprite(TABLET, {'a': '#2A2634', 'b': '#4A4458', 'c': '#8C929E', 'd': '#7A3FB0', 'e': '#C98BFF'}), 'item', 'ritual_eternal_binding.png')
    save(glyph(CURSE_GLYPH, {'o': '#9AA3B5', 'p': '#B05ADB'}), 'item', 'glyph_curse_of_shackles.png')
    save(glyph(EXEC_GLYPH, {'t': '#8BF3FF', 'o': '#9AA3B5'}), 'item', 'glyph_chain_execution.png')
    save(sprite(CURSED_ICON, {'o': '#9AA3B5', 'p': '#5A2A80', 'P': '#B05ADB'}), 'mob_effect', 'cursed_chains.png')
