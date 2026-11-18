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
    'catto_trowel': [
        '................',
        '................',
        '.........ssss...',
        '........sTTTTs..',
        '.......sTTTTTs..',
        '......sTTTTTs...',
        '......sTTTTs....',
        '.....s.ssss.....',
        '....bB..........',
        '...bB...........',
        '..bB............',
        '.pP.............',
        '.pp.............',
        '................',
        '................',
        '................',
    ],
    'catto_rod': [
        '..............bw',
        '.............bBw',
        '............bB.w',
        '...........bB..w',
        '..........bB...w',
        '.........bB....w',
        '........bB.....w',
        '.......bB......w',
        '......bB.......w',
        '.....bB........w',
        '....bB.......RRw',
        '...pP........RWR',
        '..pP..........R.',
        '.pp.............',
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
        parent = 'minecraft:item/handheld' if name in ('errand_wand', 'catto_hammer', 'catto_scythe', 'catto_hatchet', 'catto_trowel', 'catto_rod') else 'minecraft:item/generated'
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


COATS = {
    # base, shade, light (belly, muzzle), point (ears, paws, tail tip), stripe, eye
    'siamese': dict(base=(236, 218, 180), shade=(214, 192, 150), light=(248, 236, 210), point=(110, 72, 48), stripe=None, eye=(70, 190, 190)),
    'ginger': dict(base=(228, 146, 66), shade=(200, 116, 46), light=(246, 210, 160), point=(188, 98, 38), stripe=(176, 88, 30), eye=(120, 190, 60)),
    'black': dict(base=(44, 40, 50), shade=(30, 28, 36), light=(70, 64, 78), point=(26, 24, 30), stripe=None, eye=(220, 200, 60)),
    'tuxedo': dict(base=(40, 38, 46), shade=(28, 26, 32), light=(240, 240, 236), point=(240, 240, 236), stripe=None, eye=(110, 200, 90)),
    'white': dict(base=(242, 242, 238), shade=(214, 214, 218), light=(252, 252, 250), point=(226, 214, 214), stripe=None, eye=(90, 150, 230)),
    'calico': dict(base=(244, 240, 232), shade=(222, 216, 206), light=(252, 250, 246), point=(60, 50, 50), stripe=None, eye=(200, 160, 50),
                   patches=[(220, 130, 50), (50, 44, 44)]),
    'gray': dict(base=(150, 150, 160), shade=(124, 124, 134), light=(196, 196, 204), point=(98, 98, 108), stripe=(104, 104, 114), eye=(210, 190, 70)),
}


def catto_texture(name, c):
    import random
    rnd = random.Random(name)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    base, shade, light, point, stripe, eye = c['base'], c['shade'], c['light'], c['point'], c['stripe'], c['eye']
    patches = c.get('patches', [])

    def coat(rect, seed, striped=False):
        fill(img, rect, base)
        noise(img, rect, [shade], seed)
        x, y, w, h = rect
        if striped and stripe:
            for j in range(y, y + h, 2):
                for i in range(x, x + w):
                    if (i + j) % 3 == 0:
                        px(img, i, j, stripe)
        for colour in patches:
            for _ in range(max(1, w * h // 18)):
                cx, cy = x + rnd.randrange(w), y + rnd.randrange(h)
                for dx in range(-1, 2):
                    for dy in range(-1, 1):
                        if x <= cx + dx < x + w and y <= cy + dy < y + h:
                            px(img, cx + dx, cy + dy, colour)
    head = faces(0, 0, 7, 6, 6)
    for f in head.values():
        coat(f, 1, True)
    fx, fy, _, _ = head['front']
    fill(img, (fx + 2, fy + 3, 3, 3), light)
    if name == 'siamese':
        for (x, y) in [(2, 3), (3, 3), (4, 3), (3, 4), (2, 4), (4, 4), (3, 5)]:
            px(img, fx + x, fy + y, point)
        for x in range(7):
            px(img, fx + x, fy, point)
    for (x, y, col) in [(1, 2, eye), (5, 2, eye), (1, 1, shade), (5, 1, shade)]:
        px(img, fx + x, fy + y, col)
    for f in faces(28, 5, 3, 2, 1).values():
        fill(img, f, light)
    mx, my, _, _ = faces(28, 5, 3, 2, 1)['front']
    px(img, mx + 1, my, PINK)
    ears = faces(40, 0, 2, 2, 1)
    for f in ears.values():
        fill(img, f, point if name in ('siamese',) else shade)
    ex, ey, _, _ = ears['front']
    px(img, ex, ey + 1, PINK)
    px(img, ex + 1, ey + 1, PINK)
    body = faces(0, 14, 6, 7, 4)
    for f in body.values():
        coat(f, 3, True)
    cx, cy, _, _ = body['front']
    fill(img, (cx + 1, cy + 1, 4, 6), light)
    for i in range(6):
        px(img, cx + i, cy + i, LEATHER)
    bkx, bky, _, _ = body['back']
    for i in range(6):
        px(img, bkx + 5 - i, bky + i, LEATHER)
    for (u, v, h) in [(22, 14, 6), (32, 14, 5)]:
        part = faces(u, v, 2, h, 2)
        for nm, f in part.items():
            coat(f, 5, True)
            if nm not in ('top', 'bottom'):
                x, y, w, hh = f
                fill(img, (x, y + hh - 2, w, 2), point)
        fill(img, part['bottom'], point)
    tail = faces(42, 14, 1, 1, 8)
    for f in tail.values():
        fill(img, f, base)
    for nm in ('top', 'bottom', 'right', 'left'):
        x, y, w, h = tail[nm]
        for k in (2, 5):
            if nm in ('top', 'bottom'):
                px(img, x, y + k, stripe or shade)
            else:
                px(img, x + k, y, stripe or shade)
    fill(img, tail['back'], point)
    pack = faces(0, 48, 5, 5, 2)
    for f in pack.values():
        fill(img, f, LEATHER)
        noise(img, f, [LEATHER_DARK], 4)
    bx, by, bw, bh = pack['back']
    fill(img, (bx, by, bw, 2), LEATHER_DARK)
    px(img, bx + 2, by + 2, GOLD)
    for f in faces(16, 48, 1, 1, 1).values():
        fill(img, f, GOLD)
    return img


GEAR = {
    # piece -> (main, light, dark, accent)
    'arcane_catto_hood': (HOOD, HOOD_LIGHT, HOOD_DARK, GOLD),
    'arcane_catto_mantle': (HOOD, HOOD_LIGHT, HOOD_DARK, GOLD),
    'catto_sombrero': ((214, 176, 96), (240, 214, 140), (160, 120, 60), (200, 50, 40)),
    'catto_poncho': ((200, 60, 50), (240, 200, 70), (70, 150, 70), (40, 40, 40)),
    'catto_druid_wreath': ((70, 140, 50), (120, 190, 70), (40, 90, 30), (240, 120, 160)),
    'catto_druid_cloak': ((70, 120, 50), (110, 170, 70), (50, 80, 36), (150, 100, 50)),
    'plain_metal': ((214, 214, 214), (250, 250, 250), (150, 150, 150), (110, 110, 110)),
}


def gear_texture(name, main, light, dark, accent):
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    # hood (open at the face) and its ears
    hood = faces(0, 0, 8, 7, 7)
    for nm, f in hood.items():
        if nm in ('front', 'bottom'):
            continue
        fill(img, f, main)
        noise(img, f, [dark, light], 2)
    hx, hy, hw, hh = hood['front']
    fill(img, (hx, hy, hw, 2), light)
    fill(img, (hx, hy, 1, hh), light)
    fill(img, (hx + hw - 1, hy, 1, hh), light)
    px(img, hx + 3, hy, accent)
    px(img, hx + 4, hy, accent)
    bx, by, _, _ = hood['back']
    for (x, y) in [(3, 2), (4, 2), (2, 3), (5, 3), (3, 4), (4, 4)]:
        px(img, bx + x, by + y, accent)
    ear = faces(32, 0, 3, 3, 2)
    for f in ear.values():
        fill(img, f, main)
    ex, ey, _, _ = ear['front']
    fill(img, (ex + 1, ey + 1, 1, 2), PINK)
    # sombrero brim and crown
    brim = faces(0, 16, 12, 1, 12)
    for f in brim.values():
        fill(img, f, main)
        noise(img, f, [dark, light], 6)
    tx, ty, tw, th = brim['top']
    for i in range(12):
        px(img, tx + i, ty, dark)
        px(img, tx + i, ty + 11, dark)
        px(img, tx, ty + i, dark)
        px(img, tx + 11, ty + i, dark)
    crown = faces(32, 6, 5, 3, 5)
    for nm, f in crown.items():
        fill(img, f, main)
        if nm not in ('top', 'bottom'):
            x, y, w, h = f
            fill(img, (x, y + h - 1, w, 1), accent)
    # wreath: a ring of leaves with flowers (top open in the middle)
    wreath = faces(0, 30, 8, 1, 7)
    for nm, f in wreath.items():
        if nm == 'bottom':
            continue
        fill(img, f, main)
        noise(img, f, [dark, light, accent], 7)
    wx, wy, ww, wh = wreath['top']
    for x in range(1, ww - 1):
        for y in range(1, wh - 1):
            img.putpixel((wx + x, wy + y), (0, 0, 0, 0))
    # helmet (plain metal, tinted in game)
    helmet = faces(34, 30, 8, 4, 7)
    for nm, f in helmet.items():
        if nm == 'bottom':
            continue
        fill(img, f, main)
        noise(img, f, [light, dark], 8)
    hx, hy, hw, hh = helmet['front']
    fill(img, (hx, hy + hh - 1, hw, 1), dark)
    # cloak over the body, cape on the back, vest
    cloak = faces(0, 40, 7, 8, 5)
    for nm, f in cloak.items():
        fill(img, f, main)
        noise(img, f, [dark, light], 9)
        if nm not in ('top', 'bottom'):
            x, y, w, h = f
            if name == 'catto_poncho':
                for j in range(h):
                    colour = (main, light, dark)[j % 3]
                    fill(img, (x, y + j, w, 1), colour)
            fill(img, (x, y + h - 1, w, 1), accent if name != 'catto_poncho' else light)
    cape = faces(42, 42, 6, 8, 1)
    for f in cape.values():
        fill(img, f, main)
        noise(img, f, [dark, light], 10)
    cx, cy, cw, ch = cape['back']
    px(img, cx + 2, cy + 3, accent)
    px(img, cx + 3, cy + 3, accent)
    px(img, cx + 2, cy + 4, accent)
    px(img, cx + 3, cy + 4, accent)
    fill(img, (cx, cy + ch - 1, cw, 1), accent)
    vest = faces(26, 52, 7, 6, 5)
    for nm, f in vest.items():
        fill(img, f, main)
        noise(img, f, [light, dark], 11)
    vx, vy, vw, vh = vest['front']
    for (x, y) in [(1, 1), (5, 1), (1, 4), (5, 4)]:
        px(img, vx + x, vy + y, dark)
    return img


def entity_textures():
    out = os.path.join(A, 'textures', 'entity', 'catto')
    os.makedirs(out, exist_ok=True)
    for name, c in COATS.items():
        catto_texture(name, c).save(os.path.join(out, name + '.png'))
    gear = os.path.join(A, 'textures', 'entity', 'catto_gear')
    os.makedirs(gear, exist_ok=True)
    for name, colours in GEAR.items():
        gear_texture(name, *colours).save(os.path.join(gear, name + '.png'))
    old = os.path.join(A, 'textures', 'entity', 'catto.png')
    if os.path.exists(old):
        os.remove(old)
    print('wrote coats and gear')


if __name__ == '__main__':
    entity_textures()
