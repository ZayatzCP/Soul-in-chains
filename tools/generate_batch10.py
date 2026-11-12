#!/usr/bin/env python3
"""1.8.0: designer textures (glyphs, mantle, notebook, 3D revolver), Create-like connected Mana Tank,
Mana Pump, Wild Mudzya Hive and an archwood Mudzya Hive. Usage: generate_batch10.py <ASSETS folder>"""
import json
import math
import os
import shutil
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from generate_batch8 import w, shaped, tag, it, NS, D, A, R  # noqa: E402

SRC = sys.argv[1]
TEX = A + 'textures/'


def copy(name, target):
    shutil.copyfile(os.path.join(SRC, name), TEX + target)
    print('copied', name, '->', target)


# ---------------------------------------------------------------- designer textures
GLYPHS = {'Beam': 'beam', 'Blackhole': 'black_hole', 'MeteorRain': 'meteor_shower', 'Tornado': 'tornado',
          'mark': 'arcane_mark', 'missile': 'arcane_missile', 'parasitism': 'parasitism', 'pursuit': 'pursuit',
          'slowmo': 'time_warp', 'splitting': 'shrapnel', 'wheather': 'weather_shift', 'Decline': 'counterspell'}
for src, glyph in GLYPHS.items():
    copy(src + '.png', 'item/glyph_' + glyph + '.png')
copy('mantle_of_reflection.png', 'item/worn/mantle_of_reflection.png')
copy('mantle_of_reflection_item.png', 'item/mantle_of_reflection.png')
copy('all_assets.png', 'item/researcher_notebook_3d.png')
copy('revolver.png', 'item/revolver_3d.png')


# ---------------------------------------------------------------- 3D revolver (Blockbench free rotations -> Java)
def rot_x(p, o, quarter):
    """Rotates point p about origin o by quarter * 90 degrees around x (Blockbench / three.js sense)."""
    y, z = p[1] - o[1], p[2] - o[2]
    for _ in range(quarter % 4):
        y, z = -z, y
    return [p[0], o[1] + y, o[2] + z]


def bake_x(element, quarter):
    o = element['rotation']['origin']
    a, b = rot_x(element['from'], o, quarter), rot_x(element['to'], o, quarter)
    element['from'] = [min(a[i], b[i]) for i in range(3)]
    element['to'] = [max(a[i], b[i]) for i in range(3)]
    faces = element['faces']
    cycle = ['up', 'south', 'down', 'north']  # +90 about x: up -> south -> down -> north -> up
    moved = {}
    for name, face in faces.items():
        if name in cycle:
            moved[cycle[(cycle.index(name) + quarter) % 4]] = face
        else:
            face = dict(face)
            face['rotation'] = (face.get('rotation', 0) + 90 * quarter) % 360
            moved[name] = face
    element['faces'] = moved


def snap(angle):
    return min([-45, -22.5, 0, 22.5, 45], key=lambda a: abs(a - angle))


def revolver():
    model = json.load(open(os.path.join(SRC, 'revolver.json')))
    for element in model['elements']:
        rotation = element.get('rotation') or {}
        if 'axis' not in rotation:
            x, y, z = rotation.get('x', 0), rotation.get('y', 0), rotation.get('z', 0)
            if x % 90 == 0 and y == 0:
                bake_x(element, int(x // 90))
                element['rotation'] = {'angle': snap(z), 'axis': 'z', 'origin': rotation['origin']}
            else:
                element['rotation'] = {'angle': snap(x or y or z), 'axis': 'x' if x else 'y' if y else 'z', 'origin': rotation['origin']}
        else:
            rotation['angle'] = snap(rotation['angle'])
        for face in element['faces'].values():
            face['texture'] = '#gun'
    model['textures'] = {'gun': NS + ':item/revolver_3d', 'particle': NS + ':item/revolver_3d'}
    for key in ('format_version', 'credit', 'groups'):
        model.pop(key, None)
    # Muzzle along +z, grip around z 7: point the muzzle forward in both views, grip in the hand.
    model['display'] = {
        'thirdperson_righthand': {'rotation': [90, 180, 0], 'translation': [-1.2, 1, 6.5], 'scale': [0.8, 0.8, 0.8]},
        'thirdperson_lefthand': {'rotation': [90, 180, 0], 'translation': [-1.2, 1, 6.5], 'scale': [0.8, 0.8, 0.8]},
        'firstperson_righthand': {'rotation': [0, 180, 0], 'translation': [1.5, 5, 0], 'scale': [0.6, 0.6, 0.6]},
        'firstperson_lefthand': {'rotation': [0, 180, 0], 'translation': [1.5, 5, 0], 'scale': [0.6, 0.6, 0.6]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, -3], 'scale': [0.5, 0.5, 0.5]},
        'fixed': {'rotation': [0, 90, 0], 'translation': [0, 3, 0], 'scale': [0.6, 0.6, 0.6]},
        'head': {'rotation': [0, 180, 0], 'translation': [0, 10, -6], 'scale': [0.8, 0.8, 0.8]},
    }
    for element in model['elements']:
        for i in range(3):
            assert -16 <= element['from'][i] <= 32 and -16 <= element['to'][i] <= 32, element
    w(A + 'models/item/revolver_3d.json', model)
    w(A + 'models/item/revolver_icon.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': NS + ':item/revolver'}})
    w(A + 'models/item/revolver.json', {'loader': 'neoforge:separate_transforms', 'gui_light': 'front',
                                        'base': {'parent': NS + ':item/revolver_3d'},
                                        'perspectives': {'gui': {'parent': NS + ':item/revolver_icon'}}})


# ---------------------------------------------------------------- Mana Tank: connected like Create's fluid tank
SIDES = ['north', 'east', 'south', 'west']
Y = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def cw(d):
    return SIDES[(SIDES.index(d) + 1) % 4]


def ccw(d):
    return SIDES[(SIDES.index(d) + 3) % 4]


def tank():
    bm = A + 'models/block/'
    for old in ['single', 'top', 'middle', 'bottom']:
        p = bm + 'mana_tank_wall_' + old + '.json'
        if os.path.exists(p):
            os.remove(p)
    for p in ['mana_tank_lid_top.json', 'mana_tank_lid_bottom.json']:
        if os.path.exists(bm + p):
            os.remove(bm + p)
    t = lambda name: NS + ':block/' + name  # noqa: E731
    # The glass wall of one block (both sides drawn, so the far wall shows through the window).
    w(bm + 'mana_tank_side.json', {'render_type': 'minecraft:translucent', 'textures': {'side': t('mana_tank_side'), 'particle': t('mana_tank_lid')},
                                   'elements': [{'from': [0, 0, 0], 'to': [16, 16, 0], 'faces': {
                                       'north': {'uv': [0, 0, 16, 16], 'texture': '#side'}, 'south': {'uv': [16, 0, 0, 16], 'texture': '#side'}}}]})
    for name, y, face in [('mana_tank_lid_up', 16, 'up'), ('mana_tank_lid_down', 0, 'down')]:
        w(bm + name + '.json', {'textures': {'lid': t('mana_tank_lid'), 'particle': t('mana_tank_lid')},
                                'elements': [{'from': [0, y, 0], 'to': [16, y, 16], 'faces': {
                                    'up': {'uv': [0, 0, 16, 16], 'texture': '#lid'}, 'down': {'uv': [0, 0, 16, 16], 'texture': '#lid'}}}]})
    # Frame strips, drawn only on the outer edges of the whole tank.
    e = -0.02
    frame = {'render_type': 'minecraft:cutout'}

    def strip(name, frm, to, faces):
        w(bm + name + '.json', dict(frame, textures={'frame': t('mana_tank_frame'), 'particle': t('mana_tank_lid')},
                                    elements=[{'from': frm, 'to': to, 'faces': {f: {'uv': uv, 'texture': '#frame'} for f, uv in faces.items()}}]))
    strip('mana_tank_frame_side_top', [0, 13, e], [16, 16, e], {'north': [0, 0, 16, 3]})
    strip('mana_tank_frame_side_bottom', [0, 0, e], [16, 3, e], {'north': [0, 13, 16, 16]})
    strip('mana_tank_frame_side_cw', [0, 0, e], [3, 16, e], {'north': [13, 0, 16, 16]})       # west edge of the north face
    strip('mana_tank_frame_side_ccw', [13, 0, e], [16, 16, e], {'north': [0, 0, 3, 16]})     # east edge of the north face
    strip('mana_tank_frame_up', [0, 16 - e, 0], [16, 16 - e, 3], {'up': [0, 0, 16, 3]})        # north edge of the lid
    strip('mana_tank_frame_down', [0, e, 0], [16, e, 3], {'down': [0, 13, 16, 16]})
    parts = [{'when': {'up': 'false'}, 'apply': {'model': NS + ':block/mana_tank_lid_up'}},
             {'when': {'down': 'false'}, 'apply': {'model': NS + ':block/mana_tank_lid_down'}}]

    def add(when, model, y):
        apply = {'model': NS + ':block/' + model}
        if y:
            apply['y'] = y
        parts.append({'when': when, 'apply': apply})
    for d in SIDES:
        y = Y[d]
        add({d: 'false'}, 'mana_tank_side', y)
        add({d: 'false', 'up': 'false'}, 'mana_tank_frame_side_top', y)
        add({d: 'false', 'down': 'false'}, 'mana_tank_frame_side_bottom', y)
        add({d: 'false', ccw(d): 'false'}, 'mana_tank_frame_side_cw', y)
        add({d: 'false', cw(d): 'false'}, 'mana_tank_frame_side_ccw', y)
        add({'up': 'false', d: 'false'}, 'mana_tank_frame_up', y)
        add({'down': 'false', d: 'false'}, 'mana_tank_frame_down', y)
    w(A + 'blockstates/mana_tank.json', {'multipart': parts})
    faces = {f: {'uv': [0, 0, 16, 16], 'texture': '#side'} for f in SIDES}
    faces.update({'up': {'uv': [0, 0, 16, 16], 'texture': '#lid'}, 'down': {'uv': [0, 0, 16, 16], 'texture': '#lid'}})
    frames = {f: {'uv': [0, 0, 16, 16], 'texture': '#frame'} for f in SIDES + ['up', 'down']}
    w(A + 'models/item/mana_tank.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
                                         'textures': {'side': t('mana_tank_side'), 'lid': t('mana_tank_lid'), 'frame': t('mana_tank_frame'),
                                                      'particle': t('mana_tank_lid')},
                                         'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces},
                                                      {'from': [-0.02, -0.02, -0.02], 'to': [16.02, 16.02, 16.02], 'faces': frames}]})


def px(img, x, y, c):
    img.putpixel((x, y), c if len(c) == 4 else c + (255,))


def tank_textures():
    copper = [(199, 123, 74), (214, 138, 90), (184, 112, 66), (226, 152, 104)]
    dark = (120, 62, 36)
    rim = (150, 82, 48)
    glass = (200, 235, 245, 60)
    gl_edge = (140, 205, 220, 210)
    side = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            c = copper[(x * 3 + y * 5 + (x * y) % 3) % 4]
            if x in (3, 12):
                c = dark                                  # plate seams around the window
            if 4 <= x <= 11:
                c = gl_edge if x in (4, 11) else glass   # a window running the whole height
            px(side, x, y, c)
    for y in (2, 13):
        for x in (1, 14):
            px(side, x, y, (240, 178, 128))               # rivets
    side.save(TEX + 'block/mana_tank_side.png')
    lid = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            c = copper[(x + 2 * y + (x * y) % 2) % 4]
            if (x + y) % 8 == 0:
                c = rim
            px(lid, x, y, c)
    for x, y in [(4, 4), (11, 4), (4, 11), (11, 11)]:
        px(lid, x, y, (240, 178, 128))
    lid.save(TEX + 'block/mana_tank_lid.png')
    frame = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                px(frame, x, y, dark)
            elif edge <= 2:
                px(frame, x, y, rim if edge == 2 else (170, 96, 56))
    for x, y in [(1, 1), (14, 1), (1, 14), (14, 14), (7, 1), (8, 14), (1, 7), (14, 8)]:
        px(frame, x, y, (240, 178, 128))
    frame.save(TEX + 'block/mana_tank_frame.png')
    for old in ['mana_tank_side_single', 'mana_tank_side_top', 'mana_tank_side_middle', 'mana_tank_side_bottom', 'mana_tank_top']:
        p = TEX + 'block/' + old + '.png'
        if os.path.exists(p):
            os.remove(p)


# ---------------------------------------------------------------- Mana Pump
def pump():
    t = NS + ':block/mana_pump'
    body = {'from': [3, 3, 3], 'to': [13, 13, 13], 'faces': {f: {'uv': [3, 3, 13, 13], 'texture': '#body'} for f in SIDES + ['up', 'down']}}
    ring = {'from': [2, 2, 1], 'to': [14, 14, 3], 'faces': {f: {'uv': [2, 2, 14, 4] if f not in ('north', 'south') else [2, 2, 14, 14], 'texture': '#ring'}
                                                             for f in SIDES + ['up', 'down']}}
    ring_back = dict(ring, **{'from': [2, 2, 13], 'to': [14, 14, 15]})
    pipe_out = {'from': [5, 5, 0], 'to': [11, 11, 1], 'faces': {f: {'uv': [5, 5, 11, 11], 'texture': '#pipe'} for f in SIDES + ['up', 'down']}}
    pipe_in = dict(pipe_out, **{'from': [5, 5, 15], 'to': [11, 11, 16]})
    arrow = {'from': [6, 13, 4], 'to': [10, 13.5, 12], 'faces': {'up': {'uv': [0, 0, 16, 16], 'texture': '#arrow'}, 'north': {'uv': [6, 0, 10, 1], 'texture': '#ring'}}}
    w(A + 'models/block/mana_pump.json', {'textures': {'body': t + '_body', 'ring': t + '_ring', 'pipe': NS + ':block/mana_pipe',
                                                       'arrow': t + '_arrow', 'particle': t + '_body'},
                                          'render_type': 'minecraft:cutout',
                                          'elements': [body, ring, ring_back, pipe_out, pipe_in, arrow]})
    rot = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    w(A + 'blockstates/mana_pump.json', {'variants': {f'facing={f}': dict({'model': NS + ':block/mana_pump'}, **r) for f, r in rot.items()}})
    w(A + 'models/item/mana_pump.json', {'parent': NS + ':block/mana_pump'})
    w(D + 'loot_table/blocks/mana_pump.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:item', 'name': NS + ':mana_pump'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    tag(R + 'data/minecraft/tags/block/mineable/pickaxe.json', [NS + ':mana_pump'])
    shaped('mana_pump', ['CAC', 'PXP', 'CCC'], {'C': 'minecraft:copper_ingot', 'A': NS + ':amber', 'P': NS + ':mana_pipe', 'X': 'minecraft:piston'})
    copper = [(199, 123, 74), (214, 138, 90), (184, 112, 66)]
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            c = copper[(x * 2 + y + (x * y) % 2) % 3]
            if x in (3, 12) or y in (3, 12):
                c = (120, 62, 36)
            if 6 <= x <= 9 and 6 <= y <= 9:
                c = (180, 76, 240) if (x + y) % 2 else (210, 120, 255)   # a Source window
            px(img, x, y, c)
    img.save(TEX + 'block/mana_pump_body.png')
    img = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            c = (120, 62, 36) if min(x, y, 15 - x, 15 - y) <= 2 else (150, 82, 48)
            if min(x, y, 15 - x, 15 - y) == 1 and (x + y) % 4 == 0:
                c = (240, 178, 128)
            px(img, x, y, c)
    img.save(TEX + 'block/mana_pump_ring.png')
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):          # an arrow pointing north (towards the outlet)
        half = max(0, 7 - y) if y < 7 else 2
        for x in range(8 - half - 1, 8 + half + 1):
            if 0 <= x < 16 and y < 15:
                px(img, x, y, (240, 160, 48) if y > 0 else (255, 220, 140))
    img.save(TEX + 'block/mana_pump_arrow.png')
    p = D + 'arcane_research/pseudo/logistics.json'
    d = json.load(open(p))
    if NS + ':mana_pump' not in d['unlocks_items']:
        d['unlocks_items'].append(NS + ':mana_pump')
    w(p, d)


# ---------------------------------------------------------------- hives
def hives():
    bm = A + 'models/block/'
    # The crafted hive: archwood planks with a dark entrance and a glowing grate on top.
    w(bm + 'mudzya_hive.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                                'textures': {'planks': 'ars_nouveau:block/archwood_planks', 'side': NS + ':block/mudzya_hive_side_overlay',
                                             'top': NS + ':block/mudzya_hive_top_overlay', 'particle': 'ars_nouveau:block/archwood_planks'},
                                'elements': [
                                    {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {f: {'uv': [0, 0, 16, 16], 'texture': '#planks', 'cullface': f}
                                                                                      for f in SIDES + ['up', 'down']}},
                                    {'from': [-0.01, -0.01, -0.01], 'to': [16.01, 16.01, 16.01], 'faces': dict(
                                        {f: {'uv': [0, 0, 16, 16], 'texture': '#side', 'cullface': f} for f in SIDES},
                                        up={'uv': [0, 0, 16, 16], 'texture': '#top', 'cullface': 'up'})}]})
    side = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                px(side, x, y, (70, 48, 92))                 # dark frame
            elif y in (5, 10) and x not in (1, 14):
                px(side, x, y, (96, 64, 128, 200))           # purple bands
    for x in range(6, 10):
        for y in range(7, 10):
            px(side, x, y, (28, 16, 40))                     # the entrance
    for x in (5, 10):
        for y in range(7, 10):
            px(side, x, y, (180, 90, 230))
    side.save(TEX + 'block/mudzya_hive_side_overlay.png')
    top = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                px(top, x, y, (70, 48, 92))
            elif 4 <= x <= 11 and 4 <= y <= 11:
                px(top, x, y, (200, 110, 250) if (x + y) % 2 else (230, 160, 255))
            elif x in (3, 12) or y in (3, 12):
                if 3 <= x <= 12 and 3 <= y <= 12:
                    px(top, x, y, (70, 48, 92))
    top.save(TEX + 'block/mudzya_hive_top_overlay.png')
    # The wild hive keeps the old purple look.
    for part in ['side', 'top', 'bottom']:
        src = TEX + 'block/mudzya_hive_' + part + '.png'
        if os.path.exists(src):
            shutil.move(src, TEX + 'block/wild_mudzya_hive_' + part + '.png')
    w(bm + 'wild_mudzya_hive.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'side': NS + ':block/wild_mudzya_hive_side', 'top': NS + ':block/wild_mudzya_hive_top', 'bottom': NS + ':block/wild_mudzya_hive_bottom'}})
    w(A + 'blockstates/wild_mudzya_hive.json', {'variants': {'': {'model': NS + ':block/wild_mudzya_hive'}}})
    w(A + 'models/item/wild_mudzya_hive.json', {'parent': NS + ':block/wild_mudzya_hive'})
    w(D + 'loot_table/blocks/wild_mudzya_hive.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': NS + ':wild_mudzya_hive', 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {
                'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]},
            {'type': 'minecraft:item', 'name': NS + ':bottle_of_source'}]}]}]})
    tag(R + 'data/minecraft/tags/block/mineable/axe.json', [NS + ':wild_mudzya_hive'])
    shaped('mudzya_hive', ['PPP', 'HSH', 'PPP'], {'P': '#c:planks/archwood', 'H': 'minecraft:honeycomb', 'S': 'ars_nouveau:source_gem'})


if __name__ == '__main__':
    revolver()
    tank()
    tank_textures()
    pump()
    hives()
    print('batch 10 written')
