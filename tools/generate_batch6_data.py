#!/usr/bin/env python3
"""Data and models of batch 6: Mushroom Caves biome and worldgen, Mudzya, Gribushka trades, Alchemical
Distiller ingredients, block/item models, loot tables, tags, recipes. Run from the repository root."""
import copy
import json
import os

R = 'src/main/resources/'
D = R + 'data/arcane_artifacts/'
A = R + 'assets/arcane_artifacts/'
NS = 'arcane_artifacts'
VANILLA = '/home/user/mcmeta/data/minecraft/'

MUSHROOMS = ['blue_mushroom', 'sourcecap', 'brawncap', 'embercap', 'swiftcap', 'leapcap', 'mendcap',
             'ghostcap', 'toxcap', 'stonecap', 'tidecap']


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


# ---------------------------------------------------------------- biome
def biome():
    lush = json.load(open(VANILLA + 'worldgen/biome/lush_caves.json'))
    b = copy.deepcopy(lush)
    b['temperature'] = 0.6
    b['downfall'] = 0.2
    b['effects']['fog_color'] = 0x9FB8FF
    b['effects']['particle'] = {"options": {"type": "minecraft:warped_spore"}, "probability": 0.006}
    feats = b['features']
    feats[7] = [NS + ':mushroom_moss_coat']
    feats[9] = ['minecraft:glow_lichen', NS + ':huge_blue_mushroom'] + [NS + ':patch_' + m for m in MUSHROOMS] + [NS + ':gribushka_colony']
    b['spawners'] = {
        "ambient": [{"type": "minecraft:bat", "maxCount": 8, "minCount": 8, "weight": 10}],
        "axolotls": [], "creature": [], "misc": [], "underground_water_creature": [], "water_ambient": [], "water_creature": [],
        "monster": [
            {"type": "minecraft:zombie", "maxCount": 3, "minCount": 1, "weight": 40},
            {"type": "minecraft:skeleton", "maxCount": 3, "minCount": 1, "weight": 40},
            {"type": "minecraft:spider", "maxCount": 2, "minCount": 1, "weight": 30},
            {"type": "minecraft:witch", "maxCount": 1, "minCount": 1, "weight": 10},
        ],
    }
    w(D + 'worldgen/biome/mushroom_caves.json', b)
    tag(R + 'data/minecraft/tags/worldgen/biome/is_overworld.json', [NS + ':mushroom_caves'])
    tag(R + 'data/minecraft/tags/worldgen/biome/has_structure/mineshaft.json', [NS + ':mushroom_caves'])
    tag(R + 'data/minecraft/tags/worldgen/biome/stronghold_biased_to.json', [NS + ':mushroom_caves'])


def underground_placement(count_mod):
    return count_mod + [
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                      "min_inclusive": {"above_bottom": 8}, "max_inclusive": {"absolute": 56}}},
        {"type": "minecraft:environment_scan", "direction_of_search": "down", "max_steps": 12,
         "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
         "target_condition": {"type": "minecraft:solid"}},
        {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1},
        {"type": "minecraft:biome"},
    ]


def worldgen():
    cf = D + 'worldgen/configured_feature/'
    pf = D + 'worldgen/placed_feature/'
    w(cf + 'mushroom_moss_coat.json', {"type": NS + ":mushroom_moss_coat", "config": {}})
    w(pf + 'mushroom_moss_coat.json', {"feature": NS + ":mushroom_moss_coat", "placement": []})
    w(cf + 'huge_blue_mushroom.json', {"type": NS + ":huge_blue_mushroom", "config": {}})
    w(pf + 'huge_blue_mushroom.json', {"feature": NS + ":huge_blue_mushroom",
                                        "placement": underground_placement([{"type": "minecraft:count", "count": 10}])})
    w(cf + 'gribushka_colony.json', {"type": NS + ":gribushka_colony", "config": {}})
    w(pf + 'gribushka_colony.json', {"feature": NS + ":gribushka_colony",
                                      "placement": underground_placement([{"type": "minecraft:rarity_filter", "chance": 4}])})
    for m in MUSHROOMS:
        block = NS + ':' + m
        w(cf + 'patch_' + m + '.json', {"type": "minecraft:random_patch", "config": {
            "tries": 32, "xz_spread": 6, "y_spread": 2,
            "feature": {"feature": {"type": "minecraft:simple_block", "config": {
                "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": block}}}},
                "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                    {"type": "minecraft:would_survive", "state": {"Name": block}}]}}]}}})
        count = 14 if m == 'blue_mushroom' else 2
        w(pf + 'patch_' + m + '.json', {"feature": NS + ':patch_' + m,
                                         "placement": underground_placement([{"type": "minecraft:count", "count": count}])})


# ---------------------------------------------------------------- creatures
def creatures():
    w(D + 'neoforge/biome_modifier/mudzya_spawns.json', {
        "type": "neoforge:add_spawns", "biomes": "#minecraft:is_beach",
        "spawners": {"type": NS + ":mudzya", "weight": 14, "minCount": 2, "maxCount": 4}})
    tag(D + 'tags/item/mudzya_food.json', ["ars_nouveau:sourceberry_bush"])
    w(D + 'loot_table/entities/mudzya.json', {"type": "minecraft:entity", "pools": [], "random_sequence": NS + ":entities/mudzya"})
    w(D + 'loot_table/entities/gribushka.json', {"type": "minecraft:entity", "random_sequence": NS + ":entities/gribushka", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": NS + ":blue_mushroom",
                                                     "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]}]})
    trades = {
        NS + ':blue_mushroom': [("minecraft:glow_berries", 2, 4), ("minecraft:glowstone_dust", 1, 3)],
        NS + ':sourcecap': [("ars_nouveau:source_gem", 1, 2)],
        NS + ':brawncap': [("minecraft:iron_ingot", 1, 2)],
        NS + ':embercap': [("minecraft:blaze_powder", 1, 2)],
        NS + ':swiftcap': [("minecraft:sugar", 3, 6)],
        NS + ':leapcap': [("minecraft:rabbit_foot", 1, 1)],
        NS + ':mendcap': [("minecraft:glistering_melon_slice", 1, 2)],
        NS + ':ghostcap': [("minecraft:phantom_membrane", 1, 1)],
        NS + ':toxcap': [("minecraft:spider_eye", 2, 3)],
        NS + ':stonecap': [("minecraft:amethyst_shard", 2, 4)],
        NS + ':tidecap': [("minecraft:prismarine_crystals", 2, 4)],
        'minecraft:brown_mushroom': [("minecraft:bone_meal", 3, 6)],
        'minecraft:red_mushroom': [("minecraft:redstone", 2, 4)],
        'minecraft:crimson_fungus': [("minecraft:nether_wart", 1, 3)],
        'minecraft:warped_fungus': [("minecraft:ender_pearl", 1, 1)],
    }
    for item, entries in trades.items():
        ns, path = item.split(':')
        w(D + f'loot_table/gribushka_trades/{ns}/{path}.json', {"type": "minecraft:barter", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [
                {"type": "minecraft:item", "name": name, "weight": 1,
                 "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
                for name, lo, hi in entries]}]})


# ---------------------------------------------------------------- distiller ingredients
def distiller():
    values = {
        "minecraft:sugar": ("minecraft:speed", 3600),
        "minecraft:rabbit_foot": ("minecraft:jump_boost", 3600),
        "minecraft:blaze_powder": ("minecraft:strength", 3600),
        "minecraft:glistering_melon_slice": ("minecraft:instant_health", 1),
        "minecraft:spider_eye": ("minecraft:poison", 900),
        "minecraft:ghast_tear": ("minecraft:regeneration", 900),
        "minecraft:magma_cream": ("minecraft:fire_resistance", 3600),
        "minecraft:pufferfish": ("minecraft:water_breathing", 3600),
        "minecraft:golden_carrot": ("minecraft:night_vision", 3600),
        "minecraft:phantom_membrane": ("minecraft:slow_falling", 1800),
        "minecraft:fermented_spider_eye": ("minecraft:weakness", 1800),
        "minecraft:turtle_scute": ("minecraft:resistance", 1200),
        NS + ":blue_mushroom": ("minecraft:night_vision", 4800),
        NS + ":sourcecap": ("ars_nouveau:mana_regen", 3600),
        NS + ":brawncap": ("minecraft:strength", 3600),
        NS + ":embercap": ("minecraft:fire_resistance", 4800),
        NS + ":swiftcap": ("minecraft:speed", 4800),
        NS + ":leapcap": ("minecraft:jump_boost", 4800),
        NS + ":mendcap": ("minecraft:regeneration", 1200),
        NS + ":ghostcap": ("minecraft:invisibility", 3600),
        NS + ":toxcap": ("minecraft:poison", 1200),
        NS + ":stonecap": ("minecraft:resistance", 2400),
        NS + ":tidecap": ("minecraft:water_breathing", 4800),
        NS + ":bottle_of_source": ("ars_nouveau:mana_regen", 2400),
    }
    w(D + 'data_maps/item/distiller_ingredients.json', {"values": {
        item: {"effect": effect, "duration": duration} for item, (effect, duration) in values.items()}})


# ---------------------------------------------------------------- blocks: models, loot, tags, recipes
def blocks():
    bs, bm, im = A + 'blockstates/', A + 'models/block/', A + 'models/item/'
    # Mushroom moss: tinted (blue to purple by position).
    w(bs + 'mushroom_moss.json', {"variants": {"": {"model": NS + ":block/mushroom_moss"}}})
    w(bm + 'mushroom_moss.json', {"parent": "minecraft:block/leaves", "render_type": "minecraft:solid",
                                   "textures": {"all": NS + ":block/mushroom_moss"}})
    w(im + 'mushroom_moss.json', {"parent": NS + ":block/mushroom_moss"})
    # Blue mushroom cap: same faces logic as the brown huge mushroom.
    vanilla = json.load(open('/home/user/mcassets/assets/minecraft/blockstates/brown_mushroom_block.json'))
    state = json.loads(json.dumps(vanilla).replace('minecraft:block/brown_mushroom_block', NS + ':block/blue_mushroom_block'))
    w(bs + 'blue_mushroom_block.json', state)
    w(bm + 'blue_mushroom_block.json', {"parent": "minecraft:block/template_single_face", "textures": {"texture": NS + ":block/blue_mushroom_block"}})
    w(bm + 'blue_mushroom_block_inventory.json', {"parent": "minecraft:block/cube_all", "textures": {"all": NS + ":block/blue_mushroom_block"}})
    w(im + 'blue_mushroom_block.json', {"parent": NS + ":block/blue_mushroom_block_inventory"})
    for m in MUSHROOMS:
        w(bs + m + '.json', {"variants": {"": {"model": NS + ":block/" + m}}})
        w(bm + m + '.json', {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": NS + ":block/" + m}})
        w(im + m + '.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":block/" + m}})
        w(D + 'loot_table/blocks/' + m + '.json', simple_loot(NS + ':' + m))
    # Mudzya hive.
    w(bs + 'mudzya_hive.json', {"variants": {"": {"model": NS + ":block/mudzya_hive"}}})
    w(bm + 'mudzya_hive.json', {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": NS + ":block/mudzya_hive_side", "top": NS + ":block/mudzya_hive_top", "bottom": NS + ":block/mudzya_hive_bottom"}})
    w(im + 'mudzya_hive.json', {"parent": NS + ":block/mudzya_hive"})
    # Alchemical distiller: 4 parts × 4 facings.
    variants = {}
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for facing, y in rot.items():
        for part in ['bottom_left', 'bottom_right', 'top_left', 'top_right']:
            v = {"model": NS + ":block/alchemical_distiller_" + part}
            if y:
                v["y"] = y
            variants[f"facing={facing},part={part}"] = v
    w(bs + 'alchemical_distiller.json', {"variants": variants})
    distiller_models(bm)
    w(im + 'alchemical_distiller.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/alchemical_distiller"}})
    w(im + 'bottle_of_source.json', {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/bottle_of_source"}})
    for egg in ['mudzya_spawn_egg', 'gribushka_spawn_egg']:
        w(im + egg + '.json', {"parent": "minecraft:item/template_spawn_egg"})
    # Loot tables.
    w(D + 'loot_table/blocks/mushroom_moss.json', simple_loot(NS + ':mushroom_moss'))
    w(D + 'loot_table/blocks/mudzya_hive.json', simple_loot(NS + ':mudzya_hive'))
    loot = simple_loot(NS + ':alchemical_distiller')
    loot['pools'][0]['conditions'].append({"condition": "minecraft:block_state_property", "block": NS + ":alchemical_distiller",
                                           "properties": {"part": "bottom_left"}})
    w(D + 'loot_table/blocks/alchemical_distiller.json', loot)
    cap = json.load(open(VANILLA + 'loot_table/blocks/brown_mushroom_block.json'))
    cap = json.loads(json.dumps(cap).replace('minecraft:brown_mushroom_block', NS + ':blue_mushroom_block')
                     .replace('minecraft:brown_mushroom', NS + ':blue_mushroom')
                     .replace('minecraft:blocks/blue', NS + ':blocks/blue'))
    w(D + 'loot_table/blocks/blue_mushroom_block.json', cap)
    # Tags.
    tag(D + 'tags/block/mushroom_moss_replaceable.json', ["#minecraft:base_stone_overworld", "#minecraft:dirt", "minecraft:gravel"])
    tag(R + 'data/minecraft/tags/block/mushroom_grow_block.json', [NS + ':mushroom_moss'])
    tag(R + 'data/minecraft/tags/block/mineable/hoe.json', [NS + ':mushroom_moss'])
    tag(R + 'data/minecraft/tags/block/mineable/axe.json', [NS + ':blue_mushroom_block', NS + ':mudzya_hive'])
    tag(R + 'data/minecraft/tags/block/mineable/pickaxe.json', [NS + ':alchemical_distiller'])
    tag(R + 'data/minecraft/tags/block/small_flowers.json', [])
    tag(R + 'data/minecraft/tags/item/villager_plantable_seeds.json', [])
    # Recipes.
    rec = D + 'recipe/'
    w(rec + 'mudzya_hive.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "HSH", "PPP"],
                                  "key": {"P": {"tag": "minecraft:planks"}, "H": {"item": "minecraft:honeycomb"},
                                          "S": {"item": "ars_nouveau:source_gem"}},
                                  "result": {"count": 1, "id": NS + ":mudzya_hive"}})
    w(rec + 'alchemical_distiller.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GBG", "RSR", "NNN"],
                                           "key": {"G": {"item": "minecraft:glass"}, "B": {"item": "minecraft:brewing_stand"},
                                                   "R": {"item": NS + ":rose_gold_ingot"}, "S": {"item": NS + ":bottle_of_source"},
                                                   "N": {"item": "minecraft:nether_wart_block"}},
                                           "result": {"count": 1, "id": NS + ":alchemical_distiller"}})
    w(rec + 'mushroom_moss.json', {"type": "minecraft:crafting_shapeless", "category": "building",
                                    "ingredients": [{"item": "minecraft:moss_block"}, {"item": NS + ":blue_mushroom"}],
                                    "result": {"count": 2, "id": NS + ":mushroom_moss"}})
    for path in ['data/minecraft/tags/block/small_flowers.json', 'data/minecraft/tags/item/villager_plantable_seeds.json']:
        if os.path.exists(R + path) and not json.load(open(R + path))['values']:
            os.remove(R + path)


def simple_loot(item):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": item}],
                                                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


def box(frm, to, tex, faces=None):
    faces = faces or ["north", "east", "south", "west", "up", "down"]
    return {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in faces}}


def distiller_models(bm):
    """Drawn for facing=north: the player looks south at it, so the right column is to the west (-X). The pipe
    leaves the top-left alembic towards -X and enters the top-right coil from +X."""
    t = {"metal": NS + ":block/distiller_metal", "glass": NS + ":block/distiller_glass", "copper": NS + ":block/distiller_copper",
         "liquid": NS + ":block/distiller_liquid", "particle": NS + ":block/distiller_metal"}
    w(bm + 'alchemical_distiller_bottom_left.json', {"textures": t, "render_type": "minecraft:cutout", "elements": [
        box([0, 0, 0], [16, 3, 16], "metal"),
        box([2, 3, 2], [14, 12, 14], "copper"),
        box([3, 12, 3], [13, 14, 13], "metal"),
        box([6, 14, 6], [10, 16, 10], "copper"),
    ]})
    w(bm + 'alchemical_distiller_bottom_right.json', {"textures": t, "render_type": "minecraft:cutout", "elements": [
        box([0, 0, 0], [16, 3, 16], "metal"),
        box([1, 3, 1], [15, 4, 15], "metal"),
        box([2, 4, 5], [5, 11, 8], "glass"), box([2.5, 4.5, 5.5], [4.5, 8, 7.5], "liquid"),
        box([6.5, 4, 5], [9.5, 11, 8], "glass"), box([7, 4.5, 5.5], [9, 9, 7.5], "liquid"),
        box([11, 4, 5], [14, 11, 8], "glass"), box([11.5, 4.5, 5.5], [13.5, 7, 7.5], "liquid"),
        box([1, 11, 1], [15, 13, 15], "metal"),
    ]})
    w(bm + 'alchemical_distiller_top_left.json', {"textures": t, "render_type": "minecraft:translucent", "elements": [
        box([4, 0, 4], [12, 2, 12], "metal"),
        box([3, 2, 3], [13, 10, 13], "glass"),
        box([4, 2, 4], [12, 6, 12], "liquid"),
        box([6, 10, 6], [10, 14, 10], "glass"),
        box([0, 13, 7], [9, 14, 9], "copper"),
    ]})
    w(bm + 'alchemical_distiller_top_right.json', {"textures": t, "render_type": "minecraft:cutout", "elements": [
        box([7, 13, 7], [16, 14, 9], "copper"),
        box([7, 4, 7], [9, 14, 9], "copper"),
        box([4, 6, 4], [12, 7, 12], "copper"), box([4, 9, 4], [12, 10, 12], "copper"),
        box([5, 0, 5], [11, 4, 11], "metal"),
    ]})


if __name__ == '__main__':
    biome()
    worldgen()
    creatures()
    distiller()
    blocks()
    print('batch 6 data written')
