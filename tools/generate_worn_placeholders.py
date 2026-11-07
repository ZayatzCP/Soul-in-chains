#!/usr/bin/env python3
"""Writes the placeholder 3D models drawn on players wearing the accessories.

Run from the repository root:  python3 tools/generate_worn_placeholders.py [--force]
Existing files are kept unless --force is given, so imported Blockbench models are never
overwritten by accident.

Coordinate convention (shared with WornModels.java and MODELS.md): 1 unit = 1 skin pixel,
(8, 8, 8) is the joint of the body part, north (-Z) is the player's front, east (+X) is the
player's right side, +Y is up. Body parts in these coordinates:
  head       x 4..12   y 8..16   z 4..12   (joint: neck)
  body       x 4..12   y -4..8   z 6..10   (joint: neck)
  right arm  x 7..11   y -2..10  z 6..10   (joint: shoulder; slim arms x 7..10)
  left arm   x 5..9    y -2..10  z 6..10   (slim arms x 6..9)
  legs       x 6..10   y -4..8   z 6..10   (joint: hip)
"""
import json
import os
import sys

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'arcane_artifacts', 'models', 'item', 'worn')
FACES = ['north', 'south', 'east', 'west', 'up', 'down']


def box(frm, to, tex, uv=(0, 0, 16, 16), tint=None):
    faces = {}
    for f in FACES:
        face = {'uv': list(uv), 'texture': '#' + tex}
        if tint is not None:
            face['tintindex'] = tint
        faces[f] = face
    return {'from': list(frm), 'to': list(to), 'faces': faces}


def model(textures, elements):
    textures = dict(textures)
    textures.setdefault('particle', next(iter(textures.values())))
    return {'textures': textures, 'elements': elements}


def wreath():
    els = [
        box((3.25, 12.5, 3.25), (12.75, 14, 4), 'vine'),     # front
        box((3.25, 12.5, 12), (12.75, 14, 12.75), 'vine'),   # back
        box((3.25, 12.5, 4), (4, 14, 12), 'vine'),           # left side (west)
        box((12, 12.5, 4), (12.75, 14, 12), 'vine'),         # right side (east)
    ]
    for c in (5, 7.25, 9.5):
        els.append(box((c, 12.25, 2.5), (c + 1.5, 14.25, 3.25), 'flower', tint=1))    # physical: front
        els.append(box((12.75, 12.25, c), (13.5, 14.25, c + 1.5), 'flower', tint=2))  # fire: right
        els.append(box((c, 12.25, 12.75), (c + 1.5, 14.25, 13.5), 'flower', tint=3))  # water: back
        els.append(box((2.5, 12.25, c), (3.25, 14.25, c + 1.5), 'flower', tint=4))    # magic: left
    return model({'vine': 'arcane_artifacts:item/worn/wreath_vine', 'flower': 'arcane_artifacts:item/worn/wreath_flower'}, els)


def gloves(x0, x1):
    return model({'gloves': 'arcane_artifacts:item/worn/gloves'}, [
        box((x0 - 0.4, -2.4, 5.6), (x1 + 0.4, 2.6, 10.4), 'gloves', uv=(0, 0, 16, 10)),
        box((x0 - 0.6, 2.6, 5.4), (x1 + 0.6, 3.6, 10.6), 'gloves', uv=(0, 10, 16, 16)),
    ])


def boots():
    return model({'boots': 'arcane_artifacts:item/worn/boots'}, [
        box((5.6, -4.4, 5.6), (10.4, 0.6, 10.4), 'boots'),
        box((5.6, -4.4, 4.8), (10.4, -2.6, 5.6), 'boots', uv=(0, 10, 16, 16)),
    ])


def ring():
    return model({'ring': 'arcane_artifacts:item/worn/ring', 'gem': 'arcane_artifacts:item/worn/gem'}, [
        box((6.45, 0.3, 5.45), (11.55, 1.1, 10.55), 'ring'),
        box((8.25, 0.1, 5.0), (9.75, 1.3, 5.45), 'gem'),
    ])


def mantle():
    return model({'mantle': 'arcane_artifacts:item/worn/mantle', 'clasp': 'arcane_artifacts:item/worn/clasp'}, [
        box((3.5, -4.5, 10.25), (12.5, 8.25, 11), 'mantle'),
        box((3.5, 7.75, 5.75), (12.5, 8.75, 10.25), 'mantle', uv=(0, 0, 16, 4)),
        box((7, 6.5, 5.25), (9, 8, 5.75), 'clasp'),
    ])


MODELS = {
    'wreath_of_manipulation': wreath(),
    'focusing_gloves_right': gloves(7, 11),
    'focusing_gloves_left': gloves(5, 9),
    'focusing_boots_right': boots(),
    'focusing_boots_left': boots(),
    'ring_of_reversal': ring(),
    'mantle_of_reflection': mantle(),
}


def main():
    force = '--force' in sys.argv
    os.makedirs(OUT, exist_ok=True)
    for name, data in MODELS.items():
        path = os.path.join(OUT, name + '.json')
        if os.path.exists(path) and not force:
            print('kept   ', os.path.relpath(path))
            continue
        with open(path, 'w') as f:
            json.dump(data, f, indent=2)
            f.write('\n')
        print('wrote  ', os.path.relpath(path))


if __name__ == '__main__':
    main()
