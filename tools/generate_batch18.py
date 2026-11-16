#!/usr/bin/env python3
"""1.17.2: the designer's Pseudomage outfit (ASSETS 1.17.3): bandolier, poncho with hood, boots, gloves and the
hat (sombrero), converted into worn models (see client/WornModels and MODELS.md for the frame: (8, 8, 8) is the
joint of the body part, north is the front, east the wearer's right).

Each designer model sits in its own frame, so every piece is moved by a hand-measured offset onto its body
part. Java models only allow one rotation axis at -45..45 in 22.5 steps; elements turned any other way go
into a sub-model of their own, and the rotation is applied in code (WornModels.Placement): the manifest
printed at the end lists them.

Usage: python3 tools/generate_batch18.py <ASSETS folder>
"""
import json
import os
import shutil
import sys

NS = 'arcane_artifacts'
A = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', NS)
VALID = {-45.0, -22.5, 0.0, 22.5, 45.0}

# source model, its texture, [(worn model, element indices, offset onto the body part, body part)]
PIECES = [
    ('PATRONAzh.json', 'patronazh', 'bandolier', [('bandolier', [0, 1, 2, 3], (-20, -16, 0), 'BODY')]),
    ('Poncho.json', 'poncho', 'poncho', [
        ('poncho', [1], (-12, -16, 0), 'BODY'),
        ('poncho_right', [0], (-17, -14, 0), 'RIGHT_ARM'),
        ('poncho_left', [3], (-7, -14, 0), 'LEFT_ARM'),
        # The hood is grown 13% so it covers the hat layer of the skin (half a pixel out from the head).
        ('poncho_hood', [2], (-12, -16, 0), 'HEAD', (1.13, (8, 11, 8)))]),
    ('pseudo_BOOTS.json', 'pseudomage_boots', 'pseudomage_boots', [
        ('pseudomage_boots_right', [0, 1, 2, 3], (-21.8, -4, 0), 'RIGHT_LEG'),
        ('pseudomage_boots_left', [4, 5, 6, 7], (-18, -4, 0), 'LEFT_LEG')]),
    ('pseudomage_gloves.json', 'pseudomage_gloves', 'pseudomage_gloves', [
        ('pseudomage_glove_right', [0, 1], (-20.77, -14.1, 0.14), 'RIGHT_ARM'),
        ('pseudomage_glove_left', [2, 3], (-16.11, -14.1, 0.14), 'LEFT_ARM')]),
    ('pseudomage_hat.json', 'pseudomage_hat', 'sombrero', [('sombrero', [0, 1, 2], (-15.67, -5, 0), 'HEAD', (1.12, (8, 16, 8)))]),
]


def rotation(e):
    """(rx, ry, rz, origin) of an element in either Blockbench export style."""
    r = e.get('rotation')
    if not r:
        return 0.0, 0.0, 0.0, [8, 8, 8]
    if 'axis' in r:
        v = {'x': 0.0, 'y': 0.0, 'z': 0.0}
        v[r['axis']] = float(r.get('angle', 0))
        return v['x'], v['y'], v['z'], r['origin']
    return float(r.get('x', 0)), float(r.get('y', 0)), float(r.get('z', 0)), r['origin']


def to_game_order(rx, ry, rz):
    """Blockbench turns an element X, then Y, then Z (matrix Rz.Ry.Rx); the game's Placement turns the pose by
    X, Y, Z in turn (matrix Rx.Ry.Rz). Returns the angles that give the same rotation in the game's order."""
    import math
    x, y, z = map(math.radians, (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    mul = lambda a, b: [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]  # noqa: E731
    R = mul(Rz, mul(Ry, Rx))
    b = math.asin(max(-1.0, min(1.0, R[0][2])))
    if abs(math.cos(b)) > 1e-6:
        a = math.atan2(-R[1][2], R[2][2])
        c = math.atan2(-R[0][1], R[0][0])
    else:   # gimbal lock: put everything on X
        a = math.atan2(R[2][1], R[1][1])
        c = 0.0
    return tuple(round(math.degrees(v), 4) + 0.0 for v in (a, b, c))


def r4(v):
    return round(v, 4)


def convert(src, tex_name, texture, name, indices, offset, part, grow=None):
    model = json.load(open(src))
    groups = {}   # placement key -> elements
    for i in indices:
        e = json.loads(json.dumps(model['elements'][i]))
        rx, ry, rz, origin = rotation(e)
        k_, c_ = grow if grow else (1.0, (0, 0, 0))

        def place(p):
            return [r4(c_[k] + (p[k] + offset[k] - c_[k]) * k_) for k in range(3)]
        moved = place(origin)
        for key in ('from', 'to'):
            e[key] = place(e[key])
        e.pop('name', None)
        nonzero = [(a, v) for a, v in zip('xyz', (rx, ry, rz)) if abs(v) > 1e-6]
        placement = None
        if not nonzero:
            e.pop('rotation', None)
        elif len(nonzero) == 1 and nonzero[0][1] in VALID:
            e['rotation'] = {'angle': nonzero[0][1], 'axis': nonzero[0][0], 'origin': moved}
        else:
            e.pop('rotation', None)
            placement = (tuple(moved), to_game_order(rx, ry, rz))
        for face in e['faces'].values():
            face['texture'] = '#t'
        groups.setdefault(placement, []).append(e)
    out = []
    for n, (placement, elements) in enumerate(sorted(groups.items(), key=lambda kv: kv[0] is not None)):
        worn = name if n == 0 else f'{name}_{n + 1}'
        for e in elements:
            for k in range(3):
                for key in ('from', 'to'):
                    if not -16 <= e[key][k] <= 32:
                        raise SystemExit(f'{worn}: {key} {e[key]} is outside -16..32')
        data = {'textures': {'t': f'{NS}:item/worn/{texture}', 'particle': f'{NS}:item/worn/{texture}'}, 'elements': elements}
        with open(os.path.join(A, 'models', 'item', 'worn', worn + '.json'), 'w') as f:
            json.dump(data, f, indent=2)
            f.write('\n')
        out.append({'name': worn, 'part': part, 'placement': placement})
    return out


def java_to_bbmodel(model, top_rotation):
    """A Java Block/Item model as the .bbmodel structure tools/bbmodel_entity.py bakes (UVs in 16ths)."""
    elements = []
    for i, e in enumerate(model['elements']):
        rx, ry, rz, origin = rotation(e)
        faces = {k: {'uv': f['uv'], 'texture': 0, **({'rotation': f['rotation']} if 'rotation' in f else {})}
                 for k, f in e['faces'].items()}
        elements.append({'uuid': str(i), 'from': e['from'], 'to': e['to'], 'rotation': [rx, ry, rz], 'origin': origin, 'faces': faces})
    groups = []

    def node(g, rot=None):
        uuid = 'g' + str(len(groups))
        groups.append({'uuid': uuid, 'name': g['name'], 'origin': g.get('origin', [8, 0, 8]), 'rotation': rot or [0, 0, 0]})
        return {'uuid': uuid, 'children': [node(c) if isinstance(c, dict) else str(c) for c in g['children']]}
    outliner = [node(g, top_rotation) for g in model['groups'] if isinstance(g, dict)]
    return {'elements': elements, 'groups': groups, 'outliner': outliner,
            'textures': [{'width': 64, 'height': 64, 'uv_width': 16, 'uv_height': 16}]}


def amanitoid_b(src):
    """The second Amanitoid has its own model (a taller, double cap): baked like the other creatures."""
    sys.path.insert(0, os.path.dirname(__file__))
    import bbmodel_entity as b
    model = java_to_bbmodel(json.load(open(os.path.join(src, 'red_mooshroom_b.json'))), [0, -90, 0])
    spec = dict(group='group7', limbs={'right_arm3': 'right_arm', 'left_arm3': 'left_arm', 'right_leg3': 'right_leg', 'left_leg3': 'left_leg'})
    quads, size = b.bake(model, spec)
    parts = b.regroup(b.to_model_space(quads, 0))
    piv = b.pivots(parts)
    clean = {k: [{'v': q['v'], 'uv': q['uv']} for q in v] for k, v in parts.items()}
    with open(os.path.join(A, 'entity_models', 'amanitoid_b.json'), 'w') as f:
        json.dump({'texture_size': list(size), 'parts': {k: {'pivot': piv[k], 'quads': v} for k, v in clean.items()}}, f, separators=(',', ':'))
    print('amanitoid_b', {k: (len(v), [round(c, 1) for c in piv[k]]) for k, v in clean.items()})


def main():
    src = sys.argv[1]
    manifest = []
    for model, tex_name, texture, pieces in PIECES:
        shutil.copyfile(os.path.join(src, tex_name + '.png'), os.path.join(A, 'textures', 'item', 'worn', texture + '.png'))
        for name, indices, offset, part, *grow in pieces:
            manifest += convert(os.path.join(src, model), tex_name, texture, name, indices, offset, part, *grow)
    # The second Amanitoid's new skin.
    for s, d in (('micilinoid_Muhamor_a', 'amanitoid_a'), ('micilinoid_Muhamor_b', 'amanitoid_b')):
        shutil.copyfile(os.path.join(src, s + '.png'), os.path.join(A, 'textures', 'entity', d + '.png'))
    for old in ('bandolier_loaded',):
        p = os.path.join(A, 'models', 'item', 'worn', old + '.json')
        if os.path.exists(p):
            os.remove(p)
    amanitoid_b(src)
    for m in manifest:
        print(json.dumps(m))


if __name__ == '__main__':
    main()
