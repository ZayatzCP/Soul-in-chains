"""Class mounts (1.17.0): the Flying Broom, the Moss Elk, the Iron Horse.

Builds each mount from boxes in entity model space (x to the creature's left, y down with the ground at 24,
front -z), with the vanilla box texture layout, and writes:
  assets/arcane_artifacts/entity_models/<name>.json   baked quads read by client/baked/BakedModels
  assets/arcane_artifacts/textures/entity/<name>.png  painted texture
  assets/arcane_artifacts/textures/item/<item>.png    16x16 icon, models/item/<item>.json
Usage: python3 tools/generate_mounts.py
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'arcane_artifacts')


# ---------------------------------------------------------------------------------------------- materials

def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def noise(base, spread):
    def paint(x, y, w, h, face, rnd):
        return shade(base, 1 + rnd.uniform(-spread, spread))
    return paint


def grain(base, horizontal):
    """Wood / straw: stripes along the long side."""
    def paint(x, y, w, h, face, rnd):
        k = 1 + ((y if horizontal else x) % 3 == 0) * -0.15 + rnd.uniform(-0.06, 0.06)
        return shade(base, k)
    return paint


def straw(x, y, w, h, face, rnd):
    base = rgb('#d9b54a')
    if face in ('north', 'south'):
        return shade(base, 1 + rnd.uniform(-0.25, 0.1))
    return shade(base, (0.85 if x % 2 else 1.05) + rnd.uniform(-0.08, 0.08))


def band(x, y, w, h, face, rnd):
    return rgb('#e0b040') if (x + y) % 4 == 0 else shade(rgb('#6a2f9a'), 1 + rnd.uniform(-0.1, 0.1))


def gem(x, y, w, h, face, rnd):
    return shade(rgb('#c05cff'), 1.25 if (x + y) % 2 else 0.9)


def fur(x, y, w, h, face, rnd):
    base = rgb('#7a5434')
    if face == 'down':
        base = rgb('#b89a76')
    k = 1 + rnd.uniform(-0.12, 0.12) - (0.1 if face in ('east', 'west') and y > h * 0.75 else 0)
    return shade(base, k)


def leg_fur(x, y, w, h, face, rnd):
    if y >= h - 2 and face not in ('up', 'down'):
        return shade(rgb('#2e2219'), 1 + rnd.uniform(-0.1, 0.1))   # hooves
    if face == 'down':
        return rgb('#2e2219')
    return shade(rgb('#6b4a2e'), 1 + rnd.uniform(-0.12, 0.12))


def moss(x, y, w, h, face, rnd):
    c = rgb('#4f8a2f') if rnd.random() > 0.25 else rgb('#7cb342')
    if rnd.random() < 0.05:
        c = rgb('#e8e070')   # a few flowers
    return shade(c, 1 + rnd.uniform(-0.1, 0.1))


def antler(x, y, w, h, face, rnd):
    if rnd.random() < 0.18:
        return shade(rgb('#5f9a35'), 1 + rnd.uniform(-0.1, 0.1))   # moss in the antlers
    return shade(rgb('#d9c7a0'), 1 + rnd.uniform(-0.1, 0.06))


def elk_head(x, y, w, h, face, rnd):
    base = fur(x, y, w, h, face, rnd)
    if face in ('east', 'west') and y == 2 and x in (2, 3):
        return rgb('#141010') if x == 2 else rgb('#f0e6d0')   # eyes
    return base


def muzzle(x, y, w, h, face, rnd):
    if face == 'north' and y == 1 and x in (0, w - 1):
        return rgb('#1a1210')   # nostrils
    return shade(rgb('#4a3324'), 1 + rnd.uniform(-0.1, 0.1))


def cap(x, y, w, h, face, rnd):
    if face == 'up' and (x * 3 + y * 5) % 4 == 0:
        return rgb('#f2eee0')
    return shade(rgb('#c8302a'), 1 + rnd.uniform(-0.08, 0.08))


def iron(x, y, w, h, face, rnd):
    base = rgb('#8e9399')
    k = 1 + rnd.uniform(-0.05, 0.05)
    if x in (0, w - 1) or y in (0, h - 1):
        k -= 0.18   # panel edges
    if (x in (1, w - 2)) and (y in (1, h - 2)) and w > 3 and h > 3:
        return rgb('#c9ccd1')   # rivets
    if y == h // 2 and w > 5:
        k -= 0.1
    return shade(base, k)


def dark_iron(x, y, w, h, face, rnd):
    return shade(rgb('#3e4248'), 1 + rnd.uniform(-0.08, 0.08))


def iron_leg(x, y, w, h, face, rnd):
    if y >= h - 2:
        return shade(rgb('#2f3236'), 1 + rnd.uniform(-0.05, 0.05))   # iron shoes
    if y in (5, 6):
        return rgb('#c8963a')   # brass knee
    return iron(x, y, w, h, face, rnd)


def brass(x, y, w, h, face, rnd):
    return shade(rgb('#c8963a'), (1.15 if (x + y) % 3 == 0 else 1) + rnd.uniform(-0.05, 0.05))


def iron_head(x, y, w, h, face, rnd):
    if face in ('east', 'west') and y == 2 and x in (2, 3):
        return rgb('#ffb030') if x == 3 else rgb('#ff7a10')   # boiler-glow eyes
    return iron(x, y, w, h, face, rnd)


def leather(x, y, w, h, face, rnd):
    if x in (0, w - 1) and face == 'up':
        return rgb('#e0b040')   # stitching
    return shade(rgb('#6b3a1e'), 1 + rnd.uniform(-0.08, 0.08))


def boiler(x, y, w, h, face, rnd):
    if face == 'north' and 2 <= x <= w - 3 and 3 <= y <= h - 3:
        return rgb('#ff9a20') if (x + y) % 2 else rgb('#e05a10')   # the firebox window
    return brass(x, y, w, h, face, rnd)


def chain(x, y, w, h, face, rnd):
    return rgb('#5c6066') if y % 2 else rgb('#a0a4aa')


# ---------------------------------------------------------------------------------------------- boxes


class Model:
    def __init__(self, tex_w, tex_h, seed):
        self.tex = Image.new('RGBA', (tex_w, tex_h), (0, 0, 0, 0))
        self.parts = {}
        self.pivots = {}
        self.cursor = [0, 0, 0]   # x, y, row height
        self.rnd = random.Random(seed)

    def alloc(self, w, h, d):
        tw, th = 2 * (d + w), d + h
        x, y, row = self.cursor
        if x + tw > self.tex.width:
            x, y, row = 0, y + row, 0
        if y + th > self.tex.height:
            raise ValueError('texture too small')
        self.cursor = [x + tw, y, max(row, th)]
        return x, y

    def box(self, part, x0, y0, z0, w, h, d, paint):
        u, v = self.alloc(w, h, d)
        x1, y1, z1 = x0 + w, y0 + h, z0 + d
        v0, v1, v2, v3 = (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)
        v4, v5, v6, v7 = (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)
        f6, f7, f8, f9, f10, f11 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
        f12, f13, f14 = v, v + d, v + d + h
        faces = [  # vanilla ModelPart.Cube: name, vertices, uv rect, painted size
            ('up', [v5, v4, v0, v1], (f7, f12, f8, f13), (w, d)),       # min y in model space: the top
            ('down', [v2, v3, v7, v6], (f8, f13, f9, f12), (w, d)),
            ('west', [v0, v4, v7, v3], (f6, f13, f7, f14), (d, h)),
            ('north', [v1, v0, v3, v2], (f7, f13, f8, f14), (w, h)),
            ('east', [v5, v1, v2, v6], (f8, f13, f10, f14), (d, h)),
            ('south', [v4, v5, v6, v7], (f10, f13, f11, f14), (w, h)),
        ]
        tw, th = self.tex.size
        for name, verts, (u1, va, u2, vb), (pw, ph) in faces:
            # paint the face's texture region
            ux, vy = int(min(u1, u2)), int(min(va, vb))
            for py in range(max(1, ph)):
                for px in range(max(1, pw)):
                    if ux + px < tw and vy + py < th:
                        self.tex.putpixel((ux + px, vy + py), paint(px, py, pw, ph, name, self.rnd))
            uv = [(u2, va), (u1, va), (u1, vb), (u2, vb)]
            self.parts.setdefault(part, []).append({
                'v': [list(map(float, p)) for p in verts],
                'uv': [[round(a / tw, 6), round(b / th, 6)] for a, b in uv]})

    def pivot(self, part, x, y, z):
        self.pivots[part] = [float(x), float(y), float(z)]

    def save(self, name):
        out = {'texture_size': list(self.tex.size),
               'parts': {k: {'pivot': self.pivots.get(k, [0.0, 0.0, 0.0]), 'quads': v} for k, v in self.parts.items()}}
        with open(os.path.join(ROOT, 'entity_models', name + '.json'), 'w') as f:
            json.dump(out, f, separators=(',', ':'))
        self.tex.save(os.path.join(ROOT, 'textures', 'entity', name + '.png'))


def four_legs(m, top, length, x, front_z, back_z, paint, size=3):
    for name, lx, lz in (('leg_front_left', x, front_z), ('leg_front_right', -x, front_z),
                         ('leg_back_left', x, back_z), ('leg_back_right', -x, back_z)):
        m.box(name, lx - size / 2, top, lz - size / 2, size, length, size, paint)
        m.pivot(name, lx, top, lz)


def broom():
    m = Model(64, 64, 11)
    wood = grain(rgb('#7a4f2a'), True)
    m.box('body', -0.5, 12.5, -16, 1, 1, 26, wood)                  # the handle
    m.box('body', -1, 12, -17, 2, 2, 1, grain(rgb('#5a3a1e'), True))
    m.box('body', -1, 12, -7, 2, 2, 2, gem)             # a source gem set in the handle
    m.box('body', -2.5, 10.5, 9, 5, 5, 2, band)
    m.box('body', -3, 10, 11, 6, 6, 5, straw)
    m.box('body', -3.5, 9.5, 15, 7, 7, 4, straw)
    m.pivot('body', 0, 13, 0)
    m.save('flying_broom')


def moss_elk():
    m = Model(128, 64, 23)
    four_legs(m, 10, 14, 3.5, -6.5, 6.5, leg_fur)
    m.box('body', -5, 0, -9, 10, 10, 19, fur)
    m.box('body', -5.5, -1.5, -6, 11, 2, 12, moss)                    # the moss blanket instead of a saddle
    m.box('body', 2, -3.5, -4, 3, 2, 3, cap)                          # mushrooms growing on it
    m.box('body', -4.5, -3.5, 2, 2, 2, 2, cap)
    m.box('body', -2.5, -7, -12, 5, 9, 5, fur)                        # neck
    m.box('body', -3, -12, -19, 6, 6, 10, elk_head)
    m.box('body', -2, -10, -22, 4, 4, 3, muzzle)
    m.box('body', -6, -11.5, -11, 3, 1, 2, fur)                       # ears
    m.box('body', 3, -11.5, -11, 3, 1, 2, fur)
    m.box('body', -1.5, 0, 10, 3, 4, 2, fur)                          # tail
    for s in (-1, 1):                                                  # antlers
        bx = 3 if s > 0 else -4
        m.box('body', bx, -19, -11, 1, 7, 1, antler)
        m.box('body', 4 if s > 0 else -8, -16, -11, 4, 1, 1, antler)
        m.box('body', 4 if s > 0 else -9, -20, -11, 5, 1, 1, antler)
        m.box('body', 8 if s > 0 else -9, -23, -11, 1, 3, 1, antler)
        m.box('body', 7 if s > 0 else -8, -19, -11, 1, 3, 1, antler)
        m.box('body', bx, -15, -14, 1, 1, 3, antler)
    m.pivot('body', 0, 10, 0)
    m.save('moss_elk')


def iron_horse():
    m = Model(128, 64, 37)
    four_legs(m, 11, 13, 3.5, -6.5, 6.5, iron_leg)
    m.box('body', -5, 1, -10, 10, 10, 20, iron)
    m.box('body', -4, 2, -12, 8, 8, 2, boiler)                        # the firebox at the chest
    m.box('body', -4, 0, -3, 8, 1, 7, leather)                        # saddle
    m.box('body', -1.5, -5, -7, 3, 6, 3, dark_iron)                   # chimney
    m.box('body', -2, -6, -7.5, 4, 1, 4, brass)
    m.box('body', -2.5, -7, -13, 5, 10, 5, iron)                      # neck
    m.box('body', -0.5, -8, -9, 1, 9, 2, brass)                       # brass comb for a mane
    m.box('body', -3, -12, -21, 6, 6, 11, iron_head)
    m.box('body', -2, -10, -24, 4, 4, 3, dark_iron)
    m.box('body', -3, -14, -12, 2, 2, 1, iron)                        # ears
    m.box('body', 1, -14, -12, 2, 2, 1, iron)
    m.box('body', -0.5, 2, 10, 1, 7, 1, chain)                        # a chain for a tail
    m.box('body', -1, 9, 9.5, 2, 2, 2, dark_iron)
    m.pivot('body', 0, 11, 0)
    m.save('iron_horse')


# ---------------------------------------------------------------------------------------------- item icons

ICONS = {
    'flying_broom': ({
        'H': '#7a4f2a', 'h': '#5a3a1e', 'B': '#6a2f9a', 'b': '#e0b040', 'S': '#d9b54a', 's': '#a8862c', 'G': '#c05cff'}, [
        '...............h',
        '..............hH',
        '.............hH.',
        '............hH..',
        '...........hG...',
        '..........hH....',
        '.........hH.....',
        '........hH......',
        '.......hH.......',
        '......Bb........',
        '....sSBB........',
        '...SsSSs........',
        '..sSSsS.........',
        '.SsSsS..........',
        'sS.sS...........',
        '.s..............']),
    'moss_elk_call': ({
        'C': '#e6d6b0', 'c': '#bfa77a', 'd': '#8a7350', 'M': '#4f8a2f', 'm': '#7cb342', 'r': '#c8302a'}, [
        '................',
        '..............dc',
        '.............dCc',
        '............dCC.',
        '...........dCCc.',
        '..........MmMm..',
        '.........mMMmM..',
        '........dCCcc...',
        '.......dCCcc....',
        '......dCCcc.....',
        '....ddCCcc......',
        '..ddCCCcc.......',
        '.dCCCcc.r.......',
        'dCCcc...........',
        'dCc.............',
        '.d..............']),
    'iron_horse': ({
        'I': '#8e9399', 'i': '#5c6066', 'L': '#c9ccd1', 'O': '#ff9a20', 'B': '#c8963a', 'K': '#2f3236'}, [
        '................',
        '.....i..i.......',
        '....iIiiIi......',
        '...iILIIIIi.....',
        '..iIIIOIIIIi....',
        '.iIIIIIIIIIBi...',
        'iKIIIIIIIIIBi...',
        'iKKIi.iIIIIBi...',
        '.ii...iIIIIIBi..',
        '......iIIIIIIBi.',
        '.....iIIIIIIIIBi',
        '.....iLIIIIIIIIi',
        '.....iIIIIIIIIIi',
        '....iBBBBBBBBBBi',
        '....iKKKKKKKKKKi',
        '.....iiiiiiiiii.']),
}


def icons():
    for name, (palette, rows) in ICONS.items():
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in palette:
                    img.putpixel((x, y), rgb(palette[ch]))
        img.save(os.path.join(ROOT, 'textures', 'item', name + '.png'))
        with open(os.path.join(ROOT, 'models', 'item', name + '.json'), 'w') as f:
            json.dump({'parent': 'minecraft:item/handheld' if name == 'flying_broom' else 'minecraft:item/generated',
                       'textures': {'layer0': 'arcane_artifacts:item/' + name}}, f, indent=2)
            f.write('\n')


if __name__ == '__main__':
    broom()
    moss_elk()
    iron_horse()
    icons()
    print('mounts written')
