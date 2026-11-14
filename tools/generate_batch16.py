#!/usr/bin/env python3
"""1.15.0: vanilla ground in the Mushroom Caves (dirt, coarse dirt, mud, mycelium), vanilla huge mushrooms,
fewer small mushrooms, more plants."""
import json
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_batch8 import w, NS, D  # noqa: E402

cf, pf = D + 'worldgen/configured_feature/', D + 'worldgen/placed_feature/'
scan_down = {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 12,
             'allowed_search_condition': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}, 'target_condition': {'type': 'minecraft:solid'}}
height = {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 8},
                                                        'max_inclusive': {'absolute': 56}}}


def placed(name, count, extra=None):
    w(pf + name + '.json', {'feature': extra or (NS + ':' + name), 'placement': [
        {'type': 'minecraft:count', 'count': count}, {'type': 'minecraft:in_square'}, height, scan_down,
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': 1}, {'type': 'minecraft:biome'}]})


def ground(name, block, depth, radius, vegetation, chance, count):
    if vegetation:
        veg = {'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': {'type': 'minecraft:weighted_state_provider', 'entries': [
            {'weight': 4, 'data': {'Name': NS + ':spore_tuft'}}, {'weight': 2, 'data': {'Name': 'minecraft:brown_mushroom'}},
            {'weight': 1, 'data': {'Name': 'minecraft:red_mushroom'}}, {'weight': 1, 'data': {'Name': NS + ':glowsprout'}}]}}}, 'placement': []}
    else:
        veg = {'feature': {'type': 'minecraft:no_op', 'config': {}}, 'placement': []}
    w(cf + name + '.json', {'type': 'minecraft:vegetation_patch', 'config': {
        'replaceable': '#' + NS + ':mushroom_cave_ground_replaceable',
        'ground_state': {'type': 'minecraft:simple_state_provider', 'state': {'Name': block}},
        'vegetation_feature': veg, 'surface': 'floor', 'depth': depth, 'extra_bottom_block_chance': 0.0, 'vertical_range': 5,
        'vegetation_chance': chance, 'xz_radius': {'type': 'minecraft:uniform', 'min_inclusive': radius[0], 'max_inclusive': radius[1]},
        'extra_edge_column_chance': 0.3}})
    placed(name, count)


ground('dirt_patch', 'minecraft:dirt', {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 3}, (2, 5), True, 0.15, 6)
ground('coarse_dirt_patch', 'minecraft:coarse_dirt', 2, (2, 4), False, 0.0, 5)
ground('mud_patch', 'minecraft:mud', 2, (2, 4), False, 0.0, 4)
ground('mycelium_patch', 'minecraft:mycelium', 1, (3, 6), True, 0.3, 6)

# Vanilla huge red and brown mushrooms, on the mycelium, dirt and moss.
placed('cave_huge_red_mushroom', 3, 'minecraft:huge_red_mushroom')
placed('cave_huge_brown_mushroom', 3, 'minecraft:huge_brown_mushroom')

# Fewer small mushrooms and blue giants; more plants.
for name, count in (('zoned_mushroom_patch', 4), ('huge_blue_mushroom', 4), ('patch_spore_tuft', 14), ('patch_glowsprout', 6),
                    ('patch_hanging_hyphae', 10)):
    p = pf + name + '.json'
    d = json.load(open(p))
    d['placement'][0]['count'] = count
    w(p, d)
p = cf + 'zoned_mushroom_patch.json'

biome = D + 'worldgen/biome/mushroom_caves.json'
d = json.load(open(biome))
veg = d['features'][9]
for old in [NS + ':mycelium_soil_patch', NS + ':mycelial_mud_patch', NS + ':cave_mycelium_patch']:
    if old in veg:
        veg.remove(old)
start = [NS + ':dirt_patch', NS + ':coarse_dirt_patch', NS + ':mud_patch', NS + ':mycelium_patch']
for i, f in enumerate(start):
    if f not in veg:
        veg.insert(1 + i, f)
for f in [NS + ':cave_huge_red_mushroom', NS + ':cave_huge_brown_mushroom']:
    if f not in veg:
        veg.insert(veg.index(NS + ':huge_blue_mushroom') + 1, f)
w(biome, d)
print(veg)
