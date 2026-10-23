package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyProvider;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.config.RCConfig;
import pl.geesio.redcircuit.util.Dirs;

/**
 * Generatory bez paliwa:
 * <ul>
 *   <li><b>Panel słoneczny</b> - w dzień pod gołym niebem (w deszczu 1/3).</li>
 *   <li><b>Termostos</b> - każda para "gorący" (lawa, ogień) + "zimny" (woda, lód, śnieg) wśród 6 sąsiadów.</li>
 *   <li><b>Generator kinetyczny</b> - z wiatrakiem (5x5 wolnego miejsca pod niebem, mocniej wysoko i w deszczu)
 *       albo kołem wodnym (3x3 wody przed generatorem).</li>
 * </ul>
 */
public class TileGenerator extends TileEntity implements IEnergyProvider, IInfoTile {
    public enum Kind { SOLAR, THERMOPILE, KINETIC }

    public static final int ROTOR_NONE = 0, ROTOR_WINDMILL = 1, ROTOR_WATER_WHEEL = 2;

    private Kind kind = Kind.SOLAR;
    private EnergyBuffer energy;
    private int lastRate;
    /** Generator kinetyczny: zamontowany wirnik i czy się obraca (synchronizowane do klienta). */
    public int rotor;
    public boolean running;

    public TileGenerator() {
        setKind(Kind.SOLAR);
    }

    public TileGenerator(Kind kind) {
        setKind(kind);
    }

    private void setKind(Kind k) {
        kind = k;
        energy = k == Kind.SOLAR ? new EnergyBuffer(1_000, 0, 200, this::markDirty) : new EnergyBuffer(10_000, 0, 400, this::markDirty);
    }

    private int output() {
        return kind == Kind.SOLAR ? 200 : 400;
    }

    public int lastRate() {
        return lastRate;
    }

    @Override
    public void showInfo(EntityPlayer player) {
        player.addChatComponentMessage(new ChatComponentTranslation("message.redcircuit.solar", lastRate, energy.get()));
    }

    // ------------------------------------------------------------------ produkcja

    private static boolean isHot(World w, int x, int y, int z) {
        Block b = w.getBlock(x, y, z);
        return b.getMaterial() == Material.lava || b == Blocks.fire;
    }

    private static boolean isCold(World w, int x, int y, int z) {
        Block b = w.getBlock(x, y, z);
        return b.getMaterial() == Material.water || b == Blocks.ice || b == Blocks.packed_ice || b == Blocks.snow;
    }

    private int computeRate() {
        World w = worldObj;
        switch (kind) {
            case SOLAR: {
                if (!w.isDaytime() || !w.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord)) return 0;
                return w.isRaining() ? RCConfig.solarRf / 3 : RCConfig.solarRf;
            }
            case THERMOPILE: {
                int hot = 0, cold = 0;
                for (ForgeDirection d : Dirs.ALL) {
                    int nx = xCoord + d.offsetX, ny = yCoord + d.offsetY, nz = zCoord + d.offsetZ;
                    if (!w.blockExists(nx, ny, nz)) continue;
                    if (isHot(w, nx, ny, nz)) hot++;
                    else if (isCold(w, nx, ny, nz)) cold++;
                }
                return Math.min(hot, cold) * RCConfig.thermopileRfPerPair;
            }
            default:
                return kineticRate();
        }
    }

    private int kineticRate() {
        if (rotor == ROTOR_NONE) return 0;
        World w = worldObj;
        ForgeDirection facing = BlockEnergyMachine.facing(w, xCoord, yCoord, zCoord);
        int cx = xCoord + facing.offsetX, cy = yCoord, cz = zCoord + facing.offsetZ;
        ForgeDirection side = Dirs.cw(facing);
        if (rotor == ROTOR_WINDMILL) {
            // Wiatrak 5x5 przed generatorem: cała przestrzeń musi być pusta, pod gołym niebem
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    int px = cx + side.offsetX * a, py = cy + b, pz = cz + side.offsetZ * a;
                    if (!w.blockExists(px, py, pz) || !w.isAirBlock(px, py, pz)) return 0;
                }
            }
            if (!w.canBlockSeeTheSky(cx, cy + 2, cz)) return 0;
            int height = Math.max(0, yCoord - 63);
            double rate = Math.min(RCConfig.windmillMaxRf, RCConfig.windmillBaseRf + height / 6.0);
            if (w.isThundering()) rate *= 2;
            else if (w.isRaining()) rate *= 1.5;
            return (int) Math.round(rate);
        }
        // Koło wodne 3x3: płynąca woda daje więcej niż stojąca
        int rate = 0;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                int px = cx + side.offsetX * a, py = cy + b, pz = cz + side.offsetZ * a;
                if (!w.blockExists(px, py, pz)) continue;
                Block bl = w.getBlock(px, py, pz);
                if (bl.getMaterial() == Material.water) {
                    rate += w.getBlockMetadata(px, py, pz) == 0 ? RCConfig.waterWheelSourceRf : RCConfig.waterWheelFlowingRf;
                } else if (!w.isAirBlock(px, py, pz)) {
                    return 0;   // koło zablokowane
                }
            }
        }
        return Math.min(rate, RCConfig.waterWheelMaxRf);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        long t = worldObj.getTotalWorldTime();
        int period = kind == Kind.KINETIC ? 40 : 20;
        if (t % period == 0) lastRate = computeRate();
        if (lastRate > 0) energy.addInternal(lastRate);
        if (kind == Kind.KINETIC) {
            boolean run = lastRate > 0;
            if (run != running) {
                running = run;
                sync();
            }
        }
        EnergyUtil.push(worldObj, xCoord, yCoord, zCoord, energy, output());
    }

    public void sync() {
        markDirty();
        if (worldObj != null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    // ------------------------------------------------------------------ zapis

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("Kind", (byte) kind.ordinal());
        energy.save(tag);
        tag.setByte("Rotor", (byte) rotor);
        tag.setBoolean("Running", running);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        int k = tag.getByte("Kind");
        setKind(Kind.values()[Math.max(0, Math.min(Kind.values().length - 1, k))]);
        energy.load(tag);
        rotor = tag.getByte("Rotor");
        running = tag.getBoolean("Running");
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("Rotor", (byte) rotor);
        tag.setBoolean("Running", running);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        rotor = pkt.func_148857_g().getByte("Rotor");
        running = pkt.func_148857_g().getBoolean("Running");
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return net.minecraft.util.AxisAlignedBB.getBoundingBox(xCoord - 3, yCoord - 3, zCoord - 3, xCoord + 4, yCoord + 4, zCoord + 4);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 96 * 96;
    }

    // ------------------------------------------------------------------ RF

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        return energy.extract(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return energy.get();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return energy.capacity;
    }
}
