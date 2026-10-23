package pl.geesio.redcircuit.wire;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import pl.geesio.redcircuit.RenderIds;
import pl.geesio.redcircuit.util.Dirs;
import pl.geesio.redcircuit.util.Pos;

import java.util.ArrayList;
import java.util.List;

/**
 * Przewód przyklejony do ścian. Ściany, na których leży, są zapisane w {@link TileWire} (1.7.10 ma tylko 4 bity metadanych).
 * W jednym bloku może być kilka przewodów tego samego rodzaju na różnych ścianach - łączą się w narożnikach
 * wewnętrznych. Przewody łączą się też prosto i przez narożniki zewnętrzne (zawijając się na krawędzi bloku).
 */
public abstract class BlockFaceWire extends Block implements WireTypes.IWire {
    /** Szerokość i wysokość przewodu w pikselach (1/16 bloku). */
    private final float width, height;

    protected BlockFaceWire(Material material, float widthPx, float heightPx) {
        super(material);
        this.width = widthPx;
        this.height = heightPx;
        setHardness(0.0F);
        setLightOpacity(0);
    }

    // ------------------------------------------------------------------ dane

    public static TileWire tile(IBlockAccess world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof TileWire w ? w : null;
    }

    public static int faces(IBlockAccess world, int x, int y, int z) {
        TileWire t = tile(world, x, y, z);
        return t == null ? 0 : t.faces;
    }

    public static boolean hasFace(IBlockAccess world, Pos p, ForgeDirection side) {
        return world.getBlock(p.x, p.y, p.z) instanceof BlockFaceWire && (faces(world, p.x, p.y, p.z) & (1 << side.ordinal())) != 0;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileWire();
    }

    // ------------------------------------------------------------------ połączenia

    /** Połączenie z urządzeniem niebędącym przewodem (bramka, dźwignia, lampa...). */
    public boolean connectsToDevice(IBlockAccess world, Pos pos, ForgeDirection side, ForgeDirection dir, Pos np, Block nb) {
        return false;
    }

    /** Tekstura przewodu-części ForgeMultipart (meta = kolor, power = sygnał). */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon partIcon(int meta, int power) {
        return getIcon(0, meta);
    }

    public WireLinks.Target target(IBlockAccess world, Pos pos, ForgeDirection side, ForgeDirection dir) {
        int type = wireType(world.getBlockMetadata(pos.x, pos.y, pos.z));
        return WireLinks.link(world, pos, type, side, dir, faces(world, pos.x, pos.y, pos.z),
                (w, np, nb) -> connectsToDevice(w, pos, side, dir, np, nb));
    }

    public WireConnections computeConnections(IBlockAccess world, int x, int y, int z) {
        Pos pos = new Pos(x, y, z);
        int faces = faces(world, x, y, z);
        WireConnections.Builder b = new WireConnections.Builder();
        for (ForgeDirection side : Dirs.ALL) {
            if ((faces & (1 << side.ordinal())) == 0) continue;
            for (ForgeDirection dir : Dirs.ALL) {
                if (Dirs.sameAxis(dir, side)) continue;
                b.set(side, dir, WireLinks.reach(target(world, pos, side, dir).link, side, dir));
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------ geometria

    /**
     * Prostopadłościany (w pikselach, 0..16) tworzące przewód na ścianie {@code side}.
     * Każdy element: {minX, minY, minZ, maxX, maxY, maxZ}.
     */
    public List<float[]> boxes(ForgeDirection side, WireConnections conn) {
        List<float[]> out = new ArrayList<>();
        float hw = width / 2f;
        float[] min = new float[3], max = new float[3];
        int sAxis = Dirs.coord(side);
        float t0 = Dirs.positive(side) ? 16 - height : 0;
        float t1 = t0 + height;

        for (int a = 0; a < 3; a++) { min[a] = 8 - hw; max[a] = 8 + hw; }
        min[sAxis] = t0;
        max[sAxis] = t1;
        out.add(new float[]{min[0], min[1], min[2], max[0], max[1], max[2]});

        for (ForgeDirection dir : Dirs.ALL) {
            if (Dirs.sameAxis(dir, side)) continue;
            int r = conn.get(side, dir);
            if (r == WireConnections.NONE) continue;
            float reach = switch (r) {
                case WireConnections.SHORT -> 8 - height;
                case WireConnections.EXTENDED -> 8 + height;
                default -> 8;
            };
            for (int a = 0; a < 3; a++) { min[a] = 8 - hw; max[a] = 8 + hw; }
            min[sAxis] = t0;
            max[sAxis] = t1;
            int dAxis = Dirs.coord(dir);
            if (Dirs.positive(dir)) {
                min[dAxis] = 8 + hw;
                max[dAxis] = 8 + reach;
            } else {
                min[dAxis] = 8 - reach;
                max[dAxis] = 8 - hw;
            }
            if (max[dAxis] > min[dAxis]) out.add(new float[]{min[0], min[1], min[2], max[0], max[1], max[2]});
            if (r == WireConnections.RISER) {
                // Pionowy łącznik przy krawędzi: od przewodu na ścianie do środka bloku (przewód w obudowie)
                float rise = Math.max(hw, 3f);
                if (Dirs.positive(side)) {
                    min[sAxis] = 8 - rise;
                    max[sAxis] = 16;
                } else {
                    min[sAxis] = 0;
                    max[sAxis] = 8 + rise;
                }
                float w = Math.max(width, 2f);
                if (Dirs.positive(dir)) {
                    min[dAxis] = 16 - w;
                    max[dAxis] = 16;
                } else {
                    min[dAxis] = 0;
                    max[dAxis] = w;
                }
                out.add(new float[]{min[0], min[1], min[2], max[0], max[1], max[2]});
            }
        }
        return out;
    }

    /** Wszystkie prostopadłościany przewodów w bloku (do zaznaczenia i rysowania). */
    public List<float[]> allBoxes(IBlockAccess world, int x, int y, int z) {
        WireConnections conn = computeConnections(world, x, y, z);
        int faces = faces(world, x, y, z);
        List<float[]> out = new ArrayList<>();
        for (ForgeDirection side : Dirs.ALL) {
            if ((faces & (1 << side.ordinal())) != 0) out.addAll(boxes(side, conn));
        }
        return out;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        List<float[]> boxes = allBoxes(world, x, y, z);
        if (boxes.isEmpty()) {
            setBlockBounds(0.4F, 0.4F, 0.4F, 0.6F, 0.6F, 0.6F);
            return;
        }
        float[] u = {16, 16, 16, 0, 0, 0};
        for (float[] b : boxes) {
            for (int i = 0; i < 3; i++) {
                u[i] = Math.min(u[i], Math.max(0, b[i]));
                u[i + 3] = Math.max(u[i + 3], Math.min(16, b[i + 3]));
            }
        }
        setBlockBounds(u[0] / 16f, u[1] / 16f, u[2] / 16f, u[3] / 16f, u[4] / 16f, u[5] / 16f);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, height / 16f, 1.0F);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
    }

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
        return RenderIds.faceWire;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return false;
    }

    // ------------------------------------------------------------------ stawianie / podparcie

    public static boolean canAttach(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        int sx = x + side.offsetX, sy = y + side.offsetY, sz = z + side.offsetZ;
        return world.getBlock(sx, sy, sz).isSideSolid(world, sx, sy, sz, side.getOpposite());
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        for (ForgeDirection d : Dirs.ALL) if (canAttach(world, x, y, z, d)) return true;
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;
        TileWire t = tile(world, x, y, z);
        if (t == null) return;
        int lost = 0;
        for (ForgeDirection d : Dirs.ALL) {
            if (t.hasFace(d.ordinal()) && !canAttach(world, x, y, z, d)) lost |= 1 << d.ordinal();
        }
        if (lost != 0) {
            int meta = world.getBlockMetadata(x, y, z);
            for (int i = 0; i < Integer.bitCount(lost); i++) dropBlockAsItem(world, x, y, z, new ItemStack(this, 1, damageDropped(meta)));
            t.faces &= ~lost;
            if (t.faces == 0) {
                world.setBlockToAir(x, y, z);
                return;
            }
            world.markBlockForUpdate(x, y, z);
            onWireChanged(world, x, y, z);
            return;
        }
        onSignalNeighborChanged(world, x, y, z, neighbor);
    }

    /** Sąsiad się zmienił (np. sygnał) - przewody przeliczają sieć. */
    protected void onSignalNeighborChanged(World world, int x, int y, int z, Block neighbor) {
    }

    /** Wywoływane, gdy przewód powstał lub zmienił ściany. */
    public void onWireChanged(World world, int x, int y, int z) {
    }

    /** Wywoływane po usunięciu przewodu. */
    protected void onWireRemoved(World world, int x, int y, int z) {
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        if (!world.isRemote) onWireRemoved(world, x, y, z);
    }

    /** Niszczenie przez gracza usuwa tylko przewód, w który celuje (jeśli jest ich kilka). */
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        TileWire t = tile(world, x, y, z);
        if (t == null || t.faceCount() <= 1) return super.removedByPlayer(world, player, x, y, z, willHarvest);
        ForgeDirection side = pickFace(world, x, y, z, player, t);
        t.faces &= ~(1 << side.ordinal());
        if (!world.isRemote) {
            if (!player.capabilities.isCreativeMode) {
                dropBlockAsItem(world, x, y, z, new ItemStack(this, 1, damageDropped(world.getBlockMetadata(x, y, z))));
            }
            world.markBlockForUpdate(x, y, z);
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            onWireChanged(world, x, y, z);
        } else {
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        }
        return false;
    }

    private ForgeDirection pickFace(World world, int x, int y, int z, EntityPlayer player, TileWire t) {
        double eye = world.isRemote ? player.getEyeHeight() - player.getDefaultEyeHeight() : player.getEyeHeight();
        Vec3 start = Vec3.createVectorHelper(player.posX, player.posY + eye, player.posZ);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * 8, look.yCoord * 8, look.zCoord * 8);
        WireConnections conn = computeConnections(world, x, y, z);
        ForgeDirection best = null;
        double bestDist = Double.MAX_VALUE;
        for (ForgeDirection side : Dirs.ALL) {
            if (!t.hasFace(side.ordinal())) continue;
            if (best == null) best = side;
            for (float[] b : boxes(side, conn)) {
                AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(x + b[0] / 16, y + b[1] / 16, z + b[2] / 16,
                        x + b[3] / 16, y + b[4] / 16, z + b[5] / 16);
                MovingObjectPosition hit = bb.calculateIntercept(start, end);
                if (hit != null) {
                    double d = hit.hitVec.squareDistanceTo(start);
                    if (d < bestDist) {
                        bestDist = d;
                        best = side;
                    }
                }
            }
        }
        return best == null ? ForgeDirection.DOWN : best;
    }

    /** Pozycje sąsiadów po przekątnej (narożniki zewnętrzne). */
    public static List<Pos> diagonals(Pos pos) {
        List<Pos> out = new ArrayList<>(12);
        for (int i = 0; i < 6; i++) {
            for (int j = i + 1; j < 6; j++) {
                ForgeDirection a = Dirs.of(i), b = Dirs.of(j);
                if (!Dirs.sameAxis(a, b)) out.add(pos.offset(a).offset(b));
            }
        }
        return out;
    }
}
