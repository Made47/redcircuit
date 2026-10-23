package pl.geesio.redcircuit.tool;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.gate.BlockGate;
import pl.geesio.redcircuit.gate.TileGate;

import java.util.List;

/**
 * Śrubokręt: PPM obraca blok (bramki, maszyny i bloki vanilla z kierunkiem - przez Block.rotateBlock).
 * Shift + PPM obraca w przeciwną stronę, a na bramce - zbiera ją do ekwipunku razem z ustawieniami.
 * <p>
 * W 1.7.10 onItemUseFirst zwracające true po stronie klienta blokuje wysłanie pakietu do serwera,
 * dlatego klient zwraca false (bramka sama ignoruje wtedy kliknięcie), a całą pracę robi serwer.
 */
public class ItemScrewdriver extends Item {

    public ItemScrewdriver() {
        setUnlocalizedName(RedCircuit.MODID + ".screwdriver");
        setTextureName(RedCircuit.MODID + ":screwdriver");
        setMaxStackSize(1);
        setFull3D();
        setCreativeTab(RCTab.TAB);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                                  float hx, float hy, float hz) {
        if (world.isRemote) return false;
        if (!player.canPlayerEdit(x, y, z, side, stack)) return false;
        Block block = world.getBlock(x, y, z);
        boolean sneaking = player.isSneaking();

        if (sneaking && block instanceof BlockGate) {
            pickUpGate(world, x, y, z, block, player);
            return true;
        }

        boolean rotated;
        if (block instanceof BlockGate) {
            rotated = block.rotateBlock(world, x, y, z, sneaking ? ForgeDirection.DOWN : ForgeDirection.UP);
        } else {
            ForgeDirection axis = ForgeDirection.getOrientation(side);
            if (sneaking) axis = axis.getOpposite();
            rotated = block.rotateBlock(world, x, y, z, axis)
                    || block.rotateBlock(world, x, y, z, sneaking ? ForgeDirection.DOWN : ForgeDirection.UP);
        }
        if (!rotated) return false;
        world.notifyBlockOfNeighborChange(x, y, z, block);
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.click", 0.5F, 1.4F);
        return true;
    }

    /** Zbiera bramkę do ekwipunku gracza; niestandardowe ustawienia zapisuje w przedmiocie. */
    private static void pickUpGate(World world, int x, int y, int z, Block block, EntityPlayer player) {
        ItemStack drop = new ItemStack(block);
        TileGate t = BlockGate.tile(world, x, y, z);
        if (t != null && t.hasCustomSettings()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(BlockGate.ITEM_TAG, t.writeSettings(new NBTTagCompound()));
            drop.setTagCompound(tag);
        }
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound(), 0.8F, 1.0F);
        world.setBlockToAir(x, y, z);
        if (!player.capabilities.isCreativeMode || !player.inventory.hasItemStack(drop)) {
            if (!player.inventory.addItemStackToInventory(drop)) player.dropPlayerItemWithRandomChoice(drop, false);
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.redcircuit.screwdriver"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.redcircuit.screwdriver_gate"));
    }
}
