"""Osiągnięcia RedCircuit (EN + PL). Wywoływane z gen_assets.py.

Pliki: data/redcircuit/advancement/*.json, tagi pomocnicze i tłumaczenia.
"""
import json
import os

M = "redcircuit:"
DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
GATES = ["not", "buffer", "and", "nand", "or", "nor", "xor", "xnor", "rs_latch", "toggle", "pulse",
         "timer", "randomizer", "counter", "sequencer", "state_cell", "synchronizer"]


def has(items, components=None):
    """Kryterium: przedmiot w ekwipunku. items: id, lista id albo '#tag'."""
    pred = {"items": items}
    if components:
        pred["components"] = components
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [pred]}}


# id, rodzic, ikona, ramka, ukryte, kryteria, EN tytuł, EN opis, PL tytuł, PL opis, nagroda xp
ADV = [
    ("root", None, M + "azurite", "task", False,
     {"ore": has([M + "raw_tin", M + "raw_silver", M + "azurite"])},
     "RedCircuit", "Find a tin, silver or azurite ore",
     "RedCircuit", "Znajdź rudę cyny, srebra albo azurytu", 0),

    # --- stopy i okablowanie
    ("alloy_furnace", "root", M + "alloy_furnace", "task", False, {"c": has(M + "alloy_furnace")},
     "Hot Mixture", "Build an Alloy Furnace", "Gorąca mieszanka", "Zbuduj piec do stopów", 0),
    ("signal_alloy", "alloy_furnace", M + "signal_alloy_ingot", "task", False, {"c": has(M + "signal_alloy_ingot")},
     "Signal Boost", "Smelt copper and redstone into Signal Alloy", "Wzmocnienie sygnału",
     "Stop miedź z redstone w stop sygnałowy", 0),
    ("red_wire", "signal_alloy", M + "signal_alloy_wire", "task", False, {"c": has(M + "signal_alloy_wire")},
     "No Signal Lost", "Craft Signal Alloy Wire", "Bez strat", "Zrób przewód ze stopu sygnałowego", 0),
    ("insulated", "red_wire", M + "insulated_wire_red", "task", False, {"c": has("#" + M + "insulated_wires")},
     "Colour Coded", "Craft an Insulated Wire", "Kolorowe kable", "Zrób przewód izolowany", 0),
    ("bundled", "insulated", M + "bundled_cable", "goal", False, {"c": has(M + "bundled_cable")},
     "Sixteen Lanes", "Craft a Bundled Cable", "Szesnaście pasów", "Zrób kabel wiązkowy", 0),
    ("all_insulated", "insulated", M + "insulated_wire_lime", "challenge", False,
     {d: has(M + f"insulated_wire_{d}") for d in DYES},
     "Full Spectrum", "Collect Insulated Wire in all 16 colours", "Pełne spektrum",
     "Zbierz przewody izolowane we wszystkich 16 kolorach", 50),

    # --- logika
    ("first_gate", "red_wire", M + "and_gate", "task", False, {"c": has("#" + M + "gates")},
     "Logical Conclusion", "Craft a logic gate", "Logiczny wniosek", "Zrób bramkę logiczną", 0),
    ("signal_machine_casing", "first_gate", M + "signal_machine_casing", "task", False,
     {"c": has(M + "signal_machine_casing")},
     "Signal Box", "Craft a Signal Machine Casing", "Nastawnia", "Zrób obudowę maszyny sygnałowej", 0),
    ("timer", "first_gate", M + "timer_gate", "task", False, {"c": has(M + "timer_gate")},
     "Tick Tock", "Craft a Timer", "Tik-tak", "Zrób zegar", 0),
    ("counter", "timer", M + "counter_gate", "task", False, {"c": has(M + "counter_gate")},
     "One, Two, Many", "Craft a Counter", "Raz, dwa, dużo", "Zrób licznik", 0),
    ("all_gates", "first_gate", M + "sequencer_gate", "challenge", False,
     {g: has(M + f"{g}_gate") for g in GATES},
     "Integrated Circuit", "Collect every kind of logic gate", "Układ scalony",
     "Zbierz wszystkie rodzaje bramek logicznych", 100),
    ("lamp", "red_wire", M + "lamp_yellow", "task", False, {"c": has("#" + M + "lamps")},
     "Let There Be Light", "Craft a Lamp", "Niech stanie się światło", "Zrób lampę", 0),
    ("wireless", "red_wire", M + "wireless_transmitter", "goal", False,
     {"c": has([M + "wireless_transmitter", M + "wireless_receiver"])},
     "Look, No Wires!", "Craft a wireless transmitter or receiver", "Patrz, bez kabli!",
     "Zrób nadajnik albo odbiornik bezprzewodowy", 0),
    ("remote", "wireless", M + "wireless_remote", "task", False, {"c": has(M + "wireless_remote")},
     "Remote Control", "Craft a Wireless Remote", "Zdalne sterowanie", "Zrób pilot bezprzewodowy", 0),

    # --- Azurity
    ("power_alloy", "alloy_furnace", M + "power_alloy_ingot", "task", False, {"c": has(M + "power_alloy_ingot")},
     "Full Power", "Smelt silver and azurite into Power Alloy", "Pełna moc",
     "Stop srebro z azurytem w stop energetyczny", 0),
    ("generator", "power_alloy", M + "solar_panel", "task", False,
     {"c": has([M + "solar_panel", M + "thermopile", M + "generator", M + "solid_fuel_generator"])},
     "Power Plant", "Build an Azurity generator", "Elektrownia", "Zbuduj generator Azurity", 0),
    ("windmill", "generator", M + "windmill", "task", False,
     {"c": has([M + "windmill", M + "water_wheel"])},
     "Renewable", "Craft a Windmill or a Water Wheel", "Odnawialne źródła", "Zrób wiatrak albo koło wodne", 0),
    ("battery_box", "generator", M + "battery_box", "task", False, {"c": has(M + "battery_box")},
     "Stored for Later", "Build a Battery Box", "Na zapas", "Zbuduj akumulator", 0),
    ("full_battery", "battery_box", M + "battery", "goal", False,
     {"c": has(M + "battery", {M + "energy": 50000})},
     "Fully Charged", "Charge a Battery to 50,000 FE", "W pełni naładowana", "Naładuj baterię do 50 000 FE", 0),
    ("motor", "power_alloy", M + "power_circuit", "task", False, {"c": has(M + "power_circuit")},
     "Closed Circuit", "Craft a Power Circuit", "Obwód zamknięty", "Zrób obwód energetyczny", 0),
    ("power_machine_casing", "motor", M + "power_machine_casing", "task", False, {"c": has(M + "power_machine_casing")},
     "Under the Hood", "Craft a Power Machine Casing", "Pod maską", "Zrób obudowę maszyny energetycznej", 0),
    ("drill", "motor", M + "electric_drill", "goal", False, {"c": has(M + "electric_drill")},
     "Hole Punch", "Craft an Electric Drill", "Dziurkacz", "Zrób wiertarkę elektryczną", 0),
    ("pump", "power_machine_casing", M + "pump", "task", False, {"c": has(M + "pump")},
     "Under Pressure", "Build a Pump", "Pod ciśnieniem", "Zbuduj pompę", 0),
    ("tank", "pump", M + "fluid_tank", "task", False, {"c": has(M + "fluid_tank")},
     "Keep It Bottled", "Craft a Fluid Tank", "Zamknięte w zbiorniku", "Zrób zbiornik na ciecz", 0),

    ("wire_cutters", "root", M + "wire_cutters", "task", False, {"c": has(M + "wire_cutters")},
     "Fine Work", "Craft Wire Cutters", "Precyzyjna robota", "Zrób nożyce do drutu", 0),
    ("blueprint_table", "root", M + "blueprint_table", "task", False, {"c": has(M + "blueprint_table")},
     "Blueprint", "Build a Blueprint Table", "Projektant", "Zbuduj stół kreślarski", 0),
    ("screwdriver", "root", M + "screwdriver", "task", False, {"c": has(M + "screwdriver")},
     "Righty-Tighty", "Craft a Screwdriver", "Lewo luzuje, prawo dokręca", "Zrób śrubokręt", 0),

    # --- rury
    ("tube", "root", M + "tube", "task", False, {"c": has(M + "tube")},
     "Series of Tubes", "Craft a Pneumatic Tube", "Rurociąg", "Zrób rurę pneumatyczną", 0),
    ("sorting", "tube", M + "sorting_machine", "goal", False, {"c": has(M + "sorting_machine")},
     "Everything in Its Place", "Build a Sorting Machine", "Wszystko na swoim miejscu", "Zbuduj maszynę sortującą", 0),
    ("requester", "tube", M + "requester", "goal", False, {"c": has(M + "requester")},
     "Fetch!", "Build a Requester", "Aport!", "Zbuduj Requester", 0),
    ("accelerator", "tube", M + "accelerator", "task", False, {"c": has(M + "accelerator")},
     "Express Delivery", "Build an Accelerator", "Przesyłka ekspresowa", "Zbuduj akcelerator", 0),

]


def gen_advancements(root, write_json):
    res = os.path.join(root, "src", "main", "resources")
    data = os.path.join(res, "data", "redcircuit")
    assets = os.path.join(res, "assets", "redcircuit")

    # Tagi pomocnicze
    tags = {
        "gates": [M + f"{g}_gate" for g in GATES],
        "lamps": [M + f"lamp_{d}" for d in DYES] + [M + f"inverted_lamp_{d}" for d in DYES],
    }
    for name, values in tags.items():
        write_json(os.path.join(data, "tags", "item", f"{name}.json"), {"values": values})

    en, pl = {}, {}
    for (aid, parent, icon, frame, hidden, criteria, en_t, en_d, pl_t, pl_d, xp) in ADV:
        key = f"advancements.redcircuit.{aid}"
        display = {
            "icon": {"id": icon},
            "title": {"translate": key + ".title"},
            "description": {"translate": key + ".description"},
            "frame": frame,
            "show_toast": aid != "root",
            "announce_to_chat": aid != "root",
            "hidden": hidden,
        }
        adv = {"display": display, "criteria": criteria}
        if parent is None:
            display["background"] = "minecraft:textures/block/smooth_stone.png"
        else:
            adv["parent"] = M + parent
        if xp:
            adv["rewards"] = {"experience": xp}
        write_json(os.path.join(data, "advancement", f"{aid}.json"), adv)
        en[key + ".title"], en[key + ".description"] = en_t, en_d
        pl[key + ".title"], pl[key + ".description"] = pl_t, pl_d

    for lang, kv in (("en_us", en), ("pl_pl", pl)):
        path = os.path.join(assets, "lang", f"{lang}.json")
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        d.update(kv)
        write_json(path, d)
