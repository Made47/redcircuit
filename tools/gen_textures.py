#!/usr/bin/env python3
"""Tekstury dla wersji 1.7.10 (Python 3 + Pillow).
Część grafik bierze z projektu 1.21 (../redcircuit), resztę rysuje: kamień pod rudami,
bazowa bryłka do przebarwienia. Sztabki i bloki metali używają tekstur z gry przebarwionych w kodzie."""
import os, random, shutil
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "..", "redcircuit", "src", "main", "resources", "assets", "redcircuit", "textures")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "redcircuit", "textures")
ITEMS, BLOCKS = os.path.join(OUT, "items"), os.path.join(OUT, "blocks")
os.makedirs(ITEMS, exist_ok=True)
os.makedirs(BLOCKS, exist_ok=True)


def copy(kind_src, name, kind_dst, new_name=None):
    src = os.path.join(SRC, kind_src, name + ".png")
    if not os.path.exists(src):
        raise SystemExit("brak pliku: " + src)
    shutil.copy(src, os.path.join(OUT, kind_dst, (new_name or name) + ".png"))


def stone():
    rnd = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 125 + rnd.randint(-14, 14)
            if rnd.random() < 0.08:
                v -= 25
            img.putpixel((x, y), (v, v, v, 255))
    return img


def recolor(img, rgb):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a:
                lum = (r * 299 + g * 587 + b * 114) / 1000 / 255
                px[x, y] = tuple(min(255, int(c * lum * 1.3)) for c in rgb) + (a,)
    return out


def ore(overlay):
    base = stone()
    base.alpha_composite(overlay.convert("RGBA").resize((16, 16), Image.NEAREST))
    return base


def nugget_base():
    """Szara bryłka (przebarwiana w kodzie na kolor metalu)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shape = ["................",
             "................",
             "................",
             "......##........",
             ".....####.......",
             "....######......",
             "...##########...",
             "...###########..",
             "..############..",
             "..###########...",
             "...#########....",
             "....######......",
             "................",
             "................",
             "................",
             "................"]
    for y, row in enumerate(shape):
        for x, c in enumerate(row):
            if c == "#":
                edge = y > 0 and shape[y - 1][x] != "#"
                low = y < 15 and shape[y + 1][x] != "#"
                v = 240 if edge else 120 if low else 200 - (x + y) % 3 * 12
                img.putpixel((x, y), (v, v, v, 255))
    return img


# Przedmioty: pyły, zębatki, azuryt (autorskie grafiki z wersji 1.21)
for name in ["iron_dust", "gold_dust", "copper_dust", "tin_dust", "silver_dust", "azurite",
             "stone_gear", "copper_gear", "gold_gear", "iron_gear", "brass_gear", "silver_gear", "diamond_gear"]:
    copy("item", name, "items")
nugget_base().save(os.path.join(ITEMS, "nugget_base.png"))

# Rudy: kamień + nakładka; miedź = nakładka cyny przebarwiona na miedziany
overlays = {o: Image.open(os.path.join(SRC, "block", o + "_ore_overlay.png")) for o in ("tin", "silver", "azurite")}
overlays["copper"] = recolor(overlays["tin"].convert("RGBA"), (230, 130, 80))
for o, ov in overlays.items():
    ore(ov).save(os.path.join(BLOCKS, o + "_ore.png"))

# Bloki
copy("block", "azurite_block", "blocks")
for n in ["alloy_furnace_front", "alloy_furnace_front_on", "alloy_furnace_side", "alloy_furnace_top"]:
    copy("block", n, "blocks")
# Przewody (etap 3): tekstury bloków + płaskie ikony przedmiotów
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
for n in ["signal_alloy_off", "signal_alloy_on", "bundled_cable", "encased_signal_alloy_wire", "encased_bundled_cable"]:
    copy("block", n, "blocks")
for c in COLORS:
    copy("block", "insulated_" + c, "blocks")
    copy("block", "encased_insulated_wire_" + c, "blocks")


def wire_icon(tex_name, thickness):
    """Ikona przedmiotu: ukośny odcinek przewodu z tekstury bloku, z ciemnym obrysem."""
    tex = Image.open(os.path.join(SRC, "block", tex_name + ".png")).convert("RGBA").resize((16, 16), Image.NEAREST)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    half = thickness / 2.0
    for y in range(16):
        for x in range(16):
            d = abs((x - 7.5) + (y - 7.5)) / 1.4142   # odległość od przekątnej (lewy dół - prawy góra)
            if d <= half and 1 <= x <= 14 and 1 <= y <= 14:
                img.putpixel((x, y), tex.getpixel((x, y)))
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3]:
                    r, g, b_, _ = img.getpixel((nx, ny))
                    out.putpixel((x, y), (r // 3, g // 3, b_ // 3, 255))
                    break
    return out


wire_icon("signal_alloy_off", 2.2).save(os.path.join(ITEMS, "signal_alloy_wire.png"))
wire_icon("bundled_cable", 4.5).save(os.path.join(ITEMS, "bundled_cable.png"))
for c in COLORS:
    wire_icon("insulated_" + c, 3.2).save(os.path.join(ITEMS, "insulated_wire_" + c + ".png"))

# Bramki (etap 4): wszystkie warianty góry + bok, płytka obwodu, śrubokręt
for f in sorted(os.listdir(os.path.join(SRC, "block"))):
    if f.startswith("gate_") and f.endswith(".png"):
        copy("block", f[:-4], "blocks")
for n in ["circuit_plate", "screwdriver"]:
    copy("item", n, "items")

# Lampy i redstone bezprzewodowy (etap 5)
for c in COLORS:
    for lit in ("on", "off"):
        copy("block", f"lamp_{c}_{lit}", "blocks")
for n in ["wireless_transmitter_top", "wireless_receiver_top", "wireless_side", "wireless_rod", "wireless_tip_off", "wireless_tip_on"]:
    copy("block", n, "blocks")
copy("item", "wireless_remote", "items")

# Rury i logistyka (etap 6)
copy("block", "tube", "blocks")
copy("block", "restriction_tube", "blocks")
for c in COLORS:
    copy("block", "tube_" + c, "blocks")
for m in ["extractor", "filter", "sorting_machine", "relay", "ejector", "deployer", "block_breaker", "igniter",
          "item_detector", "regulator", "assembler"]:
    for part in ("front", "back", "side"):
        copy("block", f"{m}_{part}", "blocks")
for n in ["buffer_side", "buffer_top", "signal_machine_casing"]:
    copy("block", n, "blocks")
copy("item", "signal_circuit", "items")

# Energia RF (etap 7)
for n in ["power_alloy_wire", "encased_power_alloy_wire", "battery_box_bottom", "battery_box_side", "battery_box_top",
          "charger_side", "charger_side_on", "charger_top", "generator_front", "generator_side", "generator_top",
          "power_machine_casing", "solar_panel_side", "solar_panel_top", "thermopile_side", "thermopile_top", "accelerator"]:
    copy("block", n, "blocks")
for m in ["electric_furnace", "macerator", "sawmill", "solid_fuel_generator", "power_alloy_furnace"]:
    for part in ("front", "front_on", "side", "top"):
        copy("block", f"{m}_{part}", "blocks")
for m in ["requester", "manager"]:
    for part in ("front", "back", "side"):
        copy("block", f"{m}_{part}", "blocks")
for n in ["battery", "windmill", "water_wheel", "sawdust", "fine_copper_wire", "fine_iron_wire", "copper_coil", "power_circuit",
          "wire_cutters", "stone_handsaw", "copper_handsaw", "gold_handsaw", "iron_handsaw", "silver_handsaw", "diamond_handsaw"]:
    copy("item", n, "items")
wire_icon("power_alloy_wire", 2.2).save(os.path.join(ITEMS, "power_alloy_wire.png"))

# Płyny (etap 8)
for n in ["pump_front", "pump_back", "pump_side", "fluid_pipe", "fluid_tank_side", "fluid_tank_top"]:
    copy("block", n, "blocks")

# Narzędzia i stół kreślarski (etap 9)
copy("block", "blueprint_table_side", "blocks")


def blueprint_top():
    """W 1.21 góra to nakładka (cutout) na deski dębowe. W 1.7.10 blok jest nieprzezroczysty, więc dziury
    w nakładce wypełniamy kolorem desek - inaczej widać przypadkowe piksele z atlasu."""
    from PIL import Image
    im = Image.open(os.path.join(SRC, "block", "blueprint_table_top.png")).convert("RGBA")
    rnd = random.Random(11)
    plank = [(162, 130, 78), (156, 127, 78), (184, 148, 95), (143, 116, 70), (175, 141, 87)]
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            base = plank[(y // 4 + rnd.randrange(2)) % len(plank)]
            if a < 255:
                k = a / 255.0
                im.putpixel((x, y), (round(r * k + base[0] * (1 - k)), round(g * k + base[1] * (1 - k)),
                                     round(b * k + base[2] * (1 - k)), 255))
    im.save(os.path.join(OUT, "blocks", "blueprint_table_top.png"))


blueprint_top()
for n in ["electric_drill", "electric_chainsaw", "multimeter", "blank_plan", "plan"]:
    copy("item", n, "items")

print("OK - tekstury w", OUT)
