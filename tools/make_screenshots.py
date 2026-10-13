"""
BBetter. Dialer – látványterv (mockup) generátor.

FONTOS: ezek NEM valódi képernyőképek, hanem az app pontos szín- és
méretértékeivel újrarajzolt látványtervek. A `screenshots/` mappa tartalmát
ez a szkript állítja elő, így bármikor újragenerálható.

Használat:
    python tools/make_screenshots.py
"""

from __future__ import annotations

import math
import os
import re

from PIL import Image, ImageDraw, ImageFilter, ImageFont

# --------------------------------------------------------------------------- méretek

W, H = 720, 1560          # a képernyő (360 × 780 dp, 2 px / dp)
PAD = 18                  # keret vastagsága
RADIUS = 62               # a keret lekerekítése
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "screenshots")

# --------------------------------------------------------------------------- paletta

DARK = dict(
    bg=(0, 0, 0), s1=(13, 13, 13), s2=(28, 28, 30), s3=(44, 44, 46),
    div=(42, 42, 44), tp=(255, 255, 255), ts=(154, 154, 160), tt=(94, 94, 100),
)
LIGHT = dict(
    bg=(245, 246, 248), s1=(255, 255, 255), s2=(231, 234, 239), s3=(216, 220, 226),
    div=(226, 229, 233), tp=(16, 18, 22), ts=(90, 97, 107), tt=(139, 146, 156),
)

GREEN = (0, 230, 118)
IDLE_GREEN_DARK = (12, 59, 34)
IDLE_TINT_DARK = (47, 127, 85)
RED = (255, 59, 48)

PALETTE = [
    (229, 57, 53), (244, 81, 30), (251, 140, 0), (253, 216, 53),
    (124, 179, 66), (0, 230, 118), (0, 137, 123), (142, 36, 170),
    (216, 27, 96), (236, 64, 122), (109, 76, 65), (84, 110, 122),
]


def font(size: int, bold: bool = False):
    name = "segoeuib.ttf" if bold else "segoeui.ttf"
    try:
        return ImageFont.truetype(name, size)
    except OSError:
        try:
            return ImageFont.truetype("arialbd.ttf" if bold else "arial.ttf", size)
        except OSError:
            return ImageFont.load_default()


# --------------------------------------------------------------------------- rajzoló segédek

def rrect(d: ImageDraw.ImageDraw, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def circle(d: ImageDraw.ImageDraw, cx, cy, r, fill=None, outline=None, width=1):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill, outline=outline, width=width)


def label(d, cx, cy, text, size, fill, bold=False, anchor="mm"):
    d.text((cx, cy), text, font=font(size, bold), fill=fill, anchor=anchor)


def portrait(n: int) -> Image.Image:
    """Szintetikus, visszafogott portré a profilképek helyére."""
    base = Image.new("RGB", (n, n))
    px = base.load()
    for y in range(n):
        t = y / n
        for x in range(n):
            u = x / n
            r = 92 + 130 * (1 - abs(u - 0.34)) - 46 * t
            g = 66 + 96 * (1 - abs(u - 0.30)) - 24 * t
            b = 78 + 74 * (1 - abs(u - 0.42)) + 34 * t
            px[x, y] = (max(0, min(255, int(r))), max(0, min(255, int(g))),
                        max(0, min(255, int(b))))

    d = ImageDraw.Draw(base)
    d.ellipse([n * 0.30, n * 0.17, n * 0.70, n * 0.69], fill=(54, 44, 48))       # haj
    d.ellipse([n * 0.335, n * 0.24, n * 0.665, n * 0.66], fill=(216, 174, 144))  # arc
    d.ellipse([n * 0.02, n * 0.76, n * 0.98, n * 1.65], fill=(58, 70, 104))      # váll
    return base.filter(ImageFilter.GaussianBlur(1.4))


def avatar(img: Image.Image, cx, cy, r, *, photo=False, color=None, initials="", ring=None):
    """Kör alakú profilkép vagy kezdőbetűs színkor."""
    cx, cy, r = int(cx), int(cy), int(r)
    layer = Image.new("RGBA", (r * 2, r * 2), (0, 0, 0, 0))
    if photo:
        art = portrait(int(r * 2.4)).resize((r * 2, r * 2), Image.LANCZOS)
        mask = Image.new("L", (r * 2, r * 2), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, r * 2, r * 2], fill=255)
        layer.paste(art, (0, 0), mask)
    else:
        ImageDraw.Draw(layer).ellipse([0, 0, r * 2, r * 2], fill=color or GREEN)
    img.paste(layer, (cx - r, cy - r), layer)

    d = ImageDraw.Draw(img)
    if ring:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=ring, width=6)
    if not photo and initials:
        label(d, cx, cy, initials, int(r * 0.78), (255, 255, 255), bold=True)


def pill(d, x, y, text, *, fill, fg, size=24, padx=22, h=48, bold=False):
    w = d.textlength(text, font=font(size, bold)) + padx * 2
    rrect(d, [x, y, x + w, y + h], h // 2, fill=fill)
    label(d, x + w / 2, y + h / 2, text, size, fg, bold=bold)
    return x + w


_PATH_NUM = re.compile(r"[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?")


def _tokenize_path(d: str):
    tokens, i = [], 0
    while i < len(d):
        ch = d[i]
        if ch in "MmLlHhVvCcSsQqTtAaZz":
            tokens.append(ch)
            i += 1
        elif ch in " ,\t\r\n":
            i += 1
        else:
            m = _PATH_NUM.match(d, i)
            if m and m.end() > i:
                tokens.append(float(m.group()))
                i = m.end()
            else:
                i += 1
    return tokens


def _path_points(d: str, subdiv: int = 16):
    """SVG path adat -> poligonkontúrok (M, L, H, V, C, Z támogatással)."""
    tokens = _tokenize_path(d)
    contours, cur = [], []
    x = y = 0.0
    start = (0.0, 0.0)
    cmd = None
    i = 0
    while i < len(tokens):
        t = tokens[i]
        if isinstance(t, str):
            cmd = t
            i += 1
            if cmd in "Zz":
                if cur:
                    cur.append(start)
                    contours.append(cur)
                    cur = []
                x, y = start
                continue
        if i >= len(tokens):
            break
        if cmd in "Mm":
            nx, ny = tokens[i], tokens[i + 1]
            i += 2
            if cmd == "m":
                nx += x
                ny += y
            x, y = nx, ny
            start = (x, y)
            if cur:
                contours.append(cur)
            cur = [(x, y)]
            cmd = "l" if cmd == "m" else "L"
        elif cmd in "Ll":
            nx, ny = tokens[i], tokens[i + 1]
            i += 2
            if cmd == "l":
                nx += x
                ny += y
            x, y = nx, ny
            cur.append((x, y))
        elif cmd in "Hh":
            nx = tokens[i]
            i += 1
            if cmd == "h":
                nx += x
            x = nx
            cur.append((x, y))
        elif cmd in "Vv":
            ny = tokens[i]
            i += 1
            if cmd == "v":
                ny += y
            y = ny
            cur.append((x, y))
        elif cmd in "Cc":
            x1, y1, x2, y2, nx, ny = tokens[i:i + 6]
            i += 6
            if cmd == "c":
                x1 += x
                y1 += y
                x2 += x
                y2 += y
                nx += x
                ny += y
            p0x, p0y = x, y
            for k in range(1, subdiv + 1):
                tt = k / subdiv
                mt = 1 - tt
                bx = mt ** 3 * p0x + 3 * mt * mt * tt * x1 + 3 * mt * tt * tt * x2 + tt ** 3 * nx
                by = mt ** 3 * p0y + 3 * mt * mt * tt * y1 + 3 * mt * tt * tt * y2 + tt ** 3 * ny
                cur.append((bx, by))
            x, y = nx, ny
        else:
            i += 1
    if cur:
        contours.append(cur)
    return contours


# A Material "call" ikon útvonala (24×24 viewport) – ugyanaz, mint az appban.
CALL_PATH = (
    "M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 "
    "1.12,0.37 2.33,0.57 3.57,0.57 0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 "
    "-17,-17 0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1 0,1.25 0.2,2.45 0.57,3.57 0.11,0.35 "
    "0.03,0.74 -0.25,1.02l-2.2,2.2z"
)
_CALL_CONTOURS = None


def phone_points(cx, cy, size, rotation=0.0):
    global _CALL_CONTOURS
    if _CALL_CONTOURS is None:
        _CALL_CONTOURS = _path_points(CALL_PATH)
    s = size / 24.0
    a = math.radians(rotation)
    ca, sa = math.cos(a), math.sin(a)
    result = []
    for contour in _CALL_CONTOURS:
        pts = []
        for px, py in contour:
            vx = (px - 12.0) * s
            vy = (py - 12.0) * s
            pts.append((int(round(cx + vx * ca - vy * sa)),
                        int(round(cy + vx * sa + vy * ca))))
        result.append(pts)
    return result


def phone_icon(d, cx, cy, size, color, rotation=0.0):
    """Pontos Material telefon-kagyló (a 135°-os elforgatás a 'bontás')."""
    for pts in phone_points(cx, cy, size, rotation):
        if len(pts) >= 3:
            d.polygon(pts, fill=color)


def search_icon(d, cx, cy, size, color):
    r = size * 0.32
    w = max(2, int(size * 0.11))
    d.ellipse([cx - r - size * 0.1, cy - r - size * 0.1, cx + r - size * 0.1, cy + r - size * 0.1],
              outline=color, width=w)
    d.line([cx + r * 0.55, cy + r * 0.55, cx + size * 0.42, cy + size * 0.42],
           fill=color, width=w)


def plus_icon(d, cx, cy, size, color, width=4):
    d.line([cx - size / 2, cy, cx + size / 2, cy], fill=color, width=width)
    d.line([cx, cy - size / 2, cx, cy + size / 2], fill=color, width=width)


def hamburger(d, cx, cy, size, color, width=4):
    for i in (-1, 0, 1):
        y = cy + i * size * 0.30
        d.line([cx - size / 2, y, cx + size / 2, y], fill=color, width=width)


def backspace(d, cx, cy, size, color):
    w = max(2, int(size * 0.09))
    d.polygon([
        (cx - size * 0.46, cy),
        (cx - size * 0.16, cy - size * 0.34),
        (cx + size * 0.46, cy - size * 0.34),
        (cx + size * 0.46, cy + size * 0.34),
        (cx - size * 0.16, cy + size * 0.34),
    ], outline=color, width=w)
    d.line([cx - size * 0.12, cy - size * 0.14, cx + size * 0.16, cy + size * 0.14], fill=color, width=w)
    d.line([cx + size * 0.16, cy - size * 0.14, cx - size * 0.12, cy + size * 0.14], fill=color, width=w)


# --------------------------------------------------------------------------- képernyők

def screen_keypad(c: dict, *, number="+36 30 123 4567", country="MAGYARORSZÁG",
                  show_pill=True, app_name="BBetter.", active_call=True):
    img = Image.new("RGB", (W, H), c["bg"])
    d = ImageDraw.Draw(img)

    # --- fejléc
    hamburger(d, 90, 56, 40, c["tp"])
    label(d, W / 2, 56, app_name, 30, c["tt"], bold=True)
    if show_pill:
        text = "Névjegy hozzáadása"
        w = 392
        rrect(d, [W - 24 - w, 18, W - 24, 94], 38, fill=c["s3"])
        plus_icon(d, W - 24 - w + 40, 56, 26, c["ts"])
        label(d, W - 24 - w + 40 + 26 + 130, 56, text, 21, c["tp"])

    # --- országsor
    label(d, 44, 268, country, 22, c["tt"], bold=True, anchor="lm")

    # --- kijelző
    label(d, W / 2, 400, number, 62, c["tp"], anchor="mm")

    # --- billentyűzet
    keys = [("1", ""), ("2", "ABC"), ("3", "DEF"),
            ("4", "GHI"), ("5", "JKL"), ("6", "MNO"),
            ("7", "PQRS"), ("8", "TUV"), ("9", "WXYZ"),
            ("*", ""), ("0", "+"), ("#", "")]
    grid_w = 536
    gap = 20
    key = (grid_w - 2 * gap) / 3
    x0 = (W - grid_w) / 2
    y0 = 620
    for i, (digit, letters) in enumerate(keys):
        row, col = divmod(i, 3)
        cx = x0 + col * (key + gap) + key / 2
        cy = y0 + row * (key + gap) + key / 2
        circle(d, cx, cy, key / 2, fill=c["s2"])
        label(d, cx, cy - (10 if letters else 0), digit, 46, c["tp"])
        if letters:
            label(d, cx, cy + 32, letters, 14, c["tt"])

    # --- alsó sor
    bottom = y0 + 4 * (key + gap) + 40
    call_bg = GREEN if active_call else IDLE_GREEN_DARK
    call_fg = (0, 0, 0) if active_call else IDLE_TINT_DARK
    circle(d, W / 2, bottom, 76, fill=call_bg)
    phone_icon(d, W / 2, bottom, 66, call_fg)
    circle(d, W - 120, bottom, 50, fill=c["s2"])
    backspace(d, W - 120, bottom, 40, c["ts"])
    return img


def screen_contacts(c: dict, *, query_field="Név vagy szám keresése"):
    img = Image.new("RGB", (W, H), c["bg"])
    d = ImageDraw.Draw(img)

    hamburger(d, 90, 56, 40, c["tp"])
    label(d, W / 2, 56, "BBetter.", 30, c["tt"], bold=True)

    # kereső
    rrect(d, [32, 108, W - 32, 196], 26, fill=c["s2"])
    label(d, 60, 152, query_field, 26, c["tt"], anchor="lm")
    search_icon(d, W - 74, 152, 40, c["ts"])

    # mappaszűrők
    x = 32
    x = pill(d, x, 224, "Mind", fill=c["tp"], fg=c["bg"], size=23, bold=True) + 14
    x = pill(d, x, 224, "Család", fill=c["s2"], fg=c["ts"], size=23) + 14
    x = pill(d, x, 224, "Munka", fill=c["s2"], fg=c["ts"], size=23) + 14
    # "＋ Mappa" – a teljes szélességű pluszjel hiányzik a fontból, ezért rajzoljuk
    mw = d.textlength("Mappa", font=font(23)) + 66
    rrect(d, [x, 224, x + mw, 272], 24, fill=c["s2"])
    plus_icon(d, x + 28, 248, 22, GREEN, width=4)
    label(d, x + 48, 248, "Mappa", 23, GREEN, anchor="lm")

    people = [
        ("Kovács Anna", "+36 30 123 4567", "Család", 0, True, None, ""),
        ("Nagy Péter", "+36 20 555 1122", "Munka", 1, False, None, "NP"),
        ("Szabó Judit", "+36 70 998 4433", None, 2, True, None, ""),
        ("Tóth Bence", "+36 30 776 2210", "Munka", 3, False, (0, 137, 123), "TB"),
        ("Horváth Eszter", "+36 1 234 5678", "Család", 4, True, None, ""),
        ("Varga Dániel", "+36 20 114 8890", None, 5, False, (216, 27, 96), "VD"),
        ("Kiss Márta", "+36 30 664 7781", "Család", 6, False, (251, 140, 0), "KM"),
    ]

    y = 286
    for name, number, folder, idx, photo, color, initials in people:
        rrect(d, [32, y, W - 32, y + 130], 36, fill=c["s1"])
        avatar(img, 92, y + 65, 46, photo=photo, color=color, initials=initials)
        label(d, 154, y + 46, name, 30, c["tp"], anchor="lm")
        label(d, 154, y + 86, number, 22, c["ts"], anchor="lm")
        if folder:
            w = d.textlength(folder.upper(), font=font(18, True)) + 26
            rrect(d, [154 + d.textlength(number, font=font(22)) + 16, y + 74,
                      154 + d.textlength(number, font=font(22)) + 16 + w, y + 100], 13,
                  fill=c["s2"])
            label(d, 154 + d.textlength(number, font=font(22)) + 16 + w / 2, y + 87,
                  folder.upper(), 18, c["ts"], bold=True)
        phone_icon(d, W - 76, y + 65, 40, GREEN)
        y += 142
        if y > H - 160:
            break

    return img


def screen_history(c: dict):
    img = Image.new("RGB", (W, H), c["bg"])
    d = ImageDraw.Draw(img)

    hamburger(d, 90, 56, 40, c["tp"])
    label(d, W / 2, 56, "BBetter.", 30, c["tt"], bold=True)

    rows = [
        ("Mai nap", "12 hívás", [
            ("Kovács Anna", "most · +36 30 123 4567", True, None, "6×", None),
            ("Nagy Péter", "2 órája · +36 20 555 1122", False, (0, 137, 123), "1×", "NP"),
            ("Ismeretlen", "3 órája · +36 70 000 1122", False, None, "3×", None),
        ]),
        ("Tegnap", "4 hívás", [
            ("Szabó Judit", "tegnap · +36 70 998 4433", True, None, "2×", None),
            ("Tóth Bence", "tegnap · +36 30 776 2210", False, (216, 27, 96), "1×", "TB"),
        ]),
        ("A héten", "9 hívás", [
            ("Horváth Eszter", "3 napja · +36 1 234 5678", True, None, "4×", None),
            ("Varga Dániel", "5 napja · +36 20 114 8890", False, (251, 140, 0), "1×", "VD"),
        ]),
    ]

    y = 120
    for title, count, items in rows:
        label(d, 44, y + 20, title, 32, c["tp"], bold=True, anchor="lm")
        label(d, 44 + d.textlength(title, font=font(32, True)) + 18, y + 24,
              count, 22, c["tt"], anchor="lm")
        y += 62
        for name, sub, photo, color, badge, initials in items:
            rrect(d, [32, y, W - 32, y + 130], 36, fill=c["s1"])
            avatar(img, 92, y + 65, 46, photo=photo, color=color, initials=initials)
            label(d, 154, y + 46, name, 30, c["tp"], anchor="lm")
            label(d, 154, y + 86, sub, 21, c["tt"], anchor="lm")
            bw = d.textlength(badge, font=font(22, True)) + 30
            rrect(d, [W - 170 - bw, y + 46, W - 170, y + 84], 19, fill=c["tp"])
            label(d, W - 170 - bw / 2, y + 65, badge, 22, c["bg"], bold=True)
            phone_icon(d, W - 76, y + 65, 40, GREEN)
            y += 142
        y += 10
    return img


def screen_incall(c: dict):
    img = Image.new("RGB", (W, H), c["bg"])
    d = ImageDraw.Draw(img)

    label(d, 44, 66, "HÍVÁSBAN", 22, c["ts"], bold=True, anchor="lm")
    circle(d, W - 62, 66, 38, fill=c["s2"])
    d.line([W - 74, 54, W - 50, 78], fill=c["ts"], width=4)
    d.line([W - 50, 54, W - 74, 78], fill=c["ts"], width=4)

    # mappacímke
    w = d.textlength("CSALÁD", font=font(19, True)) + 30
    rrect(d, [(W - w) / 2, 168, (W + w) / 2, 208], 20, fill=c["tp"])
    label(d, W / 2, 188, "CSALÁD", 19, c["bg"], bold=True)

    # profilkép
    avatar(img, W / 2, 500, 200, photo=True)

    label(d, W / 2, 790, "Kovács Anna", 56, c["tp"], bold=True)
    label(d, W / 2, 856, "+36 30 123 4567", 27, c["ts"])
    label(d, W / 2, 926, "0:42", 30, c["tt"])

    # vezérlők
    rrect(d, [32, 1040, W - 32, 1270], 60, fill=c["s1"])
    for cx, txt, icon in ((218, "Némítás", "mic"), (W - 218, "Kihangosítás", "spk")):
        circle(d, cx, 1128, 58, fill=c["s2"])
        if icon == "mic":
            d.rounded_rectangle([cx - 11, 1102, cx + 11, 1136], radius=12, fill=c["tp"])
            d.arc([cx - 22, 1116, cx + 22, 1154], start=0, end=180, fill=c["tp"], width=5)
            d.line([cx, 1150, cx, 1164], fill=c["tp"], width=5)
        else:
            d.polygon([(cx - 26, 1116), (cx - 10, 1116), (cx + 6, 1102),
                       (cx + 6, 1154), (cx - 10, 1140), (cx - 26, 1140)], fill=c["tp"])
            for k, r in ((0, 8), (1, 14), (2, 20)):
                d.arc([cx + 6, 1128 - r, cx + 6 + 2 * r, 1128 + r], start=-45, end=45,
                      fill=c["tp"], width=4)
        label(d, cx, 1218, txt, 24, c["ts"])

    # bontás
    circle(d, W / 2, 1380, 76, fill=RED)
    phone_icon_rotated(d, W / 2, 1380, 66, (0, 0, 0))
    return img


def phone_icon_rotated(d, cx, cy, size, color):
    """A bontás gomb ikonja: a kagyló 135°-kal elforgatva."""
    phone_icon(d, cx, cy, size, color, rotation=135.0)


def screen_settings(c: dict):
    img = Image.new("RGB", (W, H), c["bg"])
    d = ImageDraw.Draw(img)

    # vissza nyíl + cím
    d.line([70, 56, 96, 56], fill=c["tp"], width=5)
    d.line([72, 56, 88, 38], fill=c["tp"], width=5)
    d.line([72, 56, 88, 74], fill=c["tp"], width=5)
    label(d, 120, 56, "Beállítások", 40, c["tp"], bold=True, anchor="lm")

    def section(title, y):
        label(d, 44, y, title.upper(), 20, c["tt"], bold=True, anchor="lm")

    def row(y, title, subtitle, h=124):
        rrect(d, [32, y, W - 32, y + h], 36, fill=c["s1"])
        label(d, 76, y + 44, title, 30, c["tp"], anchor="lm")
        label(d, 76, y + 84, subtitle, 22, c["tt"], anchor="lm")
        d.line([W - 76, y + h / 2 - 9, W - 62, y + h / 2], fill=c["tt"], width=4)
        d.line([W - 76, y + h / 2 + 9, W - 62, y + h / 2], fill=c["tt"], width=4)
        return y + h + 14

    y = 140
    section("Megjelenés", y)
    y += 34
    y = row(y, "Téma", "Rendszer követése")
    y = row(y, "Nyelv", "Magyar")
    y = row(y, "App testreszabás", "BBetter. · Dialer")
    y += 20
    section("Névjegyek", y)
    y += 34
    y = row(y, "Név", "Névjegyek átnevezése")
    y = row(y, "Profilképek", "Fotó cseréje galériából, kijelöléssel")
    y = row(y, "Profil színek", "Szín a fotó nélküli névjegyekhez")
    y += 20
    section("Hangok", y)
    y += 34
    row(y, "Csengőhangok", "Általános és kontakt-szintű")
    return img


# --------------------------------------------------------------------------- keret + mentés

def framed(screen: Image.Image) -> Image.Image:
    w, h = screen.size
    out = Image.new("RGB", (w + PAD * 2, h + PAD * 2), (26, 26, 28))
    mask = Image.new("L", out.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, out.size[0] - 1, out.size[1] - 1],
                                           radius=RADIUS + PAD, fill=255)
    inner = Image.new("L", (w, h), 0)
    ImageDraw.Draw(inner).rounded_rectangle([0, 0, w - 1, h - 1], radius=RADIUS, fill=255)
    out.paste(screen, (PAD, PAD), inner)
    out.putalpha(mask)
    return out


def save(img: Image.Image, name: str):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    img.save(path, "PNG", optimize=True)
    print(f"  {name:34s} {os.path.getsize(path) / 1024:7.1f} KB")


def main() -> None:
    print("Látványtervek készítése...")

    keypad_dark = framed(screen_keypad(DARK))
    contacts_dark = framed(screen_contacts(DARK))
    history_dark = framed(screen_history(DARK))
    incall_dark = framed(screen_incall(DARK))
    settings_dark = framed(screen_settings(DARK))
    keypad_light = framed(screen_keypad(LIGHT, number="+36 30 123 4567", app_name="BBetter."))

    save(keypad_dark, "01-keypad-dark.png")
    save(contacts_dark, "02-contacts-dark.png")
    save(incall_dark, "03-incall-dark.png")
    save(history_dark, "04-history-dark.png")
    save(settings_dark, "05-settings-dark.png")
    save(keypad_light, "06-keypad-light.png")

    # --- hero kép a README tetejére
    scale = 0.72
    phones = [keypad_dark, contacts_dark, incall_dark]
    scaled = [
        p.resize((int(p.width * scale), int(p.height * scale)), Image.LANCZOS)
        for p in phones
    ]

    cw, chh = 2160, 1500
    bg = Image.new("RGB", (cw, chh), (8, 8, 10))
    bd = ImageDraw.Draw(bg)
    for i in range(chh):
        t = i / chh
        bd.line([(0, i), (cw, i)],
                fill=(int(8 + 10 * t), int(8 + 12 * t), int(12 + 20 * t)))

    glow = Image.new("RGB", (cw, chh), (8, 8, 10))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([-500, -600, 1400, 1000], fill=(0, 66, 42))
    gd.ellipse([1200, -400, 2700, 1000], fill=(0, 34, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(240))
    bg = Image.blend(bg, glow, 0.6)

    pw, ph = scaled[0].size
    gap = (cw - pw * 3) / 4
    y = chh - ph - 50
    for i, s in enumerate(scaled):
        bg.paste(s, (int(gap + i * (pw + gap)), y), s)

    bd = ImageDraw.Draw(bg)
    bd.text((cw / 2, 104), "BBetter. Dialer", font=font(78, True),
            fill=(255, 255, 255), anchor="mm")
    bd.text((cw / 2, 182),
            "Open-source Android dialer   ·   Kotlin + Jetpack Compose   ·   AMOLED minimal UI",
            font=font(29), fill=(148, 148, 156), anchor="mm")
    save(bg, "00-hero.png")

    print("Kész.")


if __name__ == "__main__":
    main()
