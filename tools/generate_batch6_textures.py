#!/usr/bin/env python3
"""Placeholder art for batch 6: mushroom moss, mushrooms, blue mushroom cap, Mudzya Hive, Alchemical
Distiller, Bottle of Source and the Mudzya / Gribushka entity textures (painted on their model UVs).
Only missing files are written unless --force is given."""
import os
import random
import sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_textures import sprite, hexc, ROOT  # noqa: E402
from PIL import Image  # noqa: E402

FORCE = '--force' in sys.argv


def save(img, *path):
    p = os.path.join(ROOT, *path)
    if os.path.exists(p) and not FORCE:
        print('kept', os.path.relpath(p))
        return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print('wrote', os.path.relpath(p))


def noise(size, palette, seed):
    rnd = random.Random(seed)
    img = Image.new('RGBA', size)
    cols = [hexc(c) for c in palette]
    for y in range(size[1]):
        for x in range(size[0]):
            img.putpixel((x, y), cols[min(len(cols) - 1, int(rnd.random() ** 1.6 * len(cols)))])
    return img


# Mushroom moss is tinted by position (blue to purple), so it is drawn in light greys.
def moss():
    img = noise((16, 16), ['#E8E8EC', '#D2D2DA', '#BDBDC8', '#F6F6FA', '#A9A9B6'], 11)
    rnd = random.Random(5)
    for _ in range(9):
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), hexc('#FFFFFF'))
    return img


def cap():
    img = noise((16, 16), ['#5AA9E6', '#4C95D6', '#6BB8F0', '#3F80C2'], 3)
    rnd = random.Random(8)
    for _ in range(7):
        x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1)]:
            img.putpixel(((x + dx) % 16, (y + dy) % 16), hexc('#D9F1FF'))
    return img


def mushroom(capc, dots, stem='#E8DCC8', stem_dark='#C9B89C'):
    rows = [
        '................',
        '................',
        '................',
        '................',
        '.....cccccc.....',
        '...ccdccccdcc...',
        '..cccccccdcccc..',
        '..ccdcccccccdc..',
        '..CCCCCCCCCCCC..',
        '.......ss.......',
        '.......ss.......',
        '......sSs.......',
        '.......ss.......',
        '......sSSs......',
        '................',
        '................',
    ]
    return sprite(rows, {'c': capc, 'C': darker(capc), 'd': dots, 's': stem, 'S': stem_dark})


def darker(h, f=0.7):
    r, g, b, _ = hexc(h)
    return '#%02X%02X%02X' % (int(r * f), int(g * f), int(b * f))


MUSHROOMS = {
    'blue_mushroom': ('#4C95D6', '#D9F1FF'),
    'sourcecap': ('#9A4CE0', '#F4D8FF'),
    'brawncap': ('#C23B3B', '#FFD0C0'),
    'embercap': ('#E07A1F', '#FFE29A'),
    'swiftcap': ('#E6C632', '#FFFFFF'),
    'leapcap': ('#7FCF4A', '#E8FFD0'),
    'mendcap': ('#E87FB0', '#FFFFFF'),
    'ghostcap': ('#E8ECF4', '#B8C2D6'),
    'toxcap': ('#3E8E3A', '#C8F070'),
    'stonecap': ('#8A8A94', '#C8C8D0'),
    'tidecap': ('#2FB8C8', '#D0FFFF'),
}


def hive(face):
    img = noise((16, 16), ['#5B3E8A', '#6B4E9A', '#4A2F75'], {'side': 1, 'top': 2, 'bottom': 3}[face])
    for x in range(16):
        img.putpixel((x, 0), hexc('#2B1F3A'))
        img.putpixel((x, 15), hexc('#2B1F3A'))
    if face == 'side':
        for y in (5, 10):
            for x in range(16):
                img.putpixel((x, y), hexc('#3A2A55'))
        for x in range(6, 10):
            for y in range(6, 10):
                img.putpixel((x, y), hexc('#1A1030'))
    if face == 'top':
        for x in range(4, 12):
            for y in range(4, 12):
                img.putpixel((x, y), hexc('#B44CF0') if (x + y) % 3 else hexc('#E0A8FF'))
    return img


def metal():
    return noise((16, 16), ['#5A5560', '#6A6570', '#4A4550', '#7A7580'], 21)


def copper():
    img = noise((16, 16), ['#C77B4A', '#D88A5A', '#B06A3A', '#E8A07A'], 22)
    for x in range(16):
        img.putpixel((x, 0), hexc('#8E4B2B'))
        img.putpixel((x, 15), hexc('#8E4B2B'))
    return img


def glass():
    img = Image.new('RGBA', (16, 16), (210, 230, 255, 60))
    for i in range(16):
        img.putpixel((i, 0), (230, 240, 255, 200))
        img.putpixel((i, 15), (230, 240, 255, 200))
        img.putpixel((0, i), (230, 240, 255, 200))
        img.putpixel((15, i), (230, 240, 255, 200))
    for i in range(3, 7):
        img.putpixel((i, i - 1), (255, 255, 255, 180))
    return img


def liquid():
    return noise((16, 16), ['#B44CF0', '#9A3AD8', '#C870FF', '#8A2BD2'], 23)


BOTTLE = [
    '................',
    '......ccc.......',
    '......w.w.......',
    '......w.w.......',
    '.....w...w......',
    '....w.....w.....',
    '...wPPPPPPPw....',
    '...wPVPPPPPw....',
    '...wPPPPPVPw....',
    '...wPPPPPPPw....',
    '...wPPVPPPPw....',
    '....wPPPPPw.....',
    '.....wwwww......',
    '................',
    '................',
    '................',
]

DISTILLER_ICON = [
    '................',
    '..gggg....ccc...',
    '.g....g..c...c..',
    '.gPPPPgcccc..c..',
    '.gPPPPg...c.c...',
    '..gggg....ccc...',
    '...cc......c....',
    '.mmmmmmmmmmmmmm.',
    '.mCCCCm.gPg.gPg.',
    '.mCCCCm.gPg.gPg.',
    '.mCCCCm.ggg.ggg.',
    '.mCCCCmmmmmmmmm.',
    '.mmmmmmmmmmmmmm.',
    '.m............m.',
    '.mmmmmmmmmmmmmm.',
    '................',
]


# ---------------------------------------------------------------- entities (painted on model UVs)
def paint_box(img, u, v, w, h, d, side, front=None, top=None, bottom=None):
    """Minecraft box UV: top (u+d, v, w×d), bottom (u+d+w, v, w×d), then a row of d×h / w×h / d×h / w×h sides."""
    def fill(x0, y0, x1, y1, color):
        for x in range(x0, x1):
            for y in range(y0, y1):
                if 0 <= x < img.width and 0 <= y < img.height:
                    img.putpixel((x, y), hexc(color))
    fill(u + d, v, u + d + w, v + d, top or side)
    fill(u + d + w, v, u + d + 2 * w, v + d, bottom or darker(side))
    fill(u, v + d, u + d, v + d + h, darker(side, 0.85))
    fill(u + d, v + d, u + d + w, v + d + h, front or side)
    fill(u + d + w, v + d, u + 2 * d + w, v + d + h, darker(side, 0.85))
    fill(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h, darker(side, 0.9))
    return (u + d, v + d)  # top-left of the front face


def mudzya(charged):
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    body = '#C870FF' if charged else '#7A3FB0'
    fx, fy = paint_box(img, 0, 0, 6, 5, 7, body, top='#9A55D0' if not charged else '#E0A8FF')
    # Eyes on the front face.
    for x in (fx + 1, fx + 4):
        img.putpixel((x, fy + 1), hexc('#FFFFFF'))
        img.putpixel((x, fy + 2), hexc('#1A0A2A'))
    # Glowing belly stripe when full of Source.
    for x in range(0, 26):
        img.putpixel((x, 7 + 3), hexc('#F4D8FF' if charged else '#5A2F85'))
    paint_box(img, 0, 12, 3, 2, 1, '#E0A8FF' if charged else '#9A55D0')
    paint_box(img, 20, 0, 1, 3, 1, '#2B1F3A')
    for x in range(5, 19):
        for y in range(18, 23):
            img.putpixel((x, y), (220, 200, 255, 150))
    return img


def gribushka(capc, spots):
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    fx, fy = paint_box(img, 0, 0, 7, 9, 6, '#E8DCC8')
    # Face: two dark eyes and a small mouth.
    for x in (fx + 1, fx + 5):
        img.putpixel((x, fy + 2), hexc('#2A1B12'))
        img.putpixel((x, fy + 3), hexc('#2A1B12'))
    img.putpixel((fx + 3, fy + 5), hexc('#8E5B4A'))
    paint_box(img, 0, 32, 14, 4, 14, capc, top=capc)
    paint_box(img, 26, 0, 9, 2, 9, capc, top=capc)
    rnd = random.Random(len(capc))
    for _ in range(14):
        x, y = rnd.randrange(14, 28), rnd.randrange(32, 46)
        img.putpixel((x, y), hexc(spots))
    for _ in range(6):
        x, y = rnd.randrange(35, 44), rnd.randrange(0, 9)
        img.putpixel((x, y), hexc(spots))
    paint_box(img, 0, 15, 2, 6, 2, '#D8CCB8')
    paint_box(img, 8, 15, 3, 4, 3, '#C9B89C')
    return img


def main():
    save(moss(), 'block', 'mushroom_moss.png')
    save(cap(), 'block', 'blue_mushroom_block.png')
    for name, (c, d) in MUSHROOMS.items():
        save(mushroom(c, d), 'block', name + '.png')
    for face in ('side', 'top', 'bottom'):
        save(hive(face), 'block', 'mudzya_hive_' + face + '.png')
    save(metal(), 'block', 'distiller_metal.png')
    save(copper(), 'block', 'distiller_copper.png')
    save(glass(), 'block', 'distiller_glass.png')
    save(liquid(), 'block', 'distiller_liquid.png')
    save(sprite(BOTTLE, {'c': '#8E4B3B', 'w': '#DCE8FF', 'P': '#9A3AD8', 'V': '#F4D8FF'}), 'item', 'bottle_of_source.png')
    save(sprite(DISTILLER_ICON, {'g': '#DCE8FF', 'P': '#B44CF0', 'c': '#D88A5A', 'm': '#5A5560', 'C': '#C77B4A'}), 'item', 'alchemical_distiller.png')
    save(mudzya(False), 'entity', 'mudzya.png')
    save(mudzya(True), 'entity', 'mudzya_charged.png')
    save(gribushka('#4C95D6', '#D9F1FF'), 'entity', 'gribushka_blue.png')
    save(gribushka('#8A4FC8', '#F4D8FF'), 'entity', 'gribushka_purple.png')
    save(gribushka('#8E5B3B', '#E8D0B0'), 'entity', 'gribushka_brown.png')


if __name__ == '__main__':
    main()
