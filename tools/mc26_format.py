"""Konwersja wygenerowanych zasobów do formatu Minecraft 26.x (wywoływane na końcu gen_assets.py).

- składniki receptur: {"item": "x"} -> "x", {"tag": "t"} -> "#t" (od 1.21.2),
  receptury pieca do stopów: {"item": "x", "count": n} -> {"ingredient": "x", "count": n},
- definicje modeli przedmiotów assets/<mod>/items/*.json (od 1.21.4), z przebarwieniem sztabek
  (dawniej RegisterColorHandlersEvent.Item).
"""
import glob
import json
import os

TINTS = {
    "tin_ingot": 0xC8D7E6, "raw_tin": 0xC8D7E6, "tin_nugget": 0xC8D7E6, "tin_block": 0xC8D7E6,
    "silver_ingot": 0xF5F8FF, "raw_silver": 0xF5F8FF, "silver_nugget": 0xF5F8FF, "silver_block": 0xF5F8FF,
    "signal_alloy_ingot": 0xFF4A3C, "power_alloy_ingot": 0x5A82FF, "brass_ingot": 0xF2C75C, "steel_ingot": 0x8C949E,
}


def _ing(x):
    if isinstance(x, dict):
        if set(x) == {"item"}:
            return x["item"]
        if set(x) == {"tag"}:
            return "#" + x["tag"]
        return x
    if isinstance(x, list):
        vals = [_ing(e) for e in x]
        if len(vals) == 1:
            return vals[0]
        if all(isinstance(v, str) and not v.startswith("#") for v in vals):
            return vals
        return x
    return x


def convert_recipe(d):
    t = d.get("type")
    if t == "minecraft:crafting_shaped":
        d["key"] = {k: _ing(v) for k, v in d["key"].items()}
    elif t == "minecraft:crafting_shapeless":
        d["ingredients"] = [_ing(v) for v in d["ingredients"]]
    elif t == "minecraft:smithing_transform":
        for k in ("template", "base", "addition"):
            d[k] = _ing(d[k])
    elif t == "redcircuit:alloying":
        out = []
        for e in d["ingredients"]:
            if "ingredient" in e:
                out.append(e)
                continue
            c = e.get("count", 1)
            o = {"ingredient": _ing({k: v for k, v in e.items() if k != "count"})}
            if c != 1:
                o["count"] = c
            out.append(o)
        d["ingredients"] = out
    elif "ingredient" in d:
        d["ingredient"] = _ing(d["ingredient"])
    return d


def convert(res_root, modid):
    for p in glob.glob(os.path.join(res_root, "data", "*", "recipe", "*.json")):
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(convert_recipe(d), f, indent=2)
            f.write("\n")
    assets = os.path.join(res_root, "assets", modid)
    items = os.path.join(assets, "items")
    os.makedirs(items, exist_ok=True)
    for p in glob.glob(os.path.join(assets, "models", "item", "*.json")):
        name = os.path.basename(p)[:-5]
        model = {"type": "minecraft:model", "model": f"{modid}:item/{name}"}
        if name in TINTS:
            model["tints"] = [{"type": "minecraft:constant", "value": TINTS[name]}]
        with open(os.path.join(items, name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"model": model}, f, indent=2)
