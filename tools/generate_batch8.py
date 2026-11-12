#!/usr/bin/env python3
"""Batch 8: Pseudomage rework (tank as one structure, worn accessories, poncho, sombrero, amber), Mudzya
nests, druid stub. Run from the repository root; textures are only written when missing unless --force."""
import colorsys
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


def box(frm, to, tex, uv=None, rotation=None, faces=None):
    faces = faces or ["north", "east", "south", "west", "up", "down"]
    e = {"from": frm, "to": to, "faces": {f: dict({"texture": "#" + tex}, **({"uv": uv} if uv else {})) for f in faces}}
    if rotation:
        e["rotation"] = rotation
    return e


def model(path, textures, elements):
    t = dict(textures)
    t.setdefault("particle", list(textures.values())[0])
    w(path, {"textures": t, "elements": elements})


# ---------------------------------------------------------------- amber
def amber():
    rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
    w(A + 'blockstates/amber_cluster.json', {"variants": {f"facing={f}": dict({"model": NS + ":block/amber_cluster"}, **r) for f, r in rot.items()}})
    w(A + 'models/block/amber_cluster.json', {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                              "textures": {"cross": NS + ":block/amber_cluster"}})
    w(A + 'models/item/amber_cluster.json', {"parent": "minecraft:item/amethyst_bud", "textures": {"layer0": NS + ":block/amber_cluster"}})
    w(A + 'models/item/amber.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/amber"}})
    w(D + 'loot_table/blocks/amber_cluster.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": NS + ":amber_cluster", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": NS + ":amber", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]})
    tag(R + 'data/minecraft/tags/block/mineable/pickaxe.json', [NS + ':amber_cluster'])
    tag(R + 'data/c/tags/item/gems/amber.json', [NS + ':amber'])
    tag(R + 'data/c/tags/item/gems.json', ['#c:gems/amber'])
    cf, pf = D + 'worldgen/configured_feature/', D + 'worldgen/placed_feature/'
    w(cf + 'amber.json', {"type": NS + ":amber", "config": {}})
    w(pf + 'amber_surface.json', {"feature": NS + ":amber", "placement": [
        {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(pf + 'amber_cave.json', {"feature": NS + ":amber", "placement": [
        {"type": "minecraft:count", "count": 8}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 0}, "max_inclusive": {"absolute": 62}}},
        {"type": "minecraft:biome"}]})
    w(D + 'neoforge/biome_modifier/amber.json', {"type": "neoforge:add_features", "biomes": "#c:is_desert",
                                                 "features": [NS + ":amber_surface", NS + ":amber_cave"], "step": "vegetal_decoration"})
    # Wild Mudzya hives on beaches.
    w(cf + 'mudzya_nest.json', {"type": NS + ":mudzya_nest", "config": {}})
    w(pf + 'mudzya_nest.json', {"feature": NS + ":mudzya_nest", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 12}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}]})
    w(D + 'neoforge/biome_modifier/mudzya_nests.json', {"type": "neoforge:add_features", "biomes": "#minecraft:is_beach",
                                                        "features": NS + ":mudzya_nest", "step": "vegetal_decoration"})


# ---------------------------------------------------------------- recipes: amber instead of rose gold
def recipes():
    shaped('revolver', ["IIA", "SG ", "L  "], {"I": "minecraft:iron_ingot", "A": NS + ":amber", "S": NS + ":bottle_of_source",
                                               "G": "minecraft:gunpowder", "L": "#minecraft:planks"})
    shaped('sawn_off', ["IIA", "IGS", "L  "], {"I": "minecraft:iron_ingot", "A": NS + ":amber", "S": NS + ":bottle_of_source",
                                               "G": "minecraft:gunpowder", "L": "#minecraft:planks"})
    w(D + 'recipe/magic_cartridge.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
                                         "ingredients": [it("minecraft:gunpowder"), it(NS + ":amber"), it("minecraft:copper_ingot"),
                                                         it(NS + ":bottle_of_source")],
                                         "result": {"count": 8, "id": NS + ":magic_cartridge"}})
    shaped('mana_goggles', ["L L", "GAG"], {"L": "minecraft:leather", "G": "minecraft:glass", "A": NS + ":amber"})
    shaped('counterfeit_ring', [" G ", "GAG", " G "], {"G": "minecraft:gold_nugget", "A": NS + ":amber"})
    shaped('counterfeit_pendant', [" S ", "S S", " A "], {"S": "minecraft:string", "A": NS + ":amber"})
    shaped('bandolier', ["L L", "ALA", "L L"], {"L": "minecraft:leather", "A": NS + ":amber"})
    shaped('poncho', ["WAW", "WWW", "W W"], {"W": "#minecraft:wool", "A": NS + ":amber"})
    shaped('sombrero', [" H ", "HAH", "SSS"], {"H": "minecraft:wheat", "A": NS + ":amber", "S": "minecraft:hay_block"})
    shaped('false_mage_guide', ["NPP", "GPP", "NPP"], {"N": NS + ":amber", "G": "minecraft:gunpowder", "P": NS + ":old_parchment"})
    shaped('druid_herbarium', ["LPP", "SPP", "LPP"], {"L": "#minecraft:leaves", "S": "#minecraft:saplings", "P": NS + ":old_parchment"})
    p = D + 'arcane_research/pseudo/counterfeit.json'
    d = json.load(open(p))
    for item in [NS + ':poncho', NS + ':sombrero']:
        if item not in d['unlocks_items']:
            d['unlocks_items'].append(item)
    w(p, d)
    tag(R + 'data/curios/tags/item/back.json', [NS + ':poncho'])
    tag(R + 'data/curios/tags/item/head.json', [NS + ':sombrero'])


def druid():
    w(D + 'arcane_research/druid/awakening.json', {
        "category": "biological", "icon": "minecraft:oak_sapling", "sort": 300,
        "subjects": ["minecraft:oak_sapling", "minecraft:moss_block", "minecraft:bone_meal"],
        "properties": ["living", "rooted"],
        "experiments": [
            {"id": "grow", "any_of": [{"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_grow", "block": "*"}]},
            {"id": "harvest", "any_of": [{"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_harvest", "block": "*"}]},
            {"id": "heal", "any_of": [{"trigger": NS + ":spell_effect", "glyph": "ars_nouveau:glyph_heal", "entity": "*"}]},
        ],
        "notes": 1})
    w(A + 'models/item/druid_herbarium.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/druid_herbarium"}})


# ---------------------------------------------------------------- tank: one structure
def tank():
    bm = A + 'models/block/'
    t = {"tex": "", "particle": NS + ":block/mana_tank_top"}
    kinds = {"single": "mana_tank_side_single", "top": "mana_tank_side_top", "middle": "mana_tank_side_middle", "bottom": "mana_tank_side_bottom"}
    for kind, tex in kinds.items():
        w(bm + 'mana_tank_wall_' + kind + '.json', {"render_type": "minecraft:translucent",
                                                    "textures": {"side": NS + ":block/" + tex, "particle": NS + ":block/mana_tank_top"},
                                                    "elements": [{"from": [0, 0, 0], "to": [16, 16, 0.01], "faces": {
                                                        "north": {"texture": "#side"}, "south": {"texture": "#side"}}}]})
    w(bm + 'mana_tank_lid_top.json', {"textures": {"lid": NS + ":block/mana_tank_top", "particle": NS + ":block/mana_tank_top"},
                                      "elements": [{"from": [0, 15.99, 0], "to": [16, 16, 16], "faces": {"up": {"texture": "#lid"}, "down": {"texture": "#lid"}}}]})
    w(bm + 'mana_tank_lid_bottom.json', {"textures": {"lid": NS + ":block/mana_tank_top", "particle": NS + ":block/mana_tank_top"},
                                         "elements": [{"from": [0, 0, 0], "to": [16, 0.01, 16], "faces": {"up": {"texture": "#lid"}, "down": {"texture": "#lid"}}}]})
    yrot = {"north": 0, "east": 90, "south": 180, "west": 270}
    parts = [{"when": {"up": "false"}, "apply": {"model": NS + ":block/mana_tank_lid_top"}},
             {"when": {"down": "false"}, "apply": {"model": NS + ":block/mana_tank_lid_bottom"}}]
    for side, y in yrot.items():
        for kind, (up, down) in {"single": ("false", "false"), "top": ("false", "true"), "middle": ("true", "true"), "bottom": ("true", "false")}.items():
            apply = {"model": NS + ":block/mana_tank_wall_" + kind}
            if y:
                apply["y"] = y
            parts.append({"when": {side: "false", "up": up, "down": down}, "apply": apply})
    w(A + 'blockstates/mana_tank.json', {"multipart": parts})
    w(A + 'models/item/mana_tank.json', {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                                         "textures": {"side": NS + ":block/mana_tank_side_single", "lid": NS + ":block/mana_tank_top",
                                                      "particle": NS + ":block/mana_tank_top"},
                                         "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                                             "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
                                             "west": {"texture": "#side"}, "up": {"texture": "#lid"}, "down": {"texture": "#lid"}}}]})
    old = bm + 'mana_tank.json'
    if os.path.exists(old):
        os.remove(old)


# ---------------------------------------------------------------- worn accessories
def worn():
    wm = A + 'models/item/worn/'
    # Bandolier: a strap across the chest and back, with cartridges when loaded.
    strap = [box([7.25, -7, 5.5], [8.75, 11, 5.8], "s", [0, 0, 2, 16], {"angle": 45, "axis": "z", "origin": [8, 2, 5.6]}),
             box([7.25, -7, 10.2], [8.75, 11, 10.5], "s", [0, 0, 2, 16], {"angle": 45, "axis": "z", "origin": [8, 2, 10.4]}),
             box([3.6, 6.6, 5.5], [4.6, 8.4, 10.5], "s", [0, 0, 2, 4]),
             box([11.4, -4, 5.5], [12.4, -2.4, 10.5], "s", [0, 0, 2, 4])]
    model(wm + 'bandolier.json', {"s": NS + ":item/worn/bandolier_worn"}, strap)
    shells = []
    for i in range(6):
        y = -4.5 + i * 2.2
        shells.append(box([7.4, y, 5.1], [8.6, y + 1.2, 5.5], "s", [4, 0, 6, 2], {"angle": 45, "axis": "z", "origin": [8, 2, 5.3]}))
    model(wm + 'bandolier_loaded.json', {"s": NS + ":item/worn/bandolier_worn"}, strap + shells)
    # Counterfeit ring: the Ring of Reversal's shape in brass and amber.
    ring = json.load(open(wm + 'ring_of_reversal.json'))
    ring['textures'] = {k: NS + ':item/worn/counterfeit_ring_3d' for k in ring['textures']}
    w(wm + 'counterfeit_ring.json', ring)
    # Pendant: a chain and the amber on the chest.
    model(wm + 'counterfeit_pendant.json', {"p": NS + ":item/counterfeit_pendant"}, [
        box([5.6, 5.0, 5.7], [6.1, 8.1, 5.9], "p", [6, 0, 7, 1]),
        box([9.9, 5.0, 5.7], [10.4, 8.1, 5.9], "p", [9, 0, 10, 1]),
        box([6.1, 4.5, 5.7], [9.9, 5.0, 5.9], "p", [6, 0, 7, 1]),
        box([6.9, 1.8, 5.35], [9.1, 4.6, 5.75], "p", [6, 5, 10, 9])])
    # Goggles: a strap round the head and two amber lenses.
    model(wm + 'mana_goggles.json', {"g": NS + ":item/mana_goggles"}, [
        box([3.6, 11, 3.6], [12.4, 12.2, 3.9], "g", [0, 4, 16, 5]),
        box([3.6, 11, 12.1], [12.4, 12.2, 12.4], "g", [0, 4, 16, 5]),
        box([3.6, 11, 3.6], [3.9, 12.2, 12.4], "g", [0, 4, 16, 5]),
        box([12.1, 11, 3.6], [12.4, 12.2, 12.4], "g", [0, 4, 16, 5]),
        box([4.6, 10.4, 3.2], [7.4, 12.8, 3.7], "g", [2, 6, 6, 9]),
        box([8.6, 10.4, 3.2], [11.4, 12.8, 3.7], "g", [10, 6, 14, 9])])
    # Sombrero: a wide brim and a tall crown.
    model(wm + 'sombrero.json', {"h": NS + ":item/worn/sombrero"}, [
        box([-3, 15.6, -3], [19, 16.6, 19], "h", [0, 0, 16, 16]),
        box([4.2, 16.6, 4.2], [11.8, 19.5, 11.8], "h", [0, 0, 8, 8]),
        box([5, 19.5, 5], [11, 21.5, 11], "h", [0, 0, 6, 6]),
        box([4.1, 16.6, 4.1], [11.9, 17.4, 11.9], "h", [0, 12, 16, 13])])
    # Poncho: over the body and the upper arms.
    model(wm + 'poncho.json', {"p": NS + ":item/worn/poncho"}, [
        box([3.4, -2.5, 5.4], [12.6, 8.3, 10.6], "p", [0, 0, 16, 16]),
        box([3.4, -3.3, 5.4], [12.6, -2.5, 5.7], "p", [0, 14, 16, 16])])
    model(wm + 'poncho_right.json', {"p": NS + ":item/worn/poncho"}, [box([6.6, 3.5, 5.6], [11.4, 10.4, 10.4], "p", [0, 0, 8, 8])])
    model(wm + 'poncho_left.json', {"p": NS + ":item/worn/poncho"}, [box([4.6, 3.5, 5.6], [9.4, 10.4, 10.4], "p", [8, 0, 16, 8])])
    im = A + 'models/item/'
    for name in ['poncho', 'sombrero']:
        w(im + name + '.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}})
    w(im + 'bandolier.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/bandolier"}, "overrides": [
        {"predicate": {NS + ":filled": 0.01}, "model": NS + ":item/bandolier_1"},
        {"predicate": {NS + ":filled": 0.4}, "model": NS + ":item/bandolier_2"},
        {"predicate": {NS + ":filled": 0.8}, "model": NS + ":item/bandolier_3"}]})
    for i in (1, 2, 3):
        w(im + f'bandolier_{i}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + f":item/bandolier_{i}"}})


# ---------------------------------------------------------------- textures
def save(img, *path):
    p = os.path.join(ROOT, *path)
    if os.path.exists(p) and not FORCE:
        return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', os.path.relpath(p))


def textures():
    rnd = random.Random(31)
    copper = [hexc(c) for c in ['#C77B4A', '#D88A5A', '#B06A3A', '#E8A07A']]
    rim = hexc('#8E4B2B')
    glass = (190, 225, 240, 70)
    edge = (150, 200, 215, 200)

    def wall(top, bottom):
        img = Image.new('RGBA', (16, 16))
        for x in range(16):
            for y in range(16):
                c = copper[rnd.randrange(4)]
                if 4 <= x <= 11 and (top is False or y >= 3) and (bottom is False or y <= 12):
                    c = edge if x in (4, 11) else glass
                if (top and y <= 1) or (bottom and y >= 14) or x in (0, 15):
                    c = rim if (y in (0, 15) and ((top and y == 0) or (bottom and y == 15))) or x in (0, 15) else copper[rnd.randrange(4)]
                img.putpixel((x, y), c)
        if top:
            for x in range(4, 12):
                img.putpixel((x, 3), edge)
        if bottom:
            for x in range(4, 12):
                img.putpixel((x, 12), edge)
        return img
    save(wall(True, True), 'block', 'mana_tank_side_single.png')
    save(wall(True, False), 'block', 'mana_tank_side_top.png')
    save(wall(False, False), 'block', 'mana_tank_side_middle.png')
    save(wall(False, True), 'block', 'mana_tank_side_bottom.png')
    liquid = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            liquid.putpixel((x, y), hexc(['#B44CF0', '#9A3AD8', '#C870FF', '#A845E8'][(x * 7 + y * 3 + rnd.randrange(2)) % 4]))
    save(liquid, 'block', 'mana_tank_liquid.png')

    save(sprite([
        '................',
        '................',
        '................',
        '................',
        '.......a........',
        '......aAa.......',
        '.....aAAAa......',
        '.....aAOAa..a...',
        '....aAAOAAa.Aa..',
        '....aAAAAAaaAAa.',
        '.a..aAAAAAaAAAa.',
        'aAa.aaaaaaaaaaa.',
        'aAAa............',
        'aaaa............',
        '................',
        '................',
    ], {'a': '#B8621A', 'A': '#F0A030', 'O': '#FFE0A0'}), 'block', 'amber_cluster.png')
    save(sprite([
        '................',
        '................',
        '................',
        '.......aa.......',
        '......aAAa......',
        '.....aAOAAa.....',
        '....aAAOAAAa....',
        '....aAAAAAAa....',
        '....aAAAAAAa....',
        '.....aAAAAa.....',
        '......aAAa......',
        '.......aa.......',
        '................',
        '................',
        '................',
        '................',
    ], {'a': '#B8621A', 'A': '#F0A030', 'O': '#FFE0A0'}), 'item', 'amber.png')
    serape = {'r': '#B83A2A', 'o': '#E0902A', 'y': '#F0D060', 'g': '#3E7A3A', 'b': '#6B3A1E', 'f': '#E8D8B0'}
    rows = []
    stripes = 'rrooyyggrrooyybb'
    for y in range(16):
        rows.append(''.join(stripes[(y + (x // 4)) % 16] if y < 14 else ('f' if x % 2 == 0 else '.') for x in range(16)))
    save(sprite(rows, serape), 'item', 'worn', 'poncho.png')
    save(sprite([
        '................',
        '................',
        '.....rrrrrr.....',
        '....rrooyyrr....',
        '...rrooyyggrr...',
        '..rrooyyggrroo..',
        '..roooyyggrrooo.',
        '..rooyyggrrooyy.',
        '..ryyggrrooyygg.',
        '..rggrrooyyggrr.',
        '..rrrooyyggrrooy',
        '...f.f.f.f.f.f..',
        '................',
        '................',
        '................',
        '................',
    ], serape), 'item', 'poncho.png')
    straw = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            straw.putpixel((x, y), hexc(['#E8C870', '#D8B058', '#F0D890', '#C89A48'][(x + 2 * y + rnd.randrange(2)) % 4]))
    for x in range(16):
        straw.putpixel((x, 12), hexc('#B83A2A'))
    save(straw, 'item', 'worn', 'sombrero.png')
    save(sprite([
        '................',
        '................',
        '................',
        '................',
        '......cccc......',
        '.....cccccc.....',
        '.....cccccc.....',
        '....rrrrrrrr....',
        '.bbbbbbbbbbbbbb.',
        'bbbbbbbbbbbbbbbb',
        '.bbbbbbbbbbbbbb.',
        '................',
        '................',
        '................',
        '................',
        '................',
    ], {'c': '#E8C870', 'r': '#B83A2A', 'b': '#D8B058'}), 'item', 'sombrero.png')
    leather = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            leather.putpixel((x, y), hexc(['#6B3A1E', '#7A4526', '#5A3018'][(x + y) % 3]))
    for x in range(4, 6):
        for y in range(0, 2):
            leather.putpixel((x, y), hexc('#F0A030'))
    save(leather, 'item', 'worn', 'bandolier_worn.png')
    base = Image.open(os.path.join(ROOT, 'item', 'bandolier.png')).convert('RGBA')
    for level, shells in ((1, 2), (2, 4), (3, 6)):
        img = base.copy()
        for i in range(shells):
            x, y = 4 + i, 4 + i
            img.putpixel((x, y), hexc('#F0A030'))
            img.putpixel((x + 1, y), hexc('#FFE0A0'))
        save(img, 'item', f'bandolier_{level}.png')
    ring = Image.open(os.path.join(ROOT, 'item', 'worn', 'ring_of_reversal_3d.png')).convert('RGBA')
    out = Image.new('RGBA', ring.size)
    for x in range(ring.width):
        for y in range(ring.height):
            r, g, b, a = ring.getpixel((x, y))
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hls_to_rgb(0.1, l, min(1, s * 1.2 + 0.2))
            out.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
    save(out, 'item', 'worn', 'counterfeit_ring_3d.png')
    save(sprite([
        '................',
        '..ggggggggggg...',
        '..gLLLLLLLLLgk..',
        '..gLLLLLLLLLgk..',
        '..gLLLfLfLLLgk..',
        '..gLLLLfLLLLgk..',
        '..gLLfffffLLgk..',
        '..gLLLLfLLLLgk..',
        '..gLLLfLfLLLgk..',
        '..gLLLLLLLLLgk..',
        '..gLLLLLLLLLgk..',
        '..gggggggggggk..',
        '...kkkkkkkkkkk..',
        '................',
        '................',
        '................',
    ], {'g': '#3E6A2A', 'L': '#7AAA4A', 'f': '#E8F0B0', 'k': '#E8D8B0'}), 'item', 'druid_herbarium.png')


if __name__ == '__main__':
    amber()
    recipes()
    druid()
    tank()
    worn()
    textures()
    print('batch 8 written')
