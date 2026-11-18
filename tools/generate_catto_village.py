#!/usr/bin/env python3
"""1.22.0: the village pieces where village Cattos live (a market stall, a storeroom and a junk pile, each with a Catto
Cushion), written as structure NBT files, plus the cushion's block assets.

Each piece faces north: the jigsaw block at the middle of the front row (y = 0) joins it to a village street, like the
vanilla houses ("minecraft:building_entrance").

Usage: python3 tools/generate_catto_village.py
"""
import gzip
import io
import json
import os
import struct

from PIL import Image

NS = 'arcane_artifacts'
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', NS)
D = os.path.join(ROOT, 'data', NS)
DATA_VERSION = 3955  # 1.21.1

# ---------------------------------------------------------------------- a tiny NBT writer

TAG_END, TAG_BYTE, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 1, 3, 8, 9, 10


class Int(int):
    pass


def _name(out, name):
    data = name.encode('utf-8')
    out.write(struct.pack('>H', len(data)))
    out.write(data)


def _tag_type(value):
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, list):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError(value)


def _payload(out, value):
    t = _tag_type(value)
    if t == TAG_BYTE:
        out.write(struct.pack('>b', 1 if value else 0))
    elif t == TAG_INT:
        out.write(struct.pack('>i', value))
    elif t == TAG_STRING:
        _name(out, value)
    elif t == TAG_LIST:
        inner = _tag_type(value[0]) if value else TAG_END
        out.write(struct.pack('>bi', inner, len(value)))
        for v in value:
            _payload(out, v)
    else:
        for k, v in value.items():
            out.write(struct.pack('>b', _tag_type(v)))
            _name(out, k)
            _payload(out, v)
        out.write(struct.pack('>b', TAG_END))


def write_nbt(path, root):
    buf = io.BytesIO()
    buf.write(struct.pack('>b', TAG_COMPOUND))
    _name(buf, '')
    _payload(buf, root)
    with gzip.open(path, 'wb') as f:
        f.write(buf.getvalue())


# ---------------------------------------------------------------------- building helpers

class Piece:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.nbt = {}

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())))
        if nbt is not None:
            self.nbt[(x, y, z)] = nbt
        else:
            self.nbt.pop((x, y, z), None)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, name, props)

    def jigsaw(self, x, z):
        self.set(x, 0, z, 'minecraft:jigsaw', {'orientation': 'north_up'}, {
            'id': 'minecraft:jigsaw', 'name': 'minecraft:building_entrance', 'target': 'minecraft:empty',
            'pool': 'minecraft:empty', 'final_state': 'minecraft:dirt_path', 'joint': 'rollable',
            'placement_priority': 0, 'selection_priority': 0})

    def save(self, path):
        palette = []
        index = {}
        blocks = []
        sx, sy, sz = self.size
        for x in range(sx):
            for y in range(sy):
                for z in range(sz):
                    key = self.blocks.get((x, y, z), ('minecraft:air', ()))
                    if key not in index:
                        index[key] = len(palette)
                        entry = {'Name': key[0]}
                        if key[1]:
                            entry['Properties'] = dict(key[1])
                        palette.append(entry)
                    block = {'pos': [x, y, z], 'state': index[key]}
                    if (x, y, z) in self.nbt:
                        block['nbt'] = self.nbt[(x, y, z)]
                    blocks.append(block)
        write_nbt(path, {'DataVersion': DATA_VERSION, 'size': [sx, sy, sz], 'palette': palette, 'blocks': blocks, 'entities': []})


CUSHION = NS + ':catto_cushion'


def stall():
    """A market stall: plank floor, a counter with barrels, fence posts and a striped awning; a cushion behind the counter."""
    p = Piece(7, 6, 6)
    p.fill(0, 0, 0, 6, 0, 5, 'minecraft:grass_block')
    p.fill(1, 0, 1, 5, 0, 5, 'minecraft:spruce_planks')
    p.jigsaw(3, 0)
    for x, z in ((1, 1), (5, 1), (1, 5), (5, 5)):
        p.fill(x, 1, z, x, 3, z, 'minecraft:spruce_fence')
    # the counter
    for x in range(2, 5):
        p.set(x, 1, 2, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    p.set(2, 2, 2, 'minecraft:spruce_slab', {'type': 'bottom', 'waterlogged': 'false'})
    p.set(4, 2, 2, 'minecraft:flower_pot')
    p.set(3, 2, 2, 'minecraft:spruce_slab', {'type': 'bottom', 'waterlogged': 'false'})
    # awning in red and white stripes
    for x in range(0, 7):
        wool = 'minecraft:red_wool' if x % 2 == 0 else 'minecraft:white_wool'
        p.fill(x, 4, 1, x, 4, 5, wool)
    p.set(3, 3, 3, 'minecraft:lantern', {'hanging': 'true', 'waterlogged': 'false'})
    # behind the counter: goods and the cushion
    p.set(1, 1, 4, 'minecraft:hay_block', {'axis': 'y'})
    p.set(5, 1, 4, 'minecraft:barrel', {'facing': 'north', 'open': 'false'})
    p.set(5, 2, 4, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    p.set(3, 1, 4, CUSHION)
    return p


def storage():
    """A storeroom: a lean-to shed with a log frame, stacked barrels and crates, a cushion on the floor."""
    p = Piece(7, 6, 7)
    p.fill(0, 0, 0, 6, 0, 6, 'minecraft:grass_block')
    p.fill(1, 0, 1, 5, 0, 5, 'minecraft:cobblestone')
    p.jigsaw(3, 0)
    for x, z in ((1, 1), (5, 1), (1, 5), (5, 5)):
        p.fill(x, 1, z, x, 3, z, 'minecraft:spruce_log', {'axis': 'y'})
    p.fill(2, 1, 5, 4, 3, 5, 'minecraft:spruce_planks')
    p.fill(1, 1, 2, 1, 3, 4, 'minecraft:spruce_planks')
    p.fill(5, 1, 2, 5, 3, 4, 'minecraft:spruce_planks')
    p.fill(1, 4, 1, 5, 4, 5, 'minecraft:spruce_slab', {'type': 'bottom', 'waterlogged': 'false'})
    for x, y in ((2, 1), (3, 1), (2, 2), (4, 1)):
        p.set(x, y, 4, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    p.set(4, 1, 3, 'minecraft:chest', {'facing': 'west', 'type': 'single', 'waterlogged': 'false'})
    p.set(2, 1, 2, 'minecraft:composter', {'level': '0'})
    p.set(3, 1, 3, CUSHION)
    p.set(3, 3, 2, 'minecraft:lantern', {'hanging': 'true', 'waterlogged': 'false'})
    return p


def junk():
    """A junk pile: coarse dirt, a broken cart of barrels, an old cauldron, composters, cobwebs and odds and ends."""
    p = Piece(7, 4, 6)
    p.fill(0, 0, 0, 6, 0, 5, 'minecraft:grass_block')
    for x, z in ((1, 1), (2, 1), (4, 1), (1, 3), (2, 4), (3, 4), (5, 2), (5, 4), (4, 3)):
        p.set(x, 0, z, 'minecraft:coarse_dirt')
    p.jigsaw(3, 0)
    p.set(1, 1, 3, 'minecraft:cauldron')
    p.set(1, 1, 4, 'minecraft:composter', {'level': '3'})
    p.set(5, 1, 4, 'minecraft:barrel', {'facing': 'east', 'open': 'true'})
    p.set(5, 2, 4, 'minecraft:oak_trapdoor', {'facing': 'south', 'half': 'bottom', 'open': 'false', 'powered': 'false', 'waterlogged': 'false'})
    p.set(5, 1, 3, 'minecraft:barrel', {'facing': 'up', 'open': 'false'})
    p.set(4, 1, 4, 'minecraft:cobweb')
    p.set(2, 1, 4, 'minecraft:bone_block', {'axis': 'x'})
    p.set(4, 1, 2, 'minecraft:oak_fence')
    p.set(1, 1, 1, 'minecraft:flower_pot')
    p.set(2, 1, 2, 'minecraft:hay_block', {'axis': 'x'})
    p.set(3, 1, 3, CUSHION)
    return p


# ---------------------------------------------------------------------- the cushion's assets

def cushion_assets():
    os.makedirs(os.path.join(A, 'textures', 'block'), exist_ok=True)
    top = Image.new('RGBA', (16, 16))
    side = Image.new('RGBA', (16, 16))
    base, dark, light, paw = (128, 70, 160), (92, 46, 120), (170, 112, 200), (232, 196, 90)
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            top.putpixel((x, y), (dark if edge else (light if (x + y) % 5 == 0 else base)) + (255,))
            side.putpixel((x, y), (dark if y in (0, 15) else base) + (255,))
    for (x, y) in [(7, 8), (8, 8), (7, 9), (8, 9), (6, 10), (9, 10), (5, 6), (7, 5), (8, 5), (10, 6)]:
        top.putpixel((x, y), paw + (255,))
    top.save(os.path.join(A, 'textures', 'block', 'catto_cushion_top.png'))
    side.save(os.path.join(A, 'textures', 'block', 'catto_cushion_side.png'))

    def w(path, data):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            json.dump(data, f, indent=2)
            f.write('\n')
    w(os.path.join(A, 'blockstates', 'catto_cushion.json'), {'variants': {'': {'model': NS + ':block/catto_cushion'}}})
    w(os.path.join(A, 'models', 'block', 'catto_cushion.json'), {
        'parent': 'minecraft:block/block',
        'textures': {'top': NS + ':block/catto_cushion_top', 'side': NS + ':block/catto_cushion_side', 'particle': NS + ':block/catto_cushion_top'},
        'elements': [{'from': [1, 0, 1], 'to': [15, 3, 15], 'faces': {
            'up': {'texture': '#top', 'uv': [1, 1, 15, 15]}, 'down': {'texture': '#side', 'uv': [1, 1, 15, 15], 'cullface': 'down'},
            'north': {'texture': '#side', 'uv': [1, 13, 15, 16]}, 'south': {'texture': '#side', 'uv': [1, 13, 15, 16]},
            'east': {'texture': '#side', 'uv': [1, 13, 15, 16]}, 'west': {'texture': '#side', 'uv': [1, 13, 15, 16]}}}]})
    w(os.path.join(A, 'models', 'item', 'catto_cushion.json'), {'parent': NS + ':block/catto_cushion'})
    w(os.path.join(D, 'loot_table', 'blocks', 'catto_cushion.json'), {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': NS + ':catto_cushion'}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    w(os.path.join(D, 'recipe', 'catto_cushion.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'decorations', 'pattern': ['CCC', 'WWW'],
        'key': {'C': {'tag': 'minecraft:wool_carpets'}, 'W': {'tag': 'minecraft:wool'}},
        'result': {'count': 2, 'id': NS + ':catto_cushion'}})


def main():
    out = os.path.join(D, 'structure', 'village')
    os.makedirs(out, exist_ok=True)
    for name, build in (('catto_stall', stall), ('catto_storage', storage), ('catto_junk', junk)):
        build().save(os.path.join(out, name + '.nbt'))
        print('wrote', name)
    cushion_assets()
    print('wrote cushion assets')


if __name__ == '__main__':
    main()
