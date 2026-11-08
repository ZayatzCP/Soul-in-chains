#!/usr/bin/env python3
"""Placeholder sprites for the staff, belt, amulet (both polarities), quiver and the quiver slot icon.
Run from the repository root. Do not run after the designer replaced these textures."""
import os
from generate_textures import sprite, save

STAFF = [
    '.............gg.',
    '............gPPg',
    '...........gPWPg',
    '............gPg.',
    '...........bbg..',
    '..........bBb...',
    '.........bBb....',
    '........bBb.....',
    '.......rBb......',
    '......bBr.......',
    '.....bBb........',
    '....bBb.........',
    '...bBb..........',
    '..bBb...........',
    '.bb.............',
    '................',
]
STAFF_PAL = {'b': '#3A2414', 'B': '#7B4A26', 'g': '#C9962B', 'P': '#B44CF0', 'W': '#F4D8FF', 'r': '#D88A6A'}

BELT = [
    '................',
    '................',
    '................',
    '................',
    'llllllllllllllll',
    'LLLLLLggggLLLLLL',
    'LLLLLLgPPgLLLLLL',
    'llllllggggllllll',
    '.pp...pp...pp...',
    'pPPp.pPPp.pPPp..',
    'pPPp.pPPp.pPPp..',
    'pPPp.pPPp.pPPp..',
    '.pp...pp...pp...',
    '................',
    '................',
    '................',
]
BELT_PAL = {'l': '#3A2414', 'L': '#7B4A26', 'g': '#C9962B', 'P': '#5B3E8A', 'p': '#2B1F3A'}

def amulet(core, glow):
    rows = [
        '....g......g....',
        '.....g....g.....',
        '......g..g......',
        '.......gg.......',
        '......rrrr......',
        '.....rCCCCr.....',
        '....rCGGGGCr....',
        '....rCGWWGCr....',
        '....rCGWWGCr....',
        '....rCGGGGCr....',
        '.....rCCCCr.....',
        '......rrrr......',
        '................',
        '................',
        '................',
        '................',
    ]
    return sprite(rows, {'g': '#C9962B', 'r': '#D88A6A', 'C': core, 'G': glow, 'W': '#FFFFFF'})

QUIVER = [
    '..........w.w...',
    '.........wsws...',
    '........ws.s....',
    '.......lsLs.....',
    '......lLLLl.....',
    '.....lLPLl......',
    '....lLPPLl......',
    '...lLLPLl.......',
    '..lLLLLl........',
    '..lLgLl.........',
    '..lLLl..........',
    '...ll...........',
    '................',
    '................',
    '................',
    '................',
]
QUIVER_PAL = {'l': '#3A2414', 'L': '#7B4A26', 'P': '#B44CF0', 'g': '#C9962B', 's': '#C8C8C8', 'w': '#EDE6D6'}

SLOT = [
    '................',
    '..........x.x...',
    '.........x.x....',
    '........x.x.....',
    '.......xxx......',
    '......x..x......',
    '.....x..x.......',
    '....x..x........',
    '...x..x.........',
    '...x.x..........',
    '...xx...........',
    '................',
    '................',
    '................',
    '................',
    '................',
]

if __name__ == '__main__':
    save(sprite(STAFF, STAFF_PAL), 'item', 'spell_staff.png')
    save(sprite(BELT, BELT_PAL), 'item', 'bottomless_belt.png')
    save(amulet('#2A6BD1', '#7FD6FF'), 'item', 'amulet_of_opposites.png')
    save(amulet('#B0263B', '#FF8A7A'), 'item', 'amulet_of_opposites_repel.png')
    save(sprite(QUIVER, QUIVER_PAL), 'item', 'spell_quiver.png')
