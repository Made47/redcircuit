package pl.geesio.redcircuit.worldgen;

import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import pl.geesio.redcircuit.block.BlockOre;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.config.RCConfig;

import java.util.Random;

/** Rudy w Overworldzie (wymiar 0). Liczba żył i włączenie - w config/redcircuit.cfg. */
public class OreGenerator implements IWorldGenerator {
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.dimensionId != 0) return;
        int bx = chunkX * 16, bz = chunkZ * 16;
        if (RCConfig.genCopper) veins(world, random, bx, bz, BlockOre.COPPER, 9, RCConfig.copperVeins, 10, 64);
        if (RCConfig.genTin) veins(world, random, bx, bz, BlockOre.TIN, 9, RCConfig.tinVeins, 5, 56);
        if (RCConfig.genSilver) veins(world, random, bx, bz, BlockOre.SILVER, 8, RCConfig.silverVeins, 5, 32);
        if (RCConfig.genAzurite) veins(world, random, bx, bz, BlockOre.AZURITE, 8, RCConfig.azuriteVeins, 2, 20);
    }

    private static void veins(World world, Random random, int bx, int bz, int meta, int size, int count, int minY, int maxY) {
        WorldGenMinable gen = new WorldGenMinable(ModBlocks.ore, meta, size, Blocks.stone);
        for (int i = 0; i < count; i++) {
            int x = bx + random.nextInt(16);
            int y = minY + random.nextInt(Math.max(1, maxY - minY));
            int z = bz + random.nextInt(16);
            gen.generate(world, random, x, y, z);
        }
    }
}
