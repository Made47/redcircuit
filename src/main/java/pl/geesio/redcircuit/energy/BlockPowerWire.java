package pl.geesio.redcircuit.energy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.Pos;
import pl.geesio.redcircuit.wire.BlockFaceWire;
import pl.geesio.redcircuit.wire.WireTypes;

/** Przewód ze stopu energetycznego przesyłający energię RF, przyklejany do ścian (jak przewody sygnałowe). */
public class BlockPowerWire extends BlockFaceWire {
    public BlockPowerWire() {
        super(Material.circuits, 2f, 2f);
        setStepSound(soundTypeMetal);
        setBlockName(RedCircuit.MODID + ".power_alloy_wire");
        setCreativeTab(RCTab.TAB);
    }

    @Override
    public int wireType(int meta) {
        return WireTypes.POWER;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TilePowerWire();
    }

    @Override
    public boolean connectsToDevice(IBlockAccess world, Pos pos, ForgeDirection side, ForgeDirection dir, Pos np, Block nb) {
        return EnergyUtil.connects(world, pos.x, pos.y, pos.z, dir);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (player.getHeldItem() != null) return false;
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TilePowerWire t) {
            EnergyUtil.showEnergy(player, t.buffer().get(), t.buffer().capacity);
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(RedCircuit.MODID + ":power_alloy_wire");
    }
}
