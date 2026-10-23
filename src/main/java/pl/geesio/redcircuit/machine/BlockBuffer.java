package pl.geesio.redcircuit.machine;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.InvUtil;

/** Bufor: magazyn 18 slotów dostępny z każdej strony (rury, lejki); daje sygnał komparatora. */
public class BlockBuffer extends Block {
    public BlockBuffer() {
        super(Material.iron);
        setBlockName(RedCircuit.MODID + ".buffer");
        setBlockTextureName(RedCircuit.MODID + ":buffer_side");
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(RCTab.TAB);
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private net.minecraft.util.IIcon top;

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister reg) {
        blockIcon = reg.registerIcon(RedCircuit.MODID + ":buffer_side");
        top = reg.registerIcon(RedCircuit.MODID + ":buffer_top");
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon getIcon(int side, int meta) {
        return side < 2 ? top : blockIcon;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileMachine(18, "container.redcircuit.buffer", true);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (!world.isRemote) player.openGui(RedCircuit.instance, GuiHandler.CHEST, world, x, y, z);
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileMachine te = BlockContainerMachine.tile(world, x, y, z);
        return te == null ? 0 : Container.calcRedstoneFromInventory(te.items);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileMachine te = BlockContainerMachine.tile(world, x, y, z);
        if (te != null) InvUtil.dropAll(world, x, y, z, te.items);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
