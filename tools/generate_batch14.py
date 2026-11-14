#!/usr/bin/env python3
"""1.13.0: Mushroom Cave ground (mycelium soil, cave mycelium, spore clay, mycelial mud), springy blue mushroom
variants, and the worldgen that spreads them."""
import json
import os
import random
import sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import ROOT  # noqa: E402
from generate_batch8 import w, tag, NS, D, A, R  # noqa: E402

rnd = random.Random(14)


def noise(base, palette, specks=(), speck_chance=0.0, seed=0):
    r = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            c = palette[r.randrange(len(palette))] if r.random() < 0.6 else base
            if specks and r.random() < speck_chance:
                c = specks[r.randrange(len(specks))]
            img.putpixel((x, y), c + (255,))
    return img


def save(img, *path):
    p = os.path.join(ROOT, *path)
    img.save(p)


# ---- ground
soil = noise((110, 82, 62), [(96, 70, 52), (124, 94, 72), (88, 64, 48)], [(206, 196, 220), (180, 168, 200)], 0.08, 1)
for i in range(5):  # pale mycelium threads
    x, y = rnd.randrange(16), rnd.randrange(16)
    for _ in range(5):
        soil.putpixel((x % 16, y % 16), (214, 204, 228, 255))
        x += rnd.choice((-1, 0, 1)); y += rnd.choice((-1, 0, 1))
save(soil, 'block', 'mycelium_soil.png')
top = noise((126, 96, 168), [(112, 84, 154), (142, 110, 186), (98, 74, 140)], [(196, 228, 250), (168, 210, 240)], 0.1, 2)
save(top, 'block', 'cave_mycelium_top.png')
side = soil.copy()
for x in range(16):
    depth = 3 + rnd.randrange(3)
    for y in range(depth):
        side.putpixel((x, y), top.getpixel((x, y)))
save(side, 'block', 'cave_mycelium_side.png')
save(noise((156, 150, 172), [(146, 140, 164), (166, 160, 182), (138, 132, 156)], [(196, 228, 250), (120, 112, 140)], 0.05, 3),
     'block', 'spore_clay.png')
save(noise((70, 52, 66), [(62, 46, 58), (80, 60, 76), (56, 40, 52)], [(150, 132, 170)], 0.06, 4), 'block', 'mycelial_mud.png')

# ---- blue mushroom variants, from the blue cap
blue = Image.open(os.path.join(ROOT, 'block', 'blue_mushroom_block.png')).convert('RGBA')
spotted = blue.copy()
for _ in range(7):
    x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
    for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1)][:rnd.choice([2, 3, 4])]:
        spotted.putpixel((x + dx, y + dy), (236, 246, 252, 255))
save(spotted, 'block', 'spotted_blue_mushroom_block.png')
glow = Image.new('RGBA', (16, 16))
for x in range(16):
    for y in range(16):
        r, g, b, a = blue.getpixel((x, y))
        glow.putpixel((x, y), (min(255, int(r * 1.25) + 30), min(255, int(g * 1.2) + 40), min(255, int(b * 1.1) + 30), 255))
for _ in range(10):
    glow.putpixel((rnd.randrange(16), rnd.randrange(16)), (240, 252, 255, 255))
save(glow, 'block', 'glowing_blue_mushroom_block.png')
gills = Image.new('RGBA', (16, 16))
for x in range(16):
    for y in range(16):
        dx, dy = x - 7.5, y - 7.5
        ring = int((dx * dx + dy * dy) ** 0.5)
        line = (int(abs(dx) * 3) + int(abs(dy) * 3)) % 3 == 0 or (x + y) % 4 == 0
        c = (86, 134, 196) if line else (148, 188, 226)
        if ring <= 1:
            c = (220, 230, 236)
        gills.putpixel((x, y), c + (255,))
save(gills, 'block', 'blue_mushroom_gills.png')

# ---- models, blockstates, items, loot
for name in ['mycelium_soil', 'spore_clay', 'mycelial_mud', 'spotted_blue_mushroom_block', 'glowing_blue_mushroom_block', 'blue_mushroom_gills']:
    w(A + 'models/block/' + name + '.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': NS + ':block/' + name}})
for name in ['mycelium_soil', 'spore_clay', 'mycelial_mud', 'spotted_blue_mushroom_block', 'glowing_blue_mushroom_block', 'blue_mushroom_gills', 'cave_mycelium']:
    w(A + 'blockstates/' + name + '.json', {'variants': {'': {'model': NS + ':block/' + name}}})
    w(A + 'models/item/' + name + '.json', {'parent': NS + ':block/' + name})
w(A + 'models/block/cave_mycelium.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': NS + ':block/cave_mycelium_top', 'side': NS + ':block/cave_mycelium_side', 'bottom': NS + ':block/mycelium_soil',
    'particle': NS + ':block/mycelium_soil'}})


def self_drop(name):
    w(D + 'loot_table/blocks/' + name + '.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:item', 'name': NS + ':' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


silk = {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [
    {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}
for name in ['mycelium_soil', 'spore_clay', 'mycelial_mud']:
    self_drop(name)
w(D + 'loot_table/blocks/cave_mycelium.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
    {'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'name': NS + ':cave_mycelium', 'conditions': [silk]},
        {'type': 'minecraft:item', 'name': NS + ':mycelium_soil', 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}]}]})
for name in ['spotted_blue_mushroom_block', 'glowing_blue_mushroom_block', 'blue_mushroom_gills']:
    w(D + 'loot_table/blocks/' + name + '.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': NS + ':' + name, 'conditions': [silk]},
            {'type': 'minecraft:item', 'name': NS + ':blue_mushroom', 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': -6, 'max': 2}},
                {'function': 'minecraft:limit_count', 'limit': {'min': 0}}, {'function': 'minecraft:explosion_decay'}]}]}]}]})

# ---- recipes: variants from the blue cap; planks from any blue cap block
def shapeless(name, ingredients, count=1):
    w(D + 'recipe/' + name + '.json', {'type': 'minecraft:crafting_shapeless', 'category': 'building',
                                       'ingredients': [{'tag': i[1:]} if i.startswith('#') else {'item': i} for i in ingredients],
                                       'result': {'count': count, 'id': NS + ':' + name}})


shapeless('spotted_blue_mushroom_block', [NS + ':blue_mushroom_block', 'minecraft:bone_meal'])
shapeless('glowing_blue_mushroom_block', [NS + ':blue_mushroom_block', 'minecraft:glowstone_dust'])
shapeless('blue_mushroom_gills', [NS + ':blue_mushroom_block', 'minecraft:string'])
shapeless('cave_mycelium', [NS + ':mycelium_soil', NS + ':mushroom_moss'])
shapeless('mycelial_mud', [NS + ':mycelium_soil', 'minecraft:water_bucket'])
tag(D + 'tags/item/blue_mushroom_blocks.json', [NS + ':blue_mushroom_block', NS + ':spotted_blue_mushroom_block',
                                                NS + ':glowing_blue_mushroom_block', NS + ':blue_mushroom_gills'])
p = D + 'recipe/blue_mushroom_planks.json'
d = json.load(open(p))
d['ingredients'] = [{'tag': NS + ':blue_mushroom_blocks'}]
w(p, d)

# ---- tags
tag(R + 'data/minecraft/tags/block/mineable/shovel.json', [NS + ':mycelium_soil', NS + ':cave_mycelium', NS + ':spore_clay', NS + ':mycelial_mud'])
tag(R + 'data/minecraft/tags/block/mineable/axe.json', [NS + ':spotted_blue_mushroom_block', NS + ':glowing_blue_mushroom_block', NS + ':blue_mushroom_gills'])
tag(R + 'data/minecraft/tags/block/mushroom_grow_block.json', [NS + ':mycelium_soil', NS + ':cave_mycelium', NS + ':mycelial_mud'])
tag(R + 'data/minecraft/tags/block/dirt.json', [NS + ':mycelium_soil', NS + ':cave_mycelium', NS + ':mycelial_mud'])
tag(D + 'tags/block/mushroom_cave_ground_replaceable.json', ['#minecraft:base_stone_overworld', NS + ':mushroom_moss', 'minecraft:dirt',
                                                             'minecraft:gravel', 'minecraft:clay'])

# ---- worldgen: patches of the new ground on cave floors (after the moss coat), mycelium with mushrooms
cf, pf = D + 'worldgen/configured_feature/', D + 'worldgen/placed_feature/'


def patch(name, block, depth, radius, vegetation, chance, surface='floor'):
    veg = {'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': {'type': 'minecraft:weighted_state_provider', 'entries': [
        {'weight': 3, 'data': {'Name': NS + ':blue_mushroom'}}, {'weight': 2, 'data': {'Name': 'minecraft:brown_mushroom'}},
        {'weight': 1, 'data': {'Name': NS + ':sourcecap'}}, {'weight': 1, 'data': {'Name': NS + ':mendcap'}}]}}}, 'placement': []} \
        if vegetation else {'feature': {'type': 'minecraft:no_op', 'config': {}}, 'placement': []}
    w(cf + name + '.json', {'type': 'minecraft:vegetation_patch', 'config': {
        'replaceable': '#' + NS + ':mushroom_cave_ground_replaceable',
        'ground_state': {'type': 'minecraft:simple_state_provider', 'state': {'Name': block}},
        'vegetation_feature': veg, 'surface': surface, 'depth': depth, 'extra_bottom_block_chance': 0.0,
        'vertical_range': 5, 'vegetation_chance': chance, 'xz_radius': {'type': 'minecraft:uniform', 'min_inclusive': radius[0], 'max_inclusive': radius[1]},
        'extra_edge_column_chance': 0.3}})
    scan = 'down' if surface == 'floor' else 'up'
    w(pf + name + '.json', {'feature': NS + ':' + name, 'placement': [
        {'type': 'minecraft:count', 'count': 18}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 0}, 'max_inclusive': {'absolute': 64}}},
        {'type': 'minecraft:environment_scan', 'direction_of_search': scan, 'max_steps': 12,
         'allowed_search_condition': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'},
         'target_condition': {'type': 'minecraft:solid'}},
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': 1 if surface == 'floor' else -1}, {'type': 'minecraft:biome'}]})


patch('cave_mycelium_patch', NS + ':cave_mycelium', 1, (3, 7), True, 0.12)
patch('mycelium_soil_patch', NS + ':mycelium_soil', {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 3}, (2, 5), False, 0.0)
patch('mycelial_mud_patch', NS + ':mycelial_mud', 2, (2, 4), False, 0.0)
patch('spore_clay_ceiling', NS + ':spore_clay', 2, (2, 4), False, 0.0, 'ceiling')
p = D + 'worldgen/biome/mushroom_caves.json'
d = json.load(open(p))
for f in [NS + ':mycelium_soil_patch', NS + ':mycelial_mud_patch', NS + ':spore_clay_ceiling', NS + ':cave_mycelium_patch']:
    if f not in d['features'][9]:
        d['features'][9].insert(0, f)
d['features'][9].remove(NS + ':cave_mycelium_patch')
d['features'][9].insert(3, NS + ':cave_mycelium_patch')
w(p, d)
print('batch 14 written')
