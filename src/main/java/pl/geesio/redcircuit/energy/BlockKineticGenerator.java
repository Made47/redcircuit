package pl.geesio.redcircuit.energy;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import pl.geesio.redcircuit.item.ModItems;
import pl.geesio.redcircuit.util.InvUtil;

/** Generator: z przodu montuje się wiatrak albo koło wodne (PPM przedmiotem). Shift + PPM pustą ręką zdejmuje wirnik. */
public class BlockKineticGenerator extends BlockEnergyMachine {
    public BlockKineticGenerator() {
        super("generator", Faces.FRONT, () -> new TileGenerator(TileGenerator.Kind.KINETIC), 0);
    }

    private static Item itemFor(int rotor) {
        return rotor == TileGenerator.ROTOR_WINDMILL ? ModItems.windmill : ModItems.waterWheel;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int s, float hx, float hy, float hz) {
        if (!(world.getTileEntity(x, y, z) instanceof TileGenerator te)) return false;
        ItemStack held = player.getHeldItem();
        int rotor = held == null ? 0 : held.getItem() == ModItems.windmill ? TileGenerator.ROTOR_WINDMILL
                : held.getItem() == ModItems.waterWheel ? TileGenerator.ROTOR_WATER_WHEEL : 0;
        if (rotor != 0) {
            if (te.rotor != TileGenerator.ROTOR_NONE) return false;
            if (!world.isRemote) {
                te.rotor = rotor;
                te.sync();
                if (!player.capabilities.isCreativeMode) held.stackSize--;
                player.addChatComponentMessage(new ChatComponentTranslation("message.redcircuit.rotor_installed"));
            }
            return true;
        }
        if (held == null && player.isSneaking() && te.rotor != TileGenerator.ROTOR_NONE) {
            if (!world.isRemote) {
                ItemStack back = new ItemStack(itemFor(te.rotor));
                te.rotor = TileGenerator.ROTOR_NONE;
                te.running = false;
                te.sync();
                if (!player.inventory.addItemStackToInventory(back)) player.dropPlayerItemWithRandomChoice(back, false);
            }
            return true;
        }
        return super.onBlockActivated(world, x, y, z, player, s, hx, hy, hz);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (world.getTileEntity(x, y, z) instanceof TileGenerator te && te.rotor != TileGenerator.ROTOR_NONE) {
            InvUtil.spawnRandom(world, x, y, z, new ItemStack(itemFor(te.rotor)));
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return false;
    }
}
