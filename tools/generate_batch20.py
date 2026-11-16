#!/usr/bin/env python3
"""1.19.3: Spore Clay bricks (textures drawn by the author): raw bricks (crafted from 4 spore clay), bricks (raw
bricks fired in a furnace) with stairs, slab and wall, brick tiles, chiseled bricks and polished spore clay.
Vanilla mud brick models are the templates (read from MCASSETS).

Usage: python3 tools/generate_batch20.py <folder with spore_bricks.png, chiseled_spore_bricks.png,
       spore_brick_tiles.png, polished_spore_clay.png, raw_spore_bricks.png>
"""
import json
import os
import sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from generate_batch8 import w, shaped, tag, NS, D, A, R  # noqa: E402

MC = os.environ.get('MCASSETS', '/home/user/mcassets') + '/assets/minecraft/'
SRC = sys.argv[1]
CUBES = ['raw_spore_bricks', 'spore_bricks', 'spore_brick_tiles', 'chiseled_spore_bricks', 'polished_spore_clay']

# ---- textures (a 16x15 drawing gets its last row repeated)
for name in CUBES:
    img = Image.open(os.path.join(SRC, name + '.png')).convert('RGBA')
    if img.size != (16, 16):
        full = Image.new('RGBA', (16, 16))
        full.paste(img, (0, 0))
        for y in range(img.height, 16):
            for x in range(16):
                full.putpixel((x, y), img.getpixel((x, img.height - 1)))
        img = full
    img.save(A + 'textures/block/' + name + '.png')


def retarget(obj):
    return json.loads(json.dumps(obj).replace('minecraft:block/mud_brick', NS + ':block/spore_brick')
                      .replace('minecraft:block/mud_bricks', NS + ':block/spore_bricks'))


# ---- cubes
for name in CUBES:
    w(A + 'blockstates/' + name + '.json', {'variants': {'': {'model': NS + ':block/' + name}}})
    w(A + 'models/block/' + name + '.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': NS + ':block/' + name}})
    w(A + 'models/item/' + name + '.json', {'parent': NS + ':block/' + name})

# ---- stairs, slab, wall from the mud brick ones
for part in ('stairs', 'slab', 'wall'):
    state = json.load(open(MC + 'blockstates/mud_brick_' + part + '.json'))
    w(A + 'blockstates/spore_brick_' + part + '.json', retarget(state))
    item = json.load(open(MC + 'models/item/mud_brick_' + part + '.json'))
    w(A + 'models/item/spore_brick_' + part + '.json', retarget(item))
for name in sorted(os.listdir(MC + 'models/block')):
    if name.startswith('mud_brick_'):
        model = json.load(open(MC + 'models/block/' + name))
        model = json.loads(json.dumps(model).replace('minecraft:block/mud_bricks', NS + ':block/spore_bricks'))
        w(A + 'models/block/spore_brick_' + name[len('mud_brick_'):], model)

# ---- loot
for name in CUBES + ['spore_brick_stairs', 'spore_brick_wall']:
    w(D + 'loot_table/blocks/' + name + '.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0,
        'entries': [{'type': 'minecraft:item', 'name': NS + ':' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
slab = NS + ':spore_brick_slab'
w(D + 'loot_table/blocks/spore_brick_slab.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
    {'type': 'minecraft:item', 'name': slab, 'functions': [
        {'function': 'minecraft:set_count', 'count': 2, 'add': False,
         'conditions': [{'condition': 'minecraft:block_state_property', 'block': slab, 'properties': {'type': 'double'}}]},
        {'function': 'minecraft:explosion_decay'}]}]}]})

# ---- recipes
CLAY, RAW, BRICKS = NS + ':spore_clay', NS + ':raw_spore_bricks', NS + ':spore_bricks'
shaped('raw_spore_bricks', ['CC', 'CC'], {'C': CLAY}, 4)
w(D + 'recipe/spore_bricks.json', {'type': 'minecraft:smelting', 'category': 'blocks', 'ingredient': {'item': RAW},
                                  'result': {'count': 1, 'id': BRICKS}, 'experience': 0.1, 'cookingtime': 200})
w(D + 'recipe/polished_spore_clay.json', {'type': 'minecraft:smelting', 'category': 'blocks', 'ingredient': {'item': BRICKS},
                                         'result': {'count': 1, 'id': NS + ':polished_spore_clay'}, 'experience': 0.1, 'cookingtime': 200})
shaped('spore_brick_tiles', ['BB', 'BB'], {'B': BRICKS}, 4)
shaped('chiseled_spore_bricks', ['S', 'S'], {'S': slab})
shaped('spore_brick_stairs', ['B  ', 'BB ', 'BBB'], {'B': BRICKS}, 4)
shaped('spore_brick_slab', ['BBB'], {'B': BRICKS}, 6)
shaped('spore_brick_wall', ['BBB', 'BBB'], {'B': BRICKS}, 6)
for result, count in (('spore_brick_tiles', 1), ('chiseled_spore_bricks', 1), ('polished_spore_clay', 1),
                      ('spore_brick_stairs', 1), ('spore_brick_slab', 2), ('spore_brick_wall', 1)):
    w(D + f'recipe/{result}_from_spore_bricks_stonecutting.json', {'type': 'minecraft:stonecutting', 'ingredient': {'item': BRICKS},
                                                                 'result': {'count': count, 'id': NS + ':' + result}})

# ---- tags
ALL = [NS + ':' + n for n in CUBES + ['spore_brick_stairs', 'spore_brick_slab', 'spore_brick_wall']]
tag(R + 'data/minecraft/tags/block/mineable/pickaxe.json', ALL)
for kind in ('block', 'item'):
    tag(R + f'data/minecraft/tags/{kind}/stairs.json', [NS + ':spore_brick_stairs'])
    tag(R + f'data/minecraft/tags/{kind}/slabs.json', [slab])
    tag(R + f'data/minecraft/tags/{kind}/walls.json', [NS + ':spore_brick_wall'])
print('spore bricks written')
