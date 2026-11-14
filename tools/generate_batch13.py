#!/usr/bin/env python3
"""1.12.0: mushroom wood sets (from the crimson wood models), mushroom moss carpet, the Mycelinoid village
structure, and fixed Mushroom Shield / Club hand transforms. Vanilla model files are read from MCASSETS."""
import json
import os
import random
import sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import ROOT  # noqa: E402
from generate_batch8 import w, shaped, tag, NS, D, A, R  # noqa: E402

MC = os.environ.get('MCASSETS', '/home/user/mcassets') + '/assets/minecraft/'
SETS = {
    'red_mushroom': dict(base=(178, 58, 48), light=(206, 84, 66), dark=(132, 38, 33), seam=(96, 26, 24), fleck=(240, 226, 210),
                         cap='minecraft:red_mushroom_block'),
    'brown_mushroom': dict(base=(150, 108, 74), light=(174, 130, 92), dark=(118, 82, 56), seam=(86, 58, 40), fleck=(228, 208, 180),
                           cap='minecraft:brown_mushroom_block'),
    'blue_mushroom': dict(base=(66, 132, 196), light=(94, 160, 222), dark=(48, 100, 158), seam=(34, 70, 118), fleck=(212, 236, 250),
                          cap=NS + ':blue_mushroom_block'),
}
PARTS = ['planks', 'stairs', 'slab', 'fence', 'fence_gate', 'door', 'trapdoor', 'button', 'pressure_plate']


def tex(img, *path):
    p = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def planks_texture(c, rnd):
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        board = y // 4
        joint = (board * 5 + 3) % 16
        for x in range(16):
            col = [c['base'], c['light'], c['dark']][(x * 3 + y * 7 + rnd.randrange(3)) % 3 if rnd.random() < 0.55 else 0]
            if y % 4 == 3:
                col = c['seam']
            elif x == joint:
                col = c['dark']
            elif rnd.random() < 0.035:
                col = c['fleck']
            img.putpixel((x, y), col + (255,))
    return img


def door_textures(c, rnd):
    top = Image.new('RGBA', (16, 16))
    bottom = Image.new('RGBA', (16, 16))
    for img, is_top in ((top, True), (bottom, False)):
        for x in range(16):
            for y in range(16):
                col = c['base'] if (x + y * 3 + rnd.randrange(2)) % 3 else c['light']
                if x in (0, 15) or (is_top and y == 0) or (not is_top and y == 15) or x == 7 or x == 8:
                    col = c['dark']
                if y in (5, 10) and 0 < x < 15:
                    col = c['seam']
                if rnd.random() < 0.03:
                    col = c['fleck']
                img.putpixel((x, y), col + (255,))
        if is_top:
            # Two little round windows, like the gills of a cap.
            for cx in (4, 11):
                for x in range(cx - 2, cx + 2):
                    for y in range(2, 6):
                        if not ((x in (cx - 2, cx + 1)) and (y in (2, 5))):
                            img.putpixel((x, y), (0, 0, 0, 0))
    return top, bottom


def trapdoor_texture(c, rnd):
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            col = c['base'] if (x * 2 + y + rnd.randrange(2)) % 3 else c['light']
            if x in (0, 15) or y in (0, 15):
                col = c['dark']
            elif x in (5, 10) or y in (5, 10):
                col = c['seam']
            elif 1 <= x <= 14 and 1 <= y <= 14 and (x not in (5, 10)) and (y not in (5, 10)) and (x + y) % 2 == 0 and 2 <= x % 5 <= 3 and 2 <= y % 5 <= 3:
                col = None
            img.putpixel((x, y), (0, 0, 0, 0) if col is None else col + (255,))
    return img


def door_item(top, bottom):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    small_top = top.resize((8, 8), Image.NEAREST)
    small_bottom = bottom.resize((8, 8), Image.NEAREST)
    img.paste(small_top, (4, 0))
    img.paste(small_bottom, (4, 8))
    return img


def retarget(obj, prefix):
    text = json.dumps(obj)
    text = text.replace('minecraft:block/crimson_door_top', f'{NS}:block/{prefix}_door_top')
    text = text.replace('minecraft:block/crimson_door_bottom', f'{NS}:block/{prefix}_door_bottom')
    text = text.replace('minecraft:block/crimson_trapdoor', f'{NS}:block/{prefix}_trapdoor')
    text = text.replace('minecraft:block/crimson_', f'{NS}:block/{prefix}_')
    text = text.replace('minecraft:item/crimson_', f'{NS}:item/{prefix}_')
    return json.loads(text)


def wood_set(prefix, c):
    rnd = random.Random(prefix)
    tex(planks_texture(c, rnd), 'block', prefix + '_planks.png')
    top, bottom = door_textures(c, rnd)
    tex(top, 'block', prefix + '_door_top.png')
    tex(bottom, 'block', prefix + '_door_bottom.png')
    tex(trapdoor_texture(c, rnd), 'block', prefix + '_trapdoor.png')
    tex(door_item(top, bottom), 'item', prefix + '_door.png')
    for part in PARTS:
        state = json.load(open(MC + 'blockstates/crimson_' + part + '.json'))
        w(A + 'blockstates/' + prefix + '_' + part + '.json', retarget(state, prefix))
        item = json.load(open(MC + 'models/item/crimson_' + part + '.json'))
        w(A + 'models/item/' + prefix + '_' + part + '.json', retarget(item, prefix))
    for name in sorted(os.listdir(MC + 'models/block')):
        if not name.startswith('crimson_'):
            continue
        part = name[len('crimson_'):-5]
        if not any(part == p or part.startswith(p + '_') for p in PARTS) or part.startswith('planks_'):
            continue
        model = retarget(json.load(open(MC + 'models/block/' + name)), prefix)
        if part.startswith('door') or part.startswith('trapdoor'):
            model['render_type'] = 'minecraft:cutout'
        w(A + 'models/block/' + prefix + '_' + part + '.json', model)
    # Loot: themselves; a double slab two, a door only from its lower half.
    for part in PARTS:
        name = NS + ':' + prefix + '_' + part
        entry = {'type': 'minecraft:item', 'name': name}
        pool = {'rolls': 1, 'bonus_rolls': 0, 'entries': [entry], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}
        if part == 'slab':
            entry['functions'] = [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [
                {'condition': 'minecraft:block_state_property', 'block': name, 'properties': {'type': 'double'}}]},
                {'function': 'minecraft:explosion_decay'}]
            pool.pop('conditions')
        if part == 'door':
            pool['conditions'] = [{'condition': 'minecraft:block_state_property', 'block': name, 'properties': {'half': 'lower'}},
                                  {'condition': 'minecraft:survives_explosion'}]
        w(D + 'loot_table/blocks/' + prefix + '_' + part + '.json', {'type': 'minecraft:block', 'pools': [pool]})
    P = NS + ':' + prefix + '_planks'
    w(D + 'recipe/' + prefix + '_planks.json', {'type': 'minecraft:crafting_shapeless', 'category': 'building', 'group': 'planks',
                                                'ingredients': [{'item': c['cap']}], 'result': {'count': 4, 'id': P}})
    pk = {'P': P}
    shaped(prefix + '_stairs', ['P  ', 'PP ', 'PPP'], pk, 4)
    shaped(prefix + '_slab', ['PPP'], pk, 6)
    shaped(prefix + '_fence', ['PSP', 'PSP'], dict(pk, S='minecraft:stick'), 3)
    shaped(prefix + '_fence_gate', ['SPS', 'SPS'], dict(pk, S='minecraft:stick'))
    shaped(prefix + '_door', ['PP', 'PP', 'PP'], pk, 3)
    shaped(prefix + '_trapdoor', ['PPP', 'PPP'], pk, 2)
    shaped(prefix + '_pressure_plate', ['PP'], pk)
    w(D + 'recipe/' + prefix + '_button.json', {'type': 'minecraft:crafting_shapeless', 'category': 'redstone',
                                                'ingredients': [{'item': P}], 'result': {'count': 1, 'id': NS + ':' + prefix + '_button'}})
    tags = {'planks': 'planks', 'stairs': 'wooden_stairs', 'slab': 'wooden_slabs', 'fence': 'wooden_fences', 'fence_gate': 'fence_gates',
            'door': 'wooden_doors', 'trapdoor': 'wooden_trapdoors', 'button': 'wooden_buttons', 'pressure_plate': 'wooden_pressure_plates'}
    for part, t in tags.items():
        for kind in ('block', 'item'):
            tag(R + f'data/minecraft/tags/{kind}/{t}.json', [NS + ':' + prefix + '_' + part])
        tag(R + 'data/minecraft/tags/block/mineable/axe.json', [NS + ':' + prefix + '_' + part])


for prefix, colours in SETS.items():
    wood_set(prefix, colours)

# ---- Mushroom moss carpet (tinted like the moss)
faces = {f: {'uv': [0, 0, 16, 16] if f in ('up', 'down') else [0, 15, 16, 16], 'texture': '#moss', 'tintindex': 0}
         for f in ['north', 'south', 'east', 'west', 'up', 'down']}
faces['down']['cullface'] = 'down'
w(A + 'models/block/mushroom_moss_carpet.json', {'textures': {'moss': NS + ':block/mushroom_moss', 'particle': NS + ':block/mushroom_moss'},
                                                 'elements': [{'from': [0, 0, 0], 'to': [16, 1, 16], 'faces': faces}]})
w(A + 'blockstates/mushroom_moss_carpet.json', {'variants': {'': {'model': NS + ':block/mushroom_moss_carpet'}}})
w(A + 'models/item/mushroom_moss_carpet.json', {'parent': NS + ':block/mushroom_moss_carpet'})
w(D + 'loot_table/blocks/mushroom_moss_carpet.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
    {'type': 'minecraft:item', 'name': NS + ':mushroom_moss_carpet'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
shaped('mushroom_moss_carpet', ['MM'], {'M': NS + ':mushroom_moss'}, 3)
tag(R + 'data/minecraft/tags/block/mineable/hoe.json', [NS + ':mushroom_moss_carpet'])

# ---- The village structure
w(D + 'worldgen/structure/mycelinoid_village.json', {'type': NS + ':mycelinoid_village', 'biomes': '#' + NS + ':has_structure/mycelinoid_village',
                                                     'step': 'underground_structures', 'spawn_overrides': {}, 'terrain_adaptation': 'none'})
w(D + 'worldgen/structure_set/mycelinoid_villages.json', {'structures': [{'structure': NS + ':mycelinoid_village', 'weight': 1}],
                                                          'placement': {'type': 'minecraft:random_spread', 'spacing': 12, 'separation': 4,
                                                                        'salt': 736281517}})
w(D + 'tags/worldgen/biome/has_structure/mycelinoid_village.json', {'replace': False, 'values': [NS + ':mushroom_caves']})
w(D + 'loot_table/chests/mycelinoid_guardhouse.json', {'type': 'minecraft:chest', 'pools': [
    {'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 5}, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:item', 'name': 'minecraft:red_mushroom', 'weight': 10,
         'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 6}}]},
        {'type': 'minecraft:item', 'name': 'minecraft:mushroom_stem', 'weight': 8,
         'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}}]},
        {'type': 'minecraft:item', 'name': 'minecraft:red_mushroom_block', 'weight': 8,
         'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}}]},
        {'type': 'minecraft:item', 'name': 'minecraft:bone', 'weight': 6,
         'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 5}}]},
        {'type': 'minecraft:item', 'name': NS + ':mushroom_club', 'weight': 2,
         'functions': [{'function': 'minecraft:set_damage', 'damage': {'type': 'minecraft:uniform', 'min': 0.3, 'max': 0.9}}]},
        {'type': 'minecraft:item', 'name': NS + ':mushroom_shield', 'weight': 2,
         'functions': [{'function': 'minecraft:set_damage', 'damage': {'type': 'minecraft:uniform', 'min': 0.3, 'max': 0.9}}]}]}]})

# ---- Mushroom Shield and Club in hand
shield = A + 'models/item/mushroom_shield.json'
blocking = A + 'models/item/mushroom_shield_blocking.json'
held = {'rotation': [90, 0, 0], 'translation': [2, 2.5, 1], 'scale': [1, 1, 1]}
raised = {'rotation': [90, 0, 0], 'translation': [2, 3.5, 0], 'scale': [1, 1, 1]}
for path, third, first in ((shield, held, {'rotation': [0, 10, 0], 'translation': [3, 1, -3], 'scale': [0.8, 0.8, 0.8]}),
                           (blocking, raised, {'rotation': [0, 0, 0], 'translation': [-2, 3, -4], 'scale': [0.8, 0.8, 0.8]})):
    m = json.load(open(path))
    m['display'].update({'thirdperson_righthand': third, 'thirdperson_lefthand': third,
                         'firstperson_righthand': first, 'firstperson_lefthand': first})
    w(path, m)
club = A + 'models/item/mushroom_club.json'
m = json.load(open(club))
m['display'] = {'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.5, 0.5], 'scale': [1.0, 1.0, 1.0]},
                'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4.5, 0.5], 'scale': [1.0, 1.0, 1.0]}}
w(club, m)
print('batch 13 written')
