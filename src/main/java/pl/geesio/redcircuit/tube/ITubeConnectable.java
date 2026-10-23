package pl.geesio.redcircuit.tube;

import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/** Bloki, do których rura podłącza się wizualnie (maszyny) - zamiast sprawdzania inwentarza. */
public interface ITubeConnectable {
    /** @param fromTube kierunek od rury do tego bloku */
    boolean connectsToTube(IBlockAccess world, int x, int y, int z, ForgeDirection fromTube);
}
