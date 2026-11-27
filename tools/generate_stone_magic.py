#!/usr/bin/env python3
"""1.43: the stone glyphs - icons, item models, Scribes' Table recipes, the two researches and the tag of blocks that can
be torn up. Run from the repository root: python3 tools/generate_stone_magic.py"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import glyph  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'arcane_artifacts')
D = os.path.join(ROOT, 'data', 'arcane_artifacts')
NS = 'arcane_artifacts'

EARTH = {'g': '#8A7A66', 't': '#5E8C3A', 'v': '#4A3A2A'}
ARCANE = {'g': '#6A6A74', 't': '#B44CF0', 'v': '#F0C64A'}

ICONS = {
    'earthen_surge': (EARTH, [
        '....##......',
        '....gg..##..',
        '.##.....gg..',
        '.gg..##.....',
        '.....gg.....',
        '..t..t..t...',
        '.t.t...t.t..',
        '............',
        '############',
        '#gggg##gggg#',
        '#vvvvvvvvvv#',
        '############']),
    'stone_cage': (EARTH, [
        '.##########.',
        '.#g#g##g#g#.',
        '.#.#.##.#.#.',
        '.#.#.tt.#.#.',
        '.#.#tttt#.#.',
        '.#.#.tt.#.#.',
        '.#.#.tt.#.#.',
        '.#.#t..t#.#.',
        '.#.#....#.#.',
        '.##########.',
        '.#gggggggg#.',
        '.##########.']),
    'stone_lances': (EARTH, [
        '.....#......',
        '....#g#.....',
        '.#..#g#..#..',
        '#g#.#g#.#g#.',
        '#g#.#g#.#g#.',
        '#g#.#g#.#g#.',
        '#g##ggg##g#.',
        '#gggggggggg#',
        '.t.t.t.t.t..',
        '############',
        '#vvvvvvvvvv#',
        '############']),
    'myriad_volley': (ARCANE, [
        '.........##.',
        '......t.#gg#',
        '.....t..#gg#',
        '..##.....##.',
        '.#gg#.t.....',
        '.#gg#t...##.',
        '..##....#gg#',
        '...t...t#gg#',
        '..t...t..##.',
        '.##..t......',
        '#gg#........',
        '.##.........']),
    'stone_wings': (ARCANE, [
        '##........##',
        '#g##....##g#',
        '#gg##..##gg#',
        '.#gg#tt#gg#.',
        '.#ggg##ggg#.',
        '..#g#tt#g#..',
        '..##.tt.##..',
        '.....tt.....',
        '....t..t....',
        '....t..t....',
        '............',
        '............']),
    'cataclysm': (ARCANE, [
        '..v.........',
        '.v.v........',
        'v.v.v.......',
        '.v.v.####...',
        '..v.#gggg#..',
        '...#gggggg#.',
        '...#gggtgg#.',
        '...#ggtggg#.',
        '...#gggggg#.',
        '....#gggg#..',
        '.....####...',
        '............']),
    'stone_dominion': (ARCANE, [
        '............',
        '.v..v..v..v.',
        '.#v.#vv#.v#.',
        '.##########.',
        '.#tttttttt#.',
        '.##########.',
        '............',
        '..##.##.##..',
        '..gg.gg.gg..',
        '..##.##.##..',
        '............',
        '............']),
}

RECIPES = {
    'earthen_surge': (55, ['ars_nouveau:earth_essence', 'minecraft:iron_shovel', 'minecraft:cobblestone', 'minecraft:dirt', 'minecraft:gravel']),
    'stone_cage': (160, ['ars_nouveau:earth_essence', 'minecraft:iron_bars', 'minecraft:iron_bars', 'minecraft:stone_bricks', 'arcane_artifacts:rose_gold_ingot']),
    'stone_lances': (160, ['ars_nouveau:earth_essence', 'minecraft:pointed_dripstone', 'minecraft:pointed_dripstone', 'minecraft:pointed_dripstone', 'minecraft:iron_sword', 'arcane_artifacts:rose_gold_ingot']),
    'myriad_volley': (160, ['ars_nouveau:manipulation_essence', 'minecraft:dispenser', 'minecraft:cobblestone', 'minecraft:cobblestone', 'minecraft:arrow', 'arcane_artifacts:rose_gold_ingot']),
    'stone_wings': (350, ['ars_nouveau:air_essence', 'ars_nouveau:manipulation_essence', 'minecraft:phantom_membrane', 'minecraft:phantom_membrane', 'minecraft:feather', 'arcane_artifacts:rose_gold_block']),
    'cataclysm': (350, ['ars_nouveau:fire_essence', 'ars_nouveau:earth_essence', 'minecraft:heavy_core', 'minecraft:fire_charge', 'minecraft:obsidian', 'arcane_artifacts:rose_gold_block']),
    'stone_dominion': (350, ['ars_nouveau:manipulation_essence', 'ars_nouveau:earth_essence', 'minecraft:lodestone', 'minecraft:deepslate', 'minecraft:amethyst_block', 'arcane_artifacts:rose_gold_block']),
}

TEARABLE = ['#minecraft:dirt', '#minecraft:sand', '#minecraft:base_stone_overworld', '#minecraft:base_stone_nether', '#minecraft:terracotta',
            'minecraft:gravel', 'minecraft:clay', 'minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:snow_block',
            'minecraft:netherrack', 'minecraft:end_stone', 'minecraft:calcite', 'minecraft:dripstone_block', 'minecraft:sandstone',
            'minecraft:red_sandstone', 'minecraft:soul_sand', 'minecraft:soul_soil', 'minecraft:mud', 'minecraft:packed_mud',
            'minecraft:cobbled_deepslate', 'minecraft:magma_block', 'minecraft:mycelium', 'minecraft:podzol', 'minecraft:grass_block']


def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def main():
    for name, (colors, rows) in ICONS.items():
        img = glyph(rows, colors)
        img.save(os.path.join(A, 'textures', 'item', f'glyph_{name}.png'))
        w(os.path.join(A, 'models', 'item', f'glyph_{name}.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/glyph_{name}'}})
    for name, (exp, inputs) in RECIPES.items():
        w(os.path.join(D, 'recipe', f'glyph_{name}.json'), {'type': 'ars_nouveau:glyph', 'exp': exp, 'inputs': [{'item': i} for i in inputs],
                                                            'output': {'count': 1, 'id': f'{NS}:glyph_{name}'}})
    w(os.path.join(D, 'tags', 'block', 'tearable.json'), {'replace': False, 'values': TEARABLE})
    # the Druid: Stone Vein, after the Awakening
    w(os.path.join(D, 'arcane_research', 'druid', 'stone_vein.json'), {
        'category': 'material', 'icon': 'minecraft:pointed_dripstone', 'sort': 310,
        'subjects': ['minecraft:pointed_dripstone', 'minecraft:cobblestone', 'minecraft:gravel'],
        'properties': ['dense', 'rooted'], 'requires': [f'{NS}:druid/awakening'], 'notes': 1,
        'experiments': [
            {'id': 'break', 'any_of': [{'trigger': f'{NS}:spell_effect', 'glyph': 'ars_nouveau:glyph_break', 'block': '*'}]},
            {'id': 'launch', 'any_of': [{'trigger': f'{NS}:spell_effect', 'glyph': 'ars_nouveau:glyph_launch', 'entity': '*'}]},
            {'id': 'dripstone', 'any_of': [{'trigger': f'{NS}:observe', 'item': 'minecraft:pointed_dripstone'}]}],
        'unlocks': [f'{NS}:glyph_earthen_surge', f'{NS}:glyph_stone_cage', f'{NS}:glyph_stone_lances']})
    # the Mage of Mysteries: Gravity of Stone, after Singularity
    w(os.path.join(D, 'arcane_research', 'gravity_of_stone.json'), {
        'category': 'spatial', 'icon': 'minecraft:lodestone', 'sort': 65,
        'subjects': ['minecraft:lodestone', 'minecraft:deepslate', 'minecraft:heavy_core'],
        'properties': ['dense', 'hungry'], 'requires': [f'{NS}:singularity'], 'notes': 1,
        'experiments': [
            {'id': 'pull', 'any_of': [{'trigger': f'{NS}:spell_effect', 'glyph': 'ars_nouveau:glyph_pull', 'entity': '*'}]},
            {'id': 'tornado', 'any_of': [{'trigger': f'{NS}:spell_effect', 'glyph': f'{NS}:glyph_tornado', 'block': '*'},
                                         {'trigger': f'{NS}:spell_effect', 'glyph': f'{NS}:glyph_tornado', 'entity': '*'}]},
            {'id': 'deepslate', 'any_of': [{'trigger': f'{NS}:observe', 'item': 'minecraft:deepslate'}]}],
        'unlocks': [f'{NS}:glyph_myriad_volley', f'{NS}:glyph_stone_wings', f'{NS}:glyph_cataclysm', f'{NS}:glyph_stone_dominion']})


if __name__ == '__main__':
    main()
