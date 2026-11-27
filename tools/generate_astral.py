#!/usr/bin/env python3
"""1.41: the Astral - textures, block and item models, loot tables, tags, the dimension, its type and biome, the monolith
feature and the Ascension rite. Run from the repository root: python3 tools/generate_astral.py"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'arcane_artifacts')
D = os.path.join(ROOT, 'data', 'arcane_artifacts')
NS = 'arcane_artifacts'

# the Astral's palette: a pale haze, a pale plain, dark monoliths with cold light in their seams
FOG = (0xB9, 0xC3, 0xD3)
FLOOR = (0xA9, 0xB0, 0xBC)
STONE = (0x23, 0x27, 0x30)
GLOW = (0xD6, 0xF7, 0xFF)
GLOW_EDGE = (0x6F, 0xC4, 0xDC)


def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


def png(name, pixels):
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), pixels(x, y) + (255,))
    path = os.path.join(A, 'textures', 'block', name + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def textures():
    r = random.Random(41)
    floor_noise = [[r.randint(-5, 5) for _ in range(16)] for _ in range(16)]
    specks = {(r.randrange(16), r.randrange(16)) for _ in range(5)}
    png('astral_stone', lambda x, y: shade(FLOOR, 10 if (x, y) in specks else floor_noise[y][x]))

    columns = [r.randint(-4, 4) for _ in range(16)]
    grain = [[r.randint(-2, 2) for _ in range(16)] for _ in range(16)]
    flecks = {(r.randrange(16), r.randrange(16)) for _ in range(4)}

    def monolith(x, y):
        return shade(STONE, columns[x] + grain[y][x] + (9 if (x, y) in flecks else 0))

    png('astral_monolith', monolith)

    def vein(x, y):
        # a cross of cold light: seams run on through vertical columns, bands through horizontal rows
        if x in (7, 8) or y in (7, 8):
            return GLOW
        if x in (6, 9) or y in (6, 9):
            return GLOW_EDGE
        return shade(monolith(x, y), 6)

    png('astral_vein', vein)

    def gate_side(x, y):
        if 6 <= x <= 9 and 3 <= y <= 12 and (x in (7, 8) or y in (5, 10)):
            return GLOW
        return monolith(x, y)

    png('astral_gate_side', gate_side)

    def gate_top(x, y):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if d < 2.2:
            return GLOW
        if d < 3.4:
            return GLOW_EDGE
        return monolith(x, y)

    png('astral_gate_top', gate_top)


def blocks():
    for name in ('astral_stone', 'astral_monolith', 'astral_vein'):
        w(os.path.join(A, 'blockstates', name + '.json'), {'variants': {'': {'model': f'{NS}:block/{name}'}}})
        w(os.path.join(A, 'models', 'block', name + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{name}'}})
        w(os.path.join(A, 'models', 'item', name + '.json'), {'parent': f'{NS}:block/{name}'})
        w(os.path.join(D, 'loot_table', 'blocks', name + '.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    side, top = f'{NS}:block/astral_gate_side', f'{NS}:block/astral_gate_top'

    def box(f, t, uv_side, uv_top):
        faces = {d: {'uv': uv_side, 'texture': '#side'} for d in ('north', 'south', 'east', 'west')}
        faces['up'] = {'uv': uv_top, 'texture': '#top'}
        faces['down'] = {'uv': uv_top, 'texture': '#side'}
        return {'from': f, 'to': t, 'faces': faces}

    w(os.path.join(A, 'blockstates', 'astral_gate.json'), {'variants': {'': {'model': f'{NS}:block/astral_gate'}}})
    w(os.path.join(A, 'models', 'block', 'astral_gate.json'), {
        'parent': 'minecraft:block/block', 'textures': {'side': side, 'top': top, 'particle': side},
        'elements': [box([2, 0, 2], [14, 3, 14], [2, 13, 14, 16], [2, 2, 14, 14]),
                     box([5, 3, 5], [11, 13, 11], [5, 3, 11, 13], [5, 5, 11, 11]),
                     box([4, 13, 4], [12, 15, 12], [4, 1, 12, 3], [0, 0, 16, 16])]})
    w(os.path.join(A, 'models', 'item', 'astral_gate.json'), {'parent': f'{NS}:block/astral_gate'})


def tags():
    for tag, names in (('mineable/pickaxe', ['astral_stone', 'astral_monolith', 'astral_vein']),
                       ('needs_diamond_tool', ['astral_monolith', 'astral_vein'])):
        path = os.path.join(ROOT, 'data', 'minecraft', 'tags', 'block', tag + '.json')
        data = json.load(open(path)) if os.path.exists(path) else {'replace': False, 'values': []}
        for n in names:
            if f'{NS}:{n}' not in data['values']:
                data['values'].append(f'{NS}:{n}')
        w(path, data)


def world():
    color = FOG[0] << 16 | FOG[1] << 8 | FOG[2]
    w(os.path.join(D, 'dimension_type', 'astral.json'), {
        'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False,
        'has_raids': False, 'has_skylight': False, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.7,
        'fixed_time': 6000, 'logical_height': 512, 'min_y': 0, 'height': 512,
        'infiniburn': '#minecraft:infiniburn_overworld', 'effects': f'{NS}:astral',
        'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    w(os.path.join(D, 'worldgen', 'biome', 'astral.json'), {
        'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
        'effects': {'fog_color': color, 'sky_color': color, 'water_color': 0x9FD8E8, 'water_fog_color': 0x5A7F8C,
                    'particle': {'options': {'type': 'minecraft:white_ash'}, 'probability': 0.004},
                    'mood_sound': {'sound': 'minecraft:ambient.basalt_deltas.mood', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0},
                    'music': {'sound': 'minecraft:music.end', 'min_delay': 6000, 'max_delay': 18000, 'replace_current_music': True}},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {},
        'features': [[f'{NS}:astral_monolith']]})
    w(os.path.join(D, 'worldgen', 'configured_feature', 'astral_monolith.json'), {'type': f'{NS}:astral_monolith', 'config': {}})
    w(os.path.join(D, 'worldgen', 'placed_feature', 'astral_monolith.json'), {'feature': f'{NS}:astral_monolith', 'placement': []})
    w(os.path.join(D, 'dimension', 'astral.json'), {
        'type': f'{NS}:astral',
        'generator': {'type': 'minecraft:flat', 'settings': {
            'biome': f'{NS}:astral', 'features': True, 'lakes': False, 'structure_overrides': [],
            'layers': [{'block': 'minecraft:bedrock', 'height': 1}, {'block': f'{NS}:astral_stone', 'height': 63}]}}})
    # the Ascension: Gate north, Fence south, Eye west and east; an eye of ender, then an echo shard at the Heart's signal
    w(os.path.join(D, 'arcane_rites', 'ascend.json'), {
        'kind': 'ascend', 'main': 'vrata', 'south': 'ograda', 'west': 'oko', 'east': 'oko', 'min_field': 9,
        'offering': {'item': 'minecraft:ender_eye', 'count': 2},
        'extra_offerings': [{'item': 'minecraft:echo_shard', 'count': 1}]})


if __name__ == '__main__':
    textures()
    blocks()
    tags()
    world()
