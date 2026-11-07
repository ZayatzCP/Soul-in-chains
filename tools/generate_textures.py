#!/usr/bin/env python3
"""Generates every texture of Arcane Artifacts as Minecraft-style pixel art.

Run from the repository root:  python3 tools/generate_textures.py
Each sprite is an ASCII grid; every character maps to a colour in the sprite's palette
('.' is transparent). Keeping the art as text makes it reviewable and easy to tweak.
"""
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'arcane_artifacts', 'textures')


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def sprite(rows, palette):
    h = len(rows)
    w = max(len(r) for r in rows)
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '.' or ch == ' ':
                continue
            img.putpixel((x, y), palette[ch] if isinstance(palette[ch], tuple) else hexc(palette[ch]))
    return img


def save(img, *path):
    p = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', os.path.relpath(p))


# ---------------------------------------------------------------- glyph icons
def glyph_base():
    """Parchment tile with a violet frame, in the spirit of Ars Nouveau glyphs."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    frame_a, frame_b, frame_dark = hexc('#B44CF0'), hexc('#8A2BD2'), hexc('#5B1A8E')
    parch = [hexc('#F3E2B8'), hexc('#E8D19C'), hexc('#D9BB7E'), hexc('#C29E62')]
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            corner = (x in (0, 15)) and (y in (0, 15))
            if corner:
                continue
            if edge:
                img.putpixel((x, y), frame_a if (x + y) % 2 == 0 else frame_b)
            elif x in (1, 14) or y in (1, 14):
                img.putpixel((x, y), frame_dark)
            else:
                d = max(abs(x - 7.5), abs(y - 7.5))
                idx = 0 if d < 3 else 1 if d < 4.5 else 2 if d < 5.5 else 3
                img.putpixel((x, y), parch[idx])
    return img


def glyph(symbol_rows, extra=None):
    base = glyph_base()
    pal = {'#': '#2A1B12', 'v': '#7A2FC0', 't': '#1E9C8A', 'g': '#C08A1E'}
    if extra:
        pal.update(extra)
    sym = sprite(symbol_rows, pal)
    base.alpha_composite(sym, (2, 2))
    return base


GLYPHS = {
    # comet curving toward a target dot
    'glyph_arcane_missile': [
        '............',
        '.........vv.',
        '........v##v',
        '.......v####',
        '......v.##v.',
        '.....#..vv..',
        '....#.......',
        '...#........',
        '..#.........',
        '.#..........',
        '#...........',
        '............',
    ],
    # rune circle with central eye-dot
    'glyph_arcane_mark': [
        '............',
        '....####....',
        '...#....#...',
        '..#..vv..#..',
        '.#..v##v..#.',
        '.#.v####v.#.',
        '.#.v####v.#.',
        '.#..v##v..#.',
        '..#..vv..#..',
        '...#....#...',
        '....####....',
        '............',
    ],
    # two bodies joined by a tendril
    'glyph_parasitism': [
        '............',
        '.##.........',
        '####........',
        '####........',
        '.##t........',
        '....t..t....',
        '.....tt.t...',
        '.........t##',
        '.........###',
        '.........###',
        '..........#.',
        '............',
    ],
    # one bolt forking into two
    'glyph_splitting': [
        '............',
        '.........##.',
        '........##..',
        '.......#....',
        '......#.....',
        '#####.#.....',
        '#####.#.....',
        '......#.....',
        '.......#....',
        '........##..',
        '.........##.',
        '............',
    ],
    # chevrons chasing forward
    'glyph_pursuit': [
        '............',
        '.#....#.....',
        '..#....#....',
        '...#....#...',
        '....#....#..',
        '.....#....#t',
        '.....#....#t',
        '....#....#..',
        '...#....#...',
        '..#....#....',
        '.#....#.....',
        '............',
    ],
}

# ---------------------------------------------------------------- accessories
WREATH_VINES = [
    '................',
    '................',
    '.....gGGGGg.....',
    '...gGg....gGg...',
    '..gG........Gg..',
    '.gG..........Gg.',
    '.G............G.',
    'gG............Gg',
    'gG............Gg',
    '.G............G.',
    '.gG..........Gg.',
    '..gG........Gg..',
    '...gGg....gGg...',
    '.....gGGGGg.....',
    '................',
    '................',
]
# flower groups are grayscale so the tint colour shows through (tint 1..4)
WREATH_FLOWERS = {
    1: [  # physical - top
        '................',
        '.......ww.......',
        '......wWWw......',
        '.......ww.......',
    ],
    2: [  # fire - right
        '', '', '', '', '', '',
        '..............w.',
        '.............wWw',
        '..............ww',
        '...............w',
    ],
    3: [  # water - bottom
        '', '', '', '', '', '', '', '', '', '', '', '',
        '......w..w......',
        '.....wWwwWw.....',
        '......w..w......',
    ],
    4: [  # magic - left gem
        '', '', '', '', '', '',
        '.w..............',
        'wWw.............',
        'wWw.............',
        '.w..............',
    ],
}
WREATH_PAL = {'g': '#2E6B3A', 'G': '#4FA35A', 'w': '#BDBDBD', 'W': '#FFFFFF'}

BOOTS = [
    '................',
    '................',
    '....yyyyy.......',
    '....vVVVv.......',
    '....vVtVv.......',
    '....vVVVv.......',
    '....vVVVv.......',
    '....vVVVv.......',
    '....vVVVvv......',
    '...vVVVVVVvv....',
    '...vVVVVVVVVv...',
    '...vVVVVVVVVVv..',
    '...yyyyyyyyyyy..',
    '...ddddddddddd..',
    '................',
    '................',
]
BOOTS_PAL = {'v': '#3B2A6B', 'V': '#5C45A8', 'y': '#E3B341', 't': '#2FD6B5', 'd': '#1E1638'}

GLOVES = [
    '................',
    '....v.v.v.......',
    '...vVvVvVv......',
    '...vVvVvVv......',
    '...vVVVVVv.v....',
    '...vVVpVVvvVv...',
    '...vVpVpVVVv....',
    '...vVVpVVVv.....',
    '...vVVVVVv......',
    '...yyyyyyy......',
    '...ytyyyty......',
    '...yyyyyyy......',
    '................',
    '................',
    '................',
    '................',
]
GLOVES_PAL = {'v': '#1F2A5C', 'V': '#34479A', 'p': '#B44CF0', 'y': '#C9962B', 't': '#2FD6B5'}

RING = [
    '................',
    '................',
    '......pPPp......',
    '.....pPWtPp.....',
    '.....PttTtP.....',
    '.....pPTTPp.....',
    '......yYYy......',
    '....yY....Yy....',
    '...yY......Yy...',
    '...Y........Y...',
    '...Y........Y...',
    '...yY......Yy...',
    '....yY....Yy....',
    '......yYYy......',
    '................',
    '................',
]
RING_PAL = {'y': '#A87A1C', 'Y': '#F0C64A', 'p': '#6A23A8', 'P': '#B44CF0', 't': '#1E9C8A', 'T': '#2FD6B5', 'W': '#FFFFFF'}

MANTLE = [
    '................',
    '.....yYYy.......',
    '....vvYYvv......',
    '...vVVttVVv.....',
    '..vVVvttvVVv....',
    '..vVvVttVvVv....',
    '.vVVvVttVvVVv...',
    '.vVvVVttVVvVv...',
    '.vVvVVttVVvVv...',
    'vVVvVVttVVvVVv..',
    'vVvVVVttVVVvVv..',
    'vVvVVVttVVVvVv..',
    'vVVvVVttVVvVVv..',
    'pvpvpvttvpvpvp..',
    '.p.p.p..p.p.p...',
    '................',
]
MANTLE_PAL = {'v': '#4A1C7A', 'V': '#7A35C0', 't': '#2FD6B5', 'y': '#A87A1C', 'Y': '#F0C64A', 'p': '#B44CF0'}

# ---------------------------------------------------------------- entities
MISSILE = [
    '................',
    '.......P........',
    '......PWP.......',
    '...p..PWP..p....',
    '....pPPWPPp.....',
    '....PWWWWWP.....',
    '..PPWWWWWWWPP...',
    '.PWWWWWWWWWWWP..',
    '..PPWWWWWWWPP...',
    '....PWWWWWP.....',
    '....pPPWPPp.....',
    '...p..PWP..p....',
    '......PWP.......',
    '.......P........',
    '................',
    '................',
]
MISSILE_PAL = {'p': (106, 35, 168, 160), 'P': (180, 76, 240, 230), 'W': (255, 236, 255, 255)}
SHARD_PAL = {'p': (30, 156, 138, 160), 'P': (47, 214, 181, 230), 'W': (235, 255, 250, 255)}

# ---------------------------------------------------------------- mob effects (18x18)
EFFECTS = {
    'arcane_mark': ([
        '..................',
        '......PPPPPP......',
        '....PP......PP....',
        '...P...pppp...P...',
        '..P...p....p...P..',
        '..P..p..WW..p..P..',
        '.P..p..WWWW..p..P.',
        '.P..p.WWWWWW.p..P.',
        '.P..p.WWWWWW.p..P.',
        '.P..p..WWWW..p..P.',
        '..P..p..WW..p..P..',
        '..P...p....p...P..',
        '...P...pppp...P...',
        '....PP......PP....',
        '......PPPPPP......',
        '..................',
        '..................',
        '..................',
    ], {'P': '#B44CF0', 'p': '#7A2FC0', 'W': '#F3D9FF'}),
    'parasite_host': ([
        '..................',
        '.......rrrr.......',
        '.....rrRRRRrr.....',
        '....rRRRRRRRRr....',
        '...rRRRkkkkRRRr...',
        '...rRRk....kRRr...',
        '..rRRk..tt..kRRr..',
        '..rRRk.tTTt.kRRr..',
        '..rRRk.tTTt.kRRr..',
        '..rRRk..tt..kRRr..',
        '...rRRk....kRRr...',
        '...rRRRkkkkRRRr...',
        '....rRRRRRRRRr....',
        '.....rrRRRRrr.....',
        '.......rrrr.......',
        '..................',
        '..................',
        '..................',
    ], {'r': '#3A0A24', 'R': '#7A1A4C', 'k': '#1A0610', 't': '#1E9C8A', 'T': '#2FD6B5'}),
    'parasitic_bond': ([
        '..................',
        '..TT..............',
        '.TWWT.............',
        '.TWWT.............',
        '..TTt.............',
        '.....t............',
        '......t...........',
        '.......tt.........',
        '.........tt.......',
        '...........t......',
        '............t.....',
        '.............tRR..',
        '.............RkkR.',
        '.............RkkR.',
        '..............RR..',
        '..................',
        '..................',
        '..................',
    ], {'T': '#2FD6B5', 't': '#1E9C8A', 'W': '#E8FFFA', 'R': '#7A1A4C', 'k': '#3A0A24'}),
    'parasitic_recovery': ([
        '..................',
        '.....gggggggg.....',
        '.....gGGGGGGg.....',
        '......gGGGGg......',
        '.......gTTg.......',
        '........gg........',
        '........gg........',
        '.......g..g.......',
        '......g.TT.g......',
        '.....g.TTTT.g.....',
        '.....gTTTTTTg.....',
        '.....gggggggg.....',
        '..................',
        '..................',
        '..................',
        '..................',
        '..................',
        '..................',
    ], {'g': '#6B4E7A', 'G': '#A98BC0', 'T': '#2FD6B5'}),
    'arcane_adaptation': ([
        '..................',
        '........PP........',
        '.......PWWP.......',
        '......PWWWWP......',
        '.....PPWWWWPP.....',
        '....P..PWWP..P....',
        '...P....PP....P...',
        '..P....pppp....P..',
        '..P...p....p...P..',
        '..P...p....p...P..',
        '..P....pppp....P..',
        '...P..........P...',
        '....P........P....',
        '.....PP....PP.....',
        '.......PPPP.......',
        '..................',
        '..................',
        '..................',
    ], {'P': '#7A6CFF', 'p': '#B44CF0', 'W': '#E6E2FF'}),
}

FEET_SLOT = [
    '................',
    '................',
    '................',
    '....oooo........',
    '....o..o........',
    '....o..o........',
    '....o..o........',
    '....o..o........',
    '....o..o........',
    '...o...o........',
    '..o....ooooo....',
    '.o..........o...',
    '.o..........o...',
    '.oooooooooooo...',
    '................',
    '................',
]

WORN_VINE = ['GgGgGgGgGgGgGgGg'] * 16
WORN_FLOWER = ['WwWwWwWwWwWwWwWw', 'wWwWwWwWwWwWwWwW'] * 8
WORN_TEXTURES = {
    'boots': (['yyyyyyyyyyyyyyyy'] * 3 + ['vVvVvVvVvVvVvVvV', 'VvVvVvVvVvVvVvVv'] * 5 + ['dddddddddddddddd'] * 3, BOOTS_PAL),
    'gloves': (['vVvVvVvVvVvVvVvV', 'VvVvVpVvVvVpVvVv'] * 5 + ['yyyyyyyyyyyyyyyy', 'ytyyyyyytyyyyyyt', 'yyyyyyyyyyyyyyyy'] * 2, GLOVES_PAL),
    'ring': (['yYyYyYyYyYyYyYyY', 'YyYyYyYyYyYyYyYy'] * 8, RING_PAL),
    'gem': (['tTtTtTtTtTtTtTtT', 'TtTtTtTtTtTtTtTt'] * 8, RING_PAL),
    'mantle': (['vVvVvVvVvVvVvVvV', 'VvVvVvVvVvVvVvVv'] * 6 + ['pvpvpvpvpvpvpvpv'] * 2 + ['tttttttttttttttt'] * 2, MANTLE_PAL),
    'clasp': (['yYyYyYyYyYyYyYyY', 'YyYyYyYyYyYyYyYy'] * 8, MANTLE_PAL),
}


ROSE_GOLD_INGOT = [
    '................',
    '................',
    '................',
    '................',
    '.......hhhhhh...',
    '.....hhLLLLLHh..',
    '...hhLLLHHHHHd..',
    '..hLLHHHHHHHdd..',
    '..hHHHHHHHHddd..',
    '..dHHHHHHHddd...',
    '..ddHHHHHddd....',
    '...ddddddd......',
    '................',
    '................',
    '................',
    '................',
]
ROSE_GOLD_PAL = {'h': '#C77B66', 'H': '#E7A08B', 'L': '#FFE1D6', 'd': '#8E4B3B'}


def main():
    save(sprite(ROSE_GOLD_INGOT, ROSE_GOLD_PAL), 'item', 'rose_gold_ingot.png')
    for name, rows in GLYPHS.items():
        save(glyph(rows), 'item', name + '.png')

    save(sprite(WREATH_VINES, WREATH_PAL), 'item', 'wreath_of_manipulation_vines.png')
    for group, rows in WREATH_FLOWERS.items():
        rows = rows + [''] * (16 - len(rows))
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        img.alpha_composite(sprite([r.ljust(16, '.') for r in rows], WREATH_PAL))
        save(img, 'item', f'wreath_of_manipulation_flowers_{group}.png')
    save(sprite(WORN_VINE, WREATH_PAL), 'item', 'worn', 'wreath_vine.png')
    save(sprite(WORN_FLOWER, WREATH_PAL), 'item', 'worn', 'wreath_flower.png')
    for name, (rows, pal) in WORN_TEXTURES.items():
        save(sprite(rows, pal), 'item', 'worn', name + '.png')

    save(sprite(BOOTS, BOOTS_PAL), 'item', 'focusing_boots.png')
    save(sprite(GLOVES, GLOVES_PAL), 'item', 'focusing_gloves.png')
    save(sprite(RING, RING_PAL), 'item', 'ring_of_reversal.png')
    save(sprite(MANTLE, MANTLE_PAL), 'item', 'mantle_of_reflection.png')

    save(sprite(MISSILE, MISSILE_PAL), 'entity', 'arcane_missile.png')
    save(sprite(MISSILE, SHARD_PAL), 'entity', 'arcane_missile_shard.png')

    for name, (rows, pal) in EFFECTS.items():
        save(sprite(rows, pal), 'mob_effect', name + '.png')

    save(sprite(FEET_SLOT, {'o': (255, 255, 255, 70)}), 'slot', 'empty_feet_slot.png')

    # Mod list logo: the five artifacts on a dark violet card, scaled 4x.
    logo = Image.new('RGBA', (96, 32), hexc('#1A1030'))
    for i, rows in enumerate([None, BOOTS, GLOVES, RING, MANTLE]):
        tile = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        if rows is None:
            tile.alpha_composite(sprite(WREATH_VINES, WREATH_PAL))
            colours = ['#D9DEE8', '#FF8A1F', '#2FD6E0', '#B44CF0']
            for g, frows in WREATH_FLOWERS.items():
                frows = [r.ljust(16, '.') for r in frows + [''] * (16 - len(frows))]
                tile.alpha_composite(sprite(frows, {'w': colours[g - 1], 'W': '#FFFFFF'}))
        else:
            pal = [BOOTS_PAL, GLOVES_PAL, RING_PAL, MANTLE_PAL][i - 1]
            tile = sprite(rows, pal)
        logo.alpha_composite(tile, (2 + i * 19, 8))
    logo = logo.resize((384, 128), Image.NEAREST)
    p = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'arcane_artifacts_logo.png')
    logo.save(p)
    print('wrote', os.path.relpath(p))


if __name__ == '__main__':
    main()
