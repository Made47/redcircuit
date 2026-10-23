package pl.geesio.redcircuit.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

/** Zwykły blok z jedną teksturą (np. obudowa maszyny). */
public class BlockSimple extends Block {
    public BlockSimple(String name, Material material, float hardness, SoundType sound) {
        super(material);
        setBlockName(RedCircuit.MODID + "." + name);
        setBlockTextureName(RedCircuit.MODID + ":" + name);
        setHardness(hardness);
        setResistance(10.0F);
        setStepSound(sound);
        setCreativeTab(RCTab.TAB);
    }
}
