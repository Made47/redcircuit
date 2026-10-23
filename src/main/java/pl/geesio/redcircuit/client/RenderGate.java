package pl.geesio.redcircuit.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.gate.BlockGate;
import pl.geesio.redcircuit.gate.TileGate;

/**
 * Bramka: płyta 16x2x16 px. Boki z tekstury gate_side, góra z tekstury stanu bramki obróconej
 * tak, by górna krawędź tekstury (wyjście) wskazywała kierunek wyjścia - jak "y" w modelach 1.21.
 */
public class RenderGate implements ISimpleBlockRenderingHandler {
    private static final double H = 2.0 / 16.0;
    private final int id;

    public RenderGate(int id) {
        this.id = id;
    }

    /** Obrót tekstury góry w krokach 90° w prawo: N = 0, E = 1, S = 2, W = 3. */
    private static int rotation(int facing) {
        return switch (facing) {
            case 5 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            default -> 0;
        };
    }

    /**
     * Górna ściana z UV obróconymi o {@code rot} ćwierćobrotów. Narożniki świata: NW, NE, SE, SW;
     * narożnik tekstury i trafia do narożnika świata (i + rot) % 4.
     */
    private static void drawTop(Tessellator t, double x, double y, double z, IIcon icon, int rot) {
        double[] u = {icon.getMinU(), icon.getMaxU(), icon.getMaxU(), icon.getMinU()};
        double[] v = {icon.getMinV(), icon.getMinV(), icon.getMaxV(), icon.getMaxV()};
        double[] cx = {x, x + 1, x + 1, x};
        double[] cz = {z, z, z + 1, z + 1};
        // kolejność wierzchołków jak w renderFaceYPos: SE, NE, NW, SW
        int[] order = {2, 1, 0, 3};
        for (int c : order) {
            int ti = (c - rot + 4) & 3;
            t.addVertexWithUV(cx[c], y, cz[c], u[ti], v[ti]);
        }
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks rb) {
        if (!(block instanceof BlockGate gate)) return false;
        TileGate te = BlockGate.tile(world, x, y, z);
        IIcon side = rb.hasOverrideBlockTexture() ? rb.overrideBlockTexture : gate.sideIcon();
        IIcon top = rb.hasOverrideBlockTexture() ? rb.overrideBlockTexture : gate.topIcon(te);
        int rot = te == null ? 0 : rotation(te.facing);

        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        rb.setRenderBounds(0, 0, 0, 1, H, 1);
        if (block.shouldSideBeRendered(world, x, y - 1, z, 0)) {
            t.setColorOpaque_F(0.5F, 0.5F, 0.5F);
            rb.renderFaceYNeg(block, x, y, z, side);
        }
        t.setColorOpaque_F(0.8F, 0.8F, 0.8F);
        rb.renderFaceZNeg(block, x, y, z, side);
        rb.renderFaceZPos(block, x, y, z, side);
        t.setColorOpaque_F(0.6F, 0.6F, 0.6F);
        rb.renderFaceXNeg(block, x, y, z, side);
        rb.renderFaceXPos(block, x, y, z, side);
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        drawTop(t, x, y + H, z, top, rot);
        rb.setRenderBounds(0, 0, 0, 1, 1, 1);
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks rb) {
        if (!(block instanceof BlockGate gate)) return;
        IIcon side = gate.sideIcon();
        Tessellator t = Tessellator.instance;
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        rb.setRenderBounds(0, 0, 0, 1, H, 1);
        t.startDrawingQuads();
        t.setNormal(0.0F, -1.0F, 0.0F);
        rb.renderFaceYNeg(block, 0, 0, 0, side);
        t.setNormal(0.0F, 0.0F, -1.0F);
        rb.renderFaceZNeg(block, 0, 0, 0, side);
        t.setNormal(0.0F, 0.0F, 1.0F);
        rb.renderFaceZPos(block, 0, 0, 0, side);
        t.setNormal(-1.0F, 0.0F, 0.0F);
        rb.renderFaceXNeg(block, 0, 0, 0, side);
        t.setNormal(1.0F, 0.0F, 0.0F);
        rb.renderFaceXPos(block, 0, 0, 0, side);
        t.setNormal(0.0F, 1.0F, 0.0F);
        drawTop(t, 0, H, 0, gate.topIcon(null), 0);
        t.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        rb.setRenderBounds(0, 0, 0, 1, 1, 1);
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return id;
    }
}
