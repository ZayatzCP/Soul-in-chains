#!/usr/bin/env python3
"""1.19.0: the designer's Druid outfit (ASSETS 1.19.0): mantle with hood, hat, gloves -> worn models, the same way
as tools/generate_batch18.py (see its docstring). The designer's Blockbench frame is the player template: body
x 4..12, y 12..24, head y 24..32, right arm x 12..16, left arm x 0..4, shoulders at y 22.

Usage: python3 tools/generate_batch19.py <ASSETS folder>
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(__file__))
import generate_batch18 as b18  # noqa: E402

BODY = (0, -16, 0)
HEAD = (0, -16, 0)
RIGHT_ARM = (-5, -14, 0)
LEFT_ARM = (5, -14, 0)

PIECES = [
    ('druid_mantle.json', 'druid_mantle', 'druid_mantle', [
        ('druid_mantle', [1, 5, 6], BODY, 'BODY'),
        ('druid_mantle_hood', [0], HEAD, 'HEAD', (1.02, (8, 12, 8))),
        ('druid_mantle_right', [2, 3], RIGHT_ARM, 'RIGHT_ARM'),
        ('druid_mantle_left', [4], LEFT_ARM, 'LEFT_ARM')]),
    ('druid_hat.json', 'druid_hat', 'druid_hat', [('druid_hat', list(range(10)), (12.3, -1.6, 0), 'HEAD')]),
    ('Druid_GLOVES.json', 'druid_gloves', 'druid_gloves', [
        ('druid_glove_right', [0], RIGHT_ARM, 'RIGHT_ARM'),
        ('druid_glove_left', [1], LEFT_ARM, 'LEFT_ARM')]),
]


def gribulka_jr(src):
    """The young Gribulka (gribulka_jr.json): baked like the other creatures, limbs grouped by hand."""
    import bbmodel_entity as b
    model = json.load(open(os.path.join(src, 'gribulka_jr.json')))
    # The designer's left and right are the creature's right and left.
    model['groups'] = [{'name': 'gribulka_jr', 'origin': [8, 0, 8], 'children': [0, 1,
        {'name': 'left_arm', 'origin': [4, 5, 8], 'children': [2]}, {'name': 'right_arm', 'origin': [12, 5, 8], 'children': [5]},
        {'name': 'left_leg', 'origin': [6, 2, 8], 'children': [3]}, {'name': 'right_leg', 'origin': [10, 2, 8], 'children': [4]}]}]
    bb = b18.java_to_bbmodel(model, [0, 0, 0])
    bb['textures'][0].update({'width': 32, 'height': 32})
    limbs = {n: n for n in ('right_arm', 'left_arm', 'right_leg', 'left_leg')}
    quads, size = b.bake(bb, dict(group='gribulka_jr', limbs=limbs))
    parts = b.regroup(b.to_model_space(quads, 0))
    piv = b.pivots(parts)
    clean = {k: [{'v': q['v'], 'uv': q['uv']} for q in v] for k, v in parts.items()}
    with open(os.path.join(b18.A, 'entity_models', 'gribulka_jr.json'), 'w') as f:
        json.dump({'texture_size': list(size), 'parts': {k: {'pivot': piv[k], 'quads': v} for k, v in clean.items()}}, f, separators=(',', ':'))
    shutil.copyfile(os.path.join(src, 'gribulka_Jr.png'), os.path.join(b18.A, 'textures', 'entity', 'gribulka_jr.png'))
    print('gribulka_jr', {k: [round(c, 1) for c in piv[k]] for k in clean})


def main():
    src = sys.argv[1]
    manifest = []
    for model, tex_name, texture, pieces in PIECES:
        shutil.copyfile(os.path.join(src, tex_name + '.png'), os.path.join(b18.A, 'textures', 'item', 'worn', texture + '.png'))
        for name, indices, offset, part, *grow in pieces:
            manifest += b18.convert(os.path.join(src, model), tex_name, texture, name, indices, offset, part, *grow)
    gribulka_jr(src)
    for m in manifest:
        print(json.dumps(m))


if __name__ == '__main__':
    main()
