#!/usr/bin/env python3
"""Bakes the creatures of a Blockbench "free" model (.bbmodel) into the mod's entity model format.

    python3 tools/bbmodel_entity.py <file.bbmodel> <out_dir>

Each creature is one top-level group of the .bbmodel (see MOBS). Element and group rotations are applied
(Blockbench order ZYX), the creature is centred on its feet and turned to face north (-z), and every face
becomes a textured quad in entity model space (x to the creature's left, y down from 24, front -z),
grouped into the parts the game animates: body, right_arm, left_arm, right_leg, left_leg (or the four
legs of a quadruped). Output: <out_dir>/<name>.json = {"texture_size": [w, h], "parts": {name: {"pivot",
"quads": [{"v": [[x, y, z] x4], "uv": [[u, v] x4]}]}}}.
"""
import json
import math
import sys

# creature -> top group, the groups that are limbs, extra rules, the turn (degrees) that makes it face north
MOBS = {
    'mycelinoid': dict(group='group', limbs={'right arm': 'right_arm', 'left arm': 'left_arm', 'right leg': 'right_leg', 'left leg': 'left_leg'}),
    'amanitoid': dict(group='group2', limbs={'right_arm2': 'right_arm', 'left_arm2': 'left_arm', 'right_leg2': 'right_leg', 'left_leg2': 'left_leg'}),
    'elder_mycelinoid': dict(group='group11', limbs={'r_arm': 'right_arm', 'r_leg': 'right_leg', 'l_leg': 'left_leg'}, left_arm_x=(-126.5, -120.0)),
    'gribulka': dict(group='group3', limbs={'r_arm2': 'right_arm', 'l_arm2': 'left_arm', 'r_leg2': 'right_leg', 'l_leg2': 'left_leg'}),
    'gribulka_hive': dict(group='group4', limbs={}, quadruped=True),
}
FACES = ['north', 'south', 'east', 'west', 'up', 'down']


def rot_matrix(rx, ry, rz):
    """Blockbench / three.js Euler order ZYX: R = Rz * Ry * Rx."""
    x, y, z = map(math.radians, (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    mul = lambda a, b: [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]  # noqa: E731
    return mul(Rz, mul(Ry, Rx))


def apply(m, origin, p):
    v = [p[i] - origin[i] for i in range(3)]
    return [sum(m[i][k] * v[k] for k in range(3)) + origin[i] for i in range(3)]


def corners(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        'down': [[x0, y0, z1], [x0, y0, z0], [x1, y0, z0], [x1, y0, z1]],
        'up': [[x0, y1, z0], [x0, y1, z1], [x1, y1, z1], [x1, y1, z0]],
        'north': [[x1, y1, z0], [x1, y0, z0], [x0, y0, z0], [x0, y1, z0]],
        'south': [[x0, y1, z1], [x0, y0, z1], [x1, y0, z1], [x1, y1, z1]],
        'west': [[x0, y1, z0], [x0, y0, z0], [x0, y0, z1], [x0, y1, z1]],
        'east': [[x1, y1, z1], [x1, y0, z1], [x1, y0, z0], [x1, y1, z0]],
    }[face]


def bake(model, spec):
    groups = {g['uuid']: g for g in model.get('groups', [])}
    elements = {e['uuid']: e for e in model['elements']}
    textures = model['textures']
    top = next(c for c in model['outliner'] if isinstance(c, dict) and groups.get(c['uuid'], {}).get('name') == spec['group'])
    collected = []   # (part, element, [group chain innermost first])

    def walk(node, chain, part):
        group = groups[node['uuid']]
        chain = [group] + chain
        name = group.get('name')
        part = spec['limbs'].get(name, part)
        for child in node.get('children', []):
            if isinstance(child, dict):
                walk(child, chain, part)
            elif child in elements:
                collected.append((part, elements[child], chain))
    walk(top, [], 'body')
    quads = {}
    tex_size = None
    for part, e, chain in collected:
        if e.get('visibility') is False or e.get('export') is False:
            continue
        m = rot_matrix(*(e.get('rotation') or [0, 0, 0]))
        origin = e.get('origin') or [0, 0, 0]
        for face in FACES:
            data = e['faces'].get(face)
            if not data or data.get('texture') is None:
                continue
            tex = textures[data['texture']]
            uw, uh = tex.get('uv_width', tex['width']), tex.get('uv_height', tex['height'])
            tex_size = tex_size or (tex['width'], tex['height'])
            pts = [apply(m, origin, p) for p in corners(face, e['from'], e['to'])]
            for g in chain:
                pts = [apply(rot_matrix(*(g.get('rotation') or [0, 0, 0])), g.get('origin') or [0, 0, 0], p) for p in pts]
            u1, v1, u2, v2 = data['uv']
            uv = [[u1 / uw, v1 / uh], [u1 / uw, v2 / uh], [u2 / uw, v2 / uh], [u2 / uw, v1 / uh]]
            shift = int(data.get('rotation', 0)) // 90
            uv = [uv[(i + shift) % 4] for i in range(4)]
            quads.setdefault(part, []).append({'v': pts, 'uv': uv})
    # Rules that are easier by position than by group.
    if 'left_arm_x' in spec:
        lo, hi = spec['left_arm_x']
        body = []
        for q in quads['body']:
            xs = [p[0] for p in q['v']]
            ys = [p[1] for p in q['v']]
            if lo <= min(xs) and max(xs) <= hi and max(ys) <= 21:
                quads.setdefault('left_arm', []).append(q)
            else:
                body.append(q)
        quads['body'] = body
    if spec.get('quadruped'):
        body, legs = [], []
        for q in quads['body']:
            ys = [p[1] for p in q['v']]
            (legs if max(ys) <= 10.01 and min(ys) >= -0.01 else body).append(q)
        quads['body'] = body
        # Four legs: group the leg quads by which corner of the body they stand under.
        cx = sum(p[0] for q in legs for p in q['v']) / (4 * len(legs))
        cz = sum(p[2] for q in legs for p in q['v']) / (4 * len(legs))
        for q in legs:
            mx = sum(p[0] for p in q['v']) / 4
            mz = sum(p[2] for p in q['v']) / 4
            name = ('leg_front_' if mz < cz else 'leg_back_') + ('left' if mx > cx else 'right')
            quads.setdefault(name, []).append(q)
    return quads, tex_size


def to_model_space(quads, turn):
    """Centre on the feet, turn to face north, then entity model space: x mirrored, y down from 24."""
    allp = [p for qs in quads.values() for q in qs for p in q['v']]
    feet = [p for p in allp if p[1] <= min(a[1] for a in allp) + 0.5]
    cx = (min(p[0] for p in feet) + max(p[0] for p in feet)) / 2
    cz = (min(p[2] for p in feet) + max(p[2] for p in feet)) / 2
    y0 = min(p[1] for p in allp)
    a = math.radians(turn)
    out = {}
    for part, qs in quads.items():
        nq = []
        for q in qs:
            vs = []
            for x, y, z in q['v']:
                x, z = x - cx, z - cz
                x, z = x * math.cos(a) + z * math.sin(a), -x * math.sin(a) + z * math.cos(a)
                vs.append([round(-x, 4), round(24 - (y - y0), 4), round(z, 4)])
            nq.append({'v': vs, 'uv': [[round(u, 5), round(v, 5)] for u, v in q['uv']]})
        out[part] = nq
    return out


def pivots(parts):
    result = {}
    for part, qs in parts.items():
        pts = [p for q in qs for p in q['v']]
        x = (min(p[0] for p in pts) + max(p[0] for p in pts)) / 2
        z = (min(p[2] for p in pts) + max(p[2] for p in pts)) / 2
        top = min(p[1] for p in pts)              # model space: y grows downwards
        if 'arm' in part:
            result[part] = [x, top + 1, z]
        elif 'leg' in part:
            result[part] = [x, top, z]
        else:
            legs = [min(p[1] for q in parts[k] for p in q['v']) for k in parts if 'leg' in k]
            result[part] = [0.0, min(legs) if legs else 24.0, 0.0]
    return result


if __name__ == '__main__':
    model = json.load(open(sys.argv[1]))
    out = sys.argv[2]
    turns = json.loads(sys.argv[3]) if len(sys.argv) > 3 else {}
    for name, spec in MOBS.items():
        quads, size = bake(model, spec)
        parts = to_model_space(quads, turns.get(name, 0))
        piv = pivots(parts)
        json.dump({'texture_size': list(size), 'parts': {k: {'pivot': piv[k], 'quads': v} for k, v in parts.items()}},
                  open(f'{out}/{name}.json', 'w'), separators=(',', ':'))
        print(name, {k: len(v) for k, v in parts.items()})
