#!/usr/bin/env python3
"""1.44: the Thicket Lights - the glow (halo and core), the Light in a Jar and the Caged Light (textures, models, states),
the cage's loot, the Dark Forest spawns. Run from the repository root: python3 tools/generate_thicket_lights.py"""
import json
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'arcane_artifacts')
D = os.path.join(ROOT, 'data', 'arcane_artifacts')
NS = 'arcane_artifacts'
COLORS = {'amber': 0xFFC45C, 'moss': 0xA6E36B, 'dusk': 0xC9A2FF, 'frost': 0xA2E6FF, 'rose': 0xFF8FB4}


def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def save(img, *parts):
    path = os.path.join(A, 'textures', *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def rgb(c):
    return (c >> 16 & 255, c >> 8 & 255, c & 255)


def glow():
    # the halo: white, fading out from the middle (tinted by the renderer)
    size = 32
    halo = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - 15.5, y - 15.5) / 15.5
            a = max(0.0, 1 - d) ** 2.2
            halo.putpixel((x, y), (255, 255, 255, int(a * 255)))
    save(halo, 'entity', 'thicket_light_halo.png')
    # the core: a bright dot with four short rays
    core = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            d = math.hypot(dx, dy)
            ray = (min(dx, dy) < 0.6 and d < 7.5) * (1 - d / 7.5)
            a = max(0.0, 1 - d / 3.2) + ray * 0.9
            core.putpixel((x, y), (255, 255, 255, int(min(1.0, a) * 255)))
    save(core, 'entity', 'thicket_light_core.png')


def blocks():
    # the jar: clear glass with a rim, a cork lid; the cage: dark iron bars
    jar = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            shine = x in (2, 3) and 2 <= y <= 10
            jar.putpixel((x, y), (200, 225, 230, 210) if edge else (235, 250, 255, 150) if shine else (210, 235, 240, 60))
    save(jar, 'block', 'light_jar.png')
    cork = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = 8 * ((x * 7 + y * 13) % 3)
            cork.putpixel((x, y), (150 + v, 105 + v, 62 + v // 2, 255))
    save(cork, 'block', 'light_jar_lid.png')
    bars = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 5, 10, 15) or y in (0, 15):
                bars.putpixel((x, y), (58, 56, 62, 255) if (x + y) % 3 else (82, 80, 88, 255))
    save(bars, 'block', 'light_cage_bars.png')
    plate = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            plate.putpixel((x, y), (52, 50, 56, 255) if (x + y) % 4 else (72, 70, 78, 255))
    save(plate, 'block', 'light_cage_plate.png')
    for name, c in COLORS.items():
        core = Image.new('RGBA', (16, 16))
        r, g, b = rgb(c)
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5) / 8
                k = max(0.0, 1 - d)
                core.putpixel((x, y), (int(r + (255 - r) * k * 0.7), int(g + (255 - g) * k * 0.7), int(b + (255 - b) * k * 0.7), 255))
        save(core, 'block', f'light_core_{name}.png')


def box(f, t, tex, uv=None, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    return {'from': f, 'to': t, 'faces': {d: ({'uv': uv, 'texture': tex} if uv else {'texture': tex}) for d in faces}}


def models():
    jar_states, cage_states = {}, {}
    for name in COLORS:
        core = f'{NS}:block/light_core_{name}'
        w(os.path.join(A, 'models', 'block', f'light_jar_{name}.json'), {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
            'textures': {'glass': f'{NS}:block/light_jar', 'lid': f'{NS}:block/light_jar_lid', 'core': core, 'particle': f'{NS}:block/light_jar'},
            'elements': [box([6.5, 2, 6.5], [9.5, 5, 9.5], '#core', [4, 4, 12, 12]),
                         box([5, 0, 5], [11, 8, 11], '#glass'),
                         box([6, 8, 6], [10, 10, 10], '#lid', [4, 4, 12, 12])]})
        w(os.path.join(A, 'models', 'block', f'light_cage_{name}.json'), {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'bars': f'{NS}:block/light_cage_bars', 'plate': f'{NS}:block/light_cage_plate', 'core': core, 'particle': f'{NS}:block/light_cage_plate'},
            'elements': [box([6.5, 3, 6.5], [9.5, 6, 9.5], '#core', [4, 4, 12, 12]),
                         box([4, 0, 4], [12, 1, 12], '#plate', [4, 4, 12, 12]),
                         box([4, 9, 4], [12, 10, 12], '#plate', [4, 4, 12, 12]),
                         box([4, 1, 4], [12, 9, 12], '#bars', [0, 0, 16, 16], ('north', 'south', 'east', 'west')),
                         box([7, 10, 7], [9, 12, 9], '#plate', [7, 7, 9, 9])]})
        jar_states[f'color={name}'] = {'model': f'{NS}:block/light_jar_{name}'}
        cage_states[f'color={name}'] = {'model': f'{NS}:block/light_cage_{name}'}
    w(os.path.join(A, 'blockstates', 'light_jar.json'), {'variants': jar_states})
    w(os.path.join(A, 'blockstates', 'light_cage.json'), {'variants': cage_states})
    w(os.path.join(A, 'models', 'item', 'light_jar.json'), {'parent': f'{NS}:block/light_jar_amber'})
    w(os.path.join(A, 'models', 'item', 'light_cage.json'), {'parent': f'{NS}:block/light_cage_dusk'})
    w(os.path.join(A, 'models', 'item', 'thicket_light_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})


def data():
    w(os.path.join(D, 'loot_table', 'blocks', 'light_cage.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:iron_nugget',
                                                               'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}}]}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    w(os.path.join(D, 'neoforge', 'biome_modifier', 'thicket_light_spawns.json'), {
        'type': 'neoforge:add_spawns', 'biomes': 'minecraft:dark_forest',
        'spawners': {'type': f'{NS}:thicket_light', 'weight': 20, 'minCount': 2, 'maxCount': 4}})


if __name__ == '__main__':
    glow()
    blocks()
    models()
    data()
