#!/usr/bin/env python3
"""1.11.0: Amanitoid (fly-agaric guardian), Mushroom Shield and Club, Spore Infection, mushroom-house furniture."""
import colorsys
import json
import os
import random
import sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save, ROOT  # noqa: E402
from generate_batch8 import w, shaped, tag, NS, D, A, R  # noqa: E402

rnd = random.Random(12)
# ---- Amanitoid texture: the Mycelinoid's, with a red, white-spotted fly-agaric cap.
src = Image.open(os.path.join(ROOT, 'entity', 'gribushka.png')).convert('RGBA')
img = src.copy()


def redden(x0, y0, x1, y1, spots):
    for x in range(x0, x1):
        for y in range(y0, y1):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            _, l, _ = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hls_to_rgb(0.0, 0.28 + l * 0.32, 0.78)
            img.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
    for _ in range(spots):
        sx, sy = rnd.randrange(x0 + 1, x1 - 2), rnd.randrange(y0 + 1, y1 - 2)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1)][:rnd.choice([1, 3, 4])]:
            img.putpixel((sx + dx, sy + dy), (244, 238, 224, 255))


redden(0, 0, 22, 22, 9)       # cap top and underside
redden(36, 14, 58, 20, 5)     # cap rim
img.save(os.path.join(ROOT, 'entity', 'amanitoid.png'))
print('wrote amanitoid.png')
w(A + 'models/item/amanitoid_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})

# ---- Spore Infection icon (18x18)
save(sprite([
    '..................', '.......aaaa.......', '.....aaAAAAaa.....', '....aAAoAAoAAa....', '....aAAAAAAAAa....',
    '.....aaaaaaaa.....', '.......cddc.......', '.......cddc.......', '..s....cddc...s...', '.s.s...cddc..s.s..',
    '..s...cddddc..s...', '......cccccc......', '...s..........s...', '..s.s........s.s..', '...s....s.....s...',
    '.......s.s........', '........s.........', '..................'],
    {'a': '#7A5A2A', 'A': '#C89A5A', 'o': '#F0E0B0', 'c': '#B8A890', 'd': '#E8DCC8', 's': '#D8C890'}), 'mob_effect', 'spore_infection.png')

# ---- Mushroom Club (handheld) and Shield
save(sprite([
    '..........rrrr..', '.........rRRwRr.', '........rRwRRRRr', '........rRRRRwRr', '.........rRRRRr.',
    '........sSsrrr..', '.......sSs......', '......sSs.......', '.....sSs........', '....sSs.........',
    '...sSs..........', '..sSs...........', '.bSs............', 'bbs.............', 'bb..............', '................'],
    {'r': '#8A1E18', 'R': '#C8302A', 'w': '#F4EEE0', 's': '#C8BCA0', 'S': '#EDE4CC', 'b': '#6B4A26'}), 'item', 'mushroom_club.png')
w(A + 'models/item/mushroom_club.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': NS + ':item/mushroom_club'}})
save(sprite([
    '....rrrrrrrr....', '..rrRRRwRRRRrr..', '.rRRwRRRRRRwRRr.', '.rRRRRRRwRRRRRr.', 'rRwRRRRRRRRRRwRr',
    'rRRRRRwRRRRRRRRr', '.rRRRRRRRRwRRRr.', '..rrrrrrrrrrrr..', '....sSSSSSSs....', '....sSSSSSSs....',
    '....sSSssSSs....', '....sSSSSSSs....', '.....sSSSSs.....', '......sSSs......', '.......ss.......', '................'],
    {'r': '#8A1E18', 'R': '#C8302A', 'w': '#F4EEE0', 's': '#B8AC90', 'S': '#EDE4CC'}), 'item', 'mushroom_shield.png')
shield_display = {
    'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [10, 6, -4], 'scale': [1, 1, 1]},
    'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [10, 6, 12], 'scale': [1, 1, 1]},
    'firstperson_righthand': {'rotation': [0, 180, 5], 'translation': [-10, 2, -10], 'scale': [1.25, 1.25, 1.25]},
    'firstperson_lefthand': {'rotation': [0, 180, 5], 'translation': [10, 0, -10], 'scale': [1.25, 1.25, 1.25]},
    'gui': {'rotation': [15, -25, -5], 'translation': [2, 3, 0], 'scale': [0.65, 0.65, 0.65]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [-2, 4, -5], 'scale': [0.5, 0.5, 0.5]},
    'ground': {'translation': [4, 4, 2], 'scale': [0.25, 0.25, 0.25]}}
blocking_display = dict(shield_display)
blocking_display.update({
    'thirdperson_righthand': {'rotation': [45, 135, 0], 'translation': [3.51, 11, -2], 'scale': [1, 1, 1]},
    'thirdperson_lefthand': {'rotation': [45, 135, 0], 'translation': [13.51, 3, 5], 'scale': [1, 1, 1]},
    'firstperson_righthand': {'rotation': [0, 180, -5], 'translation': [-15, 5, -11], 'scale': [1.25, 1.25, 1.25]},
    'firstperson_lefthand': {'rotation': [0, 180, -5], 'translation': [5, 5, -11], 'scale': [1.25, 1.25, 1.25]}})
faces = {f: {'uv': [0, 0, 16, 16], 'texture': '#s'} for f in ['north', 'south']}
faces.update({f: {'uv': [0, 0, 1, 16], 'texture': '#s'} for f in ['east', 'west']})
faces.update({f: {'uv': [0, 0, 16, 1], 'texture': '#s'} for f in ['up', 'down']})
shield_elements = [{'from': [-2, -2, 8], 'to': [14, 14, 9], 'faces': faces},
                   {'from': [5, 3, 9], 'to': [7, 9, 10], 'faces': {f: {'uv': [8, 9, 10, 13], 'texture': '#s'} for f in ['north', 'south', 'east', 'west', 'up', 'down']}}]
w(A + 'models/item/mushroom_shield.json', {'gui_light': 'front', 'textures': {'s': NS + ':item/mushroom_shield', 'particle': NS + ':item/mushroom_shield'},
                                           'elements': shield_elements, 'display': shield_display,
                                           'overrides': [{'predicate': {'blocking': 1}, 'model': NS + ':item/mushroom_shield_blocking'}]})
w(A + 'models/item/mushroom_shield_blocking.json', {'gui_light': 'front', 'textures': {'s': NS + ':item/mushroom_shield', 'particle': NS + ':item/mushroom_shield'},
                                                    'elements': shield_elements, 'display': blocking_display})
shaped('mushroom_shield', ['RSR', 'RRR', ' R '], {'R': 'minecraft:red_mushroom_block', 'S': 'minecraft:mushroom_stem'})
shaped('mushroom_club', [' R ', ' S ', ' S '], {'R': 'minecraft:red_mushroom_block', 'S': 'minecraft:mushroom_stem'})

# ---- Furniture
bm = A + 'models/block/'
red = 'minecraft:block/red_mushroom_block'
stem = 'minecraft:block/mushroom_stem'
inner = 'minecraft:block/mushroom_block_inside'


def cube(frm, to, side, top, bottom=None):
    bottom = bottom or top
    return {'from': frm, 'to': to, 'faces': {
        'north': {'texture': side}, 'south': {'texture': side}, 'east': {'texture': side}, 'west': {'texture': side},
        'up': {'texture': top}, 'down': {'texture': bottom}}}


w(bm + 'mushroom_table.json', {'parent': 'minecraft:block/block', 'textures': {'cap': red, 'stem': stem, 'inside': inner, 'particle': red},
                               'elements': [cube([0, 13, 0], [16, 16, 16], '#cap', '#cap', '#inside'), cube([5, 0, 5], [11, 13, 11], '#stem', '#stem')]})
w(bm + 'mushroom_stool.json', {'parent': 'minecraft:block/block', 'textures': {'cap': red, 'stem': stem, 'inside': inner, 'particle': red},
                               'elements': [cube([2, 6, 2], [14, 9, 14], '#cap', '#cap', '#inside'), cube([5, 0, 5], [11, 6, 11], '#stem', '#stem')]})
w(bm + 'mushroom_lamp.json', {'parent': 'minecraft:block/block', 'textures': {'glow': 'minecraft:block/shroomlight', 'stem': stem, 'particle': 'minecraft:block/shroomlight'},
                              'elements': [cube([3, 4, 3], [13, 9, 13], '#glow', '#glow'), cube([6, 9, 6], [10, 16, 10], '#stem', '#stem')]})
for name in ['mushroom_table', 'mushroom_stool', 'mushroom_lamp']:
    w(A + 'blockstates/' + name + '.json', {'variants': {'': {'model': NS + ':block/' + name}}})
    w(A + 'models/item/' + name + '.json', {'parent': NS + ':block/' + name})
    w(D + 'loot_table/blocks/' + name + '.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:item', 'name': NS + ':' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
tag(R + 'data/minecraft/tags/block/mineable/axe.json', [NS + ':mushroom_table', NS + ':mushroom_stool', NS + ':mushroom_lamp'])
shaped('mushroom_table', ['RRR', ' S ', ' S '], {'R': 'minecraft:red_mushroom_block', 'S': 'minecraft:mushroom_stem'})
shaped('mushroom_stool', ['RR', 'SS'], {'R': 'minecraft:red_mushroom_block', 'S': 'minecraft:mushroom_stem'}, 2)
shaped('mushroom_lamp', [' S ', 'RLR'], {'S': 'minecraft:mushroom_stem', 'R': 'minecraft:red_mushroom_block', 'L': 'minecraft:shroomlight'})

# ---- Loot: the guardian, the house barrels
w(D + 'loot_table/entities/amanitoid.json', {'type': 'minecraft:entity', 'pools': [
    {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:red_mushroom', 'functions': [
        {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}},
        {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]},
    {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:red_mushroom_block', 'functions': [
        {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}}]}]}]})
mush = lambda n, lo, hi, wgt: {'type': 'minecraft:item', 'name': n, 'weight': wgt, 'functions': [  # noqa: E731
    {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
w(D + 'loot_table/chests/mycelinoid_house.json', {'type': 'minecraft:chest', 'pools': [
    {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6}, 'bonus_rolls': 0, 'entries': [
        mush('minecraft:red_mushroom', 2, 6, 10), mush('minecraft:brown_mushroom', 2, 6, 10), mush(NS + ':blue_mushroom', 1, 4, 8),
        mush(NS + ':sourcecap', 1, 2, 3), mush(NS + ':mendcap', 1, 2, 3), mush(NS + ':swiftcap', 1, 2, 3), mush(NS + ':leapcap', 1, 2, 3),
        mush('minecraft:bone_meal', 2, 8, 8), mush('minecraft:glow_berries', 2, 6, 6), mush('minecraft:mushroom_stew', 1, 1, 4),
        mush(NS + ':bottle_of_source', 1, 2, 2), mush('minecraft:moss_block', 1, 4, 5)]}]})

# ---- Who cannot sneeze: no nose, no lungs.
w(D + 'tags/entity_type/cannot_sneeze.json', {'replace': False, 'values': [
    'minecraft:skeleton', 'minecraft:stray', 'minecraft:wither_skeleton', 'minecraft:bogged', 'minecraft:skeleton_horse',
    'minecraft:wither', 'minecraft:blaze', 'minecraft:breeze', 'minecraft:snow_golem', 'minecraft:iron_golem', 'minecraft:shulker',
    'minecraft:slime', 'minecraft:magma_cube', 'minecraft:vex', 'minecraft:allay', 'minecraft:ghast', 'minecraft:phantom',
    'minecraft:endermite', 'minecraft:silverfish', 'minecraft:guardian', 'minecraft:elder_guardian', 'minecraft:ender_dragon',
    'minecraft:armor_stand', 'minecraft:squid', 'minecraft:glow_squid', 'minecraft:cod', 'minecraft:salmon',
    'minecraft:tropical_fish', 'minecraft:pufferfish', 'minecraft:tadpole', 'minecraft:creeper', 'minecraft:warden',
    'minecraft:enderman', 'minecraft:bee', 'minecraft:spider', 'minecraft:cave_spider',
    NS + ':mudzya', NS + ':gribushka', NS + ':amanitoid']})
print('batch 12 written')
