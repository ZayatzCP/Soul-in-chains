#!/usr/bin/env python3
"""1.7.1: low amber cluster sprite, Duel Focus icon, reworked Pseudomage recipes, rarer amber."""
import json
import os
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, save  # noqa: E402
from generate_batch8 import shaped, it, w, D, NS  # noqa: E402

AMBER = {'a': '#9A4E12', 'A': '#E8902A', 'O': '#FFD27A', 'h': '#FFF0C0'}
# A small cluster lying low, like a flower on the sand: it stays inside the bottom 7 pixels.
save(sprite([
    '................', '................', '................', '................',
    '................', '................', '................', '................',
    '................',
    '.......ah.......',
    '...a...AO...a...',
    '..aOa.aAOa.aAa..',
    '..aAa.aAAa.aOA..',
    '.aAOAaaAOAaAAAa.',
    '.aAAAaAAAAaAAAa.',
    '..aaaaaaaaaaaa..',
], AMBER), 'block', 'amber_cluster.png')
save(sprite([
    '..................', '........aa........', '.......aOAa.......', '......aOAAAa......',
    '.....aOhAAAAa.....', '....aAhAAAAAAa....', '...aAAAAAAAAAAa...', '...aAAAAAAAAAAa...',
    '..aAAAAa..aAAAAa..', '..aAAAa....aAAAa..', '..aAAAa....aAAAa..', '..aAAAAa..aAAAAa..',
    '...aAAAAAAAAAAa...', '...aAAAAAAAAAAa...', '....aAAAAAAAAa....', '.....aAAAAAAa.....',
    '......aaaaaa......', '..................'], AMBER), 'mob_effect', 'duel_focus.png')

L, A, S = 'minecraft:leather', NS + ':amber', 'minecraft:string'
shaped('revolver', ["IIA", "BGL", "P  "], {"I": "minecraft:iron_ingot", "A": A, "B": NS + ":bottle_of_source",
                                           "G": "minecraft:gunpowder", "L": L, "P": "#minecraft:planks"})
shaped('sawn_off', ["IIA", "IGB", "PL "], {"I": "minecraft:iron_ingot", "A": A, "B": NS + ":bottle_of_source",
                                           "G": "minecraft:gunpowder", "L": L, "P": "#minecraft:planks"})
shaped('bandolier', ["LSL", "ACA", "LSL"], {"L": L, "S": S, "A": A, "C": "minecraft:copper_ingot"})
shaped('counterfeit_ring', [" A ", "G G", " G "], {"A": A, "G": "minecraft:gold_ingot"})
shaped('counterfeit_pendant', ["SLS", "L L", " A "], {"S": S, "L": L, "A": A})
shaped('mana_goggles', ["LSL", "AIA"], {"L": L, "S": S, "A": A, "I": "minecraft:iron_ingot"})
shaped('poncho', ["WLW", "WAW", "W W"], {"W": "#minecraft:wool", "L": L, "A": A})
shaped('sombrero', [" W ", "LGL", "WLW"], {"W": "minecraft:wheat", "L": L, "G": NS + ":mana_goggles"})
shaped('mana_tank', ["CAC", "G G", "CGC"], {"C": "minecraft:copper_ingot", "A": A, "G": "minecraft:glass"})

pf = 'src/main/resources/data/arcane_artifacts/worldgen/placed_feature/'
w(pf + 'amber_surface.json', {"feature": NS + ":amber", "placement": [
    {"type": "minecraft:rarity_filter", "chance": 6}, {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
w(pf + 'amber_cave.json', {"feature": NS + ":amber", "placement": [
    {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
    {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 0}, "max_inclusive": {"absolute": 62}}},
    {"type": "minecraft:biome"}]})
print('batch 9 written')
