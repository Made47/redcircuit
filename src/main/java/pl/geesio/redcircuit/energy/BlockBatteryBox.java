package pl.geesio.redcircuit.energy;

import cofh.api.energy.IEnergyContainerItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

/**
 * Akumulator (1 000 000 RF). PPM otwiera okno ze slotami na baterie. PPM baterią (lub innym przedmiotem
 * z energią RF) ładuje ją z akumulatora, Shift + PPM rozładowuje ją do akumulatora.
 */
public class BlockBatteryBox extends BlockEnergyMachine {
    public BlockBatteryBox() {
        super("battery_box", Faces.BOX, TileBatteryBox::new, 0);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int s, float hx, float hy, float hz) {
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof IEnergyContainerItem item)) {
            return super.onBlockActivated(world, x, y, z, player, s, hx, hy, hz);
        }
        if (world.isRemote) return true;
        if (!(world.getTileEntity(x, y, z) instanceof TileBatteryBox be)) return false;
        EnergyBuffer box = be.energy();
        int moved = 0;
        // Pętla, bo przedmioty mają ograniczenie RF na jedno wywołanie
        for (int i = 0; i < 1000; i++) {
            int step;
            if (player.isSneaking()) {
                step = item.extractEnergy(held, box.space(), false);
                if (step > 0) box.set(box.get() + step);
            } else {
                step = item.receiveEnergy(held, box.get(), false);
                if (step > 0) box.set(box.get() - step);
            }
            if (step <= 0) break;
            moved += step;
        }
        if (moved > 0) world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.orb", 0.4F, player.isSneaking() ? 0.8F : 1.6F);
        player.addChatComponentMessage(new ChatComponentTranslation("message.redcircuit.battery_item", held.getDisplayName(),
                item.getEnergyStored(held), item.getMaxEnergyStored(held), box.get(), box.capacity));
        return true;
    }
}
