#!/usr/bin/env python3
"""Pliki .lang (en_US, pl_PL) dla 1.7.10. Nazwy, które istnieją w wersji 1.21, są brane z jej plików json."""
import json, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "..", "redcircuit", "src", "main", "resources", "assets", "redcircuit", "lang")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "redcircuit", "lang")
EN = json.load(open(os.path.join(SRC, "en_us.json"), encoding="utf-8"))
PL = json.load(open(os.path.join(SRC, "pl_pl.json"), encoding="utf-8"))
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]

entries = []   # (klucz, en, pl)


def add(key, en, pl):
    entries.append((key, en, pl))


def from_new(key_old, key_new):
    """Nazwa z wersji 1.21 (klucz json) pod kluczem 1.7.10."""
    add(key_old, EN[key_new], PL[key_new])


add("itemGroup.redcircuit", "RedCircuit", "RedCircuit")

# --- Etap 1-2: materiały
for i, ore in enumerate(["copper", "tin", "silver", "azurite"]):
    if ore == "copper":
        add("tile.redcircuit.ore.copper.name", "Copper Ore", "Ruda miedzi")
    else:
        from_new(f"tile.redcircuit.ore.{ore}.name", f"block.redcircuit.{ore}_ore")
add("tile.redcircuit.storage.copper.name", "Block of Copper", "Blok miedzi")
for m in ["tin", "silver", "azurite"]:
    from_new(f"tile.redcircuit.storage.{m}.name", f"block.redcircuit.{m}_block")
add("tile.redcircuit.storage.brass.name", "Block of Brass", "Blok mosiądzu")
add("tile.redcircuit.storage.steel.name", "Block of Steel", "Blok stali")
from_new("tile.redcircuit.alloy_furnace.name", "block.redcircuit.alloy_furnace")
from_new("container.redcircuit.alloy_furnace", "block.redcircuit.alloy_furnace")
add("item.redcircuit.copper_ingot.name", "Copper Ingot", "Sztabka miedzi")
for n in ["tin_ingot", "silver_ingot", "brass_ingot", "steel_ingot", "signal_alloy_ingot", "power_alloy_ingot"]:
    from_new(f"item.redcircuit.{n}.name", f"item.redcircuit.{n}")
add("item.redcircuit.copper_nugget.name", "Copper Nugget", "Bryłka miedzi")
for n in ["tin_nugget", "silver_nugget", "iron_dust", "gold_dust", "copper_dust", "tin_dust", "silver_dust", "azurite",
          "stone_gear", "copper_gear", "gold_gear", "iron_gear", "brass_gear", "silver_gear", "diamond_gear"]:
    from_new(f"item.redcircuit.{n}.name", f"item.redcircuit.{n}")

# --- Etap 3: przewody
from_new("tile.redcircuit.signal_alloy_wire.name", "block.redcircuit.signal_alloy_wire")
from_new("tile.redcircuit.bundled_cable.name", "block.redcircuit.bundled_cable")
from_new("tile.redcircuit.encased_signal_alloy_wire.name", "block.redcircuit.encased_signal_alloy_wire")
from_new("tile.redcircuit.encased_bundled_cable.name", "block.redcircuit.encased_bundled_cable")
for c in COLORS:
    from_new(f"tile.redcircuit.insulated_wire.{c}.name", f"block.redcircuit.insulated_wire_{c}")
    from_new(f"tile.redcircuit.encased_insulated_wire.{c}.name", f"block.redcircuit.encased_insulated_wire_{c}")

# --- Etap 4: bramki
GATES = ["not", "buffer", "and", "nand", "or", "nor", "xor", "xnor", "rs_latch", "toggle", "pulse", "timer",
         "randomizer", "counter", "sequencer", "state_cell", "synchronizer"]
for g in GATES:
    from_new(f"tile.redcircuit.{g}_gate.name", f"block.redcircuit.{g}_gate")
for n in ["circuit_plate", "screwdriver"]:
    from_new(f"item.redcircuit.{n}.name", f"item.redcircuit.{n}")
for k in sorted(EN):
    if k.startswith("screen.redcircuit.gate.") or k.startswith("tooltip.redcircuit.screwdriver"):
        from_new(k, k)

# --- Etap 5: lampy i redstone bezprzewodowy
for c in COLORS:
    for block in ("lamp", "inverted_lamp"):
        from_new(f"tile.redcircuit.{block}.{c}.name", f"block.redcircuit.{block}_{c}")
for n in ["wireless_transmitter", "wireless_receiver"]:
    from_new(f"tile.redcircuit.{n}.name", f"block.redcircuit.{n}")
from_new("item.redcircuit.wireless_remote.name", "item.redcircuit.wireless_remote")
for k in ["screen.redcircuit.frequency", "tooltip.redcircuit.frequency", "tooltip.redcircuit.remote_hint"]:
    from_new(k, k)

# --- Etap 6: rury i logistyka
LOGISTICS = ["tube", "restriction_tube", "extractor", "filter", "sorting_machine", "relay", "ejector", "buffer", "deployer",
             "block_breaker", "igniter", "item_detector", "regulator", "assembler", "signal_machine_casing"]
for n in LOGISTICS:
    from_new(f"tile.redcircuit.{n}.name", f"block.redcircuit.{n}")
from_new("item.redcircuit.signal_circuit.name", "item.redcircuit.signal_circuit")
for n in ["relay", "filter", "sorting_machine", "regulator", "buffer", "ejector", "deployer", "assembler"]:
    from_new(f"container.redcircuit.{n}", f"container.redcircuit.{n}")

# --- Etap 7: energia RF (w 1.21 "FE" - tu "RF")
def rf(t):
    return t.replace(" FE", " RF").replace("FE/t", "RF/t").replace("(FE)", "(RF)")


def from_new_rf(key_old, key_new):
    add(key_old, rf(EN[key_new]), rf(PL[key_new]))


ENERGY_BLOCKS = ["power_alloy_wire", "encased_power_alloy_wire", "power_machine_casing", "battery_box", "solar_panel", "thermopile",
                 "generator", "solid_fuel_generator", "electric_furnace", "macerator", "sawmill", "charger",
                 "power_alloy_furnace", "requester", "manager", "accelerator"]
for n in ENERGY_BLOCKS:
    from_new(f"tile.redcircuit.{n}.name", f"block.redcircuit.{n}")
for n in ["battery", "windmill", "water_wheel", "sawdust", "fine_copper_wire", "fine_iron_wire", "copper_coil", "power_circuit",
          "wire_cutters", "stone_handsaw", "copper_handsaw", "gold_handsaw", "iron_handsaw", "silver_handsaw", "diamond_handsaw"]:
    from_new(f"item.redcircuit.{n}.name", f"item.redcircuit.{n}")
for k in ["container.redcircuit.requester", "container.redcircuit.manager", "message.redcircuit.energy", "message.redcircuit.solar",
          "message.redcircuit.battery_item", "message.redcircuit.rotor_installed", "tooltip.redcircuit.battery",
          "tooltip.redcircuit.battery_hint", "tooltip.redcircuit.durability"]:
    from_new_rf(k, k)

# --- Etap 8: płyny
for n in ["pump", "fluid_pipe", "fluid_tank"]:
    from_new(f"tile.redcircuit.{n}.name", f"block.redcircuit.{n}")
for k in ["message.redcircuit.pump", "message.redcircuit.pump_empty", "message.redcircuit.pipe"]:
    from_new_rf(k, k)

# --- Etap 9: narzędzia i stół kreślarski
from_new("tile.redcircuit.blueprint_table.name", "block.redcircuit.blueprint_table")
for n in ["electric_drill", "electric_chainsaw", "multimeter", "blank_plan", "plan"]:
    from_new(f"item.redcircuit.{n}.name", f"item.redcircuit.{n}")
for k in ["item.redcircuit.plan.named", "tooltip.redcircuit.plan.result", "gui.redcircuit.blueprint_table.plan",
          "gui.redcircuit.blueprint_table.storage", "tooltip.redcircuit.electric_tool", "meter.redcircuit.wire_power",
          "meter.redcircuit.signal_in", "meter.redcircuit.signal_out", "meter.redcircuit.channels", "meter.redcircuit.channels_none",
          "meter.redcircuit.energy", "meter.redcircuit.fluid", "message.redcircuit.drill_1x1", "message.redcircuit.drill_3x3",
          "tooltip.redcircuit.drill_mode_1x1", "tooltip.redcircuit.drill_mode_3x3", "tooltip.redcircuit.drill_toggle"]:
    from_new_rf(k, k)

# --- Etap 10: integracje (Waila - teksty z Jade w wersji 1.21)
for k in sorted(EN):
    if k.startswith("jade.redcircuit."):
        from_new_rf("waila." + k[len("jade."):], k)

os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, "en_US.lang"), "w", encoding="utf-8") as f:
    f.write("# RedCircuit - English (generated by tools/gen_lang.py)\n" + "".join(f"{k}={e}\n" for k, e, p in entries))
with open(os.path.join(OUT, "pl_PL.lang"), "w", encoding="utf-8") as f:
    f.write("# RedCircuit - polski (generowane przez tools/gen_lang.py)\n" + "".join(f"{k}={p}\n" for k, e, p in entries))
print("OK -", len(entries), "wpisów")
