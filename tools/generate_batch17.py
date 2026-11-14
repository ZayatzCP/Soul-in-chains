#!/usr/bin/env python3
"""1.16.0: the designer's creatures (baked from gribnid.bbmodel), new glyph icons, and the revolver and
sawn-off 3D models from revolver.bbmodel. Usage: generate_batch17.py <ASSETS folder> <baked folder>"""
import base64
import io
import json
import os
import shutil
import sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from generate_batch8 import w, NS, D, A  # noqa: E402

SRC, BAKED = sys.argv[1], sys.argv[2]
TEX = A + 'textures/'

# ---- creatures
for name in ['mycelinoid', 'amanitoid', 'elder_mycelinoid', 'gribulka', 'gribulka_hive']:
    shutil.copyfile(os.path.join(BAKED, name + '.json'), A + 'entity_models/' + name + '.json')
for src, dst in [('micilinoid_a', 'mycelinoid_a'), ('micilinoid_b', 'mycelinoid_b'), ('micilinoid_Muhamor_a', 'amanitoid_a'),
                 ('micilinoid_Muhamor_b', 'amanitoid_b'), ('Elder_micilinoid', 'elder_mycelinoid'), ('Gribulka', 'gribulka'),
                 ('Hive_of_Gribulka', 'gribulka_hive')]:
    shutil.copyfile(os.path.join(SRC, src + '.png'), TEX + 'entity/' + dst + '.png')
for old in [A + 'entity_models/gribushka.json', TEX + 'entity/gribushka.png', TEX + 'entity/amanitoid.png']:
    if os.path.exists(old):
        os.remove(old)
for egg in ['elder_mycelinoid_spawn_egg', 'gribulka_spawn_egg', 'gribulka_hive_spawn_egg']:
    w(A + 'models/item/' + egg + '.json', {'parent': 'minecraft:item/template_spawn_egg'})


def drops(name, entries):
    w(D + 'loot_table/entities/' + name + '.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': item, 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}},
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}
        for item, lo, hi in entries]})


drops('elder_mycelinoid', [('minecraft:brown_mushroom', 1, 3), (NS + ':bottle_of_source', 0, 1), (NS + ':sourcecap', 0, 2)])
drops('gribulka', [('minecraft:brown_mushroom', 0, 1), (NS + ':spore_tuft', 0, 1)])
drops('gribulka_hive', [('minecraft:mushroom_stem', 1, 3), ('minecraft:brown_mushroom_block', 1, 3), (NS + ':bottle_of_source', 0, 1)])
biome = D + 'worldgen/biome/mushroom_caves.json'
d = json.load(open(biome))
creatures = [s for s in d['spawners'].get('creature', []) if not s['type'].startswith(NS + ':gribulka')]
creatures += [{'type': NS + ':gribulka', 'weight': 12, 'minCount': 2, 'maxCount': 4},
              {'type': NS + ':gribulka_hive', 'weight': 3, 'minCount': 1, 'maxCount': 1}]
d['spawners']['creature'] = creatures
w(biome, d)

# ---- glyph icons
shutil.copyfile(os.path.join(SRC, 'AURA.png'), TEX + 'item/glyph_aura.png')
shutil.copyfile(os.path.join(SRC, 'FEAR.png'), TEX + 'item/glyph_fear.png')

# ---- guns
CYCLE = {'x': ['up', 'south', 'down', 'north'], 'z': ['east', 'up', 'west', 'down'], 'y': ['north', 'east', 'south', 'west']}
AXES = {'x': (1, 2), 'y': (2, 0), 'z': (0, 1)}


def quarter(element, axis, origin, quarters):
    """Bakes a rotation by quarters * 90 degrees (right-handed) about axis into the element's box and faces."""
    quarters %= 4
    if not quarters:
        return
    a, b = AXES[axis]

    def turn(p):
        p = list(p)
        for _ in range(quarters):
            u, v = p[a] - origin[a], p[b] - origin[b]
            p[a], p[b] = origin[a] - v, origin[b] + u
        return p
    f, t = turn(element['from']), turn(element['to'])
    element['from'] = [min(f[i], t[i]) for i in range(3)]
    element['to'] = [max(f[i], t[i]) for i in range(3)]
    cycle = CYCLE[axis]
    moved = {}
    for name, face in element['faces'].items():
        face = dict(face)
        if name in cycle:
            moved[cycle[(cycle.index(name) + quarters) % 4]] = face
        else:
            face['rotation'] = (face.get('rotation', 0) + 90 * quarters) % 360
            moved[name] = face
    element['faces'] = moved


def snap(angle):
    return min([-45, -22.5, 0, 22.5, 45], key=lambda x: abs(x - angle))


def gun(model, texture_index, shift_x, texture_name):
    out = []
    tex = model['textures'][texture_index]
    uw = tex.get('uv_width', tex['width'])
    for e in model['elements']:
        faces = {k: v for k, v in e['faces'].items() if v.get('texture') == texture_index}
        if not faces or any(v.get('texture') not in (texture_index, None) for v in e['faces'].values()):
            continue
        el = {'from': list(e['from']), 'to': list(e['to']), 'faces': {}}
        for k, v in faces.items():
            uv = [c * 16.0 / uw for c in v['uv']]
            face = {'uv': uv, 'texture': '#gun'}
            if v.get('rotation'):
                face['rotation'] = v['rotation']
            el['faces'][k] = face
        rx, ry, rz = e.get('rotation') or [0, 0, 0]
        origin = list(e.get('origin') or [8, 8, 8])
        # Blockbench applies x first, then y, then z: bake the quarter turns, keep one small angle.
        for axis, angle in (('x', rx), ('y', ry), ('z', rz)):
            q = int(round(angle / 90.0))
            if abs(angle - q * 90) < 1e-6:
                quarter(el, axis, origin, q)
            else:
                base = int(angle // 90) if angle > 0 else -int((-angle) // 90)
                quarter(el, axis, origin, base)
                rest = angle - base * 90
                el['rotation'] = {'angle': snap(rest), 'axis': axis, 'origin': origin}
        el['from'][0] += shift_x
        el['to'][0] += shift_x
        if 'rotation' in el:
            el['rotation']['origin'] = [el['rotation']['origin'][0] + shift_x] + el['rotation']['origin'][1:]
        out.append(el)
    for el in out:
        for i in range(3):
            assert -16 <= el['from'][i] <= 32 and -16 <= el['to'][i] <= 32, el
    display = {k: v for k, v in model['display'].items() if k != 'on_shelf'}
    return {'textures': {'gun': NS + ':item/' + texture_name, 'particle': NS + ':item/' + texture_name}, 'elements': out, 'display': display}


rev = json.load(open(os.path.join(SRC, 'revolver.bbmodel')))


def texture_png(index, target):
    data = rev['textures'][index]['source'].split(',', 1)[1]
    Image.open(io.BytesIO(base64.b64decode(data))).save(TEX + 'item/' + target + '.png')


texture_png(7, 'revolver_3d')
texture_png(0, 'sawn_off_3d')
w(A + 'models/item/revolver_3d.json', gun(rev, 7, 0.0, 'revolver_3d'))
w(A + 'models/item/sawn_off_3d.json', gun(rev, 0, 26.35, 'sawn_off_3d'))
w(A + 'models/item/sawn_off_icon.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': NS + ':item/sawn_off'}})
w(A + 'models/item/sawn_off.json', {'loader': 'neoforge:separate_transforms', 'gui_light': 'front',
                                    'base': {'parent': NS + ':item/sawn_off_3d'}, 'perspectives': {'gui': {'parent': NS + ':item/sawn_off_icon'}}})
print('batch 17 written')
