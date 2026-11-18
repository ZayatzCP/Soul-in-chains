#!/usr/bin/env python3
"""1.20.0: Catto's item sprites (16x16 pixel art from the maps below) and their item models.

Usage: python3 tools/generate_catto.py
"""
import json
import os

from PIL import Image

NS = 'arcane_artifacts'
A = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', NS)

PALETTE = {
    '.': None,
    'k': (40, 24, 48), 'p': (78, 40, 110), 'P': (142, 88, 190), 'L': (200, 160, 235),
    'g': (130, 86, 20), 'y': (222, 170, 48), 'Y': (255, 226, 120),
    'b': (86, 56, 34), 'B': (140, 96, 58), 'n': (186, 140, 90),
    'w': (236, 226, 200), 'W': (255, 250, 236), 'r': (160, 50, 40), 'R': (230, 90, 80),
    'c': (40, 70, 140), 'C': (80, 130, 210), 'i': (170, 205, 245),
    's': (90, 90, 100), 'S': (150, 150, 160), 'T': (200, 200, 210),
    'm': (200, 90, 140), 'M': (250, 160, 200),
    'f': (120, 200, 60), 'F': (230, 255, 120), 'o': (255, 170, 40),
    'q': (60, 160, 160), 'Q': (130, 230, 220),
    'h': (176, 132, 60), 'H': (228, 196, 110), 'v': (46, 104, 40), 'V': (100, 168, 60), 'e': (130, 36, 36),
}

SPRITES = {
    'arcane_collar': [
        '................',
        '................',
        '.....pppppp.....',
        '...ppPPLLPPpp...',
        '..pPLp....pPPp..',
        '.pPp........pPp.',
        '.pP..........Pp.',
        '.pP..........Pp.',
        '.pPp........pPp.',
        '..pPPp....pPPp..',
        '...ppPPggPPpp...',
        '......gYyg......',
        '.....gyYyyg.....',
        '.....gyyyyg.....',
        '......gkkg......',
        '................',
    ],
    'catto_bell': [
        '.......pp.......',
        '......pPPp......',
        '.......gg.......',
        '......gYyg......',
        '.....gYYyyg.....',
        '.....gYyyyg.....',
        '....gYYyyyyg....',
        '....gYyyyyyg....',
        '...gYYyyyyyyg...',
        '...gYyyyyyyyg...',
        '..gyyyyyyyyyyg..',
        '.gggggggggggggg.',
        '.gyyyyyyyyyyyyg.',
        '..ggggg..ggggg..',
        '.......gg.......',
        '.......kk.......',
    ],
    'errand_wand': [
        '............P.L.',
        '...........PLYL.',
        '............LYLP',
        '...........b.L..',
        '..........bB....',
        '.........bB.....',
        '........bB......',
        '.......bB.......',
        '......pP........',
        '.....bB.........',
        '....bB..........',
        '...bB...........',
        '..bB............',
        '.bB.............',
        '.b..............',
        '................',
    ],
    'errand_scroll': [
        '................',
        '..nnnnnnnnnnnn..',
        '.nBwwwwwwwwwwBn.',
        '..nwwwwwwwwwwn..',
        '...wssssswww....',
        '...wwwwwwwww....',
        '...wsssswwwwpp..',
        '...wwwwwwwwpPPp.',
        '...wssssssswpp..',
        '...wwwwwwwwwPp..',
        '...wsssswwwwpP..',
        '...wwwwwwwwww...',
        '..nwwwwwwwwwwn..',
        '.nBwwwwwwwwwwBn.',
        '..nnnnnnnnnnnn..',
        '................',
    ],
    'blueprint_parchment': [
        '................',
        '.cccccccccccccc.',
        '.cCCCCCCCCCCCCc.',
        '.cCiiiiiiiCCCCc.',
        '.cCiCCCCCiCCCCc.',
        '.cCiCCCCCiiiiCc.',
        '.cCiCCCCCCCCiCc.',
        '.cCiCCiiiiCCiCc.',
        '.cCiCCiCCiCCiCc.',
        '.cCiiiiCCiiiiCc.',
        '.cCCCCCCCCCCCCc.',
        '.cCCiCiCiCiCCCc.',
        '.cCCCCCCCCCCWCc.',
        '.cCCCCCCCCCWWCc.',
        '.cccccccccccccc.',
        '................',
    ],
    'traveler_pack': [
        '................',
        '.....bbbbbb.....',
        '....b......b....',
        '....b......b....',
        '..bbbbbbbbbbbb..',
        '.bBBBBBBBBBBBBb.',
        '.bBnnnnnnnnnnBb.',
        '.bBnnnnyynnnnBb.',
        '.bBBBBBgyBBBBBb.',
        '.bBBBBBBBBBBBBb.',
        '.bBBnnBBBBnnBBb.',
        '.bBBnnBBBBnnBBb.',
        '.bBBBBBBBBBBBBb.',
        '..bbbbbbbbbbbb..',
        '................',
        '................',
    ],
    'expedition_pack': [
        '.....pppppp.....',
        '....p......p....',
        '....p......p....',
        '..pppppppppppp..',
        '.pBBBBBBBBBBBBp.',
        '.pBnnnnnnnnnnBp.',
        '.pBnnnnyynnnnBp.',
        '.pPPPPPgyPPPPPp.',
        '.pBBBBBBBBBBBBp.',
        '.pBBnnBBBBnnBBp.',
        '.pBBnnBBBBnnBBp.',
        '.pBBBBBBBBBBBBp.',
        '.pPPPPPPPPPPPPp.',
        '..pppppppppppp..',
        '...bb......bb...',
        '................',
    ],
    'bottomless_pack': [
        '.....pppppp.....',
        '....p......p....',
        '....p......p....',
        '..kkkkkkkkkkkk..',
        '.kppppppppppppk.',
        '.kpPPPPPPPPPPpk.',
        '.kpPkkkkkkkkPpk.',
        '.kpPkkLkkkkkPpk.',
        '.kpPkkkkkYkkPpk.',
        '.kpPkLkkkkkkPpk.',
        '.kpPkkkkkkLkPpk.',
        '.kpPPPPPPPPPPpk.',
        '.kppppyyyyppppk.',
        '..kkkkkkkkkkkk..',
        '................',
        '................',
    ],
    'arcane_catto_hood': [
        '................',
        '...pp......pp...',
        '..pPPp....pPPp..',
        '..pPLPp..pPLPp..',
        '..pPPPPppPPPPp..',
        '..pPPPPPPPPPPp..',
        '..pPPPyYYyPPPp..',
        '..pPPPPyyPPPPp..',
        '..pPLLLLLLLLPp..',
        '..pPL......LPp..',
        '..pPL......LPp..',
        '..pPL......LPp..',
        '..pPPLLLLLLPPp..',
        '...ppppppppppp..',
        '................',
        '................',
    ],
    'arcane_catto_mantle': [
        '................',
        '.....pppppp.....',
        '....pPPyyPPp....',
        '...pPPPPPPPPp...',
        '..pPPPPLLPPPPp..',
        '..pPPPPLLPPPPp..',
        '..pPPPPPPPPPPp..',
        '.pPPPPPyYPPPPPp.',
        '.pPPPPPPPPPPPPp.',
        '.pPPPPPPPPPPPPp.',
        '.pPPPPPPPPPPPPp.',
        '.pyyyyyyyyyyyyp.',
        '..pppppppppppp..',
        '................',
        '................',
        '................',
    ],
    'catto_sombrero': [
        '................',
        '................',
        '................',
        '......hhhh......',
        '.....hHHHHh.....',
        '.....hHHHHh.....',
        '....hHHHHHHh....',
        '....heReRReh....',
        '.hhhhhhhhhhhhhh.',
        'hHHHHHHHHHHHHHHh',
        'hHHHHHHHHHHHHHHh',
        '.hhHHHHHHHHHHhh.',
        '...hhhhhhhhhh...',
        '................',
        '................',
        '................',
    ],
    'catto_poncho': [
        '................',
        '......kkkk......',
        '.....k....k.....',
        '....RRRRRRRR....',
        '...RRRRRRRRRR...',
        '..YYYYYYYYYYYY..',
        '..VVVVVVVVVVVV..',
        '.RRRRRRRRRRRRRR.',
        '.RRRRRRRRRRRRRR.',
        '.YYYYYYYYYYYYYY.',
        '.VVVVVVVVVVVVVV.',
        '.RRRRRRRRRRRRRR.',
        '.Y.Y.Y.Y.Y.Y.Y..',
        '................',
        '................',
        '................',
    ],
    'catto_druid_wreath': [
        '................',
        '................',
        '.....vVVVVv.....',
        '...vVVvMVvVVv...',
        '..vVv......vVv..',
        '.vVv........vVv.',
        '.VM..........YV.',
        '.vV..........Vv.',
        '.VY..........MV.',
        '.vVv........vVv.',
        '..vVv......vVv..',
        '...vVVvYVvVVv...',
        '.....vVVVVv.....',
        '................',
        '................',
        '................',
    ],
    'catto_druid_cloak': [
        '................',
        '.....vvvvvv.....',
        '....vVVBBVVv....',
        '...vVVVBBVVVv...',
        '..vVVVVVVVVVVv..',
        '..vVVbVVVVbVVv..',
        '..vVVVVVVVVVVv..',
        '.vVVVVVMVVVVVVv.',
        '.vVbVVVVVVVbVVv.',
        '.vVVVVVVVVVVVVv.',
        '.vVVVVYVVVVVVVv.',
        '.vVvVvVvVvVvVvv.',
        '..v.v.v.v.v.v...',
        '................',
        '................',
        '................',
    ],
    'catto_hammer': [
        '................',
        '.......sssss....',
        '......sTTTTSs...',
        '.....sTTTTTSs...',
        '......sSSSSss...',
        '.......ssBs.....',
        '........bB......',
        '.......bB.......',
        '......bB........',
        '.....bB.........',
        '....bB..........',
        '...bB...........',
        '..pP............',
        '..pp............',
        '................',
        '................',
    ],
    'catto_scythe': [
        '................',
        '....sssss.......',
        '..sTTTTTTss.....',
        '.sTT....sTTs....',
        '.ss.......sTs...',
        '...........sBs..',
        '...........bB...',
        '..........bB....',
        '.........bB.....',
        '........bB......',
        '.......bB.......',
        '......bB........',
        '.....bB.........',
        '....pp..........',
        '................',
        '................',
    ],
    'catto_hatchet': [
        '................',
        '................',
        '......sss.......',
        '.....sTTTs......',
        '....sTTTTs......',
        '....sTTTsBb.....',
        '.....sssbB......',
        '.......bB.......',
        '......bB........',
        '.....bB.........',
        '....bB..........',
        '...bB...........',
        '..pP............',
        '................',
        '................',
        '................',
    ],
    'ritual_familiar_bond': [
        '................',
        '...ssssssssss...',
        '..sSSSSSSSSSSs..',
        '..sSTTSSSSTTSs..',
        '..sSSSSSSSSSSs..',
        '..sSSPSSSSPSSs..',
        '..sSPPSSSSPPSs..',
        '..sSSSSPPSSSSs..',
        '..sSSSPPPPSSSs..',
        '..sSSPPLLPPSSs..',
        '..sSSSPPPPSSSs..',
        '..sSSSSSSSSSSs..',
        '..sSTTSSSSTTSs..',
        '..sSSSSSSSSSSs..',
        '...ssssssssss...',
        '................',
    ],
}


def draw(rows):
    assert len(rows) == 16, rows
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (row, len(row))
        for x, ch in enumerate(row):
            colour = PALETTE[ch]
            if colour is not None:
                image.putpixel((x, y), colour + (255,))
    return image


def main():
    tex = os.path.join(A, 'textures', 'item')
    models = os.path.join(A, 'models', 'item')
    for name, rows in SPRITES.items():
        draw(rows).save(os.path.join(tex, name + '.png'))
        parent = 'minecraft:item/handheld' if name in ('errand_wand', 'catto_hammer', 'catto_scythe', 'catto_hatchet') else 'minecraft:item/generated'
        with open(os.path.join(models, name + '.json'), 'w') as f:
            json.dump({'parent': parent, 'textures': {'layer0': f'{NS}:item/{name}'}}, f, indent=2)
            f.write('\n')
        print('wrote', name)


if __name__ == '__main__':
    main()


# ---------------------------------------------------------------------- the placeholder Catto (entity texture)

CREAM = (236, 218, 180)
CREAM_LIGHT = (248, 236, 210)
CREAM_SHADE = (214, 192, 150)
POINT = (110, 72, 48)
POINT_DARK = (78, 50, 34)
HOOD = (78, 46, 120)
HOOD_LIGHT = (122, 80, 170)
HOOD_DARK = (52, 30, 82)
GOLD = (232, 182, 60)
GOLD_DARK = (160, 112, 30)
PINK = (236, 150, 160)
EYE = (70, 190, 190)
LEATHER = (120, 80, 44)
LEATHER_DARK = (86, 56, 30)


def faces(u, v, w, h, d):
    """Texture rectangles (x, y, width, height) of a model box at texOffs (u, v) with size w x h x d."""
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h),
    }


def fill(img, rect, colour):
    x, y, w, h = rect
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), colour + (255,))


def px(img, x, y, colour):
    img.putpixel((x, y), colour + (255,))


def noise(img, rect, colours, seed):
    import random
    rnd = random.Random(seed)
    x, y, w, h = rect
    for i in range(x, x + w):
        for j in range(y, y + h):
            if rnd.random() < 0.18:
                px(img, i, j, rnd.choice(colours))


def catto_texture():
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    # Head 7x6x6: cream, dark mask around the muzzle and dark crown, two teal eyes.
    head = faces(0, 0, 7, 6, 6)
    for f in head.values():
        fill(img, f, CREAM)
        noise(img, f, [CREAM_SHADE, CREAM_LIGHT], 1)
    fx, fy, _, _ = head['front']
    for x in range(7):
        px(img, fx + x, fy, POINT)
    for (x, y) in [(2, 3), (3, 3), (4, 3), (3, 4), (2, 4), (4, 4), (3, 5)]:
        px(img, fx + x, fy + y, POINT)
    for (x, y, c) in [(1, 2, EYE), (5, 2, EYE), (1, 1, POINT_DARK), (5, 1, POINT_DARK)]:
        px(img, fx + x, fy + y, c)
    fill(img, head['top'], POINT)
    # Muzzle 3x2x1: light, pink nose on top of the front.
    for f in faces(28, 5, 3, 2, 1).values():
        fill(img, f, CREAM_LIGHT)
    mx, my, _, _ = faces(28, 5, 3, 2, 1)['front']
    px(img, mx + 1, my, PINK)
    # Hood 8x7x7: violet with a light trim; the face side is open.
    hood = faces(0, 32, 8, 7, 7)
    for name, f in hood.items():
        if name in ('front', 'bottom'):
            continue
        fill(img, f, HOOD)
        noise(img, f, [HOOD_DARK, HOOD_LIGHT], 2)
    hx, hy, hw, hh = hood['front']
    for x in range(hw):
        px(img, hx + x, hy, HOOD_LIGHT)
        px(img, hx + x, hy + 1, HOOD_LIGHT)
    for y in range(hh):
        px(img, hx, hy + y, HOOD_LIGHT)
        px(img, hx + hw - 1, hy + y, HOOD_LIGHT)
    px(img, hx + 3, hy, GOLD)
    px(img, hx + 4, hy, GOLD)
    tx, ty, tw, th = hood['top']
    for x in range(3, 5):
        for y in range(2, 5):
            px(img, tx + x, ty + y, GOLD if (x + y) % 2 else GOLD_DARK)
    bx, by, bw, bh = hood['back']
    for (x, y) in [(3, 2), (4, 2), (2, 3), (5, 3), (3, 4), (4, 4)]:
        px(img, bx + x, by + y, GOLD)
    # Hood ears 3x4x2: violet outside, pink inside (front).
    ear = faces(32, 32, 3, 3, 2)
    for f in ear.values():
        fill(img, f, HOOD)
    ex, ey, _, _ = ear['front']
    fill(img, (ex + 1, ey + 1, 1, 2), PINK)
    fill(img, ear['top'], HOOD_DARK)
    # Body 6x7x4: cream, light belly, a strap across the chest.
    body = faces(0, 14, 6, 7, 4)
    for f in body.values():
        fill(img, f, CREAM)
        noise(img, f, [CREAM_SHADE], 3)
    cx, cy, _, _ = body['front']
    fill(img, (cx + 1, cy + 2, 4, 5), CREAM_LIGHT)
    for i in range(6):
        px(img, cx + i, cy + i if i < 7 else cy + 6, LEATHER)
    bkx, bky, _, _ = body['back']
    for i in range(6):
        px(img, bkx + 5 - i, bky + i, LEATHER)
    # Arms 2x6x2 and legs 2x5x2: cream with dark paws and a stripe.
    for (u, v, h) in [(22, 14, 6), (32, 14, 5)]:
        part = faces(u, v, 2, h, 2)
        for name, f in part.items():
            fill(img, f, CREAM)
            if name not in ('top', 'bottom'):
                x, y, w, hh = f
                fill(img, (x, y + hh - 2, w, 2), POINT)
                fill(img, (x, y + 1, w, 1), POINT)
        fill(img, part['bottom'], POINT_DARK)
    # Tail 1x1x8: cream with dark rings and a dark tip.
    tail = faces(42, 14, 1, 1, 8)
    for f in tail.values():
        fill(img, f, CREAM)
    for name in ('top', 'bottom', 'right', 'left'):
        x, y, w, h = tail[name]
        horizontal = name in ('top', 'bottom')
        for k in (2, 5, 7):
            if horizontal:
                px(img, x, y + k, POINT)
            else:
                px(img, x + k, y, POINT)
    fill(img, tail['back'], POINT_DARK)
    # Pack 5x5x2: leather with a gold buckle and flap.
    pack = faces(0, 48, 5, 5, 2)
    for f in pack.values():
        fill(img, f, LEATHER)
        noise(img, f, [LEATHER_DARK], 4)
    px_, py_, pw, ph = pack['back']
    fill(img, (px_, py_, pw, 2), LEATHER_DARK)
    px(img, px_ + 2, py_ + 2, GOLD)
    # Bell 1x1x1.
    for f in faces(16, 48, 1, 1, 1).values():
        fill(img, f, GOLD)
    img.save(os.path.join(A, 'textures', 'entity', 'catto.png'))
    print('wrote entity catto')


if __name__ == '__main__':
    catto_texture()
