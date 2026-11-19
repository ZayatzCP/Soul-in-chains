#!/usr/bin/env python3
"""Phylactery Amulet (5 looks) and Shackled Boots: icons, worn models and their textures.

Run from the repository root:  python3 tools/generate_necro_phylactery.py
Every amulet icon keeps its cord colour at pixel (7, 1): the worn model takes the cord colour from there.
"""
import json
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'arcane_artifacts')
CORD = {'k': '#2A2430'}

STYLES = {
    # 0 crow skull
    0: ([
        '................',
        '.......k........',
        '......k.k.......',
        '.....k...k......',
        '....k.....k.....',
        '....k.bbbb.k....',
        '....bbbbbbbb....',
        '...bbHbbbbHbb...',
        '...bbHbbbbHbbBB.',
        '...bbbbbbbbbbBBB',
        '....bbbbbbbbB...',
        '.....bb.bb......',
        '................',
        '................',
        '................',
        '................',
    ], {'b': '#E8E2CF', 'B': '#9A8F78', 'H': '#1A1620'}),
    # 1 caged heart
    1: ([
        '................',
        '.......k........',
        '......k.k.......',
        '.....k...k......',
        '....k.ccc.k.....',
        '....kc.c.ck.....',
        '....cRRcRRc.....',
        '....cRRRRRc.....',
        '....cRRRRRc.....',
        '.....cRRRc......',
        '......cRc.......',
        '.......c........',
        '................',
        '................',
        '................',
        '................',
    ], {'c': '#8C929E', 'R': '#A8202C'}),
    # 2 hourglass
    2: ([
        '................',
        '.......k........',
        '......k.k.......',
        '.....k...k......',
        '....k.ggggg.....',
        '......g...g.....',
        '......gsssg.....',
        '.......gsg......',
        '........g.......',
        '.......g.g......',
        '......g.s.g.....',
        '......gsssg.....',
        '......ggggg.....',
        '................',
        '................',
        '................',
    ], {'g': '#C8A040', 's': '#2EC4D6'}),
    # 3 black stone
    3: ([
        '................',
        '.......k........',
        '......k.k.......',
        '.....k...k......',
        '....k.sss.k.....',
        '.....sOOOs......',
        '....sOOoOOs.....',
        '....sOoOOOs.....',
        '....sOOOOOs.....',
        '.....sOOOs......',
        '......sss.......',
        '................',
        '................',
        '................',
        '................',
        '................',
    ], {'s': '#8C929E', 'O': '#14101A', 'o': '#5A3A8A'}),
    # 4 rag doll
    4: ([
        '................',
        '.......k........',
        '......k.k.......',
        '.....k...k......',
        '....k.www.k.....',
        '.....wXwXw......',
        '.....wwwww......',
        '.....wrrrw......',
        '....wwwwwww.....',
        '.....wwwww......',
        '.....ww.ww......',
        '.....ww.ww......',
        '................',
        '................',
        '................',
        '................',
    ], {'w': '#C8B090', 'X': '#14101A', 'r': '#A8202C'}),
}
BOOTS = [
    '................',
    '................',
    '..cccc....cccc..',
    '..cDDc....cDDc..',
    '..DDDD....DDDD..',
    '..DDDD....DDDD..',
    '..DDDD....DDDD..',
    '..DDDDD...DDDDD.',
    '..DDDDDD..DDDDDD',
    '..dddddd..dddddd',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]
WORN_BOOTS = ['DDDDDDDDDDDDDDDD'] * 10 + ['dddddddddddddddd'] * 2 + ['cccccccccccccccc'] * 4


def write(path, data):
    p = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')
    print('wrote', os.path.relpath(p))


def faces(tex, uv_front, uv_other):
    return {d: {"texture": tex, "uv": uv_front if d == 'north' else uv_other} for d in ['north', 'east', 'south', 'west', 'up', 'down']}


def amulet_model(style):
    tex = '#p'
    cord = [7, 1, 8, 2]
    elements = [
        {"from": [5.4, 4.6, 5.6], "to": [5.9, 8.0, 6.0], "faces": faces(tex, cord, cord)},
        {"from": [10.1, 4.6, 5.6], "to": [10.6, 8.0, 6.0], "faces": faces(tex, cord, cord)},
        {"from": [5.4, 4.6, 5.6], "to": [10.6, 5.0, 6.0], "faces": faces(tex, cord, cord)},
        {"from": [6.4, 1.2, 5.2], "to": [9.6, 4.8, 5.8], "faces": faces(tex, [3, 4, 13, 14], cord)},
    ]
    return {"textures": {"p": f"arcane_artifacts:item/phylactery_amulet_{style}", "particle": f"arcane_artifacts:item/phylactery_amulet_{style}"},
            "elements": elements}


def boots_model():
    tex = '#b'
    return {"textures": {"b": "arcane_artifacts:item/worn/shackled_boots", "particle": "arcane_artifacts:item/worn/shackled_boots"},
            "elements": [
                {"from": [5.5, -4.3, 5.5], "to": [10.5, 0.6, 10.5], "faces": faces(tex, [0, 0, 5, 10], [0, 0, 5, 10])},
                {"from": [5.25, 0.6, 5.25], "to": [10.75, 1.6, 10.75], "faces": faces(tex, [0, 12, 16, 16], [0, 12, 16, 16])},
            ]}


if __name__ == '__main__':
    for style, (rows, pal) in STYLES.items():
        palette = dict(CORD)
        palette.update(pal)
        save(sprite(rows, palette), 'item', f'phylactery_amulet_{style}.png')
        write(f'models/item/worn/phylactery_amulet_{style}.json', amulet_model(style))
        if style > 0:
            write(f'models/item/phylactery_amulet_{style}.json',
                  {"parent": "minecraft:item/generated", "textures": {"layer0": f"arcane_artifacts:item/phylactery_amulet_{style}"}})
    write('models/item/phylactery_amulet.json', {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "arcane_artifacts:item/phylactery_amulet_0"},
        "overrides": [{"predicate": {"arcane_artifacts:style": s / 10.0 - 0.05}, "model": f"arcane_artifacts:item/phylactery_amulet_{s}"}
                      for s in range(1, len(STYLES))]})
    save(sprite(BOOTS, {'c': '#6E7280', 'D': '#2A2634', 'd': '#14121A'}), 'item', 'shackled_boots.png')
    write('models/item/shackled_boots.json', {"parent": "minecraft:item/generated", "textures": {"layer0": "arcane_artifacts:item/shackled_boots"}})
    save(sprite(WORN_BOOTS, {'c': '#6E7280', 'D': '#2A2634', 'd': '#14121A'}), 'item', 'worn/shackled_boots.png')
    write('models/item/worn/shackled_boots_right.json', boots_model())
    write('models/item/worn/shackled_boots_left.json', boots_model())
