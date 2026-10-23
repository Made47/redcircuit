package pl.geesio.redcircuit.client;

import net.minecraft.world.IBlockAccess;

import java.util.List;

/** Blok rysowany z prostopadłościanów (w pikselach) przez {@link RenderBoxes}; world == null - przedmiot. */
public interface IBoxBlock {
    List<float[]> renderBoxes(IBlockAccess world, int x, int y, int z);
}
