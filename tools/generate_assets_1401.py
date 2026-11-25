#!/usr/bin/env python3
"""1.40.1: the designer's ASSETS 1.40.1 - new icons (amber, the three mushroom doors, bottle of Source, crystal ball,
Focusing shield), the spotted blue mushroom block, the Mudzya hive (one 32x32 sheet: front, top, side, bottom), the
Druid's boots (worn on both legs, like tools/generate_batch19.py) and the 3D Mushroom Shield.

Usage: python3 tools/generate_assets_1401.py <ASSETS folder>
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(__file__))
import generate_batch18 as b18  # noqa: E402

A = b18.A
NS = 'arcane_artifacts'


def w(path, data):
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    src = sys.argv[1]
    for name in ('amber', 'blue_mushroom_door', 'brown_mushroom_door', 'red_mushroom_door', 'bottle_of_source', 'crystal_ball', 'focusing_shield'):
        shutil.copyfile(os.path.join(src, name + '.png'), os.path.join(A, 'textures', 'item', name + '.png'))
    shutil.copyfile(os.path.join(src, 'spotted_blue_mushroom_block.png'), os.path.join(A, 'textures', 'block', 'spotted_blue_mushroom_block.png'))

    # the Mudzya hive: front with the entrance (top left), top (top right), sides (bottom left), bottom (bottom right)
    shutil.copyfile(os.path.join(src, 'mudzya_hive.png'), os.path.join(A, 'textures', 'block', 'mudzya_hive.png'))
    t = NS + ':block/mudzya_hive'
    quarter = {'front': [0, 0, 8, 8], 'top': [8, 0, 16, 8], 'side': [0, 8, 8, 16], 'bottom': [8, 8, 16, 16]}
    faces = {'north': 'front', 'south': 'side', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'bottom'}
    w(os.path.join(A, 'models', 'block', 'mudzya_hive.json'), {
        'parent': 'minecraft:block/block', 'textures': {'all': t, 'particle': t},
        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16],
                      'faces': {d: {'uv': quarter[q], 'texture': '#all', 'cullface': d} for d, q in faces.items()}}]})

    # the Druid's boots: one model per leg; the designer grouped the second toe with the left leg, it sits on the right
    shutil.copyfile(os.path.join(src, 'Druid_boots.png'), os.path.join(A, 'textures', 'item', 'worn', 'druid_boots.png'))
    manifest = b18.convert(os.path.join(src, 'druid_boots.json'), 'Druid_boots', 'druid_boots', 'druid_boots_right', [0, 3], (-1.9, -4, 0), 'RIGHT_LEG')
    manifest += b18.convert(os.path.join(src, 'druid_boots.json'), 'Druid_boots', 'druid_boots', 'druid_boots_left', [1, 2], (1.9, -4, 0), 'LEFT_LEG')

    # the Mushroom Shield in 3D: turned half round (its face to the front) and centred like the old flat shield, so the
    # hand and GUI positions of the old model still fit; tilted spots keep only their main (x) tilt - 1.21.1 models take one axis
    shutil.copyfile(os.path.join(src, 'mushield.png'), os.path.join(A, 'textures', 'item', 'mushroom_shield_3d.png'))
    model = json.load(open(os.path.join(src, 'mushield.json')))
    swap = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east', 'up': 'up', 'down': 'down'}
    elements = []
    for e in model['elements']:
        f, to = e['from'], e['to']
        n = {'from': [round(8 - to[0], 4), round(f[1] - 1, 4), round(9.5 - to[2], 4)],
             'to': [round(8 - f[0], 4), round(to[1] - 1, 4), round(9.5 - f[2], 4)]}
        rot = e.get('rotation')
        if rot:
            o = rot['origin']
            origin = [round(8 - o[0], 4), round(o[1] - 1, 4), round(9.5 - o[2], 4)]
            if 'axis' in rot:
                angle = float(rot.get('angle', 0))
                axis = rot['axis']
            else:
                axis, angle = 'x', -22.5 if rot.get('x', 0) < 0 else 22.5
            if angle != 0:
                n['rotation'] = {'angle': angle if axis == 'y' else -angle, 'axis': axis, 'origin': origin}
        n['faces'] = {swap[k]: {**v, 'texture': '#s'} for k, v in e['faces'].items()}
        elements.append(n)
    for name in ('mushroom_shield', 'mushroom_shield_blocking'):
        p = os.path.join(A, 'models', 'item', name + '.json')
        m = json.load(open(p))
        m['textures'] = {'s': NS + ':item/mushroom_shield_3d', 'particle': NS + ':item/mushroom_shield_3d'}
        m['elements'] = elements
        w(p, m)
    for m in manifest:
        print(json.dumps(m))


if __name__ == '__main__':
    main()
