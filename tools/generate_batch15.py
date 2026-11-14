#!/usr/bin/env python3
"""1.14.0: cave plants, zoned mushroom patches (fewer), a shield shaped and held like the vanilla one."""
import json
import os
import random
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save  # noqa: E402
from generate_batch8 import w, tag, NS, D, A, R  # noqa: E402

# ---- plants
save(sprite([
    '................', '................', '................', '................', '................', '................',
    '.......a........', '..a....A...a....', '..A.a..A..aA....', '...AA..A.A.A..a.', '.a.AA.aAAA.A.aA.',
    '.AA.A.AA.AAA.AA.', '..AAAAA..AAAAA..', '...AAAAAAAAAA...', '....bAAbbAAb....', '....bbbbbbbb....'],
    {'a': '#E0D0F8', 'A': '#9A78C8', 'b': '#6A4E8E'}), 'block', 'spore_tuft.png')
save(sprite([
    '................', '................', '................', '................', '................', '.....o....o.....',
    '....oOo..oOo....', '.....s....s..o..', '..o..s....s.oOo.', '.oOo.s...s...s..', '..s..s..s....s..',
    '...s.s.s....s...', '....sss....s....', '.....ss...s.....', '......sssss.....', '.......sss......'],
    {'o': '#9FE8FF', 'O': '#F0FCFF', 's': '#6AA8C8'}), 'block', 'glowsprout.png')
save(sprite([
    '.hh.hhh..hh.hhh.', '.hh..hh..hh..hh.', '..h..hh...h..h..', '..h...h...h..h..', '..h...h..h...h..', '..h...h..h...h..',
    '...h..h..h..h...', '...h..h..h..h...', '...h.....h..h...', '...h.....h......', '...o.....h......', '..oOo....o......',
    '...o....oOo.....', '..........o.....', '................', '................'],
    {'h': '#D8D0E8', 'o': '#B8E8F8', 'O': '#F0FCFF'}), 'block', 'hanging_hyphae.png')
for name in ['spore_tuft', 'glowsprout', 'hanging_hyphae']:
    w(A + 'models/block/' + name + '.json', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout',
                                             'textures': {'cross': NS + ':block/' + name}})
    w(A + 'blockstates/' + name + '.json', {'variants': {'': {'model': NS + ':block/' + name}}})
    w(A + 'models/item/' + name + '.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS + ':block/' + name}})
    w(D + 'loot_table/blocks/' + name + '.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:item', 'name': NS + ':' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
tag(R + 'data/minecraft/tags/block/replaceable_by_trees.json', [NS + ':spore_tuft', NS + ':glowsprout', NS + ':hanging_hyphae'])

cf, pf = D + 'worldgen/configured_feature/', D + 'worldgen/placed_feature/'
scan_down = {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 12,
             'allowed_search_condition': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}, 'target_condition': {'type': 'minecraft:solid'}}
scan_up = dict(scan_down, direction_of_search='up')
height = {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 8},
                                                        'max_inclusive': {'absolute': 56}}}


def plant_patch(name, block, tries, count, ceiling=False):
    w(cf + name + '.json', {'type': 'minecraft:random_patch', 'config': {'tries': tries, 'xz_spread': 6, 'y_spread': 2, 'feature': {
        'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': {'type': 'minecraft:simple_state_provider', 'state': {'Name': block}}}},
        'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
            {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'},
            {'type': 'minecraft:would_survive', 'state': {'Name': block}}]}}]}}})
    w(pf + name + '.json', {'feature': NS + ':' + name, 'placement': [
        {'type': 'minecraft:count', 'count': count}, {'type': 'minecraft:in_square'}, height, scan_up if ceiling else scan_down,
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': -1 if ceiling else 1}, {'type': 'minecraft:biome'}]})


plant_patch('patch_spore_tuft', NS + ':spore_tuft', 40, 6)
plant_patch('patch_glowsprout', NS + ':glowsprout', 20, 3)
plant_patch('patch_hanging_hyphae', NS + ':hanging_hyphae', 30, 5, ceiling=True)

# ---- mushrooms: one zoned patch feature instead of eleven, and fewer of them
w(cf + 'zoned_mushroom_patch.json', {'type': NS + ':zoned_mushroom_patch', 'config': {}})
w(pf + 'zoned_mushroom_patch.json', {'feature': NS + ':zoned_mushroom_patch', 'placement': [
    {'type': 'minecraft:count', 'count': 8}, {'type': 'minecraft:in_square'}, height, scan_down,
    {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': 1}, {'type': 'minecraft:biome'}]})
huge = pf + 'huge_blue_mushroom.json'
d = json.load(open(huge))
d['placement'][0]['count'] = 6
w(huge, d)
biome = D + 'worldgen/biome/mushroom_caves.json'
d = json.load(open(biome))
old = [f for f in d['features'][9] if f.startswith(NS + ':patch_')]
for f in old:
    d['features'][9].remove(f)
    os.remove(cf + f.split(':')[1] + '.json')
    os.remove(pf + f.split(':')[1] + '.json')
for f in [NS + ':zoned_mushroom_patch', NS + ':patch_spore_tuft', NS + ':patch_glowsprout', NS + ':patch_hanging_hyphae']:
    if f not in d['features'][9]:
        d['features'][9].append(f)
w(biome, d)
print('removed', len(old), 'old mushroom patches')

# ---- the Mushroom Shield: the vanilla shield's shape and place in the hand
faces = lambda tex_uv: {f: {'uv': tex_uv, 'texture': '#s'} for f in ['north', 'south']}  # noqa: E731
plate = {'from': [-7, -7, 1], 'to': [7, 7, 2], 'faces': dict(faces([1, 1, 15, 15]), **{
    f: {'uv': [0, 7, 1, 8], 'texture': '#s'} for f in ['east', 'west', 'up', 'down']})}
handle = {'from': [-1, -3, -5], 'to': [1, 3, 1], 'faces': {f: {'uv': [7, 9, 9, 13], 'texture': '#s'}
                                                            for f in ['north', 'south', 'east', 'west', 'up', 'down']}}
held = {
    'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [10, 6, -4], 'scale': [1, 1, 1]},
    'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [10, 6, 12], 'scale': [1, 1, 1]},
    'firstperson_righthand': {'rotation': [0, 180, 5], 'translation': [-10, 2, -10], 'scale': [1.25, 1.25, 1.25]},
    'firstperson_lefthand': {'rotation': [0, 180, 5], 'translation': [10, 0, -10], 'scale': [1.25, 1.25, 1.25]},
    'gui': {'rotation': [15, -25, -5], 'translation': [2, 3, 0], 'scale': [0.65, 0.65, 0.65]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [-2, 4, -5], 'scale': [0.5, 0.5, 0.5]},
    'ground': {'translation': [4, 4, 2], 'scale': [0.25, 0.25, 0.25]}}
blocking = dict(held)
blocking.update({
    'thirdperson_righthand': {'rotation': [45, 135, 0], 'translation': [3.51, 11, -2], 'scale': [1, 1, 1]},
    'thirdperson_lefthand': {'rotation': [45, 135, 0], 'translation': [13.51, 3, 5], 'scale': [1, 1, 1]},
    'firstperson_righthand': {'rotation': [0, 180, -5], 'translation': [-15, 5, -11], 'scale': [1.25, 1.25, 1.25]},
    'firstperson_lefthand': {'rotation': [0, 180, -5], 'translation': [5, 5, -11], 'scale': [1.25, 1.25, 1.25]}})
tex = {'s': NS + ':item/mushroom_shield', 'particle': NS + ':item/mushroom_shield'}
w(A + 'models/item/mushroom_shield.json', {'gui_light': 'front', 'textures': tex, 'elements': [plate, handle], 'display': held,
                                           'overrides': [{'predicate': {'blocking': 1}, 'model': NS + ':item/mushroom_shield_blocking'}]})
w(A + 'models/item/mushroom_shield_blocking.json', {'gui_light': 'front', 'textures': tex, 'elements': [plate, handle], 'display': blocking})
print('batch 15 written')
