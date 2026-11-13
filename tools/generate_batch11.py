#!/usr/bin/env python3
"""1.10.0: Pseudomage's spurred boots and the Druid's placeholder gear (mantle, hat, gloves, boots)."""
import json
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save  # noqa: E402
from generate_batch8 import w, shaped, tag, box, model, NS, D, A, R  # noqa: E402

# ---- Pseudomage's Boots
wm = A + 'models/item/worn/'
boot = [box([5.6, -4.25, 5.6], [10.4, 2.0, 10.4], 'b', [0, 0, 8, 10]),          # shaft
        box([5.6, 1.4, 5.4], [10.4, 2.4, 10.6], 'b', [8, 0, 16, 2]),            # turned-down cuff
        box([5.6, -4.25, 4.2], [10.4, -2.4, 5.6], 'b', [0, 10, 8, 13]),         # toe
        box([5.5, -4.4, 4.1], [10.5, -4.0, 10.5], 'b', [8, 12, 16, 16]),        # sole
        box([7.6, -3.4, 10.4], [8.4, -2.8, 11.8], 's', [0, 0, 2, 2]),           # spur
        box([7.7, -4.0, 11.8], [8.3, -2.2, 12.4], 's', [2, 0, 4, 4])]           # rowel
for side in ('right', 'left'):
    model(wm + 'pseudomage_boots_' + side + '.json', {'b': NS + ':item/worn/pseudomage_boots', 's': NS + ':item/worn/spur'}, boot)
save(sprite([
    '6666666666666666', '6555555555555556', '6555555555555556', '6544444444444456',
    '6555555555555556', '6555555555555556', '6555555555555556', '6555555555555556',
    '6555555555555556', '6555555555555556', '6555555555555556', '6555555555555556',
    '6444444444444446', '6444444444444446', '3333333333333333', '3333333333333333'],
    {'6': '#4A2A14', '5': '#7A4A26', '4': '#8E5A30', '3': '#2E1A0C'}), 'item', 'worn', 'pseudomage_boots.png')
save(sprite(['aAAa', 'AooA', 'AooA', 'aAAa'], {'a': '#B07820', 'A': '#F0C040', 'o': '#FFE890'}), 'item', 'worn', 'spur.png')
save(sprite([
    '................',
    '................',
    '....666666......',
    '....655556......',
    '....644446......',
    '....655556......',
    '....655556......',
    '....655556......',
    '....6555556.....',
    '....65555556....',
    '..A.655555556...',
    '.AoA6555555556..',
    '..A.6555555556..',
    '....3333333333..',
    '................',
    '................'], {'6': '#4A2A14', '5': '#7A4A26', '4': '#F0A030', '3': '#2E1A0C', 'A': '#F0C040', 'o': '#FFE890'}),
    'item', 'pseudomage_boots.png')
w(A + 'models/item/pseudomage_boots.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS + ':item/pseudomage_boots'}})
shaped('pseudomage_boots', [' A ', 'NBN', ' M '], {'A': NS + ':amber', 'N': 'minecraft:gold_nugget', 'B': 'minecraft:leather_boots', 'M': 'minecraft:magma_cream'})
tag(R + 'data/curios/tags/item/feet.json', [NS + ':pseudomage_boots'])
p = D + 'arcane_research/pseudo/counterfeit.json'
d = json.load(open(p))
if NS + ':pseudomage_boots' not in d['unlocks_items']:
    d['unlocks_items'].append(NS + ':pseudomage_boots')
w(p, d)

# ---- Druid placeholders
G = {'g': '#3E6A2A', 'G': '#5E9A3A', 'l': '#8ACB5A', 'b': '#6B4A26', 'B': '#8E6A3A', 'f': '#E8F0B0', 'r': '#C04A4A'}
icons = {
    'druid_mantle': ['................', '......gggg......', '.....gGGGGg.....', '....gGllllGg....', '....gGl..lGg....',
                     '...gGGl..lGGg...', '...gGGGllGGGg...', '..gGGGGGGGGGGg..', '..gGGlGGGGlGGg..', '..gGGGGGGGGGGg..',
                     '.gGGGGGlGGGGGGg.', '.gGlGGGGGGGlGGg.', '.gGGGGGGGGGGGGg.', '.gggggggggggggg.', '................', '................'],
    'druid_hat': ['................', '................', '.......ll.......', '......lGGl......', '.....gGGGGg.....', '....gGGfGGGg....',
                  '....gGGGGGGg....', '...gGGGGGGGGg...', '...bbbbbbbbbb...', '.gGGGGGGGGGGGGg.', 'gGGGGlGGGGlGGGGg', '.gggggggggggggg.',
                  '................', '................', '................', '................'],
    'druid_gloves': ['................', '................', '...b.b.b........', '...BbBbBb.......', '...BBBBBb.......', '...BBBBBbB......',
                     '...BBBBBBBb.....', '...BBBBBBBb.....', '...gGGGGGg......', '...gGlGlGg......', '...gggggggg.....', '................',
                     '.......b.b.b....', '.......BbBbBb...', '.......BBBBBb...', '.......ggggg....'],
    'druid_boots': ['................', '................', '...gggg.........', '...gGGg.........', '...gGGg.........', '...bBBb.........',
                    '...bBBb.........', '...bBBbbbb......', '...bBBBBBBb.....', '...bbbbbbbb.....', '................', '........gggg....',
                    '........gGGg....', '........bBBbbbb.', '........bBBBBBBb', '........bbbbbbbb'],
}
slots = {'druid_mantle': 'back', 'druid_hat': 'head', 'druid_gloves': 'hands', 'druid_boots': 'feet'}
for name, rows in icons.items():
    save(sprite(rows, G), 'item', name + '.png')
    w(A + 'models/item/' + name + '.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS + ':item/' + name}})
    tag(R + 'data/curios/tags/item/' + slots[name] + '.json', [NS + ':' + name])
p = D + 'curios/entities/arcane_artifacts.json'
d = json.load(open(p))
if 'hands' not in d['slots']:
    d['slots'].append('hands')
w(p, d)
print('batch 11 written')
