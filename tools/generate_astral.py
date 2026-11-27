#!/usr/bin/env python3
"""1.41-1.42: the Astral, the White Abyss - textures, block and item models, the snow of oblivion and what it hides, the
Watcher's skin, the sounds (synthesised here, encoded with ffmpeg), the dimension, its type and biome, the terrain feature
and the Ascension rite. The plain and the monoliths cannot be broken, so they have no loot tables or tool tags.
Run from the repository root: python3 tools/generate_astral.py (needs Pillow, numpy and ffmpeg with libvorbis)."""
import json
import os
import random
import subprocess
import tempfile
import wave

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'arcane_artifacts')
D = os.path.join(ROOT, 'data', 'arcane_artifacts')
NS = 'arcane_artifacts'

# the White Abyss: white, grey, graphite and absolute black. A white sky, a white crust and whiter snow, black monoliths;
# only the gate (graphite) carries a cold white light.
FOG = (0xEC, 0xEE, 0xF1)
FLOOR = (0xDD, 0xE0, 0xE5)
SNOW = (0xF4, 0xF6, 0xF8)
STONE = (0x0A, 0x0A, 0x0B)
GRAPHITE = (0x2C, 0x2D, 0x30)
GLOW = (0xFF, 0xFF, 0xFF)
GLOW_EDGE = (0xB8, 0xBC, 0xC2)
SAMPLE_RATE = 22050


def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c)


def png(name, pixels, size=(16, 16), folder='block'):
    img = Image.new('RGBA', size)
    for y in range(size[1]):
        for x in range(size[0]):
            img.putpixel((x, y), pixels(x, y) + (255,))
    path = os.path.join(A, 'textures', folder, name + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def textures():
    r = random.Random(41)
    floor_noise = [[r.randint(-4, 3) for _ in range(16)] for _ in range(16)]
    png('astral_stone', lambda x, y: shade(FLOOR, floor_noise[y][x]))
    snow_noise = [[r.randint(-3, 2) for _ in range(16)] for _ in range(16)]
    png('astral_snow', lambda x, y: shade(SNOW, snow_noise[y][x]))

    def monolith(x, y):
        # absolute black: no grain, no seams, nothing
        return STONE

    png('astral_monolith', monolith)
    grain = [[r.randint(-2, 2) for _ in range(16)] for _ in range(16)]

    def graphite(x, y):
        return shade(GRAPHITE, grain[y][x])

    def gate_side(x, y):
        if 6 <= x <= 9 and 3 <= y <= 12 and (x in (7, 8) or y in (5, 10)):
            return GLOW
        return graphite(x, y)

    png('astral_gate_side', gate_side)

    def gate_top(x, y):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if d < 2.2:
            return GLOW
        if d < 3.4:
            return GLOW_EDGE
        return graphite(x, y)

    png('astral_gate_top', gate_top)
    # the Watcher: a silhouette, black all over (an enderman's 64x32 layout)
    png('watcher', lambda x, y: (0x05, 0x05, 0x05), size=(64, 32), folder='entity')


def blocks():
    for name in ('astral_stone', 'astral_monolith'):
        w(os.path.join(A, 'blockstates', name + '.json'), {'variants': {'': {'model': f'{NS}:block/{name}'}}})
        w(os.path.join(A, 'models', 'block', name + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{name}'}})
        w(os.path.join(A, 'models', 'item', name + '.json'), {'parent': f'{NS}:block/{name}'})
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
    # the snow of oblivion: vanilla snow-layer shapes with its own texture
    tex = f'{NS}:block/astral_snow'
    variants = {}
    for layers in range(1, 8):
        name = f'astral_snow_height{layers * 2}'
        w(os.path.join(A, 'models', 'block', name + '.json'), {'parent': f'minecraft:block/snow_height{layers * 2}',
                                                                 'textures': {'texture': tex, 'particle': tex}})
        variants[f'layers={layers}'] = {'model': f'{NS}:block/{name}'}
    w(os.path.join(A, 'models', 'block', 'astral_snow_block.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex}})
    variants['layers=8'] = {'model': f'{NS}:block/astral_snow_block'}
    w(os.path.join(A, 'blockstates', 'astral_snow.json'), {'variants': variants})
    w(os.path.join(A, 'models', 'item', 'astral_snow.json'), {'parent': f'{NS}:block/astral_snow_height2'})
    forgotten()


def named(item, key, extra=None, weight=1):
    """An entry of something forgotten: its own name and the line 'whose it was, no one remembers'."""
    functions = [{'function': 'minecraft:set_name', 'target': 'item_name', 'name': {'translate': f'arcane_artifacts.forgotten.{key}'}},
                 {'function': 'minecraft:set_lore', 'mode': 'replace_all',
                  'lore': [{'translate': 'arcane_artifacts.forgotten.lore', 'italic': True, 'color': 'gray'}]}]
    return {'type': 'minecraft:item', 'name': item, 'weight': weight, 'functions': (extra or []) + functions}


def forgotten():
    """What the snow of oblivion hides: one dig in sixteen gives up a thing that belonged to someone, in another world."""
    damaged = [{'function': 'minecraft:set_damage', 'damage': {'type': 'minecraft:uniform', 'min': 0.05, 'max': 0.35}}]
    # in the enchanting-table alphabet: no one can read it any more
    unknown = {'text': 'i was here and i had a name but the snow came and now nobody remembers it', 'font': 'minecraft:alt'}
    book = [{'function': 'minecraft:set_book_cover', 'title': '...', 'author': '?', 'generation': 3},
            {'function': 'minecraft:set_written_book_pages', 'mode': {'type': 'replace_all'}, 'pages': [unknown, unknown, unknown]}]
    w(os.path.join(D, 'loot_table', 'blocks', 'astral_snow.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.0625}],
                   'entries': [named('minecraft:written_book', 'book', book, 4),
                               named('minecraft:iron_sword', 'blade', damaged, 3),
                               named('minecraft:bow', 'bow', damaged, 2),
                               named('minecraft:name_tag', 'name', None, 3),
                               named('minecraft:compass', 'compass', None, 2),
                               named('minecraft:clock', 'clock', None, 2),
                               named('minecraft:bricks', 'brick', [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}}], 3),
                               named('minecraft:flower_pot', 'pot', None, 2),
                               named('minecraft:leather_boots', 'boot', damaged, 2),
                               named('minecraft:lantern', 'lantern', None, 1),
                               named('minecraft:music_disc_11', 'record', None, 1)]}]})


def write_ogg(name, samples):
    """Mono float samples in [-1, 1] to assets/arcane_artifacts/sounds/astral/<name>.ogg."""
    out = os.path.join(A, 'sounds', 'astral', name + '.ogg')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'a.wav')
        with wave.open(wav, 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SAMPLE_RATE)
            f.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '3',
                        '-map_metadata', '-1', '-fflags', '+bitexact', out], check=True)


def sounds():
    rng = np.random.default_rng(42)
    # the hum: a low, slow drone that loops seamlessly (every frequency and swell fits 24 s exactly)
    seconds = 24
    n = SAMPLE_RATE * seconds
    t = np.arange(n) / SAMPLE_RATE
    hum = sum(a * np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) for f, a in ((41.0, 1.0), (55.0, 0.6), (82.0, 0.35), (123.0, 0.18), (164.5, 0.06)))
    spectrum = np.zeros(n // 2 + 1, dtype=complex)
    freqs = np.fft.rfftfreq(n, 1 / SAMPLE_RATE)
    band = (freqs > 25) & (freqs < 220)
    spectrum[band] = np.exp(1j * rng.uniform(0, 2 * np.pi, band.sum())) / np.maximum(freqs[band], 1) ** 0.5
    rumble = np.fft.irfft(spectrum, n)
    rumble *= 0.35 / np.abs(rumble).max()
    swell = (1 + 0.25 * np.sin(2 * np.pi * t / 12)) * (1 + 0.12 * np.sin(2 * np.pi * t / 8 + 1.0))
    hum = (hum / np.abs(hum).max() * 0.7 + rumble) * swell
    write_ogg('hum', hum / np.abs(hum).max() * 0.55)
    # far strikes of metal: a struck bar's inharmonic partials, slow decay, a few late echoes
    for i, f0 in enumerate((143.0, 188.0, 247.0), 1):
        n = SAMPLE_RATE * 7
        t = np.arange(n) / SAMPLE_RATE
        tone = np.zeros(n)
        for ratio, amp, decay in ((1.0, 1.0, 3.2), (2.756, 0.55, 1.9), (5.404, 0.3, 1.1), (8.933, 0.15, 0.6)):
            for detune in (1.0, 1.0035):
                tone += amp * np.exp(-t / decay) * np.sin(2 * np.pi * f0 * ratio * detune * t)
        tone *= np.minimum(1, t / 0.004)
        out = tone.copy()
        for delay, gain in ((0.37, 0.35), (0.83, 0.2), (1.46, 0.12), (2.2, 0.06)):
            k = int(delay * SAMPLE_RATE)
            out[k:] += gain * tone[:n - k]
        write_ogg(f'metal{i}', out / np.abs(out).max() * 0.6)
    # the silence that stands in for music
    write_ogg('silence', np.zeros(SAMPLE_RATE * 2))
    w(os.path.join(A, 'sounds.json'), {
        'astral.hum': {'sounds': [{'name': f'{NS}:astral/hum'}]},
        'astral.metal': {'subtitle': 'arcane_artifacts.subtitle.astral_metal',
                         'sounds': [{'name': f'{NS}:astral/metal{i}', 'volume': 0.6} for i in (1, 2, 3)]},
        'astral.silence': {'sounds': [{'name': f'{NS}:astral/silence', 'stream': True}]}})


def world():
    color = FOG[0] << 16 | FOG[1] << 8 | FOG[2]
    w(os.path.join(D, 'dimension_type', 'astral.json'), {
        'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False,
        'has_raids': False, 'has_skylight': False, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 1.0,
        'fixed_time': 6000, 'logical_height': 512, 'min_y': 0, 'height': 512,
        'infiniburn': '#minecraft:infiniburn_overworld', 'effects': f'{NS}:astral',
        'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    w(os.path.join(D, 'worldgen', 'biome', 'astral.json'), {
        'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
        'effects': {'fog_color': color, 'sky_color': color, 'water_color': 0xC8CCD2, 'water_fog_color': 0x8A8F96,
                    'particle': {'options': {'type': 'minecraft:white_ash'}, 'probability': 0.006},
                    'ambient_sound': f'{NS}:astral.hum',
                    'additions_sound': {'sound': f'{NS}:astral.metal', 'tick_chance': 0.0008},
                    'music': {'sound': f'{NS}:astral.silence', 'min_delay': 12000, 'max_delay': 24000, 'replace_current_music': True}},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {},
        'features': [[f'{NS}:astral_terrain']]})
    w(os.path.join(D, 'worldgen', 'configured_feature', 'astral_terrain.json'), {'type': f'{NS}:astral_terrain', 'config': {}})
    w(os.path.join(D, 'worldgen', 'placed_feature', 'astral_terrain.json'), {'feature': f'{NS}:astral_terrain', 'placement': []})
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
    sounds()
    world()
