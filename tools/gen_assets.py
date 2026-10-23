#!/usr/bin/env python3
"""
Generator zasobów RedCircuit: tekstury (oryginalne, proceduralne), modele,
blockstates, receptury, loot tables, tagi i tłumaczenia.

Uruchom z katalogu projektu:  python3 tools/gen_assets.py
"""
import json
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFont

MODID = "redcircuit"
ROOT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", MODID)
DATA = os.path.join(RES, "data", MODID)
MC_DATA = os.path.join(RES, "data", "minecraft")

DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
DYE_RGB = {
    "white": (233, 236, 236), "orange": (240, 118, 19), "magenta": (189, 68, 179),
    "light_blue": (58, 175, 217), "yellow": (248, 197, 39), "lime": (112, 185, 25),
    "pink": (237, 141, 172), "gray": (62, 68, 71), "light_gray": (142, 142, 134),
    "cyan": (21, 137, 145), "purple": (121, 42, 172), "blue": (53, 57, 157),
    "brown": (114, 71, 40), "green": (84, 109, 27), "red": (160, 39, 34), "black": (25, 25, 29),
}
DYE_PL = {
    "white": "biały", "orange": "pomarańczowy", "magenta": "karmazynowy", "light_blue": "jasnoniebieski",
    "yellow": "żółty", "lime": "jasnozielony", "pink": "różowy", "gray": "szary",
    "light_gray": "jasnoszary", "cyan": "błękitny", "purple": "fioletowy", "blue": "niebieski",
    "brown": "brązowy", "green": "zielony", "red": "czerwony", "black": "czarny",
}

GATES = ["not", "buffer", "and", "nand", "or", "nor", "xor", "xnor",
         "rs_latch", "toggle", "pulse", "timer", "randomizer",
         "counter", "sequencer", "state_cell", "synchronizer"]
GATE_LABEL = {"not": "NOT", "buffer": "BUF", "and": "AND", "nand": "NAND", "or": "OR", "nor": "NOR",
              "xor": "XOR", "xnor": "XNOR", "rs_latch": "RS", "toggle": "TGL", "pulse": "PLS",
              "timer": "TMR", "randomizer": "RND",
              "counter": "CNT", "sequencer": "SEQ", "state_cell": "SC", "synchronizer": "SYN"}
GATE_EN = {"not": "NOT Gate", "buffer": "Buffer Gate", "and": "AND Gate", "nand": "NAND Gate",
           "or": "OR Gate", "nor": "NOR Gate", "xor": "XOR Gate", "xnor": "XNOR Gate",
           "rs_latch": "RS Latch", "toggle": "Toggle Latch", "pulse": "Pulse Former",
           "timer": "Timer", "randomizer": "Randomizer",
           "counter": "Counter", "sequencer": "Sequencer", "state_cell": "State Cell", "synchronizer": "Synchronizer"}
GATE_PL = {"not": "Bramka NOT", "buffer": "Bufor", "and": "Bramka AND", "nand": "Bramka NAND",
           "or": "Bramka OR", "nor": "Bramka NOR", "xor": "Bramka XOR", "xnor": "Bramka XNOR",
           "rs_latch": "Zatrzask RS", "toggle": "Przerzutnik T", "pulse": "Formownik impulsu",
           "timer": "Zegar", "randomizer": "Generator losowy",
           "counter": "Licznik", "sequencer": "Sekwencer", "state_cell": "Komórka stanu", "synchronizer": "Synchronizator"}
# Które wejścia są używane: (lewo, tył, prawo)
# Bramki z wyłączanymi wejściami (okno bramki) i ich wejścia jako bity 1=L, 2=B, 4=R
DISABLE_GATES = {"and", "nand", "or", "nor", "xor", "xnor", "toggle", "buffer", "not"}
# Bramki z bocznymi wyjściami (wyłączanymi w oknie)
SIDE_OUTPUT_GATES = {"buffer", "not"}
DISABLE_PROPS = ((1, "disabled_left"), (2, "disabled_back"), (4, "disabled_right"))


def gate_input_bits(g):
    l, b, r = GATE_INPUTS[g]
    return (1 if l else 0) | (2 if b else 0) | (4 if r else 0)


def gate_switch_bits(g):
    """Strony wyłączane w oknie: wejścia, a w buforze boczne wyjścia."""
    return 1 | 4 if g in SIDE_OUTPUT_GATES else gate_input_bits(g)


def gate_disable_masks(g):
    """Dopuszczalne maski wyłączonych wejść (co najmniej jedno wejście zostaje); [0] dla pozostałych bramek."""
    if g not in DISABLE_GATES:
        return [0]
    full = gate_switch_bits(g)
    if g in SIDE_OUTPUT_GATES:
        return [m for m in range(8) if m & ~full == 0]          # oba boczne wyjścia można wyłączyć
    return [m for m in range(8) if m & ~full == 0 and m != full]


# Bramki z drugim, przeciwnym wyjściem z tyłu
BACK_OUTPUT_GATES = {"rs_latch", "toggle"}

def gate_variants(g):
    """Warianty modelu bramki: (sufiks, warunki stanu bloku, argumenty tekstury)."""
    if g in MIRRORABLE_GATES:
        return [("", {"mirrored": "false"}, {}), ("_mirrored", {"mirrored": "true"}, {"mirrored": True})]
    if g in DISABLE_GATES:
        out = []
        for m in gate_disable_masks(g):
            when = {prop: ("true" if m & bit else "false") for bit, prop in DISABLE_PROPS if gate_switch_bits(g) & bit}
            out.append((f"_d{m}" if m else "", when, {"disabled": m}))
        return out
    if g == "counter":      # tylne wyjście: licznik na zerze
        return [("", {"state": "false"}, {}), ("_zero", {"state": "true"}, {"zero": True})]
    if g == "randomizer":   # losowe boczne wyjścia (lewe = state, prawe = input)
        return [(f"_l{int(a)}r{int(b)}" if a or b else "", {"state": str(a).lower(), "input": str(b).lower()},
                 {"rl": a, "rr": b}) for a in (False, True) for b in (False, True)]
    return [("", {}, {})]


# Bramki z wersją lustrzaną (strona wyjścia przełączana w oknie)
MIRRORABLE_GATES = {"state_cell"}

GATE_INPUTS = {"not": (0, 1, 0), "buffer": (0, 1, 0), "and": (1, 1, 1), "nand": (1, 1, 1),
               "or": (1, 1, 1), "nor": (1, 1, 1), "xor": (1, 0, 1), "xnor": (1, 0, 1),
               "rs_latch": (1, 0, 1), "toggle": (1, 0, 1), "pulse": (0, 1, 0),
               "timer": (0, 1, 0), "randomizer": (0, 1, 0),
               "counter": (1, 0, 1), "sequencer": (0, 1, 0), "state_cell": (0, 1, 1), "synchronizer": (1, 1, 1)}

CABLE_THICKNESS = {"signal_alloy": 2.0, "insulated": 3.0, "bundled_cable": 4.0, "power_alloy_wire": 3.0, "tube": 6.0}
DIRS = ["north", "south", "east", "west", "up", "down"]

rng = random.Random(4792)


# ---------------------------------------------------------------- helpers

def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def save_png(img, kind, name):
    path = os.path.join(ASSETS, "textures", kind, name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def clamp(v):
    return max(0, min(255, int(v)))


def shade(rgb, f):
    return tuple(clamp(c * f) for c in rgb)


def noisy(size, rgb, amount=0.08):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            f = 1.0 + rng.uniform(-amount, amount)
            px[x, y] = shade(rgb, f) + (255,)
    return img


def border(img, rgb, width=1):
    d = ImageDraw.Draw(img)
    s = img.size[0]
    for i in range(width):
        d.rectangle([i, i, s - 1 - i, s - 1 - i], outline=rgb + (255,))
    return img


# ---------------------------------------------------------------- textures

def tex_cable(rgb, stripe=None):
    img = noisy(16, rgb, 0.06)
    d = ImageDraw.Draw(img)
    for i in range(0, 16, 4):
        d.line([(0, i), (15, i)], fill=shade(rgb, 0.8) + (255,))
    if stripe:
        d.line([(0, 7), (15, 7)], fill=stripe + (255,))
        d.line([(0, 8), (15, 8)], fill=stripe + (255,))
    return img


def tex_bundled():
    img = noisy(16, (52, 48, 44), 0.06)
    px = img.load()
    for i, dye in enumerate(DYES):
        x = (i % 4) * 4 + 1
        y = (i // 4) * 4 + 1
        for dx in range(2):
            for dy in range(2):
                px[x + dx, y + dy] = DYE_RGB[dye] + (255,)
    return img


def tex_tube(paint=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    frame = (184, 115, 51) if paint is None else DYE_RGB[paint]
    for y in range(16):
        for x in range(16):
            edge = x < 2 or x > 13 or y < 2 or y > 13
            if edge:
                px[x, y] = shade(frame, 1.0 + rng.uniform(-0.1, 0.1)) + (255,)
            elif (x + y) % 7 == 0:
                px[x, y] = (210, 235, 240, 255)  # refleks szkła
    return img


def tex_gate_top(gate, lit, mirrored=False, disabled=0, zero=False, rl=False, rr=False):
    s = 32
    img = noisy(s, (120, 120, 118), 0.05)
    d = ImageDraw.Draw(img)
    border(img, (70, 70, 70), 1)
    on = (255, 40, 30)
    off = (110, 20, 15)
    trace = on if lit else off
    # Wyjście (góra tekstury = północ = przód)
    d.rectangle([14, 1, 17, 8], fill=trace + (255,))
    d.polygon([(10, 8), (21, 8), (15, 3)], fill=trace + (255,))
    l, b, r = GATE_INPUTS[gate]
    if mirrored:
        l, r = r, l
    # wyłączone wejścia (bity 1=L, 2=B, 4=R) nie mają wypustki - tylko zaślepiony otwór w obudowie
    plug = (88, 88, 86)
    if disabled & 1:
        l = 0
        d.rectangle([1, 14, 3, 17], fill=plug + (255,))
    if disabled & 2:
        b = 0
        d.rectangle([14, 28, 17, 30], fill=plug + (255,))
    if disabled & 4:
        r = 0
        d.rectangle([28, 14, 30, 17], fill=plug + (255,))
    if l:
        d.rectangle([1, 14, 7, 17], fill=off + (255,))
    if r:
        d.rectangle([24, 14, 30, 17], fill=off + (255,))
    if b:
        d.rectangle([14, 24, 17, 30], fill=off + (255,))
    # Boczne wyjścia (bufor): świecą razem z przodem; wyłączone - zaślepka jak przy wejściu
    if gate in SIDE_OUTPUT_GATES:
        if not disabled & 1:
            d.rectangle([2, 14, 8, 17], fill=trace + (255,))
            d.polygon([(4, 10), (4, 21), (0, 15)], fill=trace + (255,))
        if not disabled & 4:
            d.rectangle([23, 14, 29, 17], fill=trace + (255,))
            d.polygon([(27, 10), (27, 21), (31, 15)], fill=trace + (255,))
    # Generator losowy: boczne wyjścia z własnym, losowym stanem
    if gate == "randomizer":
        for x0, x1, tri, st in ((2, 8, [(4, 10), (4, 21), (0, 15)], rl), (23, 29, [(27, 10), (27, 21), (31, 15)], rr)):
            c = on if st else off
            d.rectangle([x0, 14, x1, 17], fill=c + (255,))
            d.polygon(tri, fill=c + (255,))
    # Licznik: tylne wyjście świeci na zerze
    if gate == "counter":
        c = on if zero else off
        d.rectangle([14, 23, 17, 30], fill=c + (255,))
        d.polygon([(10, 26), (21, 26), (15, 31)], fill=c + (255,))
    # Drugie wyjście z tyłu (zatrzask RS, przerzutnik T): świeci, gdy przód jest zgaszony
    if gate in BACK_OUTPUT_GATES:
        back = off if lit else on
        d.rectangle([14, 23, 17, 30], fill=back + (255,))
        d.polygon([(10, 26), (21, 26), (15, 31)], fill=back + (255,))
    # Etykieta
    label = GATE_LABEL[gate]
    try:
        font = ImageFont.load_default(size=9 if len(label) <= 3 else 7)
    except TypeError:
        font = ImageFont.load_default()
    bbox = d.textbbox((0, 0), label, font=font)
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.rectangle([5, 10, 26, 22], fill=(40, 40, 42, 255))
    d.text(((s - w) / 2 - bbox[0], 16 - h / 2 - bbox[1]), label,
           fill=(255, 90, 70, 255) if lit else (200, 200, 200, 255), font=font)
    return img


def tex_machine(base, accent, symbol):
    img = noisy(16, base, 0.07)
    border(img, shade(base, 0.6), 1)
    d = ImageDraw.Draw(img)
    if symbol == "arrow_out":
        d.rectangle([5, 5, 10, 10], fill=(30, 30, 30, 255))
        d.rectangle([6, 6, 9, 9], fill=accent + (255,))
    elif symbol == "hole":
        d.rectangle([4, 4, 11, 11], fill=(25, 25, 25, 255))
        d.rectangle([4, 4, 11, 11], outline=accent + (255,))
    elif symbol == "grid":
        for i in (5, 10):
            d.line([(2, i), (13, i)], fill=accent + (255,))
            d.line([(i, 2), (i, 13)], fill=accent + (255,))
    elif symbol == "stripe":
        d.rectangle([2, 7, 13, 8], fill=accent + (255,))
    return img


def tex_solar_top():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                px[x, y] = (150, 150, 150, 255)
            elif x % 5 == 0 or y % 5 == 0:
                px[x, y] = (190, 200, 210, 255)
            else:
                px[x, y] = shade((30, 50, 140), 1 + rng.uniform(-0.1, 0.15)) + (255,)
    return img


def tex_furnace_front(lit):
    img = tex_machine((125, 125, 130), (60, 90, 200), None)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 7, 12, 13], fill=(20, 20, 20, 255))
    if lit:
        for x in range(4, 12):
            h = rng.randint(1, 4)
            d.line([(x, 12), (x, 12 - h)], fill=(255, rng.randint(120, 200), 40, 255))
    d.rectangle([3, 2, 12, 4], fill=(60, 90, 200, 255))
    return img


def tex_battery_side():
    img = tex_machine((90, 90, 96), (60, 90, 200), None)
    d = ImageDraw.Draw(img)
    d.rectangle([5, 3, 10, 12], fill=(30, 30, 30, 255))
    d.rectangle([6, 7, 9, 11], fill=(60, 110, 255, 255))
    return img


def outline_shape(inside, colors, outline_rgb):
    """Rysuje kształt z maską inside[y][x] -> klucz koloru, z ciemnym obrysem jak w teksturach vanilli."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            k = inside[y][x]
            if not k:
                continue
            edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or not inside[y + dy][x + dx]
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            px[x, y] = (outline_rgb if edge else colors[k]) + (255,)
    return img


def tex_ingot(rgb):
    """Sztabka jako bryła w rzucie izometrycznym: romboidalna górna ścianka,
    krótka ścianka z lewej i długa ścianka z przodu (dłuższa oś biegnie w prawo w górę)."""
    O = (0.5, 6.5)          # lewy narożnik górnej ścianki
    L = (11.0, -4.5)        # długa krawędź (w prawo do góry)
    W = (4.0, 2.5)          # krótka krawędź (w prawo w dół)
    H = 4.0                 # grubość sztabki

    def add(a, b):
        return (a[0] + b[0], a[1] + b[1])

    A, B = O, add(O, L)
    C, D = add(B, W), add(O, W)
    down = lambda p: (p[0], p[1] + H)
    top = [A, B, C, D]
    left = [A, D, down(D), down(A)]
    front = [D, C, down(C), down(D)]

    big = 8  # rysujemy w 8x większej skali i zmniejszamy, żeby krawędzie były równe
    img = Image.new("RGBA", (16 * big, 16 * big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    sc = lambda poly: [(x * big, y * big) for x, y in poly]
    d.polygon(sc(left), fill=shade(rgb, 0.88) + (255,))
    d.polygon(sc(front), fill=shade(rgb, 0.66) + (255,))
    d.polygon(sc(top), fill=shade(rgb, 1.22) + (255,))
    img = img.resize((16, 16), Image.NEAREST)

    px = img.load()
    mask = [[px[x, y][3] > 0 for x in range(16)] for y in range(16)]
    out = img.copy()
    opx = out.load()
    outline = shade(rgb, 0.38) + (255,)
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            if any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or not mask[y + dy][x + dx]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                opx[x, y] = outline
    # Jasna krawędź na styku górnej ścianki z przodem i odblask
    for t in range(1, 9):
        x = int(D[0] + (C[0] - D[0]) * t / 9)
        y = int(D[1] + (C[1] - D[1]) * t / 9) - 1
        if 0 <= x < 16 and 0 <= y < 16 and mask[y][x] and opx[x, y] != outline:
            opx[x, y] = shade(rgb, 1.45) + (255,)
    for x, y in ((3, 6), (4, 6), (4, 5)):
        if mask[y][x] and opx[x, y] != outline:
            opx[x, y] = shade(rgb, 1.6) + (255,)
    return out


def tex_plate():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 5, 14, 11], fill=(150, 150, 148, 255), outline=(90, 90, 90, 255))
    d.line([(3, 8), (12, 8)], fill=(180, 30, 25, 255))
    d.rectangle([6, 7, 8, 9], fill=(230, 50, 40, 255))
    return img


def gen_textures():
    save_png(tex_cable((110, 18, 14)), "block", "signal_alloy_off")
    save_png(tex_cable((235, 40, 28)), "block", "signal_alloy_on")
    for dye in DYES:
        save_png(tex_cable(DYE_RGB[dye]), "block", "insulated_" + dye)
    save_png(tex_bundled(), "block", "bundled_cable")
    save_png(tex_cable((50, 90, 220), stripe=(140, 190, 255)), "block", "power_alloy_wire")
    save_png(tex_tube(), "block", "tube")
    for dye in DYES:
        save_png(tex_tube(dye), "block", "tube_" + dye)

    save_png(noisy(16, (150, 150, 146), 0.05), "block", "gate_side")
    for g in GATES:
        for suffix, _, kw in gate_variants(g):
            save_png(tex_gate_top(g, False, **kw), "block", f"gate_{g}_off{suffix}")
            save_png(tex_gate_top(g, True, **kw), "block", f"gate_{g}_on{suffix}")

    save_png(tex_machine((120, 92, 60), (200, 40, 30), "arrow_out"), "block", "extractor_front")
    save_png(tex_machine((120, 92, 60), (60, 60, 60), "hole"), "block", "extractor_back")
    save_png(tex_machine((120, 92, 60), (200, 40, 30), "stripe"), "block", "extractor_side")
    save_png(tex_machine((100, 110, 120), (200, 40, 30), "arrow_out"), "block", "filter_front")
    save_png(tex_machine((100, 110, 120), (60, 60, 60), "hole"), "block", "filter_back")
    save_png(tex_machine((100, 110, 120), (230, 230, 230), "grid"), "block", "filter_side")
    save_png(tex_machine((70, 90, 70), (200, 40, 30), "arrow_out"), "block", "sorting_machine_front")
    save_png(tex_machine((70, 90, 70), (60, 60, 60), "hole"), "block", "sorting_machine_back")
    save_png(tex_machine((70, 90, 70), (240, 200, 60), "grid"), "block", "sorting_machine_side")

    save_png(tex_furnace_front(False), "block", "electric_furnace_front")
    save_png(tex_furnace_front(True), "block", "electric_furnace_front_on")
    save_png(tex_machine((125, 125, 130), (60, 90, 200), "stripe"), "block", "electric_furnace_side")
    save_png(tex_machine((110, 110, 115), (60, 90, 200), "grid"), "block", "electric_furnace_top")
    save_png(tex_solar_top(), "block", "solar_panel_top")
    save_png(tex_machine((140, 140, 140), (60, 90, 200), "stripe"), "block", "solar_panel_side")
    save_png(tex_battery_side(), "block", "battery_box_side")
    save_png(tex_machine((90, 90, 96), (60, 110, 255), "hole"), "block", "battery_box_top")
    save_png(noisy(16, (70, 70, 74), 0.05), "block", "battery_box_bottom")

    save_png(tex_plate(), "item", "circuit_plate")


# ---------------------------------------------------------------- models

def faces(tex, auto_uv=False):
    if auto_uv:   # UV z położenia elementu - tekstura ciągnie się przez środek i ramiona bez powtórzeń
        return {f: {"texture": tex} for f in ["north", "south", "east", "west", "up", "down"]}
    return {f: {"uv": [0, 0, 16, 16], "texture": tex} for f in ["north", "south", "east", "west", "up", "down"]}


def arm_box(direction, half):
    a, b = 8 - half, 8 + half
    return {
        "north": ([a, a, 0], [b, b, a]),
        "south": ([a, a, b], [b, b, 16]),
        "west": ([0, a, a], [a, b, b]),
        "east": ([b, a, a], [16, b, b]),
        "down": ([a, 0, a], [b, a, b]),
        "up": ([a, b, a], [b, 16, b]),
    }[direction]


def cable_models(prefix, texture, half, auto_uv=False):
    """Tworzy model środka i 6 ramion dla kabla. Zwraca słownik nazw modeli."""
    base = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"t": texture, "particle": texture}}
    a, b = 8 - half, 8 + half
    center = dict(base, elements=[{"from": [a, a, a], "to": [b, b, b], "faces": faces("#t", auto_uv)}])
    write_json(os.path.join(ASSETS, "models", "block", f"{prefix}_center.json"), center)
    for d in DIRS:
        f, t = arm_box(d, half)
        write_json(os.path.join(ASSETS, "models", "block", f"{prefix}_{d}.json"),
                   dict(base, elements=[{"from": f, "to": t, "faces": faces("#t", auto_uv)}]))
    # Model do ekwipunku: środek + ramiona N/S
    inv_elements = [{"from": [a, a, a], "to": [b, b, b], "faces": faces("#t", auto_uv)}]
    for d in ("north", "south"):
        f, t = arm_box(d, half)
        inv_elements.append({"from": f, "to": t, "faces": faces("#t", auto_uv)})
    write_json(os.path.join(ASSETS, "models", "block", f"{prefix}_inventory.json"),
               dict(base, elements=inv_elements))


def simple_cable(block, texture, thickness, auto_uv=False):
    half = thickness / 2
    cable_models(block, texture, half, auto_uv)
    parts = [{"apply": {"model": f"{MODID}:block/{block}_center"}}]
    for d in DIRS:
        parts.append({"when": {d: "true"}, "apply": {"model": f"{MODID}:block/{block}_{d}"}})
    write_json(os.path.join(ASSETS, "blockstates", f"{block}.json"), {"multipart": parts})
    item_model(block, f"{MODID}:block/{block}_inventory")


def item_model(item, parent):
    write_json(os.path.join(ASSETS, "models", "item", f"{item}.json"), {"parent": parent})


def vanilla_tinted_item(item, vanilla_texture):
    """Przedmiot korzystający z tekstury z gry (np. sztabki żelaza), przebarwionej w kodzie (ItemColor)."""
    write_json(os.path.join(ASSETS, "models", "item", f"{item}.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": vanilla_texture}})


def generated_item(item):
    write_json(os.path.join(ASSETS, "models", "item", f"{item}.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{item}"}})


FACE_WIRES = ["signal_alloy_wire"] + [f"insulated_wire_{d}" for d in DYES] + ["bundled_cable", "power_alloy_wire"]


def face_wire(block, texture, w, h):
    a, b = 8 - w / 2, 8 + w / 2
    write_json(os.path.join(ASSETS, "models", "block", f"{block}_flat.json"), {
        "parent": "minecraft:block/block",
        "textures": {"t": texture, "particle": texture},
        "elements": [{"from": [a, 0, 0], "to": [b, h, 16], "faces": faces("#t")}]})
    write_json(os.path.join(ASSETS, "blockstates", f"{block}.json"),
               {"multipart": [{"apply": {"model": f"{MODID}:block/{block}_flat"}}]})
    item_model(block, f"{MODID}:block/{block}_flat")


def gen_models():
    # Przewody przyklejane do ścian: geometria generowana w kodzie (FaceWireModel).
    # Tu tylko model zastępczy (płaski odcinek na podłodze) dla blockstate i przedmiotu.
    face_wire("signal_alloy_wire", f"{MODID}:block/signal_alloy_off", 2, 2)
    for dye in DYES:
        face_wire(f"insulated_wire_{dye}", f"{MODID}:block/insulated_{dye}", 4, 3)
    face_wire("bundled_cable", f"{MODID}:block/bundled_cable", 6, 4)
    face_wire("power_alloy_wire", f"{MODID}:block/power_alloy_wire", 2, 2)

    # Rura: ramiona wspólne, środek zależny od koloru
    cable_models("tube", f"{MODID}:block/tube", 3.0)
    parts = [{"when": {"paint": "16"}, "apply": {"model": f"{MODID}:block/tube_center"}}]
    for i, dye in enumerate(DYES):
        write_json(os.path.join(ASSETS, "models", "block", f"tube_center_{dye}.json"),
                   {"parent": f"{MODID}:block/tube_center", "render_type": "minecraft:cutout",
                    "textures": {"t": f"{MODID}:block/tube_{dye}",
                                                                         "particle": f"{MODID}:block/tube_{dye}"}})
        parts.append({"when": {"paint": str(i)}, "apply": {"model": f"{MODID}:block/tube_center_{dye}"}})
    for d in DIRS:
        parts.append({"when": {d: "true"}, "apply": {"model": f"{MODID}:block/tube_{d}"}})
    write_json(os.path.join(ASSETS, "blockstates", "tube.json"), {"multipart": parts})
    item_model("tube", f"{MODID}:block/tube_inventory")

    # Bramki
    gate_template = {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#side"},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 2, 16],
            "faces": {
                "down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "north": {"uv": [0, 14, 16, 16], "texture": "#side"},
                "south": {"uv": [0, 14, 16, 16], "texture": "#side"},
                "west": {"uv": [0, 14, 16, 16], "texture": "#side"},
                "east": {"uv": [0, 14, 16, 16], "texture": "#side"},
            }
        }]
    }
    write_json(os.path.join(ASSETS, "models", "block", "gate_base.json"), gate_template)
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for g in GATES:
        variants = gate_variants(g)
        for suffix, _, _ in variants:
            for lit in ("off", "on"):
                write_json(os.path.join(ASSETS, "models", "block", f"{g}_gate_{lit}{suffix}.json"),
                           {"parent": f"{MODID}:block/gate_base",
                            "textures": {"top": f"{MODID}:block/gate_{g}_{lit}{suffix}", "side": f"{MODID}:block/gate_side"}})
        parts = []
        for facing, y in rot.items():
            for lit, powered in (("off", "false"), ("on", "true")):
                for suffix, extra, _ in variants:
                    apply = {"model": f"{MODID}:block/{g}_gate_{lit}{suffix}"}
                    if y:
                        apply["y"] = y
                    when = {"facing": facing, "powered": powered, **extra}
                    parts.append({"when": when, "apply": apply})
        write_json(os.path.join(ASSETS, "blockstates", f"{g}_gate.json"), {"multipart": parts})
        item_model(f"{g}_gate", f"{MODID}:block/{g}_gate_off")

    # Maszyny z 6 kierunkami (przód na północy modelu)
    rot6 = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
            "up": {"x": 270}, "down": {"x": 90}}
    for m in ("extractor", "filter", "sorting_machine"):
        write_json(os.path.join(ASSETS, "models", "block", f"{m}.json"), {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MODID}:block/{m}_side",
                "north": f"{MODID}:block/{m}_front",
                "south": f"{MODID}:block/{m}_back",
                "east": f"{MODID}:block/{m}_side",
                "west": f"{MODID}:block/{m}_side",
                "up": f"{MODID}:block/{m}_side",
                "down": f"{MODID}:block/{m}_side",
            }})
        parts = []
        for facing, r in rot6.items():
            parts.append({"when": {"facing": facing}, "apply": dict({"model": f"{MODID}:block/{m}"}, **r)})
        write_json(os.path.join(ASSETS, "blockstates", f"{m}.json"), {"multipart": parts})
        item_model(m, f"{MODID}:block/{m}")

    # Piec elektryczny
    for lit, tex in (("", "electric_furnace_front"), ("_on", "electric_furnace_front_on")):
        write_json(os.path.join(ASSETS, "models", "block", f"electric_furnace{lit}.json"), {
            "parent": "minecraft:block/orientable",
            "textures": {"top": f"{MODID}:block/electric_furnace_top",
                         "front": f"{MODID}:block/{tex}",
                         "side": f"{MODID}:block/electric_furnace_side"}})
    parts = []
    for facing, y in rot.items():
        for lit, val in (("", "false"), ("_on", "true")):
            apply = {"model": f"{MODID}:block/electric_furnace{lit}"}
            if y:
                apply["y"] = y
            parts.append({"when": {"facing": facing, "lit": val}, "apply": apply})
    write_json(os.path.join(ASSETS, "blockstates", "electric_furnace.json"), {"multipart": parts})
    item_model("electric_furnace", f"{MODID}:block/electric_furnace")

    # Panel słoneczny
    write_json(os.path.join(ASSETS, "models", "block", "solar_panel.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MODID}:block/solar_panel_side",
                     "top": f"{MODID}:block/solar_panel_top", "side": f"{MODID}:block/solar_panel_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
            "down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"},
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
            "north": {"uv": [0, 10, 16, 16], "texture": "#side"},
            "south": {"uv": [0, 10, 16, 16], "texture": "#side"},
            "west": {"uv": [0, 10, 16, 16], "texture": "#side"},
            "east": {"uv": [0, 10, 16, 16], "texture": "#side"}}}]})
    write_json(os.path.join(ASSETS, "blockstates", "solar_panel.json"),
               {"variants": {"": {"model": f"{MODID}:block/solar_panel"}}})
    item_model("solar_panel", f"{MODID}:block/solar_panel")

    # Akumulator
    write_json(os.path.join(ASSETS, "models", "block", "battery_box.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{MODID}:block/battery_box_top", "bottom": f"{MODID}:block/battery_box_bottom",
                     "side": f"{MODID}:block/battery_box_side"}})
    write_json(os.path.join(ASSETS, "blockstates", "battery_box.json"),
               {"variants": {"": {"model": f"{MODID}:block/battery_box"}}})
    item_model("battery_box", f"{MODID}:block/battery_box")

    generated_item("circuit_plate")
    for item in ("signal_alloy_ingot", "power_alloy_ingot"):
        vanilla_tinted_item(item, "minecraft:item/iron_ingot")


# ---------------------------------------------------------------- data

def all_blocks():
    blocks = ["signal_alloy_wire"] + [f"insulated_wire_{d}" for d in DYES] + ["bundled_cable"]
    blocks += [f"{g}_gate" for g in GATES]
    blocks += ["tube", "extractor", "filter", "sorting_machine",
               "power_alloy_wire", "solar_panel", "battery_box", "electric_furnace"]
    return blocks


def gen_loot():
    for b in all_blocks():
        if b in FACE_WIRES:
            # Jeden przedmiot za każdą ścianę, na której leży przewód.
            pools = [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{b}"}],
                "conditions": [
                    {"condition": "minecraft:survives_explosion"},
                    {"condition": "minecraft:block_state_property", "block": f"{MODID}:{b}",
                     "properties": {d: "true"}}]
            } for d in DIRS]
        else:
            pools = [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{b}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]
            }]
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{b}.json"), {
            "type": "minecraft:block",
            "pools": pools,
            "random_sequence": f"{MODID}:blocks/{b}"
        })


def gen_tags():
    write_json(os.path.join(DATA, "tags", "item", "insulated_wires.json"),
               {"values": [f"{MODID}:insulated_wire_{d}" for d in DYES]})
    write_json(os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json"), {
        "replace": False,
        "values": [f"{MODID}:{b}" for b in ("extractor", "filter", "sorting_machine",
                                            "solar_panel", "battery_box", "electric_furnace", "tube")]})


# Składniki zamieniane na wspólne tagi (c:), żeby działały też odpowiedniki z innych modów (modpacki).
COMMON_TAGS = {
    "minecraft:iron_ingot": "c:ingots/iron", "minecraft:copper_ingot": "c:ingots/copper",
    "minecraft:gold_ingot": "c:ingots/gold", "minecraft:netherite_ingot": "c:ingots/netherite",
    "minecraft:iron_nugget": "c:nuggets/iron", "minecraft:gold_nugget": "c:nuggets/gold",
    "minecraft:redstone": "c:dusts/redstone", "minecraft:redstone_block": "c:storage_blocks/redstone",
    "minecraft:diamond": "c:gems/diamond", "minecraft:quartz": "c:gems/quartz",
    "minecraft:glass": "c:glass_blocks", "minecraft:chest": "c:chests/wooden",
    "minecraft:cobblestone": "c:cobblestones/normal", "minecraft:string": "c:strings",
    "minecraft:stick": "c:rods/wooden", "minecraft:netherrack": "c:netherracks",
    "minecraft:obsidian": "c:obsidians", "minecraft:leather": "c:leathers",
    "minecraft:slime_ball": "c:slime_balls",
    "redcircuit:tin_ingot": "c:ingots/tin", "redcircuit:silver_ingot": "c:ingots/silver",
    "redcircuit:brass_ingot": "c:ingots/brass",
    "redcircuit:signal_alloy_ingot": "c:ingots/signal_alloy", "redcircuit:power_alloy_ingot": "c:ingots/power_alloy",
    "redcircuit:steel_ingot": "c:ingots/steel",
    "redcircuit:tin_nugget": "c:nuggets/tin", "redcircuit:silver_nugget": "c:nuggets/silver",
    "redcircuit:azurite": "c:dusts/azurite",
}
for _d in DYES:
    COMMON_TAGS[f"minecraft:{_d}_dye"] = f"c:dyes/{_d}"
for _g in ("stone", "copper", "gold", "iron", "brass", "silver", "diamond", "netherite"):
    COMMON_TAGS[f"redcircuit:{_g}_gear"] = f"c:gears/{_g}"


def ing(x):
    if x.startswith("#"):
        return {"tag": x[1:]}
    if x in COMMON_TAGS:
        return {"tag": COMMON_TAGS[x]}
    return {"item": x}


def tex_lamp(color, lit):
    """Lampa: rama z ciemnego metalu, szklany klosz w kolorze; zapalona - jasna z poświatą w środku."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    frame = (58, 60, 66)
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                px[x, y] = shade(frame, 0.8) + (255,)
            elif edge == 1:
                px[x, y] = frame + (255,)
            else:
                # odległość od środka -> jasność klosza
                dx, dy = x - 7.5, y - 7.5
                d = (dx * dx + dy * dy) ** 0.5 / 7.0
                if lit:
                    f = 1.35 - 0.45 * d
                    c = tuple(min(255, int(ch * f + 60 * (1 - d))) for ch in color)
                else:
                    f = 0.42 - 0.08 * d
                    c = tuple(int(ch * f + 18) for ch in color)
                # delikatne "szkło" - ukośne odblaski
                if (x + y) % 7 == 0 and edge > 2:
                    c = tuple(min(255, int(ch * 1.12)) for ch in c)
                px[x, y] = c + (255,)
    return img


def fem(adj):
    """Żeńska forma przymiotnika koloru (lampa): biały -> biała, jasnoniebieski -> jasnoniebieska."""
    if adj.endswith("ki"):
        return adj[:-1] + "a"
    if adj.endswith("y"):
        return adj[:-1] + "a"
    return adj


# Opisy w oknie bramki (EN, PL). "Przód" to strona ze strzałką.
GATE_DESC = {
    "not": ("Front, left and right are on when the back input is off. Side outputs can be switched off.",
            "Przód, lewo i prawo świecą, gdy tylne wejście jest wyłączone. Boczne wyjścia można wyłączyć."),
    "buffer": ("Repeats the back input to the front, left and right (isolates and refreshes a signal). Side outputs can be switched off.",
               "Przenosi sygnał z tyłu na przód, lewo i prawo (oddziela i odświeża sygnał). Boczne wyjścia można wyłączyć."),
    "and": ("Output is on when all enabled inputs (left, back, right) are on. Disabled inputs are ignored; the back one is disabled by default.",
            "Wyjście świeci, gdy wszystkie włączone wejścia (lewe, tylne, prawe) są zasilone. Wyłączone są pomijane; tylne jest domyślnie wyłączone."),
    "nand": ("Output is off only when all enabled inputs (left, back, right) are on. The back input is disabled by default.",
             "Wyjście gaśnie tylko wtedy, gdy wszystkie włączone wejścia (lewe, tylne, prawe) są zasilone. Tylne jest domyślnie wyłączone."),
    "or": ("Output is on when any enabled input (left, back, right) is on.",
           "Wyjście świeci, gdy którekolwiek włączone wejście (lewe, tylne, prawe) jest zasilone."),
    "nor": ("Output is on when no enabled input (left, back, right) is on.",
            "Wyjście świeci, gdy żadne włączone wejście (lewe, tylne, prawe) nie jest zasilone."),
    "xor": ("Output is on when exactly one of the left and right inputs is on.",
            "Wyjście świeci, gdy zasilone jest dokładnie jedno z wejść: lewe albo prawe."),
    "xnor": ("Output is on when the left and right inputs are equal (both on or both off).",
             "Wyjście świeci, gdy lewe i prawe wejście są w tym samym stanie (oba wł. albo oba wył.)."),
    "rs_latch": ("Remembers one bit. The set input turns the front on, the reset input turns the back on - one of them is always lit.",
                 "Pamięta jeden bit. Wejście ustawiające zapala przód, kasujące - tył; zawsze świeci jedno z nich."),
    "toggle": ("A pulse on the left or right input switches the output between front and back. Right-click toggles by hand, Shift + right-click opens this window.",
               "Impuls na lewym albo prawym wejściu przełącza wyjście między przodem a tyłem. PPM przełącza ręcznie, Shift + PPM otwiera to okno."),
    "pulse": ("A rising edge at the back gives a short pulse at the front.",
              "Zbocze narastające z tyłu daje krótki impuls z przodu."),
    "timer": ("Pulses the front, left and right sides every set time. A signal at the back stops it.",
              "Co ustawiony czas daje impuls z przodu, z lewej i z prawej. Sygnał z tyłu go zatrzymuje."),
    "randomizer": ("While the back is powered, the front, left and right outputs each turn on or off at random.",
                   "Gdy tył jest zasilony, przód, lewo i prawo niezależnie od siebie losowo się zapalają i gasną."),
    "counter": ("Left counts up, right counts down. Front is on at the maximum, back is on at zero.",
                "Lewe liczy w górę, prawe w dół. Przód świeci przy maksimum, tył przy zerze."),
    "sequencer": ("Lights each side in turn: front, right, back, left. A signal at the back pauses it.",
                  "Zapala kolejno strony: przód, prawo, tył, lewo. Sygnał z tyłu go wstrzymuje."),
    "state_cell": ("A pulse at the back starts the countdown; the side output is on while counting, then the front pulses.",
                   "Impuls z tyłu uruchamia odliczanie; boczne wyjście świeci w trakcie, potem przód daje impuls."),
    "synchronizer": ("Waits for a pulse on both left and right, then pulses the front. Back resets.",
                     "Czeka na impuls z lewej i z prawej, potem daje impuls z przodu. Tył kasuje oczekiwanie."),
}


def gen_gate_messages():
    texts = {
        "en_us": {"message.redcircuit.counter_max": "Counter maximum: %s",
                  "screen.redcircuit.gate.time": "Time (s):", "screen.redcircuit.gate.ticks": "= %s ticks",
                  "screen.redcircuit.gate.max": "Maximum:", "screen.redcircuit.gate.reset": "Reset count",
                  "screen.redcircuit.gate.count": "Count: %s / %s",
                  "screen.redcircuit.gate.sound_on": "Sound: on", "screen.redcircuit.gate.sound_off": "Sound: muted",
                  "screen.redcircuit.gate.side_right": "Running output: right side", "screen.redcircuit.gate.side_left": "Running output: left side",
                  "subtitles.redcircuit.gate_tick": "Gate ticks",
                  "screen.redcircuit.gate.inputs": "Inputs:", "screen.redcircuit.gate.outputs": "Outputs:",
                  "screen.redcircuit.gate.side.left": "Left", "screen.redcircuit.gate.side.back": "Back",
                  "screen.redcircuit.gate.side.right": "Right",
                  "screen.redcircuit.gate.input_on": "%s: on", "screen.redcircuit.gate.input_off": "%s: off",
                  "screen.redcircuit.gate.rs_left": "Left sets, right resets", "screen.redcircuit.gate.rs_right": "Right sets, left resets",
                  "screen.redcircuit.gate.rs_quiet_off": "Inputs light up with outputs", "screen.redcircuit.gate.rs_quiet_on": "Outputs: front and back only",
                  **{f"screen.redcircuit.gate.desc.{g}": t[0] for g, t in GATE_DESC.items()}},
        "pl_pl": {"message.redcircuit.counter_max": "Maksimum licznika: %s",
                  "screen.redcircuit.gate.time": "Czas (s):", "screen.redcircuit.gate.ticks": "= %s ticków",
                  "screen.redcircuit.gate.max": "Maksimum:", "screen.redcircuit.gate.reset": "Wyzeruj licznik",
                  "screen.redcircuit.gate.count": "Stan: %s / %s",
                  "screen.redcircuit.gate.sound_on": "Dźwięk: wł.", "screen.redcircuit.gate.sound_off": "Dźwięk: wyciszony",
                  "screen.redcircuit.gate.side_right": "Wyjście w trakcie: prawa strona", "screen.redcircuit.gate.side_left": "Wyjście w trakcie: lewa strona",
                  "subtitles.redcircuit.gate_tick": "Bramka tyka",
                  "screen.redcircuit.gate.inputs": "Wejścia:", "screen.redcircuit.gate.outputs": "Wyjścia:",
                  "screen.redcircuit.gate.side.left": "Lewe", "screen.redcircuit.gate.side.back": "Tylne",
                  "screen.redcircuit.gate.side.right": "Prawe",
                  "screen.redcircuit.gate.input_on": "%s: wł.", "screen.redcircuit.gate.input_off": "%s: wył.",
                  "screen.redcircuit.gate.rs_left": "Lewe ustawia, prawe kasuje", "screen.redcircuit.gate.rs_right": "Prawe ustawia, lewe kasuje",
                  "screen.redcircuit.gate.rs_quiet_off": "Wejścia świecą razem z wyjściami", "screen.redcircuit.gate.rs_quiet_on": "Wyjścia: tylko przód i tył",
                  **{f"screen.redcircuit.gate.desc.{g}": t[1] for g, t in GATE_DESC.items()}}}
    # Dźwięk tykania bramek - korzysta z pliku z gry (random/click), można go podmienić paczką zasobów
    write_json(os.path.join(ASSETS, "sounds.json"), {
        "gate_tick": {"subtitle": "subtitles.redcircuit.gate_tick",
                      "sounds": [{"name": "minecraft:random/click", "volume": 0.6, "pitch": 1.6}]}})
    for lang, data_ in texts.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def tex_restriction_tube():
    """Rura restrykcyjna: stalowa rama z ciemnymi opaskami (zwężenia), szklany środek."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    frame = (150, 152, 162)
    for y in range(16):
        for x in range(16):
            edge = x < 2 or x > 13 or y < 2 or y > 13
            if edge:
                c = shade(frame, 1.0 + rng.uniform(-0.08, 0.08))
                if x in (5, 6, 9, 10) or y in (5, 6, 9, 10):
                    c = shade(frame, 0.55)   # opaski
                px[x, y] = c + (255,)
            elif (x + y) % 7 == 0:
                px[x, y] = (210, 235, 240, 255)
    return img


def tex_accelerator(core=False):
    """Akcelerator: azurytowa obudowa z jasnymi pierścieniami (ramiona) / cewką (środek)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    blue = (52, 92, 210)
    for y in range(16):
        for x in range(16):
            edge = x < 2 or x > 13 or y < 2 or y > 13
            if core:
                c = shade(blue, 0.8 + 0.25 * ((x + y) % 2))
                if y in (3, 7, 11) or x in (3, 7, 11):
                    c = (130, 190, 255)
                if edge:
                    c = shade(blue, 0.55)
                px[x, y] = c + (255,)
            elif edge:
                c = shade(blue, 1.0 + rng.uniform(-0.08, 0.08))
                if x in (4, 11) or y in (4, 11):
                    c = (130, 190, 255)
                px[x, y] = c + (255,)
            elif (x + y) % 7 == 0:
                px[x, y] = (210, 235, 240, 255)
    return img


def tube_variant(name, texture, center_texture=None, center_half=3.0, paintable_centers=True):
    """Blockstate/modele rury-odmiany (restrykcyjna, akcelerator) - jak zwykła rura."""
    cable_models(name, f"{MODID}:block/{texture}", 3.0)
    if center_texture is not None or center_half != 3.0:
        a, b = 8 - center_half, 8 + center_half
        ct = f"{MODID}:block/{center_texture or texture}"
        write_json(os.path.join(ASSETS, "models", "block", f"{name}_center.json"), {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"t": ct, "particle": ct},
            "elements": [{"from": [a, a, a], "to": [b, b, b], "faces": faces("#t")}]})
        inv = json.load(open(os.path.join(ASSETS, "models", "block", f"{name}_inventory.json")))
        inv["textures"]["c"] = ct
        inv["elements"][0] = {"from": [a, a, a], "to": [b, b, b], "faces": faces("#c")}
        write_json(os.path.join(ASSETS, "models", "block", f"{name}_inventory.json"), inv)
    parts = [{"when": {"paint": "16"}, "apply": {"model": f"{MODID}:block/{name}_center"}}]
    for i, dye in enumerate(DYES):
        if paintable_centers:
            write_json(os.path.join(ASSETS, "models", "block", f"{name}_center_{dye}.json"),
                       {"parent": f"{MODID}:block/{name}_center", "render_type": "minecraft:cutout",
                        "textures": {"t": f"{MODID}:block/tube_{dye}", "particle": f"{MODID}:block/tube_{dye}"}})
            parts.append({"when": {"paint": str(i)}, "apply": {"model": f"{MODID}:block/{name}_center_{dye}"}})
        else:
            parts.append({"when": {"paint": str(i)}, "apply": {"model": f"{MODID}:block/{name}_center"}})
    for d in DIRS:
        parts.append({"when": {d: "true"}, "apply": {"model": f"{MODID}:block/{name}_{d}"}})
    write_json(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"multipart": parts})
    item_model(name, f"{MODID}:block/{name}_inventory")
    write_json(os.path.join(DATA, "loot_table", "blocks", f"{name}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/{name}"})


def tex_wireless_top(label, color):
    s = 32
    img = noisy(s, (110, 112, 118), 0.05)
    d = ImageDraw.Draw(img)
    border(img, (64, 66, 72), 1)
    d.rectangle([12, 12, 19, 19], fill=(40, 40, 44, 255))          # podstawa anteny
    try:
        font = ImageFont.load_default(size=9)
    except TypeError:
        font = ImageFont.load_default()
    bbox = d.textbbox((0, 0), label, font=font)
    w = bbox[2] - bbox[0]
    d.rectangle([3, 23, 28, 30], fill=(36, 36, 40, 255))
    d.text(((s - w) / 2 - bbox[0], 22 - bbox[1]), label, fill=color + (255,), font=font)
    # fale radiowe
    for r, a in ((6, 200), (9, 140)):
        d.arc([16 - r - 8, 3, 16 + r - 8, 3 + 2 * r], 200, 340, fill=color[:3] + (a,))
        d.arc([16 - r + 8, 3, 16 + r + 8, 3 + 2 * r], 200, 340, fill=color[:3] + (a,))
    return img


def tex_remote():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    body = (60, 62, 70)
    d.rectangle([4, 5, 11, 15], fill=body + (255,), outline=shade(body, 0.6) + (255,))
    d.line([(5, 6), (5, 14)], fill=shade(body, 1.4) + (255,))
    d.ellipse([6, 7, 9, 10], fill=(200, 40, 30, 255), outline=(120, 20, 15, 255))   # przycisk
    d.rectangle([6, 12, 9, 13], fill=(90, 160, 255, 255))                            # wyświetlacz
    d.line([(10, 5), (12, 0)], fill=(170, 172, 180, 255))                            # antena
    img.load()[12, 0] = (230, 60, 50, 255)
    return img


def tex_fluid_tank(top=False):
    """Zbiornik: stalowa rama z nitami, w środku przezroczyste szkło (ciecz rysuje się w środku)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    frame = (120, 124, 134)
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                px[x, y] = shade(frame, 0.6) + (255,)
            elif edge == 1:
                px[x, y] = shade(frame, 1.0 + rng.uniform(-0.06, 0.06)) + (255,)
            elif top and edge == 2:
                px[x, y] = shade(frame, 0.8) + (255,)
            elif (x - y) in (3, 4) or (x - y) == -6:
                px[x, y] = (220, 235, 245, 255)       # odblask szkła
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = shade(frame, 1.4) + (255,)          # nity
    return img


def tex_drill():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    body, blue = (70, 74, 84), (52, 104, 230)
    d.polygon([(9, 2), (13, 6), (10, 9), (6, 5)], fill=(190, 194, 204, 255), outline=(110, 112, 120, 255))  # wiertło
    d.line([(10, 3), (12, 5)], fill=(230, 232, 240, 255))
    d.line([(13, 1), (15, 0)], fill=(150, 152, 160, 255))
    d.polygon([(4, 5), (9, 10), (6, 13), (1, 8)], fill=body + (255,), outline=shade(body, 0.6) + (255,))   # korpus
    d.line([(3, 7), (7, 11)], fill=blue + (255,))
    d.rectangle([4, 11, 6, 15], fill=(40, 40, 46, 255))                                                   # uchwyt
    d.point((5, 12), fill=(200, 40, 30, 255))                                                              # spust
    return img


def tex_chainsaw():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    body, blue = (70, 74, 84), (52, 104, 230)
    d.polygon([(6, 6), (14, 0), (15, 2), (8, 8)], fill=(170, 174, 184, 255), outline=(100, 102, 110, 255))  # prowadnica
    for i in range(0, 8, 2):
        d.point((7 + i, 5 - i * 0.7), fill=(60, 60, 64, 255))                                               # łańcuch
    d.rectangle([2, 7, 8, 12], fill=body + (255,), outline=shade(body, 0.6) + (255,))
    d.line([(3, 8), (7, 8)], fill=blue + (255,))
    d.rectangle([1, 12, 4, 15], fill=(40, 40, 46, 255))
    d.line([(2, 6), (5, 6)], fill=(40, 40, 46, 255))
    return img


def tex_multimeter():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 1, 12, 14], fill=(232, 180, 40, 255), outline=(150, 110, 20, 255))
    d.rectangle([5, 3, 10, 6], fill=(150, 200, 150, 255), outline=(40, 60, 40, 255))
    d.line([(6, 5), (8, 4)], fill=(30, 50, 30, 255))
    d.ellipse([6, 8, 9, 11], fill=(50, 50, 56, 255))
    d.point((8, 9), fill=(220, 220, 230, 255))
    d.line([(4, 15), (2, 15)], fill=(200, 40, 30, 255))
    d.line([(11, 15), (13, 15)], fill=(30, 30, 34, 255))
    return img


def gen_electric():
    M = MODID + ":"
    # Ładowarka
    b, a = (88, 94, 110), (60, 110, 255)
    top = tex_machine(b, a, None)
    d = ImageDraw.Draw(top)
    for i in range(4):
        d.rectangle([2 + i * 3, 6, 3 + i * 3, 10], fill=(30, 30, 34, 255))
    side = tex_machine(b, a, None)
    d = ImageDraw.Draw(side)
    d.polygon([(9, 2), (5, 8), (8, 8), (6, 13), (11, 6), (8, 6)], fill=a + (255,))
    side_on = tex_machine(b, a, None)
    d = ImageDraw.Draw(side_on)
    d.polygon([(9, 2), (5, 8), (8, 8), (6, 13), (11, 6), (8, 6)], fill=(150, 210, 255, 255))
    save_png(top, "block", "charger_top")
    save_png(side, "block", "charger_side")
    save_png(side_on, "block", "charger_side_on")
    for lit, tex in (("", "charger_side"), ("_on", "charger_side_on")):
        write_json(os.path.join(ASSETS, "models", "block", f"charger{lit}.json"), {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": f"{MODID}:block/charger_top", "bottom": f"{MODID}:block/charger_top", "side": f"{MODID}:block/{tex}"}})
    write_json(os.path.join(ASSETS, "blockstates", "charger.json"), {"variants": {
        "lit=false": {"model": f"{MODID}:block/charger"}, "lit=true": {"model": f"{MODID}:block/charger_on"}}})
    item_model("charger", f"{MODID}:block/charger")
    simple_loot("charger")
    shaped("charger", ["IAI", "ABA", "IWI"],
           {"I": "minecraft:iron_ingot", "A": M + "power_alloy_ingot", "B": M + "battery", "W": M + "power_alloy_wire"}, M + "charger")

    # Narzędzia
    for name, tex in (("electric_drill", tex_drill()), ("electric_chainsaw", tex_chainsaw()), ("multimeter", tex_multimeter())):
        save_png(tex, "item", name)
        write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                   {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/{name}"}})
    shaped("electric_drill", ["PPP", "IMI", "IBI"],
           {"P": "minecraft:diamond_pickaxe", "I": "minecraft:iron_ingot", "M": M + "power_circuit", "B": M + "battery"}, M + "electric_drill")
    shaped("electric_chainsaw", ["AAA", "IMI", "IBI"],
           {"A": "minecraft:diamond_axe", "I": "minecraft:iron_ingot", "M": M + "power_circuit", "B": M + "battery"}, M + "electric_chainsaw")
    shapeless("multimeter", ["minecraft:comparator", M + "signal_alloy_wire", M + "power_alloy_wire", "#c:glass_panes", "minecraft:iron_ingot"],
              M + "multimeter")
    write_json(os.path.join(DATA, "tags", "block", "mineable", "drill.json"),
               {"values": ["#minecraft:mineable/pickaxe", "#minecraft:mineable/shovel"]})
    write_json(os.path.join(DATA, "tags", "block", "mineable", "chainsaw.json"),
               {"values": ["#minecraft:mineable/axe", "#minecraft:leaves", "#minecraft:wool", "minecraft:cobweb"]})
    for tag in ("enchantable/mining", "enchantable/mining_loot"):
        write_json(os.path.join(MC_DATA, "tags", "item", f"{tag}.json"),
                   {"replace": False, "values": [M + "electric_drill", M + "electric_chainsaw"]})

    texts = {
        "en_us": {"block.redcircuit.charger": "Charger", "item.redcircuit.electric_drill": "Electric Drill",
                  "item.redcircuit.electric_chainsaw": "Electric Chainsaw", "item.redcircuit.multimeter": "Multimeter",
                  "tooltip.redcircuit.electric_tool": "%s FE per block; charge it in a Charger",
                  "meter.redcircuit.wire_power": "Wire signal: %s", "meter.redcircuit.signal_in": "Signal in: %s",
                  "meter.redcircuit.signal_out": "Signal out: %s", "meter.redcircuit.channels": "Channels:",
                  "meter.redcircuit.channels_none": "Channels: none active", "meter.redcircuit.energy": "Energy: %s / %s FE",
                  "meter.redcircuit.fluid": "Fluid: %s %s / %s mB",
                  "message.redcircuit.drill_1x1": "Drill mode: 1x1", "message.redcircuit.drill_3x3": "Drill mode: 3x3",
                  "tooltip.redcircuit.drill_mode_1x1": "Mode: 1x1", "tooltip.redcircuit.drill_mode_3x3": "Mode: 3x3",
                  "tooltip.redcircuit.drill_toggle": "Shift + right-click: change mode (hold Shift while mining for a single block)"},
        "pl_pl": {"block.redcircuit.charger": "Ładowarka", "item.redcircuit.electric_drill": "Wiertarka elektryczna",
                  "item.redcircuit.electric_chainsaw": "Piła łańcuchowa", "item.redcircuit.multimeter": "Miernik",
                  "tooltip.redcircuit.electric_tool": "%s FE za blok; ładuj w ładowarce",
                  "meter.redcircuit.wire_power": "Sygnał przewodu: %s", "meter.redcircuit.signal_in": "Sygnał wejściowy: %s",
                  "meter.redcircuit.signal_out": "Sygnał wyjściowy: %s", "meter.redcircuit.channels": "Kanały:",
                  "meter.redcircuit.channels_none": "Kanały: brak aktywnych", "meter.redcircuit.energy": "Energia: %s / %s FE",
                  "meter.redcircuit.fluid": "Ciecz: %s %s / %s mB",
                  "message.redcircuit.drill_1x1": "Tryb wiertarki: 1x1", "message.redcircuit.drill_3x3": "Tryb wiertarki: 3x3",
                  "tooltip.redcircuit.drill_mode_1x1": "Tryb: 1x1", "tooltip.redcircuit.drill_mode_3x3": "Tryb: 3x3",
                  "tooltip.redcircuit.drill_toggle": "Shift + PPM: zmiana trybu (Shift podczas kopania = jeden blok)"}}
    for lang, data_ in texts.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def gen_fluid_tank():
    M = MODID + ":"
    save_png(tex_fluid_tank(False), "block", "fluid_tank_side")
    save_png(tex_fluid_tank(True), "block", "fluid_tank_top")
    write_json(os.path.join(ASSETS, "models", "block", "fluid_tank.json"), {
        "parent": "minecraft:block/cube_column", "render_type": "minecraft:cutout",
        "textures": {"side": f"{MODID}:block/fluid_tank_side", "end": f"{MODID}:block/fluid_tank_top"}})
    write_json(os.path.join(ASSETS, "blockstates", "fluid_tank.json"), {"variants": {"": {"model": f"{MODID}:block/fluid_tank"}}})
    item_model("fluid_tank", f"{MODID}:block/fluid_tank")
    write_json(os.path.join(DATA, "loot_table", "blocks", "fluid_tank.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": M + "fluid_tank",
                                "functions": [{"function": "minecraft:copy_components", "source": "block_entity",
                                               "include": [M + "fluid"]}]}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/fluid_tank"})
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "fluid_tank" not in data["values"]:
        data["values"].append(M + "fluid_tank")
    write_json(path, data)
    shaped("fluid_tank", ["IGI", "G G", "IGI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass"}, M + "fluid_tank")
    for lang, name in (("en_us", "Fluid Tank"), ("pl_pl", "Zbiornik na ciecz")):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data["block.redcircuit.fluid_tank"] = name
        write_json(path, data)


def gen_wireless():
    M = MODID + ":"
    save_png(tex_wireless_top("TX", (255, 90, 70)), "block", "wireless_transmitter_top")
    save_png(tex_wireless_top("RX", (90, 170, 255)), "block", "wireless_receiver_top")
    save_png(noisy(16, (90, 92, 98), 0.05), "block", "wireless_side")
    save_png(noisy(16, (170, 172, 180), 0.04), "block", "wireless_rod")
    save_png(noisy(16, (90, 20, 16), 0.05), "block", "wireless_tip_off")
    save_png(noisy(16, (255, 70, 50), 0.08), "block", "wireless_tip_on")
    side_faces = {f: {"uv": [0, 14, 16, 16], "texture": "#side"} for f in ("north", "south", "east", "west")}
    for block in ("wireless_transmitter", "wireless_receiver"):
        for lit in ("off", "on"):
            tip = {"from": [7, 11, 7], "to": [9, 13, 9], "faces": faces("#tip")}
            if lit == "on":
                tip["shade"] = False
                tip["neoforge_data"] = {"block_light": 15, "sky_light": 15}
            write_json(os.path.join(ASSETS, "models", "block", f"{block}_{lit}.json"), {
                "parent": "minecraft:block/block",
                "textures": {"top": f"{MODID}:block/{block}_top", "side": f"{MODID}:block/wireless_side",
                             "rod": f"{MODID}:block/wireless_rod", "tip": f"{MODID}:block/wireless_tip_{lit}",
                             "particle": f"{MODID}:block/{block}_top"},
                "elements": [
                    {"from": [0, 0, 0], "to": [16, 2, 16],
                     "faces": dict({"up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                                    "down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"}}, **side_faces)},
                    {"from": [7.5, 2, 7.5], "to": [8.5, 11, 8.5], "faces": faces("#rod")},
                    tip]})
        write_json(os.path.join(ASSETS, "blockstates", f"{block}.json"), {"variants": {
            "powered=false": {"model": f"{MODID}:block/{block}_off"},
            "powered=true": {"model": f"{MODID}:block/{block}_on"}}})
        item_model(block, f"{MODID}:block/{block}_off")
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{block}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": M + block}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{block}"})
    save_png(tex_remote(), "item", "wireless_remote")
    generated_item("wireless_remote")

    P, W = M + "circuit_plate", M + "signal_alloy_wire"
    shapeless("wireless_transmitter", [P, W, "minecraft:ender_pearl", "minecraft:gold_ingot", "minecraft:redstone_torch"],
              M + "wireless_transmitter")
    shapeless("wireless_receiver", [P, W, "minecraft:ender_pearl", "minecraft:gold_ingot", "minecraft:comparator"],
              M + "wireless_receiver")
    shapeless("wireless_remote", [M + "wireless_transmitter", "minecraft:stone_button", "minecraft:iron_ingot"],
              M + "wireless_remote")

    texts = {
        "en_us": {"block.redcircuit.wireless_transmitter": "Wireless Transmitter",
                  "block.redcircuit.wireless_receiver": "Wireless Receiver",
                  "item.redcircuit.wireless_remote": "Wireless Remote",
                  "screen.redcircuit.frequency": "Frequency:",
                  "tooltip.redcircuit.frequency": "Frequency: %s",
                  "tooltip.redcircuit.remote_hint": "Hold right-click: transmit, Shift + right-click: settings",
                  "jade.redcircuit.wireless_on": "Active", "jade.redcircuit.wireless_off": "Inactive",
                  "config.jade.plugin_redcircuit.wireless": "RedCircuit: wireless redstone"},
        "pl_pl": {"block.redcircuit.wireless_transmitter": "Nadajnik bezprzewodowy",
                  "block.redcircuit.wireless_receiver": "Odbiornik bezprzewodowy",
                  "item.redcircuit.wireless_remote": "Pilot bezprzewodowy",
                  "screen.redcircuit.frequency": "Częstotliwość:",
                  "tooltip.redcircuit.frequency": "Częstotliwość: %s",
                  "tooltip.redcircuit.remote_hint": "Trzymaj PPM: nadawanie, Shift + PPM: ustawienia",
                  "jade.redcircuit.wireless_on": "Aktywny", "jade.redcircuit.wireless_off": "Nieaktywny",
                  "config.jade.plugin_redcircuit.wireless": "RedCircuit: redstone bezprzewodowy"}}
    for lang, data_ in texts.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def gen_tube_variants():
    M = MODID + ":"
    save_png(tex_restriction_tube(), "block", "restriction_tube")
    tube_variant("restriction_tube", "restriction_tube")
    save_png(tex_accelerator(False), "block", "accelerator")
    save_png(tex_accelerator(True), "block", "accelerator_core")
    tube_variant("accelerator", "accelerator", "accelerator_core", 4.5, paintable_centers=False)

    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    for b in ("restriction_tube", "accelerator"):
        if M + b not in data["values"]:
            data["values"].append(M + b)
    write_json(path, data)

    shapeless("restriction_tube", [M + "tube", "minecraft:iron_ingot"], M + "restriction_tube")
    shapeless("tube_from_restriction", [M + "restriction_tube"], M + "tube")
    shaped("accelerator", ["ITI", "WMW", "ITI"],
           {"I": "minecraft:iron_ingot", "T": M + "tube", "W": M + "power_alloy_wire", "M": M + "power_machine_casing"},
           M + "accelerator")
    names = {"en_us": {"block.redcircuit.restriction_tube": "Restriction Tube", "block.redcircuit.accelerator": "Accelerator"},
             "pl_pl": {"block.redcircuit.restriction_tube": "Rura restrykcyjna", "block.redcircuit.accelerator": "Akcelerator"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def gen_jade():
    texts = {
        "en_us": {
            "jade.redcircuit.signal": "Signal: %s", "jade.redcircuit.channels": "Channels:",
            "jade.redcircuit.channels_none": "Channels: none active",
            "jade.redcircuit.output_on": "Output: on", "jade.redcircuit.output_off": "Output: off",
            "jade.redcircuit.interval": "Time: %s s", "jade.redcircuit.count": "Count: %s / %s",
            "jade.redcircuit.muted": "Sound muted",
            "jade.redcircuit.gate_disabled": "Disabled inputs: %s", "jade.redcircuit.gate_disabled_out": "Disabled outputs: %s", "jade.redcircuit.lamp_on": "Lit", "jade.redcircuit.lamp_off": "Off",
            "jade.redcircuit.lamp_inverted": "Inverted", "jade.redcircuit.generating": "Generating: %s FE/t",
            "jade.redcircuit.tube_items": "Items in tube: %s", "jade.redcircuit.progress": "Progress: %s%%",
            "jade.redcircuit.burn": "Fuel: %s s",
            "config.jade.plugin_redcircuit.signal": "RedCircuit: wire signal",
            "config.jade.plugin_redcircuit.bundled": "RedCircuit: bundled channels",
            "config.jade.plugin_redcircuit.gate": "RedCircuit: gates",
            "config.jade.plugin_redcircuit.lamp": "RedCircuit: lamps",
            "config.jade.plugin_redcircuit.generator": "RedCircuit: generators",
            "config.jade.plugin_redcircuit.tube": "RedCircuit: tubes",
            "config.jade.plugin_redcircuit.furnace": "RedCircuit: furnace progress"},
        "pl_pl": {
            "jade.redcircuit.signal": "Sygnał: %s", "jade.redcircuit.channels": "Kanały:",
            "jade.redcircuit.channels_none": "Kanały: brak aktywnych",
            "jade.redcircuit.output_on": "Wyjście: włączone", "jade.redcircuit.output_off": "Wyjście: wyłączone",
            "jade.redcircuit.interval": "Czas: %s s", "jade.redcircuit.count": "Licznik: %s / %s",
            "jade.redcircuit.muted": "Dźwięk wyciszony",
            "jade.redcircuit.gate_disabled": "Wyłączone wejścia: %s", "jade.redcircuit.gate_disabled_out": "Wyłączone wyjścia: %s", "jade.redcircuit.lamp_on": "Świeci", "jade.redcircuit.lamp_off": "Zgaszona",
            "jade.redcircuit.lamp_inverted": "Odwrócona", "jade.redcircuit.generating": "Produkcja: %s FE/t",
            "jade.redcircuit.tube_items": "Przedmioty w rurze: %s", "jade.redcircuit.progress": "Postęp: %s%%",
            "jade.redcircuit.burn": "Paliwo: %s s",
            "config.jade.plugin_redcircuit.signal": "RedCircuit: sygnał przewodów",
            "config.jade.plugin_redcircuit.bundled": "RedCircuit: kanały kabla wiązkowego",
            "config.jade.plugin_redcircuit.gate": "RedCircuit: bramki",
            "config.jade.plugin_redcircuit.lamp": "RedCircuit: lampy",
            "config.jade.plugin_redcircuit.generator": "RedCircuit: generatory",
            "config.jade.plugin_redcircuit.tube": "RedCircuit: rury",
            "config.jade.plugin_redcircuit.furnace": "RedCircuit: postęp pieców"}}
    for lang, data_ in texts.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def gen_lamps():
    M = MODID + ":"
    en, pl = {}, {}
    for dye in DYES:
        col = DYE_RGB[dye]
        for lit in (False, True):
            name = f"lamp_{dye}_{'on' if lit else 'off'}"
            save_png(tex_lamp(col, lit), "block", name)
            element = {"from": [0, 0, 0], "to": [16, 16, 16],
                       "faces": {f: {"texture": "#all", "cullface": f} for f in ["north", "south", "east", "west", "up", "down"]}}
            if lit:
                element["shade"] = False
                element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
            write_json(os.path.join(ASSETS, "models", "block", f"{name}.json"), {
                "parent": "minecraft:block/block",
                "textures": {"all": f"{MODID}:block/{name}", "particle": f"{MODID}:block/{name}"},
                "elements": [element]})
        for inv in (False, True):
            block = f"{'inverted_' if inv else ''}lamp_{dye}"
            write_json(os.path.join(ASSETS, "blockstates", f"{block}.json"), {"variants": {
                "lit=false": {"model": f"{MODID}:block/lamp_{dye}_off"},
                "lit=true": {"model": f"{MODID}:block/lamp_{dye}_on"}}})
            item_model(block, f"{MODID}:block/lamp_{dye}_{'on' if inv else 'off'}")
            write_json(os.path.join(DATA, "loot_table", "blocks", f"{block}.json"), {
                "type": "minecraft:block",
                "pools": [{"rolls": 1, "bonus_rolls": 0,
                           "entries": [{"type": "minecraft:item", "name": M + block}],
                           "conditions": [{"condition": "minecraft:survives_explosion"}]}],
                "random_sequence": f"{MODID}:blocks/{block}"})
            name_en = dye.replace("_", " ").title()
            en[f"block.{MODID}.{block}"] = f"{'Inverted ' if inv else ''}{name_en} Lamp"
            pl[f"block.{MODID}.{block}"] = f"Lampa{' odwrócona' if inv else ''} ({fem(DYE_PL[dye])})"
        # Lampa: szyby po bokach, jasnopył u góry, barwnik w środku, redstone na dole
        shaped(f"lamp_{dye}", ["GSG", "GDG", "GRG"],
               {"G": "#c:glass_panes", "S": "minecraft:glowstone_dust", "D": f"minecraft:{dye}_dye", "R": "minecraft:redstone"},
               M + f"lamp_{dye}")
        shapeless(f"inverted_lamp_{dye}", [M + f"lamp_{dye}", "minecraft:redstone_torch"], M + f"inverted_lamp_{dye}")
        shapeless(f"lamp_{dye}_from_inverted", [M + f"inverted_lamp_{dye}", "minecraft:redstone_torch"], M + f"lamp_{dye}")
    for lang, data_ in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def gen_common_tags():
    """Dodatkowe tagi c: (stopy, cienkie druty) i tagi zbiorcze (c:ingots, c:gems...)."""
    M = MODID + ":"
    extra = {
        "ingots/signal_alloy": [M + "signal_alloy_ingot"], "ingots/power_alloy": [M + "power_alloy_ingot"],
        "wires/copper": [M + "fine_copper_wire"], "wires/iron": [M + "fine_iron_wire"],
    }
    for tag, values in extra.items():
        write_json(os.path.join(RES, "data", "c", "tags", "item", f"{tag}.json"), {"values": values})
    # Tagi zbiorcze: c:ingots, c:nuggets, c:gems, c:dusts, c:ores, c:raw_materials, c:storage_blocks, c:gears
    base = os.path.join(RES, "data", "c", "tags", "item")
    for group in sorted(os.listdir(base)):
        d = os.path.join(base, group)
        if not os.path.isdir(d):
            continue
        children = sorted(f[:-5] for f in os.listdir(d) if f.endswith(".json"))
        write_json(os.path.join(base, f"{group}.json"), {"values": [f"#c:{group}/{c}" for c in children]})


def shaped(name, pattern, key, result, count=1):
    write_json(os.path.join(DATA, "recipe", f"{name}.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "pattern": pattern,
        "key": {k: ing(v) for k, v in key.items()},
        "result": {"id": result, "count": count}})


def shapeless(name, ingredients, result, count=1):
    write_json(os.path.join(DATA, "recipe", f"{name}.json"), {
        "type": "minecraft:crafting_shapeless",
        "category": "redstone",
        "ingredients": [ing(i) for i in ingredients],
        "result": {"id": result, "count": count}})


def gen_recipes():
    M = MODID + ":"
    # Sztabki stopów powstają tylko w piecach do stopów (receptury alloying_* w gen_alloy).
    shaped("signal_alloy_wire", ["III"], {"I": M + "signal_alloy_ingot"}, M + "signal_alloy_wire", 12)
    for d in DYES:
        shapeless(f"insulated_wire_{d}", [f"minecraft:{d}_wool"] + [M + "signal_alloy_wire"] * 4,
                  M + f"insulated_wire_{d}", 4)
    shaped("bundled_cable", ["SIS", "III", "SIS"],
           {"S": "minecraft:string", "I": "#" + M + "insulated_wires"}, M + "bundled_cable", 2)
    shapeless("circuit_plate", ["minecraft:smooth_stone_slab", "minecraft:redstone"], M + "circuit_plate", 2)

    P, T, W, R = M + "circuit_plate", "minecraft:redstone_torch", M + "signal_alloy_wire", "minecraft:redstone"
    gate_recipes = {
        "not": [P, T],
        "buffer": [P, T, T],
        "and": [P, T, T, T],
        "nand": [P, T, T, T, R],
        "or": [P, W, W],
        "nor": [P, W, W, T],
        "xor": [P, W, W, T, T],
        "xnor": [P, W, W, T, T, T],
        "rs_latch": [P, "minecraft:lever", T, T],
        "toggle": [P, "minecraft:lever", W],
        "pulse": [P, "minecraft:repeater", T],
        "timer": [P, "minecraft:clock"],
        "randomizer": [P, "minecraft:quartz"],
        "counter": [P, "minecraft:comparator", T, W],
        "sequencer": [P, "minecraft:clock", W, W],
        "state_cell": [P, "minecraft:clock", "minecraft:lever"],
        "synchronizer": [P, "minecraft:repeater", W, W],
    }
    for g, ings in gate_recipes.items():
        shapeless(f"{g}_gate", ings, M + f"{g}_gate")

    shaped("tube", ["CGC"], {"C": "minecraft:copper_ingot", "G": "minecraft:glass"}, M + "tube", 8)
    shaped("extractor", ["PPP", "CTC", "CWC"],
           {"P": "#minecraft:planks", "C": "minecraft:cobblestone", "T": "minecraft:piston", "W": M + "signal_machine_casing"},
           M + "extractor")
    shapeless("filter", [M + "extractor", "minecraft:hopper", M + "signal_alloy_ingot"], M + "filter")
    shapeless("sorting_machine", [M + "filter", "minecraft:chest", "minecraft:comparator",
                                  M + "signal_alloy_ingot", M + "signal_alloy_ingot"], M + "sorting_machine")

    B = M + "power_alloy_ingot"
    shaped("power_alloy_wire", ["III"], {"I": B}, M + "power_alloy_wire", 12)
    shaped("solar_panel", ["GGG", "LBL", "SSS"],
           {"G": "minecraft:glass", "L": "minecraft:lapis_lazuli", "B": B, "S": "minecraft:smooth_stone_slab"},
           M + "solar_panel")
    shaped("battery_box", ["IBI", "BRB", "IBI"],
           {"I": "minecraft:iron_ingot", "B": B, "R": "minecraft:redstone_block"}, M + "battery_box")
    shaped("electric_furnace", ["BBB", "BFB", "IMI"],
           {"B": B, "F": "minecraft:furnace", "I": "minecraft:iron_ingot", "M": M + "power_machine_casing"},
           M + "electric_furnace")


def gen_lang():
    en = {"itemGroup.redcircuit": "RedCircuit: Redstone",
          "itemGroup.redcircuit.logistics": "RedCircuit: Logistics & Machines",
          "itemGroup.redcircuit.energy": "RedCircuit: Energy & Fluids",
          "itemGroup.redcircuit.tools": "RedCircuit: Tools & Crafting",
          "itemGroup.redcircuit.materials": "RedCircuit: Materials & Blocks",
          "item.redcircuit.signal_alloy_ingot": "Signal Alloy Ingot",
          "item.redcircuit.power_alloy_ingot": "Power Alloy Ingot",
          "item.redcircuit.circuit_plate": "Circuit Plate",
          "block.redcircuit.signal_alloy_wire": "Signal Alloy Wire",
          "block.redcircuit.bundled_cable": "Bundled Cable",
          "block.redcircuit.tube": "Pneumatic Tube",
          "block.redcircuit.extractor": "Extractor",
          "block.redcircuit.filter": "Filter",
          "block.redcircuit.sorting_machine": "Sorting Machine",
          "block.redcircuit.power_alloy_wire": "Power Alloy Wire",
          "block.redcircuit.solar_panel": "Solar Panel",
          "block.redcircuit.battery_box": "Battery Box",
          "block.redcircuit.electric_furnace": "Electric Furnace",
          "container.redcircuit.filter": "Filter",
          "container.redcircuit.sorting_machine": "Sorting Machine (row 1: dye, rows 2-3: items)",
          "message.redcircuit.timer_interval": "Timer interval: %s s",
          "message.redcircuit.energy": "Energy: %s / %s FE",
          "message.redcircuit.solar": "Generating: %s FE/t, buffer: %s FE"}
    pl = {"itemGroup.redcircuit": "RedCircuit: Redstone",
          "itemGroup.redcircuit.logistics": "RedCircuit: Logistyka i maszyny",
          "itemGroup.redcircuit.energy": "RedCircuit: Energia i płyny",
          "itemGroup.redcircuit.tools": "RedCircuit: Narzędzia i wytwarzanie",
          "itemGroup.redcircuit.materials": "RedCircuit: Materiały i bloki",
          "item.redcircuit.signal_alloy_ingot": "Sztabka stopu sygnałowego",
          "item.redcircuit.power_alloy_ingot": "Sztabka stopu energetycznego",
          "item.redcircuit.circuit_plate": "Płytka obwodu",
          "block.redcircuit.signal_alloy_wire": "Przewód ze stopu sygnałowego",
          "block.redcircuit.bundled_cable": "Kabel wiązkowy",
          "block.redcircuit.tube": "Rura pneumatyczna",
          "block.redcircuit.extractor": "Ekstraktor",
          "block.redcircuit.filter": "Filtr",
          "block.redcircuit.sorting_machine": "Maszyna sortująca",
          "block.redcircuit.power_alloy_wire": "Przewód ze stopu energetycznego",
          "block.redcircuit.solar_panel": "Panel słoneczny",
          "block.redcircuit.battery_box": "Akumulator",
          "block.redcircuit.electric_furnace": "Piec elektryczny",
          "container.redcircuit.filter": "Filtr",
          "container.redcircuit.sorting_machine": "Sortowanie (rząd 1: barwnik, rzędy 2-3: przedmioty)",
          "message.redcircuit.timer_interval": "Okres zegara: %s s",
          "message.redcircuit.energy": "Energia: %s / %s FE",
          "message.redcircuit.solar": "Produkcja: %s FE/t, bufor: %s FE"}
    for d in DYES:
        en[f"block.redcircuit.insulated_wire_{d}"] = d.replace("_", " ").title() + " Insulated Wire"
        pl[f"block.redcircuit.insulated_wire_{d}"] = f"Przewód izolowany ({DYE_PL[d]})"
    for g in GATES:
        en[f"block.redcircuit.{g}_gate"] = GATE_EN[g]
        pl[f"block.redcircuit.{g}_gate"] = GATE_PL[g]
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), en)
    write_json(os.path.join(ASSETS, "lang", "pl_pl.json"), pl)



# ---------------------------------------------------------------- rudy

# nazwa: (kolor, typ dropu, narzędzie, (rozmiar żyły, ilość na chunk, minY, maxY), EN, PL nazwa rudy, EN drop, PL drop)
ORES = {
    "tin": ((170, 182, 196), "metal", "stone", (9, 10, 0, 80), "Tin", "cyny", "Tin", "cyna"),
    "silver": ((236, 240, 248), "metal", "iron", (8, 4, -48, 32), "Silver", "srebra", "Silver", "srebro"),
    "azurite": ((40, 150, 235), "dust", "iron", (8, 6, -64, 16), "Azurite", "azurytu", "Azurite", "Azuryt"),
}
RAW_PL = {"tin": "Surowa cyna", "silver": "Surowe srebro"}
INGOT_PL = {"tin": "Sztabka cyny", "silver": "Sztabka srebra"}


def tex_ore_overlay(color, seed):
    """Przezroczysta nakładka z ziarnami minerału (kładziona na kamień/łupek z gry)."""
    r = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    clusters = [(2, 2), (10, 2), (5, 7), (12, 9), (2, 12), (8, 12)]
    shapes = [[(0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)],
              [(1, 0), (0, 1), (1, 1), (2, 1), (2, 2)],
              [(0, 0), (1, 0), (2, 0), (1, 1), (2, 1)],
              [(0, 0), (0, 1), (1, 1), (1, 2), (2, 2)]]
    for cx, cy in clusters:
        cx += r.randint(-1, 1)
        cy += r.randint(-1, 1)
        shape = r.choice(shapes)
        pts = [((cx + dx) % 16, (cy + dy) % 16) for dx, dy in shape]
        # Cień pod ziarnem
        for x, y in pts:
            sx, sy = x, (y + 1) % 16
            if (sx, sy) not in pts:
                px[sx, sy] = shade(color, 0.45) + (255,)
        for i, (x, y) in enumerate(pts):
            f = 1.4 if i == 0 else (1.05 if i < 3 else 0.82)
            px[x, y] = shade(color, f) + (255,)
    return img


def tex_raw(color):
    """Bryłka surowca: nieregularny kształt z obrysem, jak surowe żelazo/miedź."""
    rows = [
        "................",
        "................",
        "......hhh.......",
        "....hhhmmhh.....",
        "...hhmmmmmmh....",
        "..hmmmmhmmmms...",
        "..hmmmhhmmmms...",
        "..hmmmmmmmsss...",
        "...mmmmmmmmss...",
        "...smmmmhmmss...",
        "...ssmmmmmsss...",
        "....sssmmsss....",
        ".....ssssss.....",
        "................",
        "................",
        "................",
    ]
    inside = [[(None if c == "." else c) for c in r] for r in rows]
    colors = {"h": shade(color, 1.3), "m": color, "s": shade(color, 0.7)}
    return outline_shape(inside, colors, shade(color, 0.4))


def tex_gem(color):
    """Klejnot w stylu szmaragdu/diamentu."""
    rows = [
        "................",
        "......oooo......",
        ".....ohhhho.....",
        "....ohhwhhmo....",
        "...ohhhhhmmmo...",
        "..ohhhhmmmmmso..",
        "..ohhhmmmmmsso..",
        "..ommmmmmmssso..",
        "...ommmmmssso...",
        "....ommmssso....",
        ".....osssso.....",
        "......oooo......",
        "................",
        "................",
        "................",
        "................",
    ]
    inside = [[(None if c == "." else c) for c in r] for r in rows]
    colors = {"h": shade(color, 1.35), "m": color, "s": shade(color, 0.7), "w": (255, 255, 255), "o": shade(color, 0.45)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if inside[y][x]:
                px[x, y] = colors[inside[y][x]] + (255,)
    return img


def tex_dust(color):
    """Kupka pyłu w stylu pyłu redstone: nieregularny kopczyk z ziaren w kilku odcieniach."""
    rows = [
        "................",
        "................",
        "................",
        "........a.......",
        ".......aba..c...",
        "....c.abdba.....",
        "...a.abdddba.a..",
        "....abddcddba...",
        "..a.bdddddddba..",
        "...abdcddddcdba.",
        "..abddddddddddb.",
        ".abddcdddddcddba",
        "..bbddddddddbba.",
        "...abbbbbbbba...",
        "................",
        "................",
    ]
    colors = {"a": shade(color, 0.45), "b": shade(color, 0.7), "d": color, "c": shade(color, 1.5)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    r = random.Random(7)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            c = colors[ch]
            if ch == "d":
                # Ziarnistość: część pikseli jaśniejsza, część ciemniejsza, kilka dziurek
                v = r.random()
                if v < 0.08:
                    continue
                c = shade(color, 1.22 if v < 0.35 else (0.85 if v > 0.8 else 1.0))
            px[x, y] = c + (255,)
    return img


def silk_or(M, block, drop_entry):
    return {"type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {"type": "minecraft:item", "name": M + block,
                     "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                         "predicates": {"minecraft:enchantments": [
                             {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                    drop_entry]}]}],
            "random_sequence": f"{MODID}:blocks/{block}"}


def cook(name, ingredients, result, xp):
    M = MODID + ":"
    ing_obj = [ing(M + i) for i in ingredients]
    ingredient = ing_obj[0] if len(ing_obj) == 1 else ing_obj
    for kind, time in (("smelting", 200), ("blasting", 100)):
        write_json(os.path.join(DATA, "recipe", f"{name}_from_{kind}.json"), {
            "type": f"minecraft:{kind}", "category": "misc",
            "ingredient": ingredient, "result": {"id": M + result},
            "experience": xp, "cookingtime": time})


def gen_ores():
    M = MODID + ":"
    en, pl = {}, {}
    tool_tags = {"stone": [], "iron": [], "diamond": []}
    pickaxe, features = [], []
    c_tags = {}
    for ore, (color, kind, tool, (size, count, ymin, ymax), en_n, pl_gen, en_drop, pl_drop) in ORES.items():
        blocks = [f"{ore}_ore", f"deepslate_{ore}_ore"]
        overlay = f"{ore}_ore_overlay"
        save_png(tex_ore_overlay(color, ore), "block", overlay)
        for b, deep in zip(blocks, (False, True)):
            # Podstawa to oryginalny kamień/łupek głębinowy z gry, na nim nakładka z minerałem.
            base_tex = "minecraft:block/deepslate" if deep else "minecraft:block/stone"
            all_faces = lambda t: {f: {"texture": t, "cullface": f} for f in DIRS}
            write_json(os.path.join(ASSETS, "models", "block", f"{b}.json"), {
                "parent": "minecraft:block/block",
                "render_type": "minecraft:cutout_mipped",
                "textures": {"particle": base_tex, "base": base_tex, "overlay": f"{MODID}:block/{overlay}"},
                "elements": [
                    {"from": [0, 0, 0], "to": [16, 16, 16], "faces": all_faces("#base")},
                    {"from": [0, 0, 0], "to": [16, 16, 16], "faces": all_faces("#overlay")}]})
            write_json(os.path.join(ASSETS, "blockstates", f"{b}.json"),
                       {"variants": {"": {"model": f"{MODID}:block/{b}"}}})
            item_model(b, f"{MODID}:block/{b}")
        en[f"block.{MODID}.{ore}_ore"] = f"{en_n} Ore"
        en[f"block.{MODID}.deepslate_{ore}_ore"] = f"Deepslate {en_n} Ore"
        pl[f"block.{MODID}.{ore}_ore"] = f"Ruda {pl_gen}"
        pl[f"block.{MODID}.deepslate_{ore}_ore"] = f"Głębinowa ruda {pl_gen}"
        pickaxe += blocks
        tool_tags[tool] += blocks
        c_tags.setdefault(("block", f"c:ores/{ore}"), []).extend(blocks)
        c_tags.setdefault(("item", f"c:ores/{ore}"), []).extend(blocks)

        # Przedmioty i dropy
        if kind == "metal":
            raw, ingot = f"raw_{ore}", f"{ore}_ingot"
            vanilla_tinted_item(raw, "minecraft:item/raw_iron")
            vanilla_tinted_item(ingot, "minecraft:item/iron_ingot")
            en[f"item.{MODID}.{raw}"] = f"Raw {en_drop}"
            en[f"item.{MODID}.{ingot}"] = f"{en_drop} Ingot"
            pl[f"item.{MODID}.{raw}"] = RAW_PL[ore]
            pl[f"item.{MODID}.{ingot}"] = INGOT_PL[ore]
            drop = {"type": "minecraft:item", "name": M + raw, "functions": [
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}
            cook(ingot, [raw], ingot, 0.7)
            cook(f"{ingot}_from_ore", blocks, ingot, 0.7)
            c_tags.setdefault(("item", f"c:ingots/{ore}"), []).append(ingot)
            c_tags.setdefault(("item", f"c:raw_materials/{ore}"), []).append(raw)
            xp_note = None
        elif kind == "gem":
            gem = ore
            save_png(tex_gem(color), "item", gem)
            generated_item(gem)
            en[f"item.{MODID}.{gem}"] = en_drop
            pl[f"item.{MODID}.{gem}"] = pl_drop
            drop = {"type": "minecraft:item", "name": M + gem, "functions": [
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}
            cook(f"{gem}_from_ore", blocks, gem, 1.0)
            c_tags.setdefault(("item", f"c:gems/{ore}"), []).append(gem)
        else:  # pył (azuryt) - jak redstone
            dust = ore
            save_png(tex_dust(color), "item", dust)
            generated_item(dust)
            en[f"item.{MODID}.{dust}"] = en_drop
            pl[f"item.{MODID}.{dust}"] = pl_drop
            drop = {"type": "minecraft:item", "name": M + dust, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 4, "max": 5}, "add": False},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                 "formula": "minecraft:uniform_bonus_count", "parameters": {"bonusMultiplier": 1}},
                {"function": "minecraft:explosion_decay"}]}
            cook(f"{dust}_from_ore", blocks, dust, 0.7)
            c_tags.setdefault(("item", f"c:dusts/{ore}"), []).append(dust)
        for b in blocks:
            write_json(os.path.join(DATA, "loot_table", "blocks", f"{b}.json"), silk_or(M, b, drop))

        # Generowanie w świecie
        write_json(os.path.join(DATA, "worldgen", "configured_feature", f"{ore}_ore.json"), {
            "type": "minecraft:ore",
            "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                 "state": {"Name": M + blocks[0]}},
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                 "state": {"Name": M + blocks[1]}}]}})
        write_json(os.path.join(DATA, "worldgen", "placed_feature", f"{ore}_ore.json"), {
            "feature": M + f"{ore}_ore",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {
                    "type": "minecraft:uniform",
                    "min_inclusive": {"absolute": ymin}, "max_inclusive": {"absolute": ymax}}},
                {"type": "minecraft:biome"}]})
        features.append(M + f"{ore}_ore")

    # Każda ruda osobno - można ją wyłączyć w config/redcircuit-common.toml
    for ore in ORES:
        write_json(os.path.join(DATA, "neoforge", "biome_modifier", f"{ore}_ore.json"), {
            "type": M + "configured_ore",
            "ore": ore,
            "biomes": "#minecraft:is_overworld",
            "features": M + f"{ore}_ore",
            "step": "underground_ores"})

    # Tagi
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data["values"] += [M + b for b in pickaxe]
    write_json(path, data)
    for tool, blocks in tool_tags.items():
        write_json(os.path.join(MC_DATA, "tags", "block", f"needs_{tool}_tool.json"),
                   {"replace": False, "values": [M + b for b in blocks]})
    for (reg, tag), values in c_tags.items():
        ns, path_ = tag.split(":")
        write_json(os.path.join(RES, "data", ns, "tags", reg, path_ + ".json"),
                   {"replace": False, "values": [M + v for v in values]})

    for lang, names in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(names)
        write_json(path, data)


# ---------------------------------------------------------------- bryłki i bloki przechowywania

METALS = {"tin": ("Tin", "cyny", "Bryłka cyny", "Blok cyny"),
          "silver": ("Silver", "srebra", "Bryłka srebra", "Blok srebra")}
GEM_BLOCKS = {"azurite": ((40, 150, 235), "Azurite", "Blok azurytu")}


def tex_gem_block(color):
    """Blok klejnotów: rama i fazowane płytki, jak bloki diamentu/szmaragdu."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = shade(color, 0.55)
            elif x in (1, 14) or y in (1, 14):
                c = shade(color, 1.35)
            else:
                # Cztery fazowane płytki
                lx, ly = (x - 2) % 6, (y - 2) % 6
                if x in (7, 8) or y in (7, 8):
                    c = shade(color, 0.7)
                elif lx + ly < 2:
                    c = shade(color, 1.45)
                elif lx + ly > 7:
                    c = shade(color, 0.8)
                else:
                    c = shade(color, 1.0 + rng.uniform(-0.05, 0.05))
            px[x, y] = c + (255,)
    return img


def tex_dust_block(color):
    """Blok pyłu w stylu bloku redstone: jasna obwódka, przejście i duża,
    nieregularna ciemna plama pośrodku, całość nakrapiana."""
    r = random.Random(23)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            # Mieszanka odległości kołowej i rombowej daje "kleksowaty" kształt plamy
            dx, dy = x - 7.5, y - 7.5
            dist = 0.6 * (dx * dx + dy * dy) ** 0.5 + 0.4 * (abs(dx) + abs(dy)) * 0.75 + r.uniform(-1.3, 1.3)
            if dist < 4.4:
                f = 0.42 + r.uniform(-0.05, 0.08)
            elif dist < 6.1:
                f = 0.58 + r.uniform(-0.08, 0.1)
            elif dist < 7.2:
                f = 0.8 + r.uniform(-0.1, 0.1)
            else:
                f = 1.0 + r.uniform(-0.1, 0.06)
            v = r.random()
            if v < 0.05:
                f *= 0.7          # ciemne ziarenko
            elif v > 0.965:
                f = 1.15          # jasne ziarenko
            px[x, y] = shade(color, f) + (255,)
    return img


def tinted_block_model(name, vanilla_texture):
    """Model pełnego bloku z teksturą z gry i tintindex (kolor nadawany w kodzie)."""
    faces_ = {f: {"texture": "#all", "cullface": f, "tintindex": 0} for f in DIRS}
    write_json(os.path.join(ASSETS, "models", "block", f"{name}.json"), {
        "parent": "minecraft:block/block",
        "textures": {"all": vanilla_texture, "particle": vanilla_texture},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces_}]})


def gen_storage():
    M = MODID + ":"
    en, pl = {}, {}
    blocks_all = []

    def simple_block_files(b, model_written=False, tex=None):
        if not model_written:
            write_json(os.path.join(ASSETS, "models", "block", f"{b}.json"),
                       {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{tex or b}"}})
        write_json(os.path.join(ASSETS, "blockstates", f"{b}.json"),
                   {"variants": {"": {"model": f"{MODID}:block/{b}"}}})
        item_model(b, f"{MODID}:block/{b}")
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{b}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": M + b}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{b}"})

    def nine(small, big, name_up, name_down):
        shaped(name_up, ["XXX", "XXX", "XXX"], {"X": M + small}, M + big)
        shapeless(name_down, [M + big], M + small, 9)

    for metal, (en_n, pl_gen, pl_nugget, pl_block) in METALS.items():
        ingot, nugget, block = f"{metal}_ingot", f"{metal}_nugget", f"{metal}_block"
        vanilla_tinted_item(nugget, "minecraft:item/iron_nugget")
        tinted_block_model(block, "minecraft:block/iron_block")
        simple_block_files(block, model_written=True)
        nine(nugget, ingot, f"{ingot}_from_nuggets", f"{nugget}_from_ingot")
        nine(ingot, block, f"{block}_from_ingots", f"{ingot}_from_block")
        en[f"item.{MODID}.{nugget}"] = f"{en_n} Nugget"
        en[f"block.{MODID}.{block}"] = f"Block of {en_n}"
        pl[f"item.{MODID}.{nugget}"] = pl_nugget
        pl[f"block.{MODID}.{block}"] = pl_block
        blocks_all.append(block)
        write_json(os.path.join(RES, "data", "c", "tags", "item", "nuggets", f"{metal}.json"),
                   {"replace": False, "values": [M + nugget]})

    for gem, (color, en_n, pl_block) in GEM_BLOCKS.items():
        block = f"{gem}_block"
        save_png(tex_dust_block(color) if gem == "azurite" else tex_gem_block(color), "block", block)
        simple_block_files(block)
        nine(gem, block, f"{block}_from_{gem}", f"{gem}_from_block")
        en[f"block.{MODID}.{block}"] = f"Block of {en_n}"
        pl[f"block.{MODID}.{block}"] = pl_block
        blocks_all.append(block)

    for b in blocks_all:
        mat = b[:-len("_block")]
        for reg in ("block", "item"):
            write_json(os.path.join(RES, "data", "c", "tags", reg, "storage_blocks", f"{mat}.json"),
                       {"replace": False, "values": [M + b]})
    # Bloki metali i klejnotów mogą być podstawą beaconu
    write_json(os.path.join(MC_DATA, "tags", "block", "beacon_base_blocks.json"),
               {"replace": False, "values": [M + b for b in blocks_all if b != "azurite_block"]})
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data["values"] += [M + b for b in blocks_all]
    write_json(path, data)
    path = os.path.join(MC_DATA, "tags", "block", "needs_iron_tool.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data["values"] += [M + b for b in blocks_all if b != "tin_block"]
    write_json(path, data)

    for lang, names in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(names)
        write_json(path, data)


# ---------------------------------------------------------------- stół kreślarski i plany

def tex_project_top():
    """Nakładka na górę stołu (kładziona na deski z gry) w stylu stołu rzemieślniczego:
    ciemna rama i siatka 3x3 równych pól 4x4, w środkowym polu mały niebieski arkusz planu."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    d = ImageDraw.Draw(img)
    dark, deep = (70, 48, 26, 255), (52, 36, 20, 255)
    d.rectangle([0, 0, 15, 15], outline=dark)
    for t in (5, 10):
        d.line([(t, 1), (t, 14)], fill=dark)
        d.line([(1, t), (14, t)], fill=dark)
    for t in (0, 5, 10, 15):
        for k in (0, 5, 10, 15):
            px[t, k] = deep                       # skrzyżowania listew
    # Pola 4x4: jasna krawędź u góry i z lewej, cień na dole i z prawej
    for c0 in (1, 6, 11):
        for r0 in (1, 6, 11):
            for k in range(4):
                px[c0 + k, r0] = (255, 236, 190, 70)
                px[c0, r0 + k] = (255, 236, 190, 70)
                px[c0 + k, r0 + 3] = (40, 26, 12, 80)
                px[c0 + 3, r0 + k] = (40, 26, 12, 80)
    # Środkowe pole: arkusz planu
    d.rectangle([7, 7, 8, 8], fill=(44, 88, 168, 255))
    px[7, 7] = (205, 222, 245, 255)
    return img


def tex_project_side():
    wood = (150, 108, 64)
    img = noisy(16, wood, 0.07)
    d = ImageDraw.Draw(img)
    for y in (4, 9, 14):
        d.line([(0, y), (15, y)], fill=shade(wood, 0.75) + (255,))
    d.rectangle([0, 0, 15, 2], fill=shade(wood, 1.15) + (255,))
    # Szuflada z uchwytem
    d.rectangle([3, 6, 12, 12], outline=shade(wood, 0.55) + (255,))
    d.rectangle([7, 8, 8, 9], fill=(190, 190, 195, 255))
    return img


def tex_plan(written):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    paper = (52, 98, 178)
    d.polygon([(2, 3), (13, 2), (14, 13), (3, 14)], fill=paper + (255,), outline=(28, 56, 110, 255))
    # Linie siatki
    for t in (5, 8, 11):
        d.line([(t, 3), (t + 1, 13)], fill=(92, 140, 210, 255))
    if written:
        # Biały "rysunek techniczny"
        d.rectangle([5, 5, 10, 10], outline=(235, 240, 250, 255))
        d.line([(5, 5), (10, 10)], fill=(235, 240, 250, 255))
        d.point([(11, 6), (12, 6), (6, 11)], fill=(235, 240, 250, 255))
    return img


def gen_project():
    M = MODID + ":"
    save_png(tex_project_top(), "block", "blueprint_table_top")
    save_png(tex_project_side(), "block", "blueprint_table_side")
    # Góra: deski z gry + nakładka z planem; spód: deski z gry; boki: własna tekstura.
    planks = "minecraft:block/oak_planks"
    side_tex = f"{MODID}:block/blueprint_table_side"
    write_json(os.path.join(ASSETS, "models", "block", "blueprint_table.json"), {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {"particle": planks, "planks": planks, "side": side_tex,
                     "overlay": f"{MODID}:block/blueprint_table_top"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "up": {"texture": "#planks", "cullface": "up"},
                "down": {"texture": "#planks", "cullface": "down"},
                "north": {"texture": "#side", "cullface": "north"},
                "south": {"texture": "#side", "cullface": "south"},
                "west": {"texture": "#side", "cullface": "west"},
                "east": {"texture": "#side", "cullface": "east"}}},
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "up": {"texture": "#overlay", "cullface": "up"}}}]})
    write_json(os.path.join(ASSETS, "blockstates", "blueprint_table.json"),
               {"variants": {"": {"model": f"{MODID}:block/blueprint_table"}}})
    item_model("blueprint_table", f"{MODID}:block/blueprint_table")
    write_json(os.path.join(DATA, "loot_table", "blocks", "blueprint_table.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": M + "blueprint_table"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/blueprint_table"})
    write_json(os.path.join(MC_DATA, "tags", "block", "mineable", "axe.json"),
               {"replace": False, "values": [M + "blueprint_table"]})

    save_png(tex_plan(False), "item", "blank_plan")
    save_png(tex_plan(True), "item", "plan")
    generated_item("blank_plan")
    generated_item("plan")

    shapeless("blank_plan", ["minecraft:paper", "minecraft:blue_dye"], M + "blank_plan")
    shaped("blueprint_table", ["SSS", "PTP", "PCP"],
           {"S": "minecraft:smooth_stone", "P": "#minecraft:planks", "T": "minecraft:crafting_table",
            "C": "minecraft:chest"}, M + "blueprint_table")

    names = {
        "en_us": {"block.redcircuit.blueprint_table": "Blueprint Table",
                  "item.redcircuit.blank_plan": "Blank Plan",
                  "item.redcircuit.plan": "Plan",
                  "item.redcircuit.plan.named": "Plan: %s",
                  "tooltip.redcircuit.plan.result": "Makes %s x %s",
                  "gui.redcircuit.blueprint_table.plan": "Write plan (blank plan in slot) / load plan into the grid",
                  "gui.redcircuit.blueprint_table.storage": "Storage"},
        "pl_pl": {"block.redcircuit.blueprint_table": "Stół kreślarski",
                  "item.redcircuit.blank_plan": "Pusty plan",
                  "item.redcircuit.plan": "Plan",
                  "item.redcircuit.plan.named": "Plan: %s",
                  "tooltip.redcircuit.plan.result": "Wytwarza: %s x %s",
                  "gui.redcircuit.blueprint_table.plan": "Zapisz plan (pusty plan w slocie) / wczytaj plan do siatki",
                  "gui.redcircuit.blueprint_table.storage": "Magazyn"},
    }
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


# ---------------------------------------------------------------- piece do stopów

def tex_bricks(base):
    img = noisy(16, base, 0.06)
    d = ImageDraw.Draw(img)
    mortar = shade(base, 0.55) + (255,)
    for y in (3, 7, 11, 15):
        d.line([(0, y), (15, y)], fill=mortar)
    for row, y0 in enumerate((0, 4, 8, 12)):
        off = 0 if row % 2 == 0 else 4
        for x in range(off, 16, 8):
            d.line([(x, y0), (x, y0 + 2)], fill=mortar)
    return img


def tex_alloy_front(lit, electric):
    if electric:
        img = tex_machine((120, 124, 132), (60, 90, 200), None)
    else:
        img = tex_bricks((150, 70, 52))
    d = ImageDraw.Draw(img)
    frame = (60, 60, 64, 255) if electric else (70, 40, 30, 255)
    d.rectangle([3, 6, 12, 13], fill=(22, 18, 18, 255), outline=frame)
    if lit:
        r = random.Random(5)
        for x in range(4, 12):
            h = r.randint(2, 5)
            col = (90, 160, 255, 255) if electric else (255, r.randint(110, 190), 40, 255)
            d.line([(x, 12), (x, 12 - h)], fill=col)
    # Tygielek / wskaźnik nad otworem
    if electric:
        d.rectangle([4, 2, 11, 3], fill=(60, 110, 255, 255) if lit else (40, 60, 120, 255))
    else:
        d.rectangle([5, 2, 10, 4], fill=(110, 110, 115, 255), outline=(60, 60, 64, 255))
    return img


def gen_alloy():
    M = MODID + ":"
    save_png(tex_bricks((150, 70, 52)), "block", "alloy_furnace_side")
    top = tex_bricks((140, 66, 50))
    ImageDraw.Draw(top).rectangle([5, 5, 10, 10], fill=(40, 30, 28, 255), outline=(90, 90, 95, 255))
    save_png(top, "block", "alloy_furnace_top")
    save_png(tex_alloy_front(False, False), "block", "alloy_furnace_front")
    save_png(tex_alloy_front(True, False), "block", "alloy_furnace_front_on")
    save_png(tex_machine((120, 124, 132), (60, 90, 200), "stripe"), "block", "power_alloy_furnace_side")
    save_png(tex_machine((110, 114, 122), (60, 110, 255), "grid"), "block", "power_alloy_furnace_top")
    save_png(tex_alloy_front(False, True), "block", "power_alloy_furnace_front")
    save_png(tex_alloy_front(True, True), "block", "power_alloy_furnace_front_on")

    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for b in ("alloy_furnace", "power_alloy_furnace"):
        for suffix in ("", "_on"):
            write_json(os.path.join(ASSETS, "models", "block", f"{b}{suffix}.json"), {
                "parent": "minecraft:block/orientable",
                "textures": {"top": f"{MODID}:block/{b}_top", "front": f"{MODID}:block/{b}_front{suffix}",
                             "side": f"{MODID}:block/{b}_side"}})
        parts = []
        for facing, y in rot.items():
            for suffix, lit in (("", "false"), ("_on", "true")):
                apply = {"model": f"{MODID}:block/{b}{suffix}"}
                if y:
                    apply["y"] = y
                parts.append({"when": {"facing": facing, "lit": lit}, "apply": apply})
        write_json(os.path.join(ASSETS, "blockstates", f"{b}.json"), {"multipart": parts})
        item_model(b, f"{MODID}:block/{b}")
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{b}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": M + b}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{b}"})
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data["values"] += [M + "alloy_furnace", M + "power_alloy_furnace"]
    write_json(path, data)

    # Receptury urządzeń (8 bloków cegieł)
    shaped("alloy_furnace", ["BBB", "B B", "BBB"], {"B": "minecraft:bricks"}, M + "alloy_furnace")
    shaped("power_alloy_furnace", ["BBB", "BAB", "IMI"],
           {"B": M + "power_alloy_ingot", "A": M + "alloy_furnace", "I": "minecraft:iron_ingot",
            "M": M + "power_machine_casing"}, M + "power_alloy_furnace")

    # Receptury stopów (typ redcircuit:alloying)
    def alloy(name, ingredients, result, count=1, time=200):
        write_json(os.path.join(DATA, "recipe", f"alloying_{name}.json"), {
            "type": M + "alloying",
            "ingredients": [dict(ing(i), count=c) if c > 1 else ing(i) for i, c in ingredients],
            "result": {"id": result, "count": count},
            "cooktime": time})
    alloy("signal_alloy_ingot", [("minecraft:copper_ingot", 1), ("minecraft:redstone", 4)], M + "signal_alloy_ingot")
    alloy("power_alloy_ingot", [(M + "silver_ingot", 1), (M + "azurite", 4)], M + "power_alloy_ingot")
    # Mosiądz: 3 miedzi + 1 cyna -> 4 sztabki
    alloy("brass_ingot", [("minecraft:copper_ingot", 3), (M + "tin_ingot", 1)], M + "brass_ingot", 4)
    vanilla_tinted_item("brass_ingot", "minecraft:item/iron_ingot")
    # Stal: 1 żelazo + 2 węgle (węgiel lub węgiel drzewny) -> 1 sztabka stali (2x dłużej)
    alloy("steel_ingot", [("minecraft:iron_ingot", 1), ("#minecraft:coals", 2)], M + "steel_ingot", 1, 400)
    vanilla_tinted_item("steel_ingot", "minecraft:item/iron_ingot")
    write_json(os.path.join(RES, "data", "c", "tags", "item", "ingots", "steel.json"),
               {"replace": False, "values": [M + "steel_ingot"]})
    write_json(os.path.join(RES, "data", "c", "tags", "item", "ingots", "brass.json"),
               {"replace": False, "values": [M + "brass_ingot"]})

    names = {
        "en_us": {"item.redcircuit.brass_ingot": "Brass Ingot", "item.redcircuit.steel_ingot": "Steel Ingot",
                  "block.redcircuit.alloy_furnace": "Alloy Furnace",
                  "block.redcircuit.power_alloy_furnace": "Power Alloy Furnace"},
        "pl_pl": {"item.redcircuit.brass_ingot": "Sztabka mosiądzu", "item.redcircuit.steel_ingot": "Sztabka stali",
                  "block.redcircuit.alloy_furnace": "Piec do stopów",
                  "block.redcircuit.power_alloy_furnace": "Elektryczny piec do stopów"},
    }
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


# ---------------------------------------------------------------- piły ręczne

SAWS = {  # materiał: (kolor ostrza, kolor rękojeści, EN, PL, składnik receptury)
    "stone": ((132, 132, 132), (60, 44, 34), "Stone", "Kamienna piła ręczna", "#minecraft:stone_tool_materials"),
    "copper": ((214, 120, 76), (64, 64, 70), "Copper", "Miedziana piła ręczna", "minecraft:copper_ingot"),
    "gold": ((250, 214, 70), (64, 64, 70), "Gold", "Złota piła ręczna", "minecraft:gold_ingot"),
    "iron": ((216, 216, 216), (64, 64, 70), "Iron", "Żelazna piła ręczna", "minecraft:iron_ingot"),
    "silver": ((236, 242, 252), (64, 64, 70), "Silver", "Srebrna piła ręczna", MODID + ":silver_ingot"),
    "diamond": ((96, 232, 222), (64, 64, 70), "Diamond", "Diamentowa piła ręczna", "minecraft:diamond"),
    "netherite": ((82, 72, 78), (52, 40, 44), "Netherite", "Netherytowa piła ręczna", None),
}


HACKSAW_ROWS = [
    "................",
    "................",
    "................",
    "....EEEEEEEEEE..",
    "....FFFFFFFFFFs.",
    "....F........Fs.",
    "....F........Fs.",
    "..HhF........Fs.",
    ".HHhFLLLLLLLLFs.",
    ".HHHFbbbbbbbbF..",
    ".HHH.t.t.t.t.t..",
    "..HH............",
    "..HH............",
    "...H............",
    "................",
    "................",
]


def tex_handsaw(blade, handle):
    """Piła do metalu: stalowy kabłąk, brzeszczot w kolorze materiału (z jaśniejszą krawędzią
    i ząbkami), rękojeść pistoletowa po lewej (w dłoni trafia na dół, jak trzonki narzędzi)."""
    frame = (92, 94, 104)
    colors = {"E": shade(frame, 1.35), "F": frame, "s": shade(frame, 0.65),
              "L": shade(blade, 1.3), "b": blade, "t": shade(blade, 0.6),
              "H": handle, "h": shade(handle, 1.3)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(HACKSAW_ROWS):
        for x, c in enumerate(row):
            if c in colors:
                px[x, y] = colors[c] + (255,)
    for x, y in ((1, 10), (2, 12), (3, 13)):
        px[x, y] = shade(handle, 0.7) + (255,)
    return img


def gen_handsaws():
    M = MODID + ":"
    en, pl = {}, {}
    for mat, (blade, handle, en_n, pl_n, ingredient) in SAWS.items():
        item = f"{mat}_handsaw"
        save_png(tex_handsaw(blade, handle), "item", item)
        write_json(os.path.join(ASSETS, "models", "item", f"{item}.json"),
                   {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/{item}"}})
        en[f"item.{MODID}.{item}"] = f"{en_n} Handsaw"
        pl[f"item.{MODID}.{item}"] = pl_n
        if ingredient:
            shaped(item, ["MMS"], {"M": ingredient, "S": "minecraft:stick"}, M + item)
    # Netheryt: ulepszenie diamentowej piły na stole kowalskim
    write_json(os.path.join(DATA, "recipe", "netherite_handsaw_smithing.json"), {
        "type": "minecraft:smithing_transform",
        "template": {"item": "minecraft:netherite_upgrade_smithing_template"},
        "base": {"item": M + "diamond_handsaw"},
        "addition": {"item": "minecraft:netherite_ingot"},
        "result": {"id": M + "netherite_handsaw", "count": 1}})
    write_json(os.path.join(DATA, "tags", "item", "handsaws.json"),
               {"values": [M + f"{m}_handsaw" for m in SAWS]})
    en[f"tooltip.{MODID}.durability"] = "Durability: %s / %s"
    pl[f"tooltip.{MODID}.durability"] = "Wytrzymałość: %s / %s"


    for lang, names in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(names)
        write_json(path, data)


# ---------------------------------------------------------------- zębatki

GEARS = {  # materiał: (kolor, EN, PL, składnik)
    "stone": ((136, 136, 136), "Stone", "Kamienna zębatka", "#minecraft:stone_tool_materials"),
    "copper": ((214, 120, 76), "Copper", "Miedziana zębatka", "minecraft:copper_ingot"),
    "gold": ((250, 214, 70), "Gold", "Złota zębatka", "minecraft:gold_ingot"),
    "iron": ((216, 216, 216), "Iron", "Żelazna zębatka", "minecraft:iron_ingot"),
    "brass": ((232, 186, 84), "Brass", "Mosiężna zębatka", MODID + ":brass_ingot"),
    "silver": ((236, 242, 252), "Silver", "Srebrna zębatka", MODID + ":silver_ingot"),
    "diamond": ((96, 232, 222), "Diamond", "Diamentowa zębatka", "minecraft:diamond"),
    "netherite": ((92, 80, 86), "Netherite", "Netherytowa zębatka", None),
}


def tex_gear(color):
    """Zębatka: pierścień z 8 zębami, otwór na oś, cieniowanie światło z lewej-góry."""
    import math
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx = cy = 7.5
    inside = [[False] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            body = r <= 5.2
            tooth = False
            for k in range(8):                          # 8 zębów: pasy wzdłuż promieni
                a = k * math.pi / 4
                ux, uy = math.cos(a), math.sin(a)
                along = dx * ux + dy * uy
                across = abs(-dx * uy + dy * ux)
                if 4.0 < along <= 7.4 and across <= 1.45:
                    tooth = True
            inside[y][x] = (body or tooth) and r > 1.7
    for y in range(16):
        for x in range(16):
            if not inside[y][x]:
                continue
            edge = any(not (0 <= x + a < 16 and 0 <= y + b < 16) or not inside[y + b][x + a]
                       for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            dx, dy = x - cx, y - cy
            light = -(dx + dy) / 14.0                   # jaśniej z lewej-góry
            if edge:
                c = shade(color, 0.45)
            else:
                c = shade(color, 1.0 + light * 0.6)
                r = math.hypot(dx, dy)
                if 1.7 < r < 2.9:
                    c = shade(color, 0.7)               # rant wokół osi
            px[x, y] = c + (255,)
    return img


def gen_gears():
    M = MODID + ":"
    en, pl = {}, {}
    for mat, (color, en_n, pl_n, ingredient) in GEARS.items():
        item = f"{mat}_gear"
        save_png(tex_gear(color), "item", item)
        generated_item(item)
        en[f"item.{MODID}.{item}"] = f"{en_n} Gear"
        pl[f"item.{MODID}.{item}"] = pl_n
        write_json(os.path.join(RES, "data", "c", "tags", "item", "gears", f"{mat}.json"),
                   {"replace": False, "values": [M + item]})
        if mat == "stone":
            shaped(item, [" M ", "M M", " M "], {"M": ingredient}, M + item)
        elif ingredient:
            # Zębatka z metalu/diamentu: 4 materiały wokół kamiennej zębatki
            shaped(item, [" M ", "MGM", " M "], {"M": ingredient, "G": M + "stone_gear"}, M + item)
    write_json(os.path.join(DATA, "recipe", "netherite_gear_smithing.json"), {
        "type": "minecraft:smithing_transform",
        "template": {"item": "minecraft:netherite_upgrade_smithing_template"},
        "base": {"item": M + "diamond_gear"},
        "addition": {"item": "minecraft:netherite_ingot"},
        "result": {"id": M + "netherite_gear", "count": 1}})
    write_json(os.path.join(RES, "data", "c", "tags", "item", "gears.json"),
               {"replace": False, "values": [M + f"{m}_gear" for m in GEARS]})
    for lang, names in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(names)
        write_json(path, data)


# ---------------------------------------------------------------- obwód energetyczny

CIRCUIT_ROWS = [
    "................",
    "................",
    "..oooooooooooo..",
    "..obbbbbbbbbbo..",
    "..obgggbbbgggo..",
    "..obbbgbbbgbbo..",
    "..obbkkkkkkbbo..",
    "..ogbkcccckbgo..",
    "..ogbkcccckbgo..",
    "..obbkkkkkkbbo..",
    "..obbgbbbbgbbo..",
    "..obggbbbbggbo..",
    "..oooooooooooo..",
    "...p.p.p.p.p....",
    "................",
    "................",
]


def tex_power_circuit(board=(44, 78, 170), edge=(24, 30, 52), led=(120, 170, 255)):
    """Obwód: kolorowa płytka (niebieska - energetyczny, czerwona - sygnałowy) z miedzianymi ścieżkami i układem."""
    colors = {"o": edge, "b": board, "g": (236, 176, 76), "k": (26, 26, 30),
              "c": (70, 72, 82), "p": (200, 202, 210)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(CIRCUIT_ROWS):
        for x, c in enumerate(row):
            if c in colors:
                px[x, y] = colors[c] + (255,)
    px[7, 7] = led + (255,)                            # dioda na układzie
    return img


def tex_power_machine_casing(stripe=(60, 90, 200)):
    """Obudowa maszyny energetycznej: stalowy panel z ramą, nitami w narożnikach i niebieskim pasem."""
    base = (112, 116, 126)
    img = noisy(16, base, 0.05)
    border(img, shade(base, 0.55), 1)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 14], outline=shade(base, 1.18) + (255,))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        d.point((x, y), fill=(60, 62, 70, 255))
        d.point((x - 1, y - 1), fill=shade(base, 1.3) + (255,))
    d.rectangle([5, 7, 10, 8], fill=stripe + (255,))
    return img


def gen_motor():
    M = MODID + ":"
    save_png(tex_power_circuit(), "item", "power_circuit")
    generated_item("power_circuit")
    shaped("power_circuit", ["IBI", "CGC", "IWI"],
           {"I": "minecraft:iron_ingot", "B": M + "power_alloy_ingot", "C": M + "copper_coil",
            "G": M + "iron_gear", "W": M + "power_alloy_wire"}, M + "power_circuit")

    # Obudowa maszyny energetycznej: korpus wszystkich maszyn na energię
    save_png(tex_power_machine_casing(), "block", "power_machine_casing")
    write_json(os.path.join(ASSETS, "models", "block", "power_machine_casing.json"),
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/power_machine_casing"}})
    write_json(os.path.join(ASSETS, "blockstates", "power_machine_casing.json"),
               {"variants": {"": {"model": f"{MODID}:block/power_machine_casing"}}})
    item_model("power_machine_casing", f"{MODID}:block/power_machine_casing")
    simple_loot("power_machine_casing")
    shaped("power_machine_casing", ["IBI", "BCB", "IBI"],
           {"I": "minecraft:iron_ingot", "B": M + "brass_ingot", "C": M + "power_circuit"}, M + "power_machine_casing")
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "power_machine_casing" not in data["values"]:
        data["values"].append(M + "power_machine_casing")
    write_json(path, data)

    # Obwód sygnałowy i obudowa maszyny sygnałowej: korpus maszyn sterowanych redstone
    save_png(tex_power_circuit((170, 40, 36), (52, 20, 20), (255, 120, 100)), "item", "signal_circuit")
    generated_item("signal_circuit")
    shaped("signal_circuit", ["ISI", "TPT", "IWI"],
           {"I": "minecraft:iron_ingot", "S": M + "signal_alloy_ingot", "T": "minecraft:redstone_torch",
            "P": M + "circuit_plate", "W": M + "signal_alloy_wire"}, M + "signal_circuit")
    save_png(tex_power_machine_casing((200, 50, 40)), "block", "signal_machine_casing")
    write_json(os.path.join(ASSETS, "models", "block", "signal_machine_casing.json"),
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/signal_machine_casing"}})
    write_json(os.path.join(ASSETS, "blockstates", "signal_machine_casing.json"),
               {"variants": {"": {"model": f"{MODID}:block/signal_machine_casing"}}})
    item_model("signal_machine_casing", f"{MODID}:block/signal_machine_casing")
    simple_loot("signal_machine_casing")
    shaped("signal_machine_casing", ["ITI", "TCT", "ITI"],
           {"I": "minecraft:iron_ingot", "T": M + "tin_ingot", "C": M + "signal_circuit"}, M + "signal_machine_casing")
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "signal_machine_casing" not in data["values"]:
        data["values"].append(M + "signal_machine_casing")
    write_json(path, data)
    for lang, kv in (("en_us", {"item.redcircuit.signal_circuit": "Signal Circuit",
                                "block.redcircuit.signal_machine_casing": "Signal Machine Casing"}),
                     ("pl_pl", {"item.redcircuit.signal_circuit": "Obwód sygnałowy",
                                "block.redcircuit.signal_machine_casing": "Obudowa maszyny sygnałowej"})):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(kv)
        write_json(path, data)

    for lang, name, casing in (("en_us", "Power Circuit", "Power Machine Casing"), ("pl_pl", "Obwód energetyczny", "Obudowa maszyny energetycznej")):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data[f"item.{MODID}.power_circuit"] = name
        data[f"block.{MODID}.power_machine_casing"] = casing
        write_json(path, data)


# ---------------------------------------------------------------- generatory

def tex_thermopile_side():
    """Bok termostosu: naprzemienne miedziane i żelazne żeberka (złącza termoelektryczne)."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            band = (x // 2) % 2
            base = (196, 112, 70) if band == 0 else (190, 192, 200)
            f = 1.0 + rng.uniform(-0.05, 0.05)
            if x % 2 == 1:
                f *= 0.8
            if y in (0, 15):
                base = (70, 72, 80)
            px[x, y] = shade(base, f) + (255,)
    return img


def tex_thermopile_top():
    img = tex_machine((96, 98, 108), (60, 110, 255), None)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 7, 12], fill=(200, 70, 40, 255))     # strona gorąca
    d.rectangle([8, 3, 12, 12], fill=(60, 130, 230, 255))   # strona zimna
    d.rectangle([3, 3, 12, 12], outline=(40, 40, 46, 255))
    return img


def tex_kinetic_side():
    """Bok generatora: wirnik z czterema łopatami na stalowej obudowie."""
    img = tex_machine((118, 122, 130), (60, 110, 255), None)
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=(70, 72, 82, 255), outline=(40, 40, 46, 255))
    blade = (220, 222, 228, 255)
    d.polygon([(7, 7), (4, 3), (7, 3)], fill=blade)
    d.polygon([(8, 7), (12, 4), (12, 7)], fill=blade)
    d.polygon([(8, 8), (11, 12), (8, 12)], fill=blade)
    d.polygon([(7, 8), (3, 11), (3, 8)], fill=blade)
    d.rectangle([7, 7, 8, 8], fill=(60, 110, 255, 255))
    return img


def tex_windmill_entity():
    """Wiatrak 64x64: cztery skrzydła z płótna na drewnianych ramionach, piasta w środku."""
    import math
    S = 64
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = S / 2
    wood, wood_d = (126, 90, 52, 255), (88, 62, 36, 255)
    cloth, cloth_d = (232, 228, 214, 255), (196, 190, 172, 255)
    for k in range(4):
        a = k * math.pi / 2
        ux, uy = math.cos(a), math.sin(a)          # kierunek ramienia
        vx, vy = -uy, ux                           # kierunek w bok (skrzydło po jednej stronie)
        # płótno: prostokąt wzdłuż ramienia, przesunięty w bok
        p = lambda t, s_: (c + ux * t + vx * s_, c + uy * t + vy * s_)
        d.polygon([p(7, 1.5), p(31, 1.5), p(31, 11), p(7, 9)], fill=cloth, outline=cloth_d)
        for t in (13, 19, 25):
            d.line([p(t, 1.5), p(t, 10)], fill=cloth_d)
        # ramię
        d.polygon([p(2, -1.5), p(31, -1.5), p(31, 1.5), p(2, 1.5)], fill=wood, outline=wood_d)
    d.ellipse([c - 5, c - 5, c + 5, c + 5], fill=(110, 112, 122, 255), outline=(50, 50, 56, 255))
    d.ellipse([c - 2, c - 2, c + 2, c + 2], fill=(60, 110, 255, 255))
    return img


def tex_water_wheel_entity():
    """Koło wodne 64x64: drewniana obręcz, szprychy i 8 łopatek."""
    import math
    S = 64
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = S / 2
    wood, wood_d, wood_l = (134, 96, 56, 255), (84, 58, 34, 255), (168, 124, 76, 255)
    for k in range(8):
        a = k * math.pi / 4
        ux, uy = math.cos(a), math.sin(a)
        vx, vy = -uy, ux
        p = lambda t, s_: (c + ux * t + vx * s_, c + uy * t + vy * s_)
        d.polygon([p(4, -1.5), p(27, -1.5), p(27, 1.5), p(4, 1.5)], fill=wood_d)          # szprycha
        d.polygon([p(17, -4), p(31.5, -4), p(31.5, 4), p(17, 4)], fill=wood_l, outline=wood_d)  # łopatka
    d.ellipse([c - 20, c - 20, c + 20, c + 20], outline=wood, width=4)
    d.ellipse([c - 20, c - 20, c + 20, c + 20], outline=wood_d, width=1)
    d.ellipse([c - 6, c - 6, c + 6, c + 6], fill=wood, outline=wood_d)
    d.ellipse([c - 2, c - 2, c + 2, c + 2], fill=(60, 110, 255, 255))
    return img


def tex_rotor_icon(big):
    return big.resize((16, 16), Image.LANCZOS)


def gen_generators():
    M = MODID + ":"
    save_png(tex_thermopile_side(), "block", "thermopile_side")
    save_png(tex_thermopile_top(), "block", "thermopile_top")
    save_png(tex_kinetic_side(), "block", "generator_front")
    save_png(tex_machine((118, 122, 130), (60, 110, 255), "stripe"), "block", "generator_side")
    save_png(tex_machine((104, 108, 116), (60, 110, 255), "grid"), "block", "generator_top")
    # Wiatrak i koło wodne w świecie: model 3D w RotorRenderer (tekstury bloków vanilla z atlasu)
    save_png(tex_rotor_icon(tex_windmill_entity()), "item", "windmill")
    save_png(tex_rotor_icon(tex_water_wheel_entity()), "item", "water_wheel")
    generated_item("windmill")
    generated_item("water_wheel")

    # Termostos: kolumna; generator: przód z wałem, obracany
    write_json(os.path.join(ASSETS, "models", "block", "thermopile.json"), {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": f"{MODID}:block/thermopile_top", "side": f"{MODID}:block/thermopile_side"}})
    write_json(os.path.join(ASSETS, "models", "block", "generator.json"), {
        "parent": "minecraft:block/orientable",
        "textures": {"top": f"{MODID}:block/generator_top", "front": f"{MODID}:block/generator_front",
                     "side": f"{MODID}:block/generator_side"}})
    write_json(os.path.join(ASSETS, "blockstates", "thermopile.json"),
               {"variants": {"": {"model": f"{MODID}:block/thermopile"}}})
    parts = []
    for facing, y in {"north": 0, "east": 90, "south": 180, "west": 270}.items():
        apply = {"model": f"{MODID}:block/generator"}
        if y:
            apply["y"] = y
        parts.append({"when": {"facing": facing}, "apply": apply})
    write_json(os.path.join(ASSETS, "blockstates", "generator.json"), {"multipart": parts})
    for b in ("thermopile", "generator"):
        item_model(b, f"{MODID}:block/{b}")
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{b}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": M + b}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{b}"})
    shaped("windmill", ["WSW", "SGS", "WSW"],
           {"W": "#minecraft:wool", "S": "minecraft:stick", "G": M + "iron_gear"}, M + "windmill")
    shaped("water_wheel", ["PSP", "SGS", "PSP"],
           {"P": "#minecraft:planks", "S": "minecraft:stick", "G": M + "iron_gear"}, M + "water_wheel")
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data["values"] += [M + "thermopile", M + "generator"]
    write_json(path, data)

    shaped("thermopile", ["CTC", "TWT", "CTC"],
           {"C": "minecraft:copper_ingot", "T": M + "tin_ingot", "W": M + "power_alloy_wire"}, M + "thermopile")
    shaped("generator", ["IGI", "GMG", "IWI"],
           {"I": "minecraft:iron_ingot", "G": M + "iron_gear", "M": M + "power_machine_casing",
            "W": M + "power_alloy_wire"}, M + "generator")

    names = {"en_us": {"block.redcircuit.thermopile": "Thermopile",
                       "block.redcircuit.generator": "Generator",
                       "item.redcircuit.windmill": "Windmill",
                       "item.redcircuit.water_wheel": "Water Wheel",
                       "message.redcircuit.rotor_installed": "Rotor installed"},
             "pl_pl": {"block.redcircuit.thermopile": "Termostos",
                       "block.redcircuit.generator": "Generator",
                       "item.redcircuit.windmill": "Wiatrak",
                       "item.redcircuit.water_wheel": "Koło wodne",
                       "message.redcircuit.rotor_installed": "Zamontowano wirnik"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


# ---------------------------------------------------------------- opisy JEI

def gen_pump():
    M = MODID + ":"
    # Tekstury: wylot (przód), kratka ssąca (tył), boki z niebieską rurą
    front = tex_machine((112, 116, 126), (60, 110, 255), None)
    d = ImageDraw.Draw(front)
    d.ellipse([4, 4, 11, 11], fill=(40, 42, 50, 255), outline=(170, 175, 185, 255))
    d.ellipse([6, 6, 9, 9], fill=(60, 110, 255, 255))
    save_png(front, "block", "pump_front")
    back = tex_machine((112, 116, 126), (60, 110, 255), None)
    d = ImageDraw.Draw(back)
    d.rectangle([3, 3, 12, 12], fill=(36, 38, 44, 255))
    for x in (4, 6, 8, 10):
        d.line([(x, 3), (x, 12)], fill=(150, 154, 164, 255))
    save_png(back, "block", "pump_back")
    side = tex_machine((112, 116, 126), (60, 110, 255), None)
    d = ImageDraw.Draw(side)
    d.rectangle([0, 6, 15, 9], fill=(52, 92, 200, 255))
    d.line([(0, 6), (15, 6)], fill=(120, 165, 255, 255))
    d.rectangle([6, 4, 9, 11], fill=(150, 154, 164, 255), outline=(60, 62, 70, 255))
    save_png(side, "block", "pump_side")

    write_json(os.path.join(ASSETS, "models", "block", "pump.json"), {
        "parent": "minecraft:block/cube",
        "textures": {"particle": f"{MODID}:block/pump_side",
                     "north": f"{MODID}:block/pump_front", "south": f"{MODID}:block/pump_back",
                     "east": f"{MODID}:block/pump_side", "west": f"{MODID}:block/pump_side",
                     "up": f"{MODID}:block/pump_side", "down": f"{MODID}:block/pump_side"}})
    rot6 = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
            "up": {"x": 270}, "down": {"x": 90}}
    write_json(os.path.join(ASSETS, "blockstates", "pump.json"), {"multipart": [
        {"when": {"facing": f}, "apply": dict({"model": f"{MODID}:block/pump"}, **r)} for f, r in rot6.items()]})
    item_model("pump", f"{MODID}:block/pump")
    write_json(os.path.join(DATA, "loot_table", "blocks", "pump.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": M + "pump"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/pump"})
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data["values"].append(M + "pump")
    write_json(path, data)

    shaped("pump", ["IBI", "GMG", "IWI"],
           {"I": "minecraft:iron_ingot", "B": "minecraft:bucket", "G": M + "brass_gear",
            "M": M + "power_machine_casing", "W": M + "power_alloy_wire"}, M + "pump")

    names = {"en_us": {"block.redcircuit.pump": "Pump",
                       "message.redcircuit.pump": "%s: %s / %s mB, energy: %s FE",
                       "message.redcircuit.pump_empty": "Empty"},
             "pl_pl": {"block.redcircuit.pump": "Pompa",
                       "message.redcircuit.pump": "%s: %s / %s mB, energia: %s FE",
                       "message.redcircuit.pump_empty": "Pusto"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def gen_fluid_pipe():
    M = MODID + ":"
    # Mosiężna rura z ciemnymi obręczami i szklanym wizjerem
    brass = (214, 172, 74)
    img = noisy(16, brass, 0.05)
    d = ImageDraw.Draw(img)
    d.line([(0, 1), (15, 1)], fill=shade(brass, 1.2) + (255,))
    d.line([(0, 14), (15, 14)], fill=shade(brass, 0.65) + (255,))
    for x in (0, 15):
        d.line([(x, 0), (x, 15)], fill=(110, 82, 30, 255))
    d.rectangle([5, 5, 10, 10], fill=(150, 200, 220, 255), outline=(110, 82, 30, 255))
    d.line([(6, 6), (7, 6)], fill=(230, 245, 255, 255))
    save_png(img, "block", "fluid_pipe")
    simple_cable("fluid_pipe", f"{MODID}:block/fluid_pipe", 6.0)

    write_json(os.path.join(DATA, "loot_table", "blocks", "fluid_pipe.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": M + "fluid_pipe"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/fluid_pipe"})
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "fluid_pipe" not in data["values"]:
        data["values"].append(M + "fluid_pipe")
    write_json(path, data)

    shaped("fluid_pipe", ["BGB"], {"B": M + "brass_ingot", "G": "minecraft:glass"}, M + "fluid_pipe", 8)

    names = {"en_us": {"block.redcircuit.fluid_pipe": "Fluid Pipe",
                       "message.redcircuit.pipe": "%s: %s / %s mB"},
             "pl_pl": {"block.redcircuit.fluid_pipe": "Rura do płynów",
                       "message.redcircuit.pipe": "%s: %s / %s mB"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


SCREWDRIVER_ROWS = [
    "................",
    "................",
    "............t...",
    "...........sTt..",
    "..........sSs...",
    ".........sSs....",
    "........sSs.....",
    ".......cCc......",
    "......HhHc......",
    ".....HhHHD......",
    "....HhHHD.......",
    "...HhHHD........",
    "..HhHHD.........",
    "..BHHD..........",
    "...BB...........",
    "................",
]


def gen_screwdriver():
    """Śrubokręt: czerwona rękojeść z czarną nasadką, stalowy trzpień, płaski grot."""
    M = MODID + ":"
    steel = (170, 174, 186)
    red = (196, 40, 32)
    colors = {"t": shade(steel, 0.7), "T": (235, 238, 245), "S": steel, "s": shade(steel, 0.6),
              "C": (60, 62, 70), "c": (36, 36, 42),
              "H": red, "h": shade(red, 1.35), "D": shade(red, 0.6), "B": (36, 36, 42)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(SCREWDRIVER_ROWS):
        for x, c in enumerate(row):
            if c in colors:
                px[x, y] = colors[c] + (255,)
    save_png(img, "item", "screwdriver")
    write_json(os.path.join(ASSETS, "models", "item", "screwdriver.json"),
               {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/screwdriver"}})
    shaped("screwdriver", [" I", "S "], {"I": "minecraft:iron_ingot", "S": "minecraft:stick"}, M + "screwdriver")
    names = {"en_us": {"item.redcircuit.screwdriver": "Screwdriver",
                       "tooltip.redcircuit.screwdriver": "Right-click: rotate block (Shift: other way)",
                       "tooltip.redcircuit.screwdriver_gate": "Shift + right-click a gate: pick it up with its settings"},
             "pl_pl": {"item.redcircuit.screwdriver": "Śrubokręt",
                       "tooltip.redcircuit.screwdriver": "PPM: obróć blok (Shift: w drugą stronę)",
                       "tooltip.redcircuit.screwdriver_gate": "Shift + PPM na bramce: zbierz ją z ustawieniami"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


WIRE_CUTTER_ROWS = [
    "................",
    ".hh.........hh..",
    ".hsh.......hsh..",
    "..hsh.....hsh...",
    "...hsh...hsh....",
    "....hsh.hsh.....",
    ".....hsshs......",
    "......hpp.......",
    ".....rhpprr.....",
    "....rRr..rRr....",
    "...rRr....rRr...",
    "..rRr......rRr..",
    "..rRr......rRr..",
    "..rr........rr..",
    "................",
    "................",
]


def tex_wire_cutters():
    """Nożyce do drutu: stalowe ostrza z nitem i czerwone, gumowane rączki."""
    colors = {"h": (70, 74, 84), "s": (196, 200, 210), "p": (120, 124, 134),
              "r": (130, 24, 24), "R": (206, 52, 44)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(WIRE_CUTTER_ROWS):
        for x, c in enumerate(row):
            if c in colors:
                px[x, y] = colors[c] + (255,)
    return img


def tex_fine_wire(rgb):
    """Szpulka cienkiego drutu: zwoje w kolorze metalu na drewnianej szpuli."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    wood = (120, 86, 50)
    d.rectangle([3, 2, 12, 3], fill=wood + (255,), outline=shade(wood, 0.6) + (255,))
    d.rectangle([3, 12, 12, 13], fill=wood + (255,), outline=shade(wood, 0.6) + (255,))
    d.rectangle([4, 4, 11, 11], fill=shade(rgb, 0.75) + (255,))
    for y in range(4, 12):
        c = shade(rgb, 1.25) if y % 2 == 0 else rgb
        d.line([(4, y), (11, y)], fill=c + (255,))
    d.line([(4, 4), (4, 11)], fill=shade(rgb, 0.55) + (255,))
    d.line([(11, 4), (11, 11)], fill=shade(rgb, 0.65) + (255,))
    # luźny koniec drutu
    d.line([(12, 9), (14, 11), (14, 14)], fill=rgb + (255,))
    return img


def gen_wire_cutters():
    M = MODID + ":"
    save_png(tex_wire_cutters(), "item", "wire_cutters")
    generated_item("wire_cutters")
    shaped("wire_cutters", ["S S", " S ", "R R"], {"S": M + "steel_ingot", "R": "#c:dyes/red"}, M + "wire_cutters")
    for name, rgb, ingot in (("fine_copper_wire", (222, 120, 72), "minecraft:copper_ingot"),
                             ("fine_iron_wire", (196, 198, 206), "minecraft:iron_ingot")):
        save_png(tex_fine_wire(rgb), "item", name)
        generated_item(name)
        shapeless(name, [ingot, M + "wire_cutters"], M + name)
    names = {"en_us": {"item.redcircuit.wire_cutters": "Wire Cutters",
                       "item.redcircuit.fine_copper_wire": "Fine Copper Wire",
                       "item.redcircuit.fine_iron_wire": "Fine Iron Wire"},
             "pl_pl": {"item.redcircuit.wire_cutters": "Nożyce do drutu",
                       "item.redcircuit.fine_copper_wire": "Cienki drut miedziany",
                       "item.redcircuit.fine_iron_wire": "Cienki drut żelazny"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def tex_copper_coil():
    """Cewka: miedziane zwoje nawinięte na żelazny rdzeń, widoczny na końcach."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    core = (170, 172, 180)
    cu = (222, 120, 72)
    # rdzeń wystający po bokach
    d.rectangle([1, 6, 3, 9], fill=core + (255,), outline=shade(core, 0.55) + (255,))
    d.rectangle([12, 6, 14, 9], fill=core + (255,), outline=shade(core, 0.55) + (255,))
    # uzwojenie
    d.rectangle([3, 3, 12, 12], fill=shade(cu, 0.6) + (255,))
    px = img.load()
    for x in range(4, 12):
        light = shade(cu, 1.3) if x % 2 == 0 else cu
        for y in range(4, 12):
            c = light if y < 6 else (cu if y < 10 else shade(cu, 0.75))
            if x % 2 == 1:
                c = shade(c, 0.85)
            px[x, y] = c + (255,)
    d.line([(4, 3), (11, 3)], fill=shade(cu, 0.55) + (255,))
    d.line([(4, 12), (11, 12)], fill=shade(cu, 0.45) + (255,))
    # wyprowadzenia drutu
    px[5, 2] = cu + (255,)
    px[5, 1] = cu + (255,)
    px[10, 13] = cu + (255,)
    px[10, 14] = cu + (255,)
    return img


def gen_coil():
    M = MODID + ":"
    save_png(tex_copper_coil(), "item", "copper_coil")
    generated_item("copper_coil")
    shaped("copper_coil", [" W ", "WIW", " W "], {"W": M + "fine_copper_wire", "I": "minecraft:iron_ingot"},
           M + "copper_coil")
    for lang, name in (("en_us", "Copper Coil"), ("pl_pl", "Cewka miedziana")):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data[f"item.{MODID}.copper_coil"] = name
        write_json(path, data)


BATTERY_ROWS = [
    "................",
    "......tTTt......",
    "......sTTs......",
    "....kKKKKKKk....",
    "....kBbbbbBk....",
    "....kBbbbbBk....",
    "....kBbbNbBk....",
    "....kBbNNNBk....",
    "....kBbbNbBk....",
    "....kBbbbbBk....",
    "....kBbbbbBk....",
    "....kDDDDDDk....",
    "....kTTTTTTk....",
    "....kssssssk....",
    ".....kkkkkk.....",
    "................",
]


def gen_battery():
    """Bateria: niebieskie ogniwo z cynowymi wieczkami i znakiem plusa z azurytu."""
    M = MODID + ":"
    tin = (200, 212, 226)
    blue = (40, 90, 200)
    colors = {"t": shade(tin, 0.7), "T": tin, "s": shade(tin, 0.6), "k": (24, 26, 36),
              "K": shade(blue, 1.5), "B": shade(blue, 0.7), "b": blue, "D": shade(blue, 0.5),
              "N": (110, 200, 255)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(BATTERY_ROWS):
        for x, c in enumerate(row):
            if c in colors:
                px[x, y] = colors[c] + (255,)
    for y in range(4, 11):           # połysk
        px[6, y] = shade(blue, 1.3) + (255,)
    save_png(img, "item", "battery")
    generated_item("battery")
    shaped("battery", [" C ", "TNT", "TNT"],
           {"C": "minecraft:copper_ingot", "T": M + "tin_ingot", "N": M + "azurite"}, M + "battery")
    names = {"en_us": {"item.redcircuit.battery": "Battery",
                       "tooltip.redcircuit.battery": "Charge: %s / %s FE",
                       "tooltip.redcircuit.battery_hint": "Right-click a Battery Box: charge, Shift: discharge",
                       "message.redcircuit.battery_item": "%s: %s / %s FE | Battery Box: %s / %s FE",
                       "gui.redcircuit.charge": "Charge",
                       "gui.redcircuit.discharge": "Discharge"},
             "pl_pl": {"item.redcircuit.battery": "Bateria",
                       "tooltip.redcircuit.battery": "Ładunek: %s / %s FE",
                       "tooltip.redcircuit.battery_hint": "PPM na akumulatorze: ładuj, Shift: rozładuj",
                       "message.redcircuit.battery_item": "%s: %s / %s FE | Akumulator: %s / %s FE",
                       "gui.redcircuit.charge": "Ładowanie",
                       "gui.redcircuit.discharge": "Rozładowanie"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def tex_casing(stripe, bundled=False):
    """Obudowa przewodu: gładka szara powłoka z paskiem w kolorze przewodu przez środek.
    Model używa UV z położenia elementów, więc pasek biegnie ciągle wzdłuż ramion."""
    base = (74, 76, 82) if bundled else (150, 152, 158)
    img = noisy(16, base, 0.035)
    px = img.load()
    # Delikatne obręcze co 4 piksele (tylko poza paskiem)
    for k in range(16):
        for o in (2, 13):
            px[k, o] = shade(base, 0.9) + (255,)
            px[o, k] = shade(base, 0.9) + (255,)
    if bundled:
        cols = [DYE_RGB[c] for c in DYES]
        for k in range(16):
            for o in range(5, 11):
                px[k, o] = shade(base, 0.8) + (255,)
                px[o, k] = shade(base, 0.8) + (255,)
        for k in range(16):
            px[k, 6] = cols[k] + (255,)
            px[k, 9] = cols[(k + 8) % 16] + (255,)
            px[6, k] = cols[(k + 4) % 16] + (255,)
            px[9, k] = cols[(k + 12) % 16] + (255,)
    else:
        light = tuple(min(255, int(c * 1.25)) for c in stripe)
        for k in range(16):
            for o in (7, 8):
                px[k, o] = stripe + (255,)
                px[o, k] = stripe + (255,)
            px[k, 7] = light + (255,)
            px[7, k] = light + (255,)
    return img


def gen_encased():
    M = MODID + ":"
    wires = [("signal_alloy_wire", (200, 30, 20), False, "Signal Alloy Wire", "Przewód ze stopu sygnałowego", 6.0)]
    for dye in DYES:
        wires.append((f"insulated_wire_{dye}", DYE_RGB[dye], False,
                      dye.replace("_", " ").title() + " Insulated Wire", f"Przewód izolowany ({DYE_PL[dye]})", 6.0))
    wires.append(("bundled_cable", None, True, "Bundled Cable", "Kabel wiązkowy", 8.0))
    wires.append(("power_alloy_wire", (60, 110, 255), False, "Power Alloy Wire", "Przewód ze stopu energetycznego", 6.0))

    en, pl = {}, {}
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        pick = json.load(f)
    for wire, color, bundled, en_n, pl_n, thick in wires:
        block = f"encased_{wire}"
        save_png(tex_casing(color, bundled), "block", block)
        simple_cable(block, f"{MODID}:block/{block}", thick, auto_uv=True)
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{block}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": M + block}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{MODID}:blocks/{block}"})
        if M + block not in pick["values"]:
            pick["values"].append(M + block)
        # 8 przewodów wokół płyty z gładkiego kamienia -> 8 przewodów w obudowie
        shaped(block, ["WWW", "WSW", "WWW"], {"W": M + wire, "S": "minecraft:smooth_stone_slab"}, M + block, 8)
        # i z powrotem: przewód w obudowie -> zwykły przewód
        shapeless(f"{wire}_from_encased", [M + block], M + wire)
        en[f"block.{MODID}.{block}"] = f"Encased {en_n}"
        pl[f"block.{MODID}.{block}"] = f"{pl_n} w obudowie"
    write_json(path, pick)
    for lang, data_ in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def gen_requester():
    M = MODID + ":"
    base, accent = (96, 104, 118), (60, 110, 255)
    front = tex_machine(base, accent, "arrow_out")
    save_png(front, "block", "requester_front")
    back = tex_machine(base, accent, "hole")
    d = ImageDraw.Draw(back)
    # haczyk "ściągający" w otworze
    hook = (200, 205, 215, 255)
    d.line([(8, 5), (8, 9)], fill=hook)
    d.line([(6, 10), (7, 10)], fill=hook)
    d.point((8, 10), fill=shade((200, 205, 215), 0.8) + (255,))
    d.line([(6, 8), (6, 9)], fill=hook)
    d.point((7, 4), fill=hook)
    d.point((8, 4), fill=hook)
    save_png(back, "block", "requester_back")
    side = tex_machine(base, accent, "stripe")
    d = ImageDraw.Draw(side)
    for x in (4, 8, 12):          # strzałki wzdłuż boku: przepływ do tyłu -> do przodu
        d.point((x - 1, 6), fill=(230, 230, 240, 255))
        d.point((x - 1, 9), fill=(230, 230, 240, 255))
    save_png(side, "block", "requester_side")
    write_json(os.path.join(ASSETS, "models", "block", "requester.json"), {
        "parent": "minecraft:block/cube",
        "textures": {"particle": f"{MODID}:block/requester_side",
                     "north": f"{MODID}:block/requester_front", "south": f"{MODID}:block/requester_back",
                     "east": f"{MODID}:block/requester_side", "west": f"{MODID}:block/requester_side",
                     "up": f"{MODID}:block/requester_side", "down": f"{MODID}:block/requester_side"}})
    rot6 = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
            "up": {"x": 270}, "down": {"x": 90}}
    write_json(os.path.join(ASSETS, "blockstates", "requester.json"), {"multipart": [
        {"when": {"facing": f}, "apply": dict({"model": f"{MODID}:block/requester"}, **r)} for f, r in rot6.items()]})
    item_model("requester", f"{MODID}:block/requester")
    write_json(os.path.join(DATA, "loot_table", "blocks", "requester.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": M + "requester"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/requester"})
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "requester" not in data["values"]:
        data["values"].append(M + "requester")
    write_json(path, data)
    shaped("requester", ["IFI", "GMG", "IWI"],
           {"I": "minecraft:iron_ingot", "F": M + "filter", "G": "minecraft:gold_ingot",
            "M": M + "power_machine_casing", "W": M + "power_alloy_wire"}, M + "requester")
    names = {"en_us": {"block.redcircuit.requester": "Requester", "container.redcircuit.requester": "Requester"},
             "pl_pl": {"block.redcircuit.requester": "Requester",
                       "container.redcircuit.requester": "Requester"}}
    for lang, data_ in names.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


def machine6(name, front, back, side):
    """Maszyna z 6 kierunkami: tekstury, model (przód na północy), blockstate, przedmiot, loot, kilof."""
    M = MODID + ":"
    for part, img in (("front", front), ("back", back), ("side", side)):
        save_png(img, "block", f"{name}_{part}")
    write_json(os.path.join(ASSETS, "models", "block", f"{name}.json"), {
        "parent": "minecraft:block/cube",
        "textures": {"particle": f"{MODID}:block/{name}_side",
                     "north": f"{MODID}:block/{name}_front", "south": f"{MODID}:block/{name}_back",
                     "east": f"{MODID}:block/{name}_side", "west": f"{MODID}:block/{name}_side",
                     "up": f"{MODID}:block/{name}_side", "down": f"{MODID}:block/{name}_side"}})
    rot6 = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
            "up": {"x": 270}, "down": {"x": 90}}
    write_json(os.path.join(ASSETS, "blockstates", f"{name}.json"), {"multipart": [
        {"when": {"facing": f}, "apply": dict({"model": f"{MODID}:block/{name}"}, **r)} for f, r in rot6.items()]})
    item_model(name, f"{MODID}:block/{name}")
    simple_loot(name)


def simple_loot(name):
    M = MODID + ":"
    write_json(os.path.join(DATA, "loot_table", "blocks", f"{name}.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": M + name}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{MODID}:blocks/{name}"})
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + name not in data["values"]:
        data["values"].append(M + name)
    write_json(path, data)


def gen_machines():
    M = MODID + ":"
    W = (230, 230, 240)

    def sym(img, pts, col):
        px = img.load()
        for x, y in pts:
            px[x, y] = col + (255,)
        return img

    # Manager - azurytowe akcenty (zasilany), z przodu sieć, z tyłu pilnowany inwentarz
    b, a = (88, 94, 110), (60, 110, 255)
    machine6("manager", tex_machine(b, a, "arrow_out"), tex_machine(b, a, "hole"), tex_machine(b, a, "grid"))
    # Regulator - żółte akcenty, waga na boku
    b, a = (112, 100, 78), (240, 200, 60)
    side = tex_machine(b, a, None)
    ImageDraw.Draw(side).line([(3, 6), (12, 6)], fill=a + (255,))
    sym(side, [(7, 5), (8, 5), (7, 7), (7, 8), (7, 9), (7, 10), (8, 10), (6, 10), (9, 10), (3, 7), (12, 7), (2, 8), (4, 8), (11, 8), (13, 8)], a)
    machine6("regulator", tex_machine(b, a, "arrow_out"), tex_machine(b, (60, 60, 60), "hole"), side)
    # Relay - zielone akcenty
    b, a = (120, 92, 60), (70, 170, 70)
    machine6("relay", tex_machine(b, a, "arrow_out"), tex_machine(b, a, "hole"), tex_machine(b, a, "stripe"))
    # Item Detector - czerwone oczko
    b, a = (104, 104, 108), (220, 50, 40)
    side = tex_machine(b, a, None)
    ImageDraw.Draw(side).ellipse([5, 5, 10, 10], fill=(30, 30, 30, 255), outline=a + (255,))
    sym(side, [(7, 7), (8, 8)], (255, 120, 100))
    machine6("item_detector", tex_machine(b, a, "arrow_out"), tex_machine(b, a, "hole"), side)
    # Igniter - płomień z przodu
    b, a = (92, 62, 52), (255, 140, 30)
    front = tex_machine(b, a, None)
    d = ImageDraw.Draw(front)
    d.rectangle([4, 4, 11, 11], fill=(30, 22, 20, 255))
    sym(front, [(7, 10), (8, 10), (6, 9), (7, 9), (8, 9), (9, 9), (6, 8), (7, 8), (8, 8), (9, 8), (7, 7), (8, 7), (8, 6), (7, 5)], a)
    sym(front, [(7, 9), (8, 9), (8, 8), (7, 8)], (255, 230, 90))
    machine6("igniter", front, tex_machine(b, (60, 60, 60), None), tex_machine(b, a, "stripe"))
    # Assembler - 16 kolorowych kropek (kanały) na boku
    b, a = (98, 108, 90), (200, 40, 30)
    side = tex_machine(b, a, None)
    for i, dye in enumerate(DYES):
        sym(side, [(3 + (i % 4) * 3, 3 + (i // 4) * 3)], DYE_RGB[dye])
    machine6("assembler", tex_machine(b, a, "arrow_out"), tex_machine(b, a, "hole"), side)
    # Ejector - strzałka wyrzutu
    b, a = (120, 92, 60), (200, 40, 30)
    front = tex_machine(b, a, None)
    d = ImageDraw.Draw(front)
    d.rectangle([4, 4, 11, 11], fill=(30, 30, 30, 255))
    d.polygon([(8, 5), (11, 8), (9, 8), (9, 11), (7, 11), (7, 8), (5, 8)], fill=a + (255,))
    machine6("ejector", front, tex_machine(b, a, "hole"), tex_machine(b, a, "stripe"))
    # Deployer - "dłoń" z przodu
    b, a = (128, 128, 132), (200, 150, 60)
    front = tex_machine(b, a, None)
    d = ImageDraw.Draw(front)
    d.rectangle([4, 4, 11, 11], fill=(30, 30, 30, 255))
    d.rectangle([6, 7, 10, 11], fill=(214, 170, 130, 255))
    for x in (6, 7, 8, 9):
        d.line([(x, 5), (x, 7)], fill=(214, 170, 130, 255))
    d.line([(10, 8), (11, 7)], fill=(214, 170, 130, 255))
    machine6("deployer", front, tex_machine(b, a, "hole"), tex_machine(b, a, "stripe"))
    # Block Breaker - zęby z przodu
    b, a = (108, 108, 114), (90, 200, 230)
    front = tex_machine(b, a, None)
    d = ImageDraw.Draw(front)
    d.rectangle([3, 3, 12, 12], fill=(30, 30, 30, 255))
    for x in range(3, 13, 3):
        d.polygon([(x, 12), (x + 1, 8), (x + 2, 12)], fill=a + (255,))
        d.polygon([(x, 3), (x + 1, 7), (x + 2, 3)], fill=a + (255,))
    machine6("block_breaker", front, tex_machine(b, a, "hole"), tex_machine(b, a, "stripe"))
    # Buffer - skrzynka bez kierunku
    b, a = (120, 92, 60), (220, 180, 60)
    save_png(tex_machine(b, a, "grid"), "block", "buffer_side")
    save_png(tex_machine(b, a, "hole"), "block", "buffer_top")
    write_json(os.path.join(ASSETS, "models", "block", "buffer.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"{MODID}:block/buffer_top", "bottom": f"{MODID}:block/buffer_top",
                     "side": f"{MODID}:block/buffer_side"}})
    write_json(os.path.join(ASSETS, "blockstates", "buffer.json"), {"variants": {"": {"model": f"{MODID}:block/buffer"}}})
    item_model("buffer", f"{MODID}:block/buffer")
    simple_loot("buffer")
    en, pl = {}, {}

    # Receptury maszyn
    I, R, RW, AW = "minecraft:iron_ingot", "minecraft:redstone", M + "signal_alloy_wire", M + "power_alloy_wire"
    shaped("manager", ["IRI", "CHC", "IWI"],
           {"I": I, "R": M + "requester", "C": "minecraft:comparator", "H": "minecraft:chest", "W": AW}, M + "manager")
    shaped("regulator", ["IFI", "TCT", "IWI"],
           {"I": I, "F": M + "filter", "T": M + "extractor", "C": "minecraft:comparator", "W": M + "signal_machine_casing"}, M + "regulator")
    shaped("relay", ["ICI", "IHI", "IWI"], {"I": I, "C": "minecraft:chest", "H": "minecraft:hopper", "W": M + "signal_machine_casing"}, M + "relay")
    shaped("ejector", ["IDI", "IHI", "IWI"], {"I": I, "D": "minecraft:dropper", "H": "minecraft:hopper", "W": M + "signal_machine_casing"}, M + "ejector")
    shaped("item_detector", ["IPI", "GOG", "IWI"],
           {"I": I, "P": "minecraft:stone_pressure_plate", "G": "minecraft:glass", "O": "minecraft:observer", "W": M + "signal_machine_casing"},
           M + "item_detector")
    shaped("igniter", ["NNN", "IFI", "IWI"],
           {"N": "minecraft:netherrack", "I": I, "F": "minecraft:flint_and_steel", "W": M + "signal_machine_casing"}, M + "igniter")
    shaped("assembler", ["IPI", "CMC", "IBI"],
           {"I": I, "P": "minecraft:piston", "C": "minecraft:chest", "M": M + "signal_machine_casing", "B": M + "bundled_cable"},
           M + "assembler")
    shaped("deployer", ["SPS", "SHS", "SWS"],
           {"S": "minecraft:cobblestone", "P": "minecraft:piston", "H": "minecraft:chest", "W": M + "signal_machine_casing"}, M + "deployer")
    shaped("block_breaker", ["SPS", "SKS", "SWS"],
           {"S": "minecraft:cobblestone", "P": "minecraft:piston", "K": "minecraft:iron_pickaxe", "W": M + "signal_machine_casing"}, M + "block_breaker")
    shaped("buffer", ["ICI", "C C", "ICI"], {"I": I, "C": "minecraft:chest"}, M + "buffer")

    names = {
        "manager": ("Manager", "Manager"), "regulator": ("Regulator", "Regulator"), "relay": ("Relay", "Przekaźnik przedmiotów"),
        "item_detector": ("Item Detector", "Detektor przedmiotów"), "igniter": ("Igniter", "Zapalarka"),
        "assembler": ("Assembler", "Asembler"), "ejector": ("Ejector", "Wyrzutnik"), "deployer": ("Deployer", "Aplikator"),
        "buffer": ("Buffer", "Magazyn buforowy"), "block_breaker": ("Block Breaker", "Niszczarka bloków"),
    }
    for k, (e, p_) in names.items():
        en[f"block.{MODID}.{k}"] = e
        pl[f"block.{MODID}.{k}"] = p_
        en[f"container.{MODID}.{k}"] = e
        pl[f"container.{MODID}.{k}"] = p_
    en[f"container.{MODID}.manager"] = "Manager (items to keep in stock)"
    pl[f"container.{MODID}.manager"] = "Manager (przedmioty do utrzymania)"
    en[f"container.{MODID}.regulator"] = "Regulator (amounts to supply)"
    pl[f"container.{MODID}.regulator"] = "Regulator (ilości do uzupełnienia)"
    en[f"container.{MODID}.assembler"] = "Assembler (slots 1-16 = channels)"
    pl[f"container.{MODID}.assembler"] = "Asembler (sloty 1-16 = kanały)"
    for lang, data_ in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(data_)
        write_json(path, data)


# ---------------------------------------------------------------- generator na paliwo stałe

def tex_fuel_gen_front(lit):
    """Przód: ciemna stalowa płyta z rusztem paleniska i kratką wentylacyjną nad nim."""
    base, accent = (96, 98, 106), (200, 120, 40)
    img = tex_machine(base, accent, None)
    d = ImageDraw.Draw(img)
    d.rectangle([3, 8, 12, 13], fill=(22, 20, 20, 255))            # palenisko
    r = random.Random(77)
    if lit:
        for x in range(4, 12):
            h = r.randint(2, 5)
            d.line([(x, 13), (x, 13 - h)], fill=(255, r.randint(130, 210), 40, 255))
        for x in range(4, 12, 2):
            d.point((x, 13), fill=(255, 240, 150, 255))
    else:
        for x in range(4, 12, 2):
            d.point((x, 12), fill=(60, 50, 45, 255))
    for x in range(3, 13, 3):                                        # pręty rusztu
        d.line([(x, 8), (x, 13)], fill=(70, 72, 78, 255))
    for y in (3, 5):                                                 # kratka wentylacyjna
        d.line([(4, y), (11, y)], fill=(40, 40, 44, 255))
    return img


def gen_fuel_generator():
    M = MODID + ":"
    base, accent = (96, 98, 106), (200, 120, 40)
    save_png(tex_fuel_gen_front(False), "block", "solid_fuel_generator_front")
    save_png(tex_fuel_gen_front(True), "block", "solid_fuel_generator_front_on")
    side = tex_machine(base, accent, "stripe")
    save_png(side, "block", "solid_fuel_generator_side")
    top = tex_machine(shade(base, 0.92), accent, None)
    d = ImageDraw.Draw(top)
    d.ellipse([5, 5, 10, 10], fill=(35, 35, 38, 255), outline=(70, 72, 78, 255))   # komin
    save_png(top, "block", "solid_fuel_generator_top")
    for lit, tex in (("", "solid_fuel_generator_front"), ("_on", "solid_fuel_generator_front_on")):
        write_json(os.path.join(ASSETS, "models", "block", f"solid_fuel_generator{lit}.json"), {
            "parent": "minecraft:block/orientable",
            "textures": {"top": f"{MODID}:block/solid_fuel_generator_top",
                         "front": f"{MODID}:block/{tex}",
                         "side": f"{MODID}:block/solid_fuel_generator_side"}})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit, val in (("", "false"), ("_on", "true")):
            v = {"model": f"{MODID}:block/solid_fuel_generator{lit}"}
            if y:
                v["y"] = y
            variants[f"facing={facing},lit={val}"] = v
    write_json(os.path.join(ASSETS, "blockstates", "solid_fuel_generator.json"), {"variants": variants})
    item_model("solid_fuel_generator", f"{MODID}:block/solid_fuel_generator")
    simple_loot("solid_fuel_generator")
    shaped("solid_fuel_generator", ["III", "CFC", "IWI"],
           {"I": "minecraft:iron_ingot", "C": M + "copper_coil", "F": "minecraft:furnace", "W": M + "power_alloy_wire"},
           M + "solid_fuel_generator")
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "solid_fuel_generator" not in data["values"]:
        data["values"].append(M + "solid_fuel_generator")
    write_json(path, data)
    texts = {
        "en_us": {"block.redcircuit.solid_fuel_generator": "Solid Fuel Generator",
                  "gui.redcircuit.generating": "Generating: %s FE/t"},
        "pl_pl": {"block.redcircuit.solid_fuel_generator": "Generator na paliwo stałe",
                  "gui.redcircuit.generating": "Produkcja: %s FE/t"}}
    for lang, kv in texts.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(kv)
        write_json(path, data)


# ---------------------------------------------------------------- macerator i pyły

DUSTS = {  # metal: (kolor, EN, PL, wejście rudy, wejście surowca, wejście sztabki, sztabka wynikowa)
    "iron": ((216, 200, 188), "Iron", "żelaza", "#c:ores/iron", "#c:raw_materials/iron", "#c:ingots/iron", "minecraft:iron_ingot"),
    "gold": ((250, 214, 70), "Gold", "złota", "#c:ores/gold", "#c:raw_materials/gold", "#c:ingots/gold", "minecraft:gold_ingot"),
    "copper": ((222, 128, 84), "Copper", "miedzi", "#c:ores/copper", "#c:raw_materials/copper", "#c:ingots/copper", "minecraft:copper_ingot"),
    "tin": ((200, 210, 222), "Tin", "cyny", "#c:ores/tin", "#c:raw_materials/tin", "#c:ingots/tin", MODID + ":tin_ingot"),
    "silver": ((238, 242, 250), "Silver", "srebra", "#c:ores/silver", "#c:raw_materials/silver", "#c:ingots/silver", MODID + ":silver_ingot"),
}


def tex_macerator_front(lit):
    """Przód maceratora: lej zasypowy u góry i dwa walce z zębami."""
    base, accent = (100, 104, 112), (60, 90, 200)
    img = tex_machine(base, accent, None)
    d = ImageDraw.Draw(img)
    d.polygon([(3, 2), (12, 2), (10, 5), (5, 5)], fill=(30, 30, 34, 255))          # lej
    d.rectangle([2, 6, 13, 13], fill=(26, 26, 30, 255))
    for cx in (5, 10):                                                             # walce
        d.ellipse([cx - 3, 7, cx + 3, 13], fill=(120, 124, 132, 255) if lit else (88, 90, 96, 255))
        for dx, dy in ((0, -3), (3, 0), (0, 3), (-3, 0)):
            d.point((cx + dx, 10 + dy), fill=(200, 200, 210, 255) if lit else (150, 150, 158, 255))
    if lit:
        d.line([(7, 9), (8, 11)], fill=(230, 230, 236, 255))
    d.rectangle([3, 14, 12, 14], fill=accent + (255,))
    return img


def gen_macerator():
    M = MODID + ":"
    base, accent = (100, 104, 112), (60, 90, 200)
    save_png(tex_macerator_front(False), "block", "macerator_front")
    save_png(tex_macerator_front(True), "block", "macerator_front_on")
    save_png(tex_machine(base, accent, "stripe"), "block", "macerator_side")
    top = tex_machine(shade(base, 0.92), accent, "hole")
    save_png(top, "block", "macerator_top")
    for lit, tex in (("", "macerator_front"), ("_on", "macerator_front_on")):
        write_json(os.path.join(ASSETS, "models", "block", f"macerator{lit}.json"), {
            "parent": "minecraft:block/orientable",
            "textures": {"top": f"{MODID}:block/macerator_top", "front": f"{MODID}:block/{tex}",
                         "side": f"{MODID}:block/macerator_side"}})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit, val in (("", "false"), ("_on", "true")):
            v = {"model": f"{MODID}:block/macerator{lit}"}
            if y:
                v["y"] = y
            variants[f"facing={facing},lit={val}"] = v
    write_json(os.path.join(ASSETS, "blockstates", "macerator.json"), {"variants": variants})
    item_model("macerator", f"{MODID}:block/macerator")
    simple_loot("macerator")
    shaped("macerator", ["FFF", "GMG", "IWI"],
           {"F": "minecraft:flint", "G": M + "iron_gear", "M": M + "power_machine_casing", "I": "minecraft:iron_ingot",
            "W": M + "power_alloy_wire"}, M + "macerator")
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "macerator" not in data["values"]:
        data["values"].append(M + "macerator")
    write_json(path, data)

    def mac(name, ingredient, result, count=1, bonus=None, bonus_count=1, chance=0.0, time=200):
        r = {"type": M + "macerating",
             "ingredient": {"tag": ingredient[1:]} if ingredient.startswith("#") else {"item": ingredient},
             "result": {"id": result, "count": count}, "time": time}
        if bonus:
            r["bonus"] = {"id": bonus, "count": bonus_count}
            r["bonus_chance"] = chance
        write_json(os.path.join(DATA, "recipe", f"macerating_{name}.json"), r)

    en, pl = {}, {}
    for metal, (color, en_n, pl_n, ore, raw, ingot, ingot_out) in DUSTS.items():
        dust = f"{metal}_dust"
        save_png(tex_dust(color), "item", dust)
        generated_item(dust)
        en[f"item.{MODID}.{dust}"] = f"{en_n} Dust"
        pl[f"item.{MODID}.{dust}"] = f"Pył {pl_n}"
        write_json(os.path.join(RES, "data", "c", "tags", "item", "dusts", f"{metal}.json"), {"values": [M + dust]})
        for kind, t in (("smelting", 200), ("blasting", 100)):
            write_json(os.path.join(DATA, "recipe", f"{metal}_ingot_from_dust_{kind}.json"), {
                "type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": M + dust},
                "result": {"id": ingot_out}, "experience": 0.3, "cookingtime": t})
        mac(f"{metal}_ore", ore, M + dust, 2, M + dust, 1, 0.1, 200)
        mac(f"raw_{metal}", raw, M + dust, 2, M + dust, 1, 0.1, 200)
        mac(f"{metal}_ingot", ingot, M + dust, 1, time=100)
    # c:dusts zbiorczo
    path = os.path.join(RES, "data", "c", "tags", "item", "dusts.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    for metal in DUSTS:
        if f"#c:dusts/{metal}" not in data["values"]:
            data["values"].append(f"#c:dusts/{metal}")
    write_json(path, data)

    # Inne mielenie
    mac("cobblestone", "#c:cobblestones/normal", "minecraft:gravel", time=100)
    mac("stone", "minecraft:stone", "minecraft:cobblestone", time=100)
    mac("gravel", "minecraft:gravel", "minecraft:sand", bonus="minecraft:flint", chance=0.1, time=100)
    mac("sandstone", "minecraft:sandstone", "minecraft:sand", 4, time=100)
    mac("glowstone", "minecraft:glowstone", "minecraft:glowstone_dust", 4, time=100)
    mac("bone", "minecraft:bone", "minecraft:bone_meal", 6, time=100)
    mac("blaze_rod", "minecraft:blaze_rod", "minecraft:blaze_powder", 4, time=100)
    mac("wool", "#minecraft:wool", "minecraft:string", 4, time=100)
    mac("sugar_cane", "minecraft:sugar_cane", "minecraft:sugar", 2, time=100)
    mac("coal_ore", "#c:ores/coal", "minecraft:coal", 2, bonus="minecraft:coal", chance=0.25)
    mac("redstone_ore", "#c:ores/redstone", "minecraft:redstone", 6, bonus="minecraft:redstone", chance=0.5)
    mac("lapis_ore", "#c:ores/lapis", "minecraft:lapis_lazuli", 8)
    mac("quartz_ore", "#c:ores/quartz", "minecraft:quartz", 2, bonus="minecraft:quartz", chance=0.5)
    mac("azurite_ore", "#c:ores/azurite", M + "azurite", 6, bonus=M + "azurite", chance=0.5)

    en.update({"block.redcircuit.macerator": "Macerator"})
    pl.update({"block.redcircuit.macerator": "Macerator"})
    for lang, kv in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(kv)
        write_json(path, data)


# ---------------------------------------------------------------- tartak

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"]


def tex_sawmill_front(lit):
    """Przód tartaku: okrągła piła z zębami w szczelinie stołu."""
    import math
    base, accent = (110, 96, 78), (200, 150, 70)
    img = tex_machine(base, accent, None)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 9, 13, 10], fill=(40, 34, 28, 255))                       # szczelina stołu
    cx, cy, r = 7.5, 8.0, 4.6
    blade = (210, 214, 222) if lit else (168, 172, 180)
    for y in range(16):
        for x in range(16):
            dist = math.hypot(x - cx, y - cy)
            ang = math.atan2(y - cy, x - cx)
            tooth = r + 0.9 * (0.5 + 0.5 * math.cos(ang * 10 + (0.6 if lit else 0)))
            if dist <= tooth and y <= 10:
                c = blade if dist > 1.3 else (70, 70, 76)
                if dist > r:
                    c = shade(blade, 0.8)
                img.putpixel((x, y), c + (255,))
    d.rectangle([3, 13, 12, 13], fill=accent + (255,))
    return img


def gen_sawmill():
    M = MODID + ":"
    base, accent = (110, 96, 78), (200, 150, 70)
    save_png(tex_sawmill_front(False), "block", "sawmill_front")
    save_png(tex_sawmill_front(True), "block", "sawmill_front_on")
    save_png(tex_machine(base, accent, "stripe"), "block", "sawmill_side")
    save_png(tex_machine(shade(base, 0.92), accent, "grid"), "block", "sawmill_top")
    for lit, tex in (("", "sawmill_front"), ("_on", "sawmill_front_on")):
        write_json(os.path.join(ASSETS, "models", "block", f"sawmill{lit}.json"), {
            "parent": "minecraft:block/orientable",
            "textures": {"top": f"{MODID}:block/sawmill_top", "front": f"{MODID}:block/{tex}",
                         "side": f"{MODID}:block/sawmill_side"}})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit, val in (("", "false"), ("_on", "true")):
            v = {"model": f"{MODID}:block/sawmill{lit}"}
            if y:
                v["y"] = y
            variants[f"facing={facing},lit={val}"] = v
    write_json(os.path.join(ASSETS, "blockstates", "sawmill.json"), {"variants": variants})
    item_model("sawmill", f"{MODID}:block/sawmill")
    simple_loot("sawmill")
    shaped("sawmill", ["PHP", "GMG", "IWI"],
           {"P": "#minecraft:planks", "H": "#" + M + "handsaws", "G": M + "iron_gear", "M": M + "power_machine_casing",
            "I": "minecraft:iron_ingot", "W": M + "power_alloy_wire"}, M + "sawmill")
    path = os.path.join(MC_DATA, "tags", "block", "mineable", "pickaxe.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    if M + "sawmill" not in data["values"]:
        data["values"].append(M + "sawmill")
    write_json(path, data)

    # Trociny: paliwo i papier
    save_png(tex_dust((196, 160, 110)), "item", "sawdust")
    generated_item("sawdust")
    write_json(os.path.join(RES, "data", "neoforge", "data_maps", "item", "furnace_fuels.json"),
               {"values": {M + "sawdust": {"burn_time": 100}}})
    shaped("paper_from_sawdust", ["SSS"], {"S": M + "sawdust"}, "minecraft:paper", 2)
    write_json(os.path.join(RES, "data", "c", "tags", "item", "dusts", "wood.json"), {"values": [M + "sawdust"]})

    def saw(name, ingredient, result, count, chance=1.0, time=100):
        r = {"type": M + "sawing",
             "ingredient": {"tag": ingredient[1:]} if ingredient.startswith("#") else {"item": ingredient},
             "result": {"id": result, "count": count}, "time": time,
             "bonus": {"id": M + "sawdust"}, "bonus_chance": chance}
        write_json(os.path.join(DATA, "recipe", f"sawing_{name}.json"), r)

    for w in WOODS:
        saw(f"{w}_logs", f"#minecraft:{w}_logs", f"minecraft:{w}_planks", 6)
    saw("crimson_stems", "#minecraft:crimson_stems", "minecraft:crimson_planks", 6)
    saw("warped_stems", "#minecraft:warped_stems", "minecraft:warped_planks", 6)
    saw("bamboo_blocks", "#minecraft:bamboo_blocks", "minecraft:bamboo_planks", 3)
    saw("planks", "#minecraft:planks", "minecraft:stick", 3, chance=0.25, time=60)

    texts = {"en_us": {"block.redcircuit.sawmill": "Sawmill", "item.redcircuit.sawdust": "Sawdust"},
             "pl_pl": {"block.redcircuit.sawmill": "Tartak", "item.redcircuit.sawdust": "Trociny"}}
    for lang, kv in texts.items():
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data.update(kv)
        write_json(path, data)


def gen_jei():
    info = {
        "thermopile": ("Generates power from a temperature difference. Each hot neighbour (lava, fire, magma, lit campfire) paired with a cold one (water, ice, snow) gives 8 FE/t, up to 24 FE/t.",
                       "Wytwarza energię z różnicy temperatur. Każda para sąsiadów: gorący (lawa, ogień, magma, zapalone ognisko) + zimny (woda, lód, śnieg) daje 8 FE/t, maks. 24 FE/t."),
        "generator": ("Needs a rotor mounted on its front: right-click with a Windmill or Water Wheel. Shift + right-click with an empty hand removes it.",
                              "Wymaga wirnika z przodu: PPM wiatrakiem albo kołem wodnym. Shift + PPM pustą ręką zdejmuje wirnik."),
        "windmill": ("Mount on a Generator. Needs a free 5x5 area in front, open to the sky. 6 FE/t at sea level, up to 32 FE/t high up; x1.5 in rain, x2 in thunderstorms.",
                     "Montuj na generatorze. Wymaga pustego pola 5x5 przed nim, pod gołym niebem. 6 FE/t nad poziomem morza, do 32 FE/t wysoko; x1,5 w deszczu, x2 w burzy."),
        "water_wheel": ("Mount on a Generator. Flowing water in the 3x3 area in front gives 4 FE/t per block, still water 1 FE/t (max 24 FE/t).",
                        "Montuj na generatorze. Płynąca woda w polu 3x3 przed nim daje 4 FE/t za blok, stojąca 1 FE/t (maks. 24 FE/t)."),
        "pump": ("Place it by clicking the fluid source (tank or lake): it sucks from the back and pushes to the front (another mod's tank/pipe, or pours the fluid out). 200 FE per bucket. A redstone signal stops it.",
                 "Stawiaj, klikając źródło płynu (zbiornik albo jezioro): zasysa z tyłu i tłoczy do przodu (zbiornik/rura innego moda albo wylewa płyn). 200 FE za wiadro. Sygnał redstone ją zatrzymuje."),
        "fluid_pipe": ("Carries fluids between the Pump and tanks/machines (also from other mods). Pipes level out among themselves and push fluid into every connected tank except the one it came from. 1000 mB per pipe, up to 250 mB/t per side.",
                       "Przesyła płyny między pompą a zbiornikami/maszynami (także z innych modów). Rury wyrównują poziom między sobą i wypychają płyn do każdego podłączonego zbiornika poza tym, z którego przyszedł. 1000 mB na rurę, do 250 mB/t na stronę."),
        "screwdriver": ("Right-click a block to rotate it: gates, machines, pumps, extractors and vanilla blocks with a direction (e.g. repeaters, observers, hoppers, logs). Shift + right-click rotates the other way; on a logic gate it picks the gate up together with its settings (time, counter maximum, disabled inputs, mute). Does not open GUIs or change the timer interval. Never wears out.",
                        "PPM na bloku obraca go: bramki, maszyny, pompy, ekstraktory i bloki vanilla z kierunkiem (np. przekaźniki, obserwatory, leje, kłody). Shift + PPM obraca w drugą stronę, a na bramce logicznej zbiera ją razem z ustawieniami (czas, maksimum licznika, wyłączone wejścia, wyciszenie). Nie otwiera GUI i nie zmienia czasu timera. Nie zużywa się."),
        "wire_cutters": ("Crafting tool for making fine wire: ingot + wire cutters = fine wire (copper or iron). Stays in the grid and loses 1 durability. Made from steel; repair with a steel ingot.",
                      "Narzędzie do robienia cienkiego drutu: sztabka + nożyce do drutu = cienki drut (miedziany lub żelazny). Zostają w siatce i tracą 1 punkt wytrzymałości. Robi się je ze stali i naprawia sztabką stali."),
        "battery": ("Portable Azurity storage (50,000 FE). Put it in the battery slot of the Electric Furnace or Power Alloy Furnace to power them. In the Battery Box: top slot charges, bottom slot discharges (or right-click / Shift + right-click the box). Works with other mods' FE chargers.",
                    "Przenośny magazyn Azurity (50 000 FE). Włożona do slotu baterii pieca elektrycznego lub elektrycznego pieca do stopów zasila je. W akumulatorze: górny slot ładuje, dolny rozładowuje (albo PPM / Shift + PPM na akumulatorze). Działa z ładowarkami FE innych modów."),
        "encased_wire": ("Wire in a hard casing that runs through the middle of the block, like a pipe. Connects to wires of the same kind (also wall-mounted ones) and works exactly like them. 8 wires around a smooth stone slab = 8 encased wires; a encased wire alone in the grid gives the plain wire back.",
                          "Przewód w twardej obudowie, biegnący środkiem bloku jak rura. Łączy się z przewodami tego samego rodzaju (także przyklejonymi do ścian) i działa dokładnie jak one. 8 przewodów wokół płyty z gładkiego kamienia = 8 przewodów w obudowie; sam przewód w obudowie w siatce oddaje zwykły przewód."),
        "requester": ("Place it by clicking a tube: the back connects to the tube network, the front is the output. On a redstone pulse it searches every inventory connected to that network, pulls an item matching the filter (empty filter = anything) and the item travels through the tubes to the Requester, which pushes it out the front. Filter slots are used in turn; the stack size in a slot sets how many items to fetch. 200 FE per pull. Right-click: filter, Shift + right-click: energy.",
                      "Stawiaj, klikając na rurę: tył łączy się z siecią rur, przód to wyjście. Na impuls redstone przeszukuje wszystkie inwentarze podłączone do tej sieci, wyciąga przedmiot pasujący do filtra (pusty filtr = dowolny) i przedmiot jedzie rurami do Requestera, który wypycha go z przodu. Sloty filtra są używane po kolei; liczba sztuk w slocie określa, ile pobrać. 200 FE za pobranie. PPM: filtr, Shift + PPM: energia."),
        "manager": ("Place it by clicking the inventory to manage; the front connects to a tube network. The filter lists items with amounts: missing items are ordered from inventories on the network, items not on the list are sent back to the network. 100 FE per order. Shift + right-click: energy.",
                    "Stawiaj, klikając pilnowany inwentarz; przód łączy się z siecią rur. Filtr to lista przedmiotów z ilościami: brakujące są zamawiane z inwentarzy w sieci, a przedmioty spoza listy odsyłane do sieci. 100 FE za zamówienie. Shift + PPM: energia."),
        "regulator": ("Moves items from the inventory behind into the inventory in front until it holds the amounts set in the filter. Emits a redstone signal when everything is supplied.",
                      "Przenosi przedmioty z inwentarza z tyłu do inwentarza z przodu, aż ten będzie zawierał ilości z filtra. Gdy wszystko jest uzupełnione, wysyła sygnał redstone."),
        "relay": ("9-slot buffer: accepts items from every side except the front and keeps pushing them out the front (tube, inventory or the ground).",
                  "Bufor 9 slotów: przyjmuje przedmioty z każdej strony poza przodem i stale wypycha je przodem (rura, inwentarz albo ziemia)."),
        "item_detector": ("Put it into a tube line (back = in, front = out). Every stack passing through sends a short redstone pulse.",
                          "Wstaw w linię rur (tył = wejście, przód = wyjście). Każdy przejeżdżający stos wysyła krótki impuls redstone."),
        "igniter": ("While powered, lights a fire in front of it and keeps it burning; turning the signal off puts the fire out.",
                    "Gdy jest zasilony, zapala ogień przed sobą i go podtrzymuje; wyłączenie sygnału gasi ogień."),
        "assembler": ("Controlled by bundled cable: turning channel N on places the block from slot N in front, turning it off collects that block back into slot N. A plain redstone pulse places the next block or collects the one in front.",
                      "Sterowany kablem wiązkowym: włączenie kanału N stawia przed nim blok ze slotu N, wyłączenie zbiera go z powrotem do slotu N. Zwykły impuls redstone stawia kolejny blok albo zbiera ten przed maszyną."),
        "ejector": ("9 slots; on each redstone pulse pushes one stack out the front (slots in turn).",
                    "9 slotów; przy każdym impulsie redstone wypycha przodem jeden stos (sloty po kolei)."),
        "deployer": ("9 slots; on a redstone pulse uses an item on the block in front like a player's right-click (places blocks, bone meal, buckets, shears...).",
                     "9 slotów; na impuls redstone używa przedmiotu na bloku przed sobą jak gracz PPM (stawia bloki, mączka kostna, wiadra, nożyce...)."),
        "buffer": ("18-slot storage accessible from every side by tubes and hoppers. Comparators read how full it is.",
                   "Magazyn 18 slotów dostępny z każdej strony dla rur i lejków. Komparator odczytuje zapełnienie."),
        "block_breaker": ("On a redstone pulse breaks the block in front (like a diamond pickaxe); the drops go out the back - into a tube, inventory or onto the ground.",
                          "Na impuls redstone rozbija blok przed sobą (jak diamentowy kilof); dropy wychodzą tyłem - do rury, inwentarza albo na ziemię."),
        "lamp": ("Lights up with a redstone signal (also from RedCircuit wires next to it). The inverted lamp works the other way round: it is lit without a signal. Lamp + redstone torch switches between normal and inverted.",
                 "Świeci przy sygnale redstone (także z przewodów RedCircuit obok). Lampa odwrócona działa odwrotnie: świeci bez sygnału. Lampa + pochodnia redstone zamienia zwykłą na odwróconą i z powrotem."),
        "counter": ("Left input (rising edge) counts up, right counts down. The front is on at the maximum, the back is on at zero. Right-click opens settings: maximum (1-1024) and reset. A comparator reads the current count.",
                    "Lewe wejście (zbocze) liczy w górę, prawe w dół. Przód świeci, gdy licznik osiągnie maksimum, tył - gdy jest na zerze. PPM otwiera ustawienia: maksimum (1-1024) i zerowanie. Komparator odczytuje bieżący stan."),
        "sequencer": ("Outputs a signal on each side in turn: front, right, back, left - one step per set time. A signal at the back pauses it. Right-click opens settings (step time).",
                      "Podaje sygnał kolejno na każdą stronę: przód, prawo, tył, lewo - jeden krok co ustawiony czas. Sygnał z tyłu go wstrzymuje. PPM otwiera ustawienia (czas kroku)."),
        "state_cell": ("A pulse at the back starts the countdown: the right side is on while counting, then the front gives a short pulse. Right-click opens settings (time).",
                       "Impuls z tyłu uruchamia odliczanie: prawa strona świeci w trakcie, a po upływie czasu przód daje krótki impuls. PPM otwiera ustawienia (czas)."),
        "synchronizer": ("Waits for a pulse on both the left and the right input (in any order), then gives a pulse at the front. A signal at the back resets it.",
                         "Czeka na impuls na lewym i prawym wejściu (w dowolnej kolejności), potem daje impuls z przodu. Sygnał z tyłu kasuje oczekiwanie."),
        "restriction_tube": ("Works like a normal tube, but routing treats it as very long: items only take it when there is no other way to the target. Restriction tube alone in the grid gives the tube back.",
                             "Działa jak zwykła rura, ale przy wyznaczaniu trasy liczy się jak bardzo długa: przedmioty jadą nią tylko wtedy, gdy nie ma innej drogi. Sama w siatce oddaje zwykłą rurę."),
        "accelerator": ("A tube segment powered with FE: every stack passing through is boosted to 4x speed and then slows down gradually over the next tubes. 20 FE per stack (configurable). Right-click shows energy.",
                        "Odcinek rury zasilany energią (FE): każdy przejeżdżający stos przyspiesza do 4x, potem stopniowo zwalnia na kolejnych rurach. 20 FE za stos (w konfiguracji). PPM pokazuje energię."),
        "wireless_transmitter": ("Place it on a block. Right-click sets the frequency (1-5000). While it receives a redstone signal it transmits on that frequency - everywhere, also in other dimensions and unloaded chunks.",
                                 "Stawiaj na bloku. PPM ustawia częstotliwość (1-5000). Gdy dostaje sygnał redstone, nadaje na tej częstotliwości - wszędzie, także w innych wymiarach i niezaładowanych chunkach."),
        "wireless_receiver": ("Right-click sets the frequency. Outputs a redstone signal (15) on all sides while any transmitter or remote is active on its frequency.",
                              "PPM ustawia częstotliwość. Daje sygnał redstone (15) na wszystkie strony, gdy na jego częstotliwości nadaje dowolny nadajnik albo pilot."),
        "wireless_remote": ("Hold right-click to transmit on its frequency (like holding a button). Shift + right-click sets the frequency.",
                            "Trzymanie PPM nadaje na jego częstotliwości (jak trzymany przycisk). Shift + PPM ustawia częstotliwość."),
        "fluid_tank": ("Holds 16 buckets of one fluid. Right-click with a bucket to fill or empty it, with an empty hand to see the contents. Pipes and pumps connect from every side; fluid flows down into a tank placed below. Keeps its contents when broken. Comparators read the level, glowing fluids (lava) give light.",
                       "Mieści 16 wiader jednej cieczy. PPM wiadrem nalewa lub zabiera ciecz, pustą ręką pokazuje zawartość. Rury i pompy łączą się z każdej strony; ciecz spływa do zbiornika postawionego niżej. Po rozbiciu zachowuje zawartość. Komparator odczytuje poziom, świecące ciecze (lawa) dają światło."),
        "sawmill": ("Cuts logs and stems into 6 planks (instead of 4) plus sawdust, and planks into 3 sticks. Uses 12 FE/t; has a battery slot. Sawdust burns in furnaces and 3 sawdust make 2 paper. Custom recipes: type redcircuit:sawing.",
                    "Tnie kłody i trzony na 6 desek (zamiast 4) plus trociny, a deski na 3 patyki. Zużywa 12 FE/t, ma slot baterii. Trociny palą się w piecu, a 3 trociny dają 2 papiery. Własne receptury: typ redcircuit:sawing."),
        "macerator": ("Grinds ores and raw ores into 2 dusts (10% chance of a third), ingots into 1 dust, and also cobblestone, gravel, bones, blaze rods, wool and more. Dusts smelt into ingots. Uses 16 FE/t; has a battery slot. Hoppers insert from any side and take results out. Custom recipes: type redcircuit:macerating.",
                      "Miele rudy i surowe rudy na 2 pyły (10% szans na trzeci), sztabki na 1 pył, a także bruk, żwir, kości, pręty płomienia, wełnę i inne. Pyły przetapia się na sztabki. Zużywa 16 FE/t, ma slot baterii. Lejki wkładają z każdej strony i wyjmują wyniki. Własne receptury: typ redcircuit:macerating."),
        "solid_fuel_generator": ("Burns anything that works in a furnace (coal, wood, lava buckets...) and makes 20 FE/t while burning: one coal gives 32,000 FE. Pushes energy into neighbouring blocks (400 FE/t per side). Stops burning when its 20,000 FE buffer is full or when it gets a redstone signal. Hoppers insert fuel from the top/sides; empty buckets come out of the bottom.",
                                 "Spala wszystko, co pali się w piecu (węgiel, drewno, wiadra lawy...) i podczas spalania daje 20 FE/t: jeden węgiel to 32 000 FE. Oddaje energię do sąsiednich bloków (400 FE/t na stronę). Przestaje palić, gdy bufor 20 000 FE jest pełny albo gdy dostaje sygnał redstone. Lejki wkładają paliwo z góry/boków, puste wiadra wychodzą od dołu."),
        "charger": ("Charges up to 4 items with FE (batteries, electric tools, items from other mods) at 1000 FE/t each. Power it with power wire. Hoppers insert from the top/sides and take out fully charged items from the bottom.",
                    "Ładuje do 4 przedmiotów z energią (baterie, narzędzia elektryczne, przedmioty innych modów) po 1000 FE/t. Zasilanie przewodem energetycznym. Lejki wkładają z góry/boków, a od dołu wyjmują w pełni naładowane."),
        "electric_drill": ("Pickaxe + shovel at diamond level that uses energy instead of durability: 50 FE per block (100,000 FE). Without energy it digs very slowly. Shift + right-click switches between 1x1 and 3x3 mining (3x3 is slower and uses energy for every block; hold Shift while mining to break a single block).",
                           "Kilof + łopata na poziomie diamentu, zużywa energię zamiast wytrzymałości: 50 FE za blok (100 000 FE). Bez energii kopie bardzo wolno. Shift + PPM przełącza kopanie 1x1 / 3x3 (3x3 jest wolniejsze i zużywa energię za każdy blok; Shift podczas kopania rozbija tylko jeden blok)."),
        "electric_chainsaw": ("Axe that also cuts leaves, wool and cobwebs; uses 50 FE per block, strong attack. Without energy it is very slow.",
                              "Siekiera, która tnie też liście, wełnę i pajęczyny; 50 FE za blok, mocny atak. Bez energii bardzo wolna."),
        "multimeter": ("Right-click a block: redstone signal (in/out, wire level), bundled cable channels, stored energy and fluids are printed in chat.",
                       "PPM na bloku: w czacie pojawia się sygnał redstone (wejście/wyjście, poziom przewodu), kanały kabla wiązkowego, energia i ciecze."),
        "solar_panel": ("Generates 10 FE/t during the day under open sky (less in rain).",
                        "Wytwarza 10 FE/t w dzień pod gołym niebem (mniej w deszczu)."),
        "tube": ("Items find the nearest inventory that accepts them. Right-click with dye to paint: painted tubes only carry items of that colour. Shift + right-click with an empty hand removes paint.",
                 "Przedmioty same szukają najbliższego inwentarza. PPM barwnikiem maluje rurę - pomalowana przewozi tylko przedmioty w tym kolorze. Shift + PPM pustą ręką zmywa farbę."),
        "sorting_machine": ("Row 1: dye (colour given to items), rows 2-3: item patterns for that column. On a redstone pulse sends a matching stack into the tube in front.",
                            "Rząd 1: barwnik (kolor nadawany przedmiotom), rzędy 2-3: wzorce dla tej kolumny. Na impuls redstone wysyła pasujący stos do rury z przodu."),
        "blueprint_table": ("Crafting table that keeps its 3x3 grid and has 18 storage slots; ingredients are taken from storage first. Put a Blank Plan in the left slot and press the button to save the recipe.",
                          "Stół rzemieślniczy, który pamięta siatkę 3x3 i ma 18 slotów magazynu - składniki są brane najpierw z magazynu. Włóż pusty plan do slotu po lewej i kliknij przycisk, aby zapisać recepturę."),
        "blank_plan": ("Write a recipe onto it in the Blueprint Table.",
                       "Zapisz na nim recepturę w Stole kreślarskim."),
        "handsaw": ("Crafting tool: stays in the grid after crafting and loses 1 durability. Used to craft the Sawmill.",
                    "Narzędzie do wytwarzania: zostaje w siatce i traci 1 punkt wytrzymałości. Składnik tartaku."),
    }
    for lang, idx in (("en_us", 0), ("pl_pl", 1)):
        path = os.path.join(ASSETS, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for k, v in info.items():
            data[f"jei.{MODID}.info.{k}"] = v[idx]
        write_json(path, data)


def gen_tag_names():
    """Nazwy tagów (tag.item.<ns>.<ścieżka>) i kategorii EMI - pokazywane przez EMI/JEI."""
    mats = {"azurite": ("Azurite", "azurytu"), "silver": ("Silver", "srebra"), "tin": ("Tin", "cyny"),
            "wood": ("Wood", "drewna"), "copper": ("Copper", "miedzi"), "gold": ("Gold", "złota"),
            "iron": ("Iron", "żelaza"), "brass": ("Brass", "mosiądzu"), "diamond": ("Diamond", "diamentu"),
            "netherite": ("Netherite", "netherytu"), "stone": ("Stone", "kamienia"),
            "power_alloy": ("Power Alloy", "stopu energetycznego"), "signal_alloy": ("Signal Alloy", "stopu sygnałowego"),
            "steel": ("Steel", "stali")}
    groups = {"dusts": ("{} Dusts", "Pyły {}"), "gears": ("{} Gears", "Zębatki z {}"), "ingots": ("{} Ingots", "Sztabki {}"),
              "nuggets": ("{} Nuggets", "Bryłki {}"), "ores": ("{} Ores", "Rudy {}"),
              "raw_materials": ("Raw {}", "Surowe rudy {}"), "storage_blocks": ("{} Storage Blocks", "Bloki {}"),
              "wires": ("{} Wires", "Druty z {}")}
    names = {"c.gears": ("Gears", "Zębatki"), "c.wires": ("Wires", "Druty"),
             "redcircuit.gates": ("Logic Gates", "Bramki logiczne"), "redcircuit.handsaws": ("Handsaws", "Piły ręczne"),
             "redcircuit.insulated_wires": ("Insulated Wires", "Przewody izolowane"), "redcircuit.lamps": ("Lamps", "Lampy")}
    base = os.path.join(RES, "data", "c", "tags", "item")
    for group, (en_f, pl_f) in groups.items():
        d = os.path.join(base, group)
        if not os.path.isdir(d):
            continue
        for f in sorted(os.listdir(d)):
            mat = f[:-5]
            if mat in mats:
                names[f"c.{group}.{mat}"] = (en_f.format(mats[mat][0]), pl_f.format(mats[mat][1]))
    lang_dir = os.path.join(ASSETS, "lang")
    for lang, idx in (("en_us", 0), ("pl_pl", 1)):
        path = os.path.join(lang_dir, lang + ".json")
        with open(path, encoding="utf-8") as fh:
            data = json.load(fh)
        for key, val in names.items():
            data["tag.item." + key] = val[idx]
        for cat, block in (("alloying", "alloy_furnace"), ("macerating", "macerator"), ("sawing", "sawmill")):
            data[f"emi.category.{MODID}.{cat}"] = data.get(f"block.{MODID}.{block}", cat)
        write_json(path, data)


if __name__ == "__main__":
    gen_textures()
    gen_models()
    gen_loot()
    gen_tags()
    gen_recipes()
    gen_lang()
    gen_ores()
    gen_storage()
    gen_project()
    gen_alloy()
    gen_handsaws()
    gen_gears()
    gen_motor()
    gen_generators()
    gen_pump()
    gen_fluid_pipe()
    gen_screwdriver()
    gen_wire_cutters()
    gen_coil()
    gen_battery()
    gen_encased()
    gen_requester()
    gen_machines()
    gen_lamps()
    gen_jade()
    gen_tube_variants()
    gen_wireless()
    gen_fluid_tank()
    gen_electric()
    gen_fuel_generator()
    gen_macerator()
    gen_sawmill()
    gen_gate_messages()
    gen_common_tags()
    gen_jei()
    from gen_patchouli import gen_patchouli
    gen_patchouli(ROOT, write_json)
    from gen_advancements import gen_advancements
    gen_advancements(ROOT, write_json)
    gen_tag_names()
    print("OK - zasoby wygenerowane w", os.path.abspath(RES))
