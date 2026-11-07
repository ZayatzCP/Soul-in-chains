#!/usr/bin/env python3
"""Writes the GameTest arena: a 9x5x9 stone floor with air above (data/arcane_artifacts/structure/arena.nbt)."""
import gzip
import os
import struct

DATA_VERSION = 3955  # Minecraft 1.21.1


def s(text):
    b = text.encode('utf-8')
    return struct.pack('>H', len(b)) + b


def tag_int(name, v):
    return b'\x03' + s(name) + struct.pack('>i', v)


def tag_string(name, v):
    return b'\x08' + s(name) + s(v)


def tag_int_list(name, values):
    return b'\x09' + s(name) + b'\x03' + struct.pack('>i', len(values)) + b''.join(struct.pack('>i', v) for v in values)


def tag_compound_list(name, compounds):
    return b'\x09' + s(name) + (b'\x0a' if compounds else b'\x00') + struct.pack('>i', len(compounds)) + b''.join(c + b'\x00' for c in compounds)


def main():
    sx, sy, sz = 9, 5, 9
    palette = [tag_string('Name', 'minecraft:stone'), tag_string('Name', 'minecraft:air')]
    blocks = []
    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                blocks.append(tag_int_list('pos', [x, y, z]) + tag_int('state', 0 if y == 0 else 1))
    root = (b'\x0a' + s('')
            + tag_int('DataVersion', DATA_VERSION)
            + tag_int_list('size', [sx, sy, sz])
            + tag_compound_list('palette', palette)
            + tag_compound_list('blocks', blocks)
            + tag_compound_list('entities', [])
            + b'\x00')
    out = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'arcane_artifacts', 'structure', 'arena.nbt')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with gzip.open(out, 'wb') as f:
        f.write(root)
    print('wrote', os.path.relpath(out))


if __name__ == '__main__':
    main()
