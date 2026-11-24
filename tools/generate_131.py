#!/usr/bin/env python3
"""Textures of 1.31: Soul Flail, Hat of Mysteries, the five druid staves and the Staff of the Five Circles.

Run from the repository root:  python3 tools/generate_131.py
"""
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save  # noqa: E402

FLAIL = [
    '..........bbb...',
    '.........bBBBb..',
    '........bBsBBBb.',
    '........bBBBBBb.',
    '........bBBBBb..',
    '.........bbbb...',
    '........c.......',
    '.......c........',
    '......c.........',
    '.....hh.........',
    '....hh..........',
    '...hh...........',
    '..hh............',
    '.gh.............',
    '.g..............',
    '................',
]
HAT = [
    '.........pp.....',
    '........pPp.....',
    '.......pPPp.....',
    '.......pPp......',
    '......pPPp......',
    '......pPPPp.....',
    '.....pPPsPp.....',
    '.....pPPPPPp....',
    '....pPPPPPPp....',
    '....gggggggg....',
    '..pppppppppppp..',
    '.pPPPPPPPPPPPPp.',
    '..pppppppppppp..',
    '................',
    '................',
    '................',
]
STAFF = [
    '..........kkk...',
    '.........kKKKk..',
    '.........kKgKk..',
    '..........kKk...',
    '.........ww.....',
    '........ww......',
    '.......ww.......',
    '......ww........',
    '.....ww.........',
    '....ww..........',
    '...ww...........',
    '..ww............',
    '.ww.............',
    '.w..............',
    '................',
    '................',
]
WOOD = {'w': '#6B4A2B'}
STAVES = {
    'druid_staff_roots': {'k': '#2F5A1E', 'K': '#5E9A2E', 'g': '#9AE05A'},
    'druid_staff_grove': {'k': '#B8B09A', 'K': '#E8E2CF', 'g': '#7FD05A'},
    'druid_staff_hive': {'k': '#8A5A10', 'K': '#E0A82A', 'g': '#FFE07A'},
    'druid_staff_sap': {'k': '#7A1F2A', 'K': '#E05A6A', 'g': '#FFB0B8'},
    'druid_staff_thorns': {'k': '#2A3A1E', 'K': '#4A6A2E', 'g': '#C83A4A'},
    'druid_staff_grand': {'k': '#2EA8BE', 'K': '#5E9A2E', 'g': '#F2D16B'},
}

if __name__ == '__main__':
    save(sprite(FLAIL, {'b': '#2A2634', 'B': '#5E626E', 's': '#2EC4D6', 'c': '#8C929E', 'h': '#3B2A20', 'g': '#8C929E'}), 'item', 'soul_flail.png')
    save(sprite(HAT, {'p': '#2A1A4A', 'P': '#4A2E7A', 's': '#F2D16B', 'g': '#D4A93A'}), 'item', 'mystery_hat.png')
    for name, pal in STAVES.items():
        save(sprite(STAFF, dict(WOOD, **pal)), 'item', name + '.png')
