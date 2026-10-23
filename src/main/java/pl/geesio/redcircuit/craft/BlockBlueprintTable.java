package pl.geesio.redcircuit.craft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import pl.geesio.redcircuit.GuiHandler;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.util.InvUtil;

/** Stół kreślarski (Blueprint Table). */
public class BlockBlueprintTable extends Block {
    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockBlueprintTable() {
        super(Material.wood);
        setBlockName(RedCircuit.MODID + ".blueprint_table");
        setHardness(2.5F);
        setStepSound(soundTypeWood);
        setCreativeTab(RCTab.TAB);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileBlueprintTable();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (!world.isRemote) player.openGui(RedCircuit.instance, GuiHandler.BLUEPRINT_TABLE, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (world.getTileEntity(x, y, z) instanceof TileBlueprintTable te) {
            InvUtil.dropAll(world, x, y, z, te.grid);
            InvUtil.dropAll(world, x, y, z, te.storage);
            InvUtil.dropAll(world, x, y, z, te.plan);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(RedCircuit.MODID + ":blueprint_table_side");
        top = reg.registerIcon(RedCircuit.MODID + ":blueprint_table_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 1) return top;
        if (side == 0) return Blocks.planks.getIcon(0, 0);
        return blockIcon;
    }
}
