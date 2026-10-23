package pl.geesio.redcircuit.gate;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RCTab;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.RenderIds;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.wire.BlockRedWire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Płaska bramka logiczna leżąca na bloku (port GateBlock z 1.21). Stan (kierunek wyjścia, POWERED/STATE/INPUT,
 * wyłączone strony, zamiana stron) trzyma TileGate - w 1.7.10 metadane mają tylko 4 bity.
 */
public class BlockGate extends Block implements BlockRedWire.IRedstoneDevice {
    /** Klucz NBT przedmiotu z ustawieniami bramki (zebranej śrubokrętem). */
    public static final String ITEM_TAG = "GateData";

    private final GateType type;

    @SideOnly(Side.CLIENT)
    private IIcon sideIcon;
    @SideOnly(Side.CLIENT)
    private Map<String, IIcon> tops;

    public BlockGate(GateType type) {
        super(Material.circuits);
        this.type = type;
        String name = type.getSerializedName() + "_gate";
        setBlockName(RedCircuit.MODID + "." + name);
        setBlockTextureName(RedCircuit.MODID + ":gate_side");
        setHardness(0.0F);
        setStepSound(soundTypeWood);
        setCreativeTab(RCTab.TAB);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    public GateType getGateType() {
        return type;
    }

    public static TileGate tile(IBlockAccess world, int x, int y, int z) {
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileGate g ? g : null;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileGate();
    }

    // ------------------------------------------------------------------ kształt

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return RenderIds.gate;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    // ------------------------------------------------------------------ strony

    /** Bit strony względem przodu: OUT_F, IN_L, IN_B albo IN_R. */
    private static int sideBit(ForgeDirection f, ForgeDirection side) {
        if (side == f) return GateType.OUT_F;
        if (side == Dirs.ccw(f)) return GateType.IN_L;
        if (side == f.getOpposite()) return GateType.IN_B;
        return GateType.IN_R;
    }

    /**
     * Czy przewód może się podłączyć do bramki od strony {@code side} (kierunek od bramki do przewodu):
     * tylko wyjścia i włączone wejścia - strony bez funkcji i wyłączone wejścia zostają rozłączone.
     */
    public static boolean connectsOn(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        if (!(world.getBlock(x, y, z) instanceof BlockGate gate) || Dirs.axis(side) == 0) return false;
        TileGate t = tile(world, x, y, z);
        if (t == null) return false;
        GateType tp = gate.type;
        int mask = (tp.inputs() | tp.extraOutputs(t.mirrored)) & ~t.disabledMask();
        return (mask & sideBit(t.facing(), side)) != 0;
    }

    /** Sekwencer: krok 0..3 w bitach STATE/INPUT; wyjście kolejno: przód, prawo, tył, lewo. */
    private static int seqStep(boolean state, boolean input) {
        return (state ? 2 : 0) | (input ? 1 : 0);
    }

    private static ForgeDirection seqOutput(ForgeDirection f, int step) {
        return switch (step) {
            case 1 -> Dirs.cw(f);
            case 2 -> f.getOpposite();
            case 3 -> Dirs.ccw(f);
            default -> f;
        };
    }

    // ------------------------------------------------------------------ redstone

    @Override
    public boolean canProvidePower() {
        return true;
    }

    /** Side w 1.7.10: -1 = góra, 0 = północ, 1 = wschód, 2 = południe, 3 = zachód (strona bramki od przewodu). */
    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        ForgeDirection d = switch (side) {
            case 0 -> ForgeDirection.NORTH;
            case 1 -> ForgeDirection.EAST;
            case 2 -> ForgeDirection.SOUTH;
            case 3 -> ForgeDirection.WEST;
            default -> null;
        };
        return d != null && connectsOn(world, x, y, z, d);
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return signal(world, x, y, z, Dirs.of(side));
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return signal(world, x, y, z, Dirs.of(side));
    }

    /**
     * Sygnał bramki dla odbiorcy; {@code direction} = kierunek od odbiorcy do bramki (jak getSignal w 1.21),
     * więc strona bramki zwrócona do odbiorcy to {@code direction.getOpposite()}.
     */
    public int signal(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
        TileGate t = tile(world, x, y, z);
        if (t == null || Dirs.axis(direction) == 0) return 0;
        ForgeDirection f = t.facing();
        ForgeDirection out = direction.getOpposite();
        switch (type) {
            case SEQUENCER:
                return t.powered && out == seqOutput(f, seqStep(t.state, t.input)) ? 15 : 0;
            case RS_LATCH: {
                // Zawsze świeci dokładnie jedno wyjście - przód (ustawiony) albo tył (skasowany); wejście aktywnego
                // stanu też świeci, chyba że w oknie wybrano "tylko przód/tył"
                boolean set = t.state;
                if (out == f) return set ? 15 : 0;
                if (out == f.getOpposite()) return set ? 0 : 15;
                if (!t.quiet) {
                    ForgeDirection setSide = t.mirrored ? Dirs.cw(f) : Dirs.ccw(f);
                    if (out == setSide) return set ? 15 : 0;
                    if (out == setSide.getOpposite()) return set ? 0 : 15;
                }
                return 0;
            }
            case TOGGLE:
                // Dwa wyjścia na zmianę - przód (stan 1) albo tył (stan 0)
                if (out == f) return t.state ? 15 : 0;
                if (out == f.getOpposite()) return t.state ? 0 : 15;
                return 0;
            case RANDOMIZER:
                // Przód = POWERED, lewo = STATE, prawo = INPUT
                if (out == f) return t.powered ? 15 : 0;
                if (out == Dirs.ccw(f)) return t.state ? 15 : 0;
                if (out == Dirs.cw(f)) return t.input ? 15 : 0;
                return 0;
            case COUNTER:
                // Przód przy maksimum (POWERED), tył na zerze (STATE)
                if (out == f) return t.powered ? 15 : 0;
                if (out == f.getOpposite()) return t.state ? 15 : 0;
                return 0;
            case BUFFER:
            case NOT: {
                // Wynik z przodu, z lewej i z prawej (boki można wyłączyć w oknie)
                if (!t.powered) return 0;
                if (out == f) return 15;
                int dis = t.disabledMask();
                if (out == Dirs.ccw(f)) return (dis & GateType.IN_L) == 0 ? 15 : 0;
                if (out == Dirs.cw(f)) return (dis & GateType.IN_R) == 0 ? 15 : 0;
                return 0;
            }
            case TIMER:
                // Impuls z przodu, z lewej i z prawej (tył to wejście "stop")
                return t.powered && out != f.getOpposite() ? 15 : 0;
            case STATE_CELL:
                // Prawa (albo lewa, z okna) strona: "w trakcie odliczania"
                if (t.state && out == (t.mirrored ? Dirs.ccw(f) : Dirs.cw(f))) return 15;
                return t.powered && out == f ? 15 : 0;
            default:
                return t.powered && out == f ? 15 : 0;
        }
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return type == GateType.COUNTER;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileGate t = tile(world, x, y, z);
        return t == null ? 0 : Math.min(15, t.count * 15 / t.max());
    }

    // ------------------------------------------------------------------ aktualizacje

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!world.isRemote) world.scheduleBlockUpdate(x, y, z, this, 1);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
            return;
        }
        // Powtórne planowanie tego samego ticku WorldServer ignoruje (jak hasScheduledTick w 1.21)
        world.scheduleBlockUpdate(x, y, z, this, 2);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        updateAllSides(world, x, y, z);
    }

    private static boolean input(World world, int x, int y, int z, ForgeDirection dir) {
        return world.getIndirectPowerLevelTo(x + dir.offsetX, y, z + dir.offsetZ, dir.ordinal()) > 0;
    }

    /** Odświeża blok po danej stronie i bloki wokół niego (sygnał mocny przez blok przed bramką). */
    private void updateSide(World world, int x, int y, int z, ForgeDirection side) {
        int nx = x + side.offsetX, nz = z + side.offsetZ;
        world.notifyBlockOfNeighborChange(nx, y, nz, this);
        world.notifyBlocksOfNeighborChange(nx, y, nz, this, side.getOpposite().ordinal());
    }

    private void updateAllSides(World world, int x, int y, int z) {
        for (ForgeDirection d : Dirs.HORIZONTAL) updateSide(world, x, y, z, d);
    }

    private void updateFront(World world, int x, int y, int z, TileGate t) {
        switch (type) {
            case SEQUENCER, STATE_CELL, TIMER, RS_LATCH, TOGGLE, BUFFER, NOT, RANDOMIZER, COUNTER ->
                    updateAllSides(world, x, y, z);   // wyjścia na kilku stronach
            default -> updateSide(world, x, y, z, t.facing());
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        TileGate t = tile(world, x, y, z);
        if (t == null) return;
        ForgeDirection facing = t.facing();
        boolean powered = t.powered, state = t.state, in = t.input;

        boolean l = input(world, x, y, z, Dirs.ccw(facing));
        boolean b = input(world, x, y, z, facing.getOpposite());
        // Sekwencer sam zasila tył w kroku 2 - przewód z tyłu oddałby mu ten sygnał i bramka by się zawiesiła
        if (type == GateType.SEQUENCER && powered && seqOutput(facing, seqStep(state, in)) == facing.getOpposite()) {
            b = false;
        }
        boolean r = input(world, x, y, z, Dirs.cw(facing));
        // Wejścia wyłączone w oknie mają wartość neutralną
        int dis = type.canDisableInputs() ? t.disabledMask() : 0;
        if (dis != 0) {
            boolean n = type.neutralInput();
            if ((dis & GateType.IN_L) != 0) l = n;
            if ((dis & GateType.IN_B) != 0) b = n;
            if ((dis & GateType.IN_R) != 0) r = n;
        }

        boolean np = powered, ns = state, ni = in;
        boolean dataChanged = false, countChanged = false;
        int delay = -1;
        switch (type) {
            case TIMER -> {
                if (b) {
                    np = false;   // zatrzymany
                } else if (powered) {
                    np = false;
                    delay = Math.max(1, interval(t) - 2);
                } else {
                    np = true;
                    delay = 2;
                    tickSound(world, x, y, z, t, 1.0F);
                }
            }
            case PULSE -> {
                boolean rising = b && !in;
                ni = b;
                if (rising) {
                    np = true;
                    delay = 2;
                } else if (powered) {
                    np = false;
                }
            }
            case RANDOMIZER -> {
                if (b) {
                    np = random.nextBoolean();
                    ns = random.nextBoolean();
                    ni = random.nextBoolean();
                    delay = 4;
                } else {
                    np = ns = ni = false;
                }
            }
            case TOGGLE -> {
                // Zbocze narastające na lewym albo prawym wejściu przełącza wyjście przód <-> tył
                boolean rising = (l && !t.lastL) || (r && !t.lastR);
                if (t.lastL != l || t.lastR != r) dataChanged = true;
                t.lastL = l;
                t.lastR = r;
                if (rising) {
                    ns = !state;
                    np = ns;
                }
            }
            case RS_LATCH -> {
                // Lewe wejście ustawia (przód), prawe kasuje (tył) - albo odwrotnie po zamianie stron.
                // Strona, która sama świeci, nie jest czytana jako wejście (chyba że w trybie "tylko przód/tył").
                boolean st = state;
                boolean sIn = t.mirrored ? r : l, rIn = t.mirrored ? l : r;
                boolean setIn = sIn && (t.quiet || !st);
                boolean resetIn = rIn && (t.quiet || st);
                if (setIn && !resetIn) st = true;
                else if (resetIn && !setIn) st = false;
                ns = st;
                np = st;
            }
            case SEQUENCER -> {
                if (b) {
                    np = false;   // wstrzymany sygnałem z tyłu
                } else {
                    if (powered) {
                        int step = (seqStep(state, in) + 1) & 3;
                        ns = (step & 2) != 0;
                        ni = (step & 1) != 0;
                        tickSound(world, x, y, z, t, step == 0 ? 1.3F : 1.1F);
                    } else {
                        np = true;
                    }
                    delay = interval(t);
                }
            }
            case COUNTER -> {
                int max = t.max();
                int before = t.count;
                if (l && !t.lastL) t.count = Math.min(max, t.count + 1);
                if (r && !t.lastR) t.count = Math.max(0, t.count - 1);
                if (t.count != before) {
                    countChanged = true;
                    tickSound(world, x, y, z, t, t.count >= max ? 1.5F : 1.2F);
                }
                t.lastL = l;
                t.lastB = b;
                t.lastR = r;
                dataChanged = true;
                np = t.count >= max;
                ns = t.count == 0;
            }
            case STATE_CELL -> {
                long now = world.getTotalWorldTime();
                if (b && !t.lastB && !state) {
                    // start odliczania
                    ns = true;
                    np = false;
                    t.due = now + t.interval;
                    delay = t.interval;
                } else if (state && now >= t.due) {
                    // koniec odliczania: impuls z przodu
                    ns = false;
                    np = true;
                    delay = 2;
                    tickSound(world, x, y, z, t, 0.9F);
                } else if (state) {
                    delay = (int) Math.max(1, t.due - now);
                } else if (powered) {
                    np = false;
                }
                t.lastB = b;
                dataChanged = true;
            }
            case SYNCHRONIZER -> {
                if (b) {
                    t.latchL = false;
                    t.latchR = false;
                } else {
                    if (l && !t.lastL) t.latchL = true;
                    if (r && !t.lastR) t.latchR = true;
                }
                t.lastL = l;
                t.lastR = r;
                if (t.latchL && t.latchR) {
                    t.latchL = false;
                    t.latchR = false;
                    np = true;
                    delay = 2;
                } else if (powered) {
                    np = false;
                }
                dataChanged = true;
            }
            default -> np = type.eval(l, b, r);
        }

        // Najpierw kolejny tick, potem sąsiedzi - inaczej przewód przed bramką zaplanuje tick za 2,
        // a nasz dłuższy zostanie odrzucony jako duplikat (zegar tykałby co 2 ticki)
        if (delay > 0) world.scheduleBlockUpdate(x, y, z, this, delay);

        boolean changed = np != powered || ns != state || ni != in;
        if (changed) {
            t.powered = np;
            t.state = ns;
            t.input = ni;
            t.sync();
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            if (np != powered || type == GateType.SEQUENCER || type == GateType.RS_LATCH
                    || type == GateType.RANDOMIZER || type == GateType.COUNTER
                    || (type == GateType.STATE_CELL && ns != state)) {
                updateFront(world, x, y, z, t);
            }
        } else if (countChanged) {
            t.sync();   // licznik w oknie
            world.notifyBlocksOfNeighborChange(x, y, z, this);   // komparatory
        } else if (dataChanged) {
            t.markDirty();
        }
    }

    /** Okres w tickach z ustawień bramki. */
    public static int interval(TileGate t) {
        return Math.max(TileGate.MIN_INTERVAL, t.interval);
    }

    /** Dźwięk tyknięcia (zegar, sekwencer, komórka stanu, licznik) - chyba że wyciszony w oknie. */
    private static void tickSound(World world, int x, int y, int z, TileGate t, float pitch) {
        if (t.muted) return;
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.click", 0.3F, pitch * 1.6F > 2.0F ? 2.0F : pitch * 1.6F);
    }

    // ------------------------------------------------------------------ interakcja

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        ItemStack held = player.getHeldItem();
        // Śrubokręt działa w onItemUseFirst (po stronie serwera) - tu nic nie robimy, żeby nie otwierać okna
        if (held != null && (held.getItem() instanceof pl.geesio.redcircuit.tool.ItemScrewdriver
                || held.getItem() instanceof pl.geesio.redcircuit.tool.ItemMultimeter)) return true;
        TileGate t = tile(world, x, y, z);
        if (t == null) return false;
        if (type == GateType.TOGGLE && !player.isSneaking()) {
            // Przerzutnik T: PPM przełącza ręcznie, Shift + PPM otwiera okno
            if (!world.isRemote) {
                boolean st = !t.state;
                t.state = st;
                t.powered = st;
                t.sync();
                world.notifyBlocksOfNeighborChange(x, y, z, this);
                updateFront(world, x, y, z, t);
                world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.click", 0.3F, st ? 0.6F : 0.5F);
            }
            return true;
        }
        if (world.isRemote) RedCircuit.proxy.openGateGui(world, x, y, z, type);
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        TileGate t = tile(world, x, y, z);
        if (t == null) return;
        int l = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        t.facing = new int[]{3, 4, 2, 5}[l];   // kierunek patrzenia gracza: S, W, N, E
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(ITEM_TAG)) {
            t.readSettings(stack.getTagCompound().getCompoundTag(ITEM_TAG));
        }
        t.disabledMask();
        if (world.isRemote) return;
        t.sync();
        updateAllSides(world, x, y, z);
        world.scheduleBlockUpdate(x, y, z, this, 1);
    }

    /** Obrót (śrubokręt): wokół osi UP w prawo, DOWN w lewo. */
    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        TileGate t = tile(world, x, y, z);
        if (t == null) return false;
        ForgeDirection f = t.facing();
        t.facing = (axis == ForgeDirection.DOWN ? Dirs.ccw(f) : Dirs.cw(f)).ordinal();
        if (!world.isRemote) {
            t.sync();
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            updateAllSides(world, x, y, z);
            world.scheduleBlockUpdate(x, y, z, this, 1);
        }
        return true;
    }

    @Override
    public ForgeDirection[] getValidRotations(World world, int x, int y, int z) {
        return new ForgeDirection[]{ForgeDirection.UP, ForgeDirection.DOWN};
    }

    /** Zastosowanie ustawień z okna (serwer, z pakietu). */
    public void applySettings(World world, int x, int y, int z, int interval, int max, boolean resetCount, boolean muted,
                              boolean mirrored, int disabled, boolean quiet) {
        TileGate t = tile(world, x, y, z);
        if (t == null) return;
        t.muted = muted;
        boolean outputsChanged = false;
        int sw = type.switchableSides();
        if (sw != 0) {
            int current = t.disabledMask();
            int mask = disabled & sw;
            if (type.canDisableInputs() && mask == type.inputs()) mask = current;   // co najmniej jedno wejście zostaje
            if (mask != current) {
                t.disabled = mask;
                outputsChanged = true;
            }
        }
        if (type == GateType.RS_LATCH && t.quiet != quiet) {
            t.quiet = quiet;
            outputsChanged = true;
        }
        if ((type == GateType.STATE_CELL || type == GateType.RS_LATCH) && t.mirrored != mirrored) {
            t.mirrored = mirrored;
            outputsChanged = true;
        }
        if (type.hasInterval()) {
            t.interval = Math.max(TileGate.MIN_INTERVAL, Math.min(TileGate.MAX_INTERVAL, interval));
            if (type == GateType.STATE_CELL && t.state) t.due = Math.min(t.due, world.getTotalWorldTime() + t.interval);
        }
        if (type == GateType.COUNTER) {
            t.max = Math.max(1, Math.min(TileGate.MAX_COUNT, max));
            if (resetCount) t.count = 0;
            t.count = Math.min(t.count, t.max);
        }
        t.sync();
        if (outputsChanged || type == GateType.COUNTER) {
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            updateAllSides(world, x, y, z);
        }
        world.scheduleBlockUpdate(x, y, z, this, 1);
    }

    // ------------------------------------------------------------------ wygląd

    /** Sufiksy wariantów tekstury góry (jak w gen_assets.py wersji 1.21). */
    public static List<String> suffixes(GateType t) {
        List<String> out = new ArrayList<>();
        int sw = t.switchableSides();
        if (t == GateType.STATE_CELL) {
            out.add("");
            out.add("_mirrored");
        } else if (sw != 0) {
            for (int m = 0; m < 8; m++) {
                if ((m & ~sw) != 0) continue;
                if (!t.switchesOutputs() && m == sw) continue;
                out.add(m == 0 ? "" : "_d" + m);
            }
        } else if (t == GateType.COUNTER) {
            out.add("");
            out.add("_zero");
        } else if (t == GateType.RANDOMIZER) {
            out.add("");
            out.add("_l0r1");
            out.add("_l1r0");
            out.add("_l1r1");
        } else {
            out.add("");
        }
        return out;
    }

    private String suffix(TileGate t) {
        if (t == null) return "";
        if (type == GateType.STATE_CELL) return t.mirrored ? "_mirrored" : "";
        if (type.switchableSides() != 0) {
            int m = t.disabledMask();
            return m == 0 ? "" : "_d" + m;
        }
        if (type == GateType.COUNTER) return t.state ? "_zero" : "";
        if (type == GateType.RANDOMIZER) {
            return t.state || t.input ? "_l" + (t.state ? 1 : 0) + "r" + (t.input ? 1 : 0) : "";
        }
        return "";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        sideIcon = reg.registerIcon(RedCircuit.MODID + ":gate_side");
        blockIcon = sideIcon;
        tops = new HashMap<>();
        String base = RedCircuit.MODID + ":gate_" + type.getSerializedName() + "_";
        for (String s : suffixes(type)) {
            tops.put("off" + s, reg.registerIcon(base + "off" + s));
            tops.put("on" + s, reg.registerIcon(base + "on" + s));
        }
    }

    @SideOnly(Side.CLIENT)
    public IIcon sideIcon() {
        return sideIcon;
    }

    /** Tekstura góry dla stanu bramki (null = przedmiot / brak danych). */
    @SideOnly(Side.CLIENT)
    public IIcon topIcon(TileGate t) {
        String key = (t != null && t.powered ? "on" : "off") + suffix(t);
        IIcon icon = tops.get(key);
        return icon != null ? icon : tops.get("off");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side == 1 ? topIcon(null) : sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return side == 1 ? topIcon(tile(world, x, y, z)) : sideIcon;
    }
}
