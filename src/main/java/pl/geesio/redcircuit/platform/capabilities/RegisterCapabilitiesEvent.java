package pl.geesio.redcircuit.platform.capabilities;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import pl.geesio.redcircuit.platform.transfer.FabricTransfer;

/**
 * Odpowiednik RegisterCapabilitiesEvent z NeoForge. Każda rejestracja trafia też do API Fabrica,
 * żeby leje, rury i maszyny innych modów widziały inwentarze, zbiorniki i energię RedCircuit.
 */
public final class RegisterCapabilitiesEvent {
    public <T, C, BE extends BlockEntity> void registerBlockEntity(BlockCapability<T, C> cap, BlockEntityType<BE> type,
                                                                   BlockCapability.BlockEntityProvider<T, C, ? super BE> provider) {
        cap.addProvider(type, provider);
        FabricTransfer.exportBlockEntity(cap, type);
    }

    public <T, C> void registerBlock(BlockCapability<T, C> cap, BlockCapability.BlockProvider<T, C> provider, Block... blocks) {
        for (Block b : blocks) cap.addProvider(b, provider);
        FabricTransfer.exportBlocks(cap, blocks);
    }

    public <T, C> void registerItem(ItemCapability<T, C> cap, ItemCapability.Provider<T, C> provider, ItemLike... items) {
        for (ItemLike i : items) {
            Item item = i.asItem();
            cap.addProvider(item, provider);
        }
    }
}
