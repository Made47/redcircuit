#!/usr/bin/env python3
"""Fabric ignoruje "render_type" w modelach JSON - generuje listę bloków dla BlockRenderLayerMap."""
import json, os, glob, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/redcircuit")
OUT = os.path.join(ROOT, "src/main/java/pl/geesio/redcircuit/client/RenderLayers.java")


def model_type(ref, seen=None):
    seen = seen or set()
    if not ref.startswith("redcircuit:") or ref in seen:
        return None
    seen.add(ref)
    path = os.path.join(ASSETS, "models", ref.split(":", 1)[1] + ".json")
    if not os.path.exists(path):
        return None
    m = json.load(open(path))
    if "render_type" in m:
        return m["render_type"]
    if "parent" in m:
        return model_type(m["parent"], seen)
    return None


cutout, mipped, translucent = [], [], []
for f in sorted(glob.glob(os.path.join(ASSETS, "blockstates", "*.json"))):
    block = os.path.basename(f)[:-5]
    refs = set(re.findall(r'"model"\s*:\s*"([^"]+)"', open(f).read()))
    types = {model_type(r) for r in refs} - {None}
    if "minecraft:translucent" in types:
        translucent.append(block)
    elif "minecraft:cutout" in types:
        cutout.append(block)
    elif "minecraft:cutout_mipped" in types:
        mipped.append(block)


def arr(xs):
    return ",\n            ".join(f'"{x}"' for x in xs)


java = f"""package pl.geesio.redcircuit.client;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import pl.geesio.redcircuit.RedCircuit;

/**
 * Warstwy renderowania bloków (Fabric ignoruje "render_type" z modeli JSON).
 * PLIK GENEROWANY przez tools/gen_fabric_layers.py - nie edytuj ręcznie.
 */
public final class RenderLayers {{
    private RenderLayers() {{}}

    private static final String[] CUTOUT = {{
            {arr(cutout)}
    }};

    private static final String[] CUTOUT_MIPPED = {{
            {arr(mipped)}
    }};

    private static final String[] TRANSLUCENT = {{
            {arr(translucent)}
    }};

    public static void register() {{
        put(CUTOUT, RenderType.cutout());
        put(CUTOUT_MIPPED, RenderType.cutoutMipped());
        put(TRANSLUCENT, RenderType.translucent());
    }}

    private static void put(String[] ids, RenderType type) {{
        for (String id : ids) {{
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(RedCircuit.MODID, id);
            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(b -> BlockRenderLayerMap.INSTANCE.putBlock(b, type));
        }}
    }}
}}
"""
open(OUT, "w").write(java)
print(f"cutout={len(cutout)} mipped={len(mipped)} translucent={len(translucent)}")
