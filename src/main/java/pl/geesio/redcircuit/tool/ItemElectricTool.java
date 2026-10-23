package pl.geesio.redcircuit.tool;

import cofh.api.energy.IEnergyContainerItem;
import com.google.common.collect.Sets;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;

import java.util.List;

/**
 * Narzędzie elektryczne (wiertarka = kilof + łopata, piła łańcuchowa = siekiera + liście/wełna/pajęczyny):
 * zamiast wytrzymałości zużywa energię (RF), poziom jak diament. Bez energii kopie bardzo wolno.
 * Wiertarka: Shift + PPM przełącza kopanie 1x1 / 3x3 (Shift podczas kopania = tylko jeden blok).
 */
public class ItemElectricTool extends ItemTool implements IEnergyContainerItem {
    public static final int CAPACITY = 100_000, RATE = 1_000, COST_PER_BLOCK = 50, COST_PER_HIT = 100;
    private static final float SPEED = 10.0F;

    private final boolean drill;
    /** Zapobiega rekurencji: rozbijanie bloków obok wywołuje onBlockDestroyed ponownie. */
    private static final ThreadLocal<Boolean> BREAKING_AREA = ThreadLocal.withInitial(() -> false);

    /** @param drill true = wiertarka (kilof + łopata, tryb 3x3), false = piła łańcuchowa (siekiera) */
    public ItemElectricTool(String name, boolean drill) {
        super(drill ? 2.0F : 5.0F, ToolMaterial.EMERALD, Sets.newHashSet());
        this.drill = drill;
        setUnlocalizedName(RedCircuit.MODID + "." + name);
        setTextureName(RedCircuit.MODID + ":" + name);
        setMaxDamage(0);
        setMaxStackSize(1);
        setCreativeTab(RCTab.TAB);
        if (drill) {
            setHarvestLevel("pickaxe", 3);
            setHarvestLevel("shovel", 3);
        } else {
            setHarvestLevel("axe", 3);
        }
    }

    // ------------------------------------------------------------------ energia (NBT "Energy")

    public static int energy(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getInteger("Energy") : 0;
    }

    private static void setEnergy(ItemStack stack, int e) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Energy", Math.max(0, Math.min(CAPACITY, e)));
    }

    private static void use(ItemStack stack, int amount) {
        setEnergy(stack, energy(stack) - amount);
    }

    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        int r = Math.max(0, Math.min(Math.min(maxReceive, RATE), CAPACITY - energy(stack)));
        if (r > 0 && !simulate) setEnergy(stack, energy(stack) + r);
        return r;
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        int r = Math.max(0, Math.min(Math.min(maxExtract, RATE), energy(stack)));
        if (r > 0 && !simulate) setEnergy(stack, energy(stack) - r);
        return r;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        return energy(stack);
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return CAPACITY;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - (double) energy(stack) / CAPACITY;
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack tool, ItemStack material) {
        return false;
    }

    // ------------------------------------------------------------------ kopanie

    public boolean isAreaMode(ItemStack stack) {
        return drill && stack.hasTagCompound() && stack.getTagCompound().getBoolean("Area");
    }

    /** Czy narzędzie jest właściwe dla bloku (prędkość i zbieranie). */
    private boolean effective(Block block, int meta) {
        Material m = block.getMaterial();
        if (drill) {
            if (block.isToolEffective("pickaxe", meta) || block.isToolEffective("shovel", meta)) return true;
            return m == Material.rock || m == Material.iron || m == Material.anvil || m == Material.ground || m == Material.grass
                    || m == Material.sand || m == Material.snow || m == Material.craftedSnow || m == Material.clay
                    || m == Material.glass || m == Material.ice || m == Material.packedIce || m == Material.redstoneLight;
        }
        if (block.isToolEffective("axe", meta)) return true;
        return m == Material.wood || m == Material.leaves || m == Material.plants || m == Material.vine || m == Material.cloth
                || m == Material.web || m == Material.gourd || m == Material.cactus || block == Blocks.web;
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        if (energy(stack) < COST_PER_BLOCK) return 1.0F;
        float speed = effective(block, meta) ? SPEED : 1.0F;
        return isAreaMode(stack) ? speed * 0.4F : speed;   // 3x3 kopie wolniej
    }

    @Override
    public boolean canHarvestBlock(Block block, ItemStack stack) {
        return effective(block, 0);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase miner) {
        if (world.isRemote) return true;
        if (block.getBlockHardness(world, x, y, z) != 0.0F) use(stack, COST_PER_BLOCK);
        if (isAreaMode(stack) && !BREAKING_AREA.get() && miner instanceof EntityPlayerMP player && !player.isSneaking()
                && effective(block, world.getBlockMetadata(x, y, z))) {
            breakArea(stack, world, block, x, y, z, player);
        }
        return true;   // bez zużycia wytrzymałości
    }

    /** Rozbija 8 bloków wokół wykopanego (w płaszczyźnie prostopadłej do ściany, w którą celuje gracz). */
    private void breakArea(ItemStack stack, World world, Block center, int x, int y, int z, EntityPlayerMP player) {
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, false);
        int face = hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? hit.sideHit : 1;
        int axis = face >> 1;   // 0 = Y, 1 = Z, 2 = X
        float centerHardness = center.getBlockHardness(world, x, y, z);
        BREAKING_AREA.set(true);
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    int px = x, py = y, pz = z;
                    switch (axis) {
                        case 0 -> { px += a; pz += b; }
                        case 1 -> { px += a; py += b; }
                        default -> { py += a; pz += b; }
                    }
                    if (energy(stack) < COST_PER_BLOCK) return;
                    Block s = world.getBlock(px, py, pz);
                    if (s.isAir(world, px, py, pz) || !effective(s, world.getBlockMetadata(px, py, pz))) continue;
                    float h = s.getBlockHardness(world, px, py, pz);
                    if (h < 0 || h > centerHardness * 1.5F + 0.5F) continue;   // nie rozbijaj dużo twardszych bloków
                    player.theItemInWorldManager.tryHarvestBlock(px, py, pz);
                }
            }
        } finally {
            BREAKING_AREA.set(false);
        }
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        use(stack, COST_PER_HIT);
        return true;
    }

    // ------------------------------------------------------------------ przełączanie trybu (wiertarka)

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (drill && player.isSneaking()) toggle(world, player, stack);
        return stack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hx, float hy, float hz) {
        if (drill && player.isSneaking()) {
            toggle(world, player, stack);
            return true;
        }
        return false;
    }

    private void toggle(World world, EntityPlayer player, ItemStack stack) {
        if (world.isRemote) return;
        boolean area = !isAreaMode(stack);
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean("Area", area);
        player.addChatComponentMessage(new ChatComponentTranslation(area ? "message.redcircuit.drill_3x3" : "message.redcircuit.drill_1x1"));
        world.playSoundAtEntity(player, "random.click", 0.4F, area ? 1.4F : 1.0F);
    }

    // ------------------------------------------------------------------ opis

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item));
        ItemStack full = new ItemStack(item);
        setEnergy(full, CAPACITY);
        list.add(full);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted("tooltip.redcircuit.battery", energy(stack), CAPACITY));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("tooltip.redcircuit.electric_tool", COST_PER_BLOCK));
        if (drill) {
            tooltip.add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal(isAreaMode(stack)
                    ? "tooltip.redcircuit.drill_mode_3x3" : "tooltip.redcircuit.drill_mode_1x1"));
            tooltip.add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("tooltip.redcircuit.drill_toggle"));
        }
    }
}
