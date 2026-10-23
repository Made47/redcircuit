package pl.geesio.redcircuit.energy;

import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/** Panel słoneczny: płyta 6 px. */
public class BlockSolarPanel extends BlockEnergyMachine {
    public BlockSolarPanel() {
        super("solar_panel", Faces.PILLAR, () -> new TileGenerator(TileGenerator.Kind.SOLAR), 0);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 6.0F / 16.0F, 1.0F);
        setLightOpacity(0);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return side == ForgeDirection.DOWN;
    }
}
