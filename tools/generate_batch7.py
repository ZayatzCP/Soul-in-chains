#!/usr/bin/env python3
"""Pseudomage data, models and textures (batch 7). Run from the repository root. Textures are only
written when missing (pass --force to redraw)."""
import json
import os
import random
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, hexc, ROOT  # noqa: E402
from PIL import Image  # noqa: E402

R = 'src/main/resources/'
D = R + 'data/arcane_artifacts/'
A = R + 'assets/arcane_artifacts/'
NS = 'arcane_artifacts'
FORCE = '--force' in sys.argv


def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


def tag(path, values):
    data = {"replace": False, "values": []}
    if os.path.exists(path):
        data = json.load(open(path))
    for v in values:
        if v not in data['values']:
            data['values'].append(v)
    w(path, data)


def it(x):
    return {"tag": x[1:]} if x.startswith('#') else {"item": x}


def shaped(name, pattern, key, count=1):
    w(D + 'recipe/' + name + '.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                       "key": {k: it(v) for k, v in key.items()}, "result": {"count": count, "id": NS + ':' + name}})


def research():
    base = D + 'arcane_research/pseudo/'
    w(base + 'firearms.json', {
        "category": "material", "icon": "minecraft:gunpowder", "sort": 200,
        "subjects": ["minecraft:gunpowder", "minecraft:crossbow", "minecraft:flint_and_steel"],
        "properties": ["explosive", "ballistic"],
        "experiments": [
            {"id": "blast", "any_of": [{"trigger": NS + ":damage_taken", "damage_type": "#minecraft:is_explosion"}]},
            {"id": "spark", "any_of": [{"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_ignite", "entity": "*"},
                                       {"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_ignite", "block": "*"}]},
            {"id": "fire", "any_of": [{"trigger": NS + ":spell_cast", "glyph": "ars_nouveau:glyph_projectile"}]},
        ],
        "notes": 1,
        "unlocks_items": [NS + ":revolver", NS + ":sawn_off", NS + ":magic_cartridge", NS + ":bandolier"]})
    w(base + 'logistics.json', {
        "category": "material", "icon": "minecraft:copper_ingot", "sort": 210,
        "subjects": ["minecraft:copper_ingot", "minecraft:hopper", "minecraft:bucket"],
        "properties": ["conductive", "contained"],
        "experiments": [
            {"id": "pour", "any_of": [{"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_conjure_water", "block": "*"}]},
            {"id": "gather", "any_of": [{"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_pickup", "block": "*"},
                                        {"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_pickup", "entity": "*"}]},
            {"id": "touch", "any_of": [{"trigger": NS + ":spell_cast", "glyph": "ars_nouveau:glyph_touch"}]},
        ],
        "notes": 1,
        "unlocks_items": [NS + ":mana_tank", NS + ":mana_pipe", NS + ":mana_dispenser", NS + ":mana_goggles"]})
    w(base + 'counterfeit.json', {
        "category": "arcane", "icon": "minecraft:gold_nugget", "sort": 220,
        "subjects": ["minecraft:gold_nugget", "minecraft:emerald", "minecraft:clock"],
        "properties": ["counterfeit", "glittering"],
        "requires": [NS + ":pseudo/firearms"],
        "experiments": [
            {"id": "vanish", "any_of": [{"trigger": NS + ":effect_applied", "effect": "minecraft:invisibility"}]},
            {"id": "smelt", "any_of": [{"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_smelt", "block": "*"}]},
            {"id": "dispel", "any_of": [{"trigger": NS + ":spell_cast", "glyph": "ars_nouveau:glyph_dispel"}]},
        ],
        "notes": 1, "notes_required": {"material": 1},
        "unlocks_items": [NS + ":counterfeit_ring", NS + ":counterfeit_pendant"]})


def recipes():
    shaped('false_mage_guide', ["NPP", "GPP", "NPP"], {"N": "minecraft:iron_nugget", "G": "minecraft:gunpowder", "P": NS + ":old_parchment"})
    shaped('revolver', ["IIR", "SG ", "L  "], {"I": "minecraft:iron_ingot", "R": NS + ":rose_gold_ingot", "S": NS + ":bottle_of_source",
                                               "G": "minecraft:gunpowder", "L": "#minecraft:planks"})
    shaped('sawn_off', ["IIR", "IGS", "L  "], {"I": "minecraft:iron_ingot", "R": NS + ":rose_gold_ingot", "S": NS + ":bottle_of_source",
                                               "G": "minecraft:gunpowder", "L": "#minecraft:planks"})
    w(D + 'recipe/magic_cartridge.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
                                         "ingredients": [it("minecraft:gunpowder"), it(NS + ":rose_gold_nugget"), it(NS + ":rose_gold_nugget"),
                                                         it(NS + ":bottle_of_source")],
                                         "result": {"count": 8, "id": NS + ":magic_cartridge"}})
    shaped('bandolier', ["L L", "CLC", "L L"], {"L": "minecraft:leather", "C": NS + ":magic_cartridge"})
    shaped('counterfeit_ring', [" G ", "GAG", " G "], {"G": "minecraft:gold_nugget", "A": "minecraft:amethyst_shard"})
    shaped('counterfeit_pendant', [" S ", "S S", " E "], {"S": "minecraft:string", "E": "minecraft:emerald"})
    shaped('mana_goggles', ["L L", "GRG"], {"L": "minecraft:leather", "G": "minecraft:glass", "R": NS + ":rose_gold_ingot"})
    shaped('mana_tank', ["CGC", "G G", "CGC"], {"C": "minecraft:copper_ingot", "G": "minecraft:glass"})
    shaped('mana_pipe', ["CGC"], {"C": "minecraft:copper_ingot", "G": "minecraft:glass"}, 8)
    shaped('mana_dispenser', ["CPC", "CSC", "CRC"], {"C": "minecraft:copper_ingot", "P": NS + ":mana_pipe", "S": NS + ":bottle_of_source",
                                                     "R": "minecraft:redstone"})


def curios():
    w(D + 'curios/slots/body.json', {"size": 1, "icon": "curios:slot/empty_body_slot", "validators": ["curios:tag"]})
    p = D + 'curios/entities/arcane_artifacts.json'
    d = json.load(open(p))
    if 'body' not in d['slots']:
        d['slots'].append('body')
    w(p, d)
    tag(R + 'data/curios/tags/item/body.json', [NS + ':bandolier'])
    tag(R + 'data/curios/tags/item/ring.json', [NS + ':counterfeit_ring'])
    tag(R + 'data/curios/tags/item/necklace.json', [NS + ':counterfeit_pendant'])
    tag(R + 'data/curios/tags/item/head.json', [NS + ':mana_goggles'])


def models():
    bs, bm, im = A + 'blockstates/', A + 'models/block/', A + 'models/item/'
    for name in ['revolver', 'sawn_off']:
        w(im + name + '.json', {"parent": "minecraft:item/handheld", "textures": {"layer0": NS + ":item/" + name}})
    for name in ['false_mage_guide', 'magic_cartridge', 'bullet', 'bandolier', 'counterfeit_ring', 'counterfeit_pendant', 'mana_goggles']:
        w(im + name + '.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}})
    # Tank: a glass box with copper rims; the inner rims vanish where tanks stack.
    w(bm + 'mana_tank.json', {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                               "textures": {"side": NS + ":block/mana_tank_side", "top": NS + ":block/mana_tank_top", "particle": NS + ":block/mana_tank_side"},
                               "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                                   "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
                                   "west": {"texture": "#side"}, "up": {"texture": "#top", "cullface": "up"}, "down": {"texture": "#top", "cullface": "down"}}}]})
    w(bs + 'mana_tank.json', {"variants": {f"down={d},up={u}": {"model": NS + ":block/mana_tank"} for d in ("false", "true") for u in ("false", "true")}})
    w(im + 'mana_tank.json', {"parent": NS + ":block/mana_tank"})
    # Pipe: core plus one arm per connection.
    w(bm + 'mana_pipe_core.json', {"parent": "minecraft:block/block", "textures": {"pipe": NS + ":block/mana_pipe", "particle": NS + ":block/mana_pipe"},
                                    "elements": [{"from": [5, 5, 5], "to": [11, 11, 11], "faces": {f: {"texture": "#pipe"} for f in
                                                                                                  ["north", "south", "east", "west", "up", "down"]}}]})
    w(bm + 'mana_pipe_arm.json', {"parent": "minecraft:block/block", "textures": {"pipe": NS + ":block/mana_pipe", "particle": NS + ":block/mana_pipe"},
                                   "elements": [{"from": [5.5, 5.5, 0], "to": [10.5, 10.5, 5], "faces": {f: {"texture": "#pipe"} for f in
                                                                                                         ["north", "east", "west", "up", "down"]}}]})
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    parts = [{"apply": {"model": NS + ":block/mana_pipe_core"}}]
    for d, r in rot.items():
        parts.append({"when": {d: "true"}, "apply": dict({"model": NS + ":block/mana_pipe_arm"}, **r)})
    w(bs + 'mana_pipe.json', {"multipart": parts})
    w(im + 'mana_pipe.json', {"parent": NS + ":block/mana_pipe_core", "display": {"gui": {"rotation": [30, 225, 0], "scale": [1.2, 1.2, 1.2]}}})
    # Dispenser.
    for mode in ('fill', 'drain'):
        w(bm + 'mana_dispenser_' + mode + '.json', {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": NS + ":block/mana_dispenser_side", "bottom": NS + ":block/mana_tank_top", "top": NS + ":block/mana_dispenser_" + mode}})
    w(bs + 'mana_dispenser.json', {"variants": {"drain=false": {"model": NS + ":block/mana_dispenser_fill"},
                                                "drain=true": {"model": NS + ":block/mana_dispenser_drain"}}})
    w(im + 'mana_dispenser.json', {"parent": NS + ":block/mana_dispenser_fill"})
    # Loot and mining.
    for name in ['mana_tank', 'mana_pipe', 'mana_dispenser']:
        w(D + 'loot_table/blocks/' + name + '.json', {"type": "minecraft:block", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": NS + ':' + name}],
             "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    tag(R + 'data/minecraft/tags/block/mineable/pickaxe.json', [NS + ':mana_tank', NS + ':mana_pipe', NS + ':mana_dispenser'])


# ---------------------------------------------------------------- textures
def save(img, *path):
    p = os.path.join(ROOT, *path)
    if os.path.exists(p) and not FORCE:
        return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', os.path.relpath(p))


def textures():
    copper = [hexc(c) for c in ['#C77B4A', '#D88A5A', '#B06A3A', '#E8A07A']]
    rnd = random.Random(7)
    side = Image.new('RGBA', (16, 16), (200, 225, 255, 40))
    for i in range(16):
        for t in (0, 15):
            side.putpixel((i, t), copper[rnd.randrange(4)])
            side.putpixel((t, i), copper[rnd.randrange(4)])
    for i in range(2, 6):
        side.putpixel((i, i + 1), (255, 255, 255, 140))
    save(side, 'block', 'mana_tank_side.png')
    top = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            top.putpixel((x, y), copper[rnd.randrange(4)] if (x in (0, 15) or y in (0, 15) or (x + y) % 5 == 0) else hexc('#8E4B2B'))
    save(top, 'block', 'mana_tank_top.png')
    pipe = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            pipe.putpixel((x, y), copper[(x * 3 + y) % 4] if y % 6 else hexc('#8E4B2B'))
    save(pipe, 'block', 'mana_pipe.png')
    disp = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            disp.putpixel((x, y), copper[rnd.randrange(4)])
    for x in range(5, 11):
        for y in range(5, 11):
            disp.putpixel((x, y), hexc('#5A5560'))
    save(disp, 'block', 'mana_dispenser_side.png')
    for mode, color in (('fill', '#B44CF0'), ('drain', '#2FD6E0')):
        img = top.copy()
        for x in range(5, 11):
            for y in range(5, 11):
                img.putpixel((x, y), hexc(color))
        save(img, 'block', 'mana_dispenser_' + mode + '.png')

    pal = {'m': '#5A5560', 'M': '#7A7580', 'r': '#D88A6A', 'w': '#6B3A1E', 'W': '#8E5530', 'o': '#FF9A2E', 'k': '#2A1B12'}
    save(sprite([
        '................',
        '................',
        '................',
        '.mmmmmmmmmmMr...',
        '.MMMMMMMMMmmrr..',
        '.......mrrrrmm..',
        '.......moomrr...',
        '........mmm.Ww..',
        '...........WWw..',
        '..........WWww..',
        '.........WWww...',
        '.........Www....',
        '.........ww.....',
        '................',
        '................',
        '................',
    ], pal), 'item', 'revolver.png')
    save(sprite([
        '................',
        '................',
        '................',
        '................',
        'mmmmmmmmmMr.....',
        'MMMMMMMMMmrr....',
        'mmmmmmmmmMrr....',
        '.......mmrrrWW..',
        '..........WWWWw.',
        '.........WWwwww.',
        '........WWwww...',
        '.......Wwww.....',
        '................',
        '................',
        '................',
        '................',
    ], pal), 'item', 'sawn_off.png')
    save(sprite([
        '................',
        '................',
        '................',
        '......oo........',
        '.....oOOo.......',
        '.....rrrr.......',
        '.....rRRr.......',
        '.....rRRr.......',
        '.....rRRr.......',
        '.....rrrr.......',
        '.....kkkk.......',
        '................',
        '................',
        '................',
        '................',
        '................',
    ], {'o': '#B44CF0', 'O': '#F4D8FF', 'r': '#D88A6A', 'R': '#E7A08B', 'k': '#8E4B3B'}), 'item', 'magic_cartridge.png')
    save(sprite([
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '.......oO.......',
        '......ooOO......',
        '.......oO.......',
        '................',
    ] + ['................'] * 6, {'o': '#FF8C1A', 'O': '#FFD08A'}), 'item', 'bullet.png')
    save(sprite([
        'll............ll',
        '.ll..........ll.',
        '..ll........ll..',
        '...ll......ll...',
        '....llllllll....',
        '....lololoLl....',
        '....lrlrlrll....',
        '....llllllll....',
        '...ll......ll...',
        '..ll........ll..',
        '.ll..........ll.',
        'll............ll',
    ] + ['................'] * 4, {'l': '#6B3A1E', 'L': '#8E5530', 'o': '#B44CF0', 'r': '#D88A6A'}), 'item', 'bandolier.png')
    save(sprite([
        '................',
        '................',
        '......gggg......',
        '.....gGAAGg.....',
        '....gg.AA.gg....',
        '....g......g....',
        '....g......g....',
        '....g......g....',
        '....gg....gg....',
        '.....gg..gg.....',
        '......gggg......',
    ] + ['................'] * 5, {'g': '#E8C030', 'G': '#FFE08A', 'A': '#B44CF0'}), 'item', 'counterfeit_ring.png')
    save(sprite([
        '....s......s....',
        '.....s....s.....',
        '......s..s......',
        '.......ss.......',
        '......gggg......',
        '.....gEEEEg.....',
        '....gEeEEEEg....',
        '....gEEEEEEg....',
        '.....gEEEEg.....',
        '......gggg......',
    ] + ['................'] * 6, {'s': '#C9C2B0', 'g': '#E8C030', 'E': '#2FB86A', 'e': '#B8FFD0'}), 'item', 'counterfeit_pendant.png')
    save(sprite([
        '................',
        '................',
        '................',
        '................',
        'llllllllllllllll',
        'l.rrrr....rrrr.l',
        '.rGGGGr..rGGGGr.',
        '.rGgGGrrrrGgGGr.',
        '.rGGGGr..rGGGGr.',
        '..rrrr....rrrr..',
    ] + ['................'] * 6, {'l': '#6B3A1E', 'r': '#D88A6A', 'G': '#FF9A2E', 'g': '#FFE0A0'}), 'item', 'mana_goggles.png')
    save(sprite([
        '................',
        '..kkkkkkkkkkk...',
        '..kPPPPPPPPPkb..',
        '..kPPPPPPPPPkb..',
        '..kPPoooooPPkb..',
        '..kPPoPPPoPPkb..',
        '..kPPoooooPPkb..',
        '..kPPPPoPPPPkb..',
        '..kPPPoooPPPkb..',
        '..kPPPPPPPPPkb..',
        '..kPPPPPPPPPkb..',
        '..kkkkkkkkkkkb..',
        '...bbbbbbbbbbb..',
        '................',
        '................',
        '................',
    ], {'k': '#3A2A1A', 'P': '#C9A36A', 'o': '#FF9A2E', 'b': '#E8D8B0'}), 'item', 'false_mage_guide.png')


if __name__ == '__main__':
    research()
    recipes()
    curios()
    models()
    textures()
    print('batch 7 written')
