package pl.geesio.redcircuit.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.wireless.BlockWireless;

/** Nadajnik / odbiornik: płytka 2 px, pręt anteny i końcówka (świeci, gdy blok jest aktywny). */
public class RenderWireless implements ISimpleBlockRenderingHandler {
    private static final double P = 1.0 / 16.0;
    private final int id;

    public RenderWireless(int id) {
        this.id = id;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks rb) {
        if (!(block instanceof BlockWireless w)) return false;
        boolean override = rb.hasOverrideBlockTexture();
        boolean on = world.getBlockMetadata(x, y, z) != 0;

        // płytka - zwykłe oświetlenie (z AO)
        rb.setRenderBounds(0, 0, 0, 1, 2 * P, 1);
        rb.renderStandardBlock(block, x, y, z);

        // pręt
        if (!override) rb.setOverrideBlockTexture(w.rodIcon());
        rb.setRenderBounds(7.5 * P, 2 * P, 7.5 * P, 8.5 * P, 11 * P, 8.5 * P);
        rb.renderStandardBlock(block, x, y, z);
        if (!override) rb.clearOverrideBlockTexture();

        // końcówka: aktywna świeci pełną jasnością
        IIcon tip = override ? rb.overrideBlockTexture : w.tipIcon(on);
        rb.setRenderBounds(7 * P, 11 * P, 7 * P, 9 * P, 13 * P, 9 * P);
        if (on) {
            Tessellator t = Tessellator.instance;
            t.setBrightness(0xF000F0);
            t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            rb.renderFaceYNeg(block, x, y, z, tip);
            rb.renderFaceYPos(block, x, y, z, tip);
            rb.renderFaceZNeg(block, x, y, z, tip);
            rb.renderFaceZPos(block, x, y, z, tip);
            rb.renderFaceXNeg(block, x, y, z, tip);
            rb.renderFaceXPos(block, x, y, z, tip);
        } else {
            if (!override) rb.setOverrideBlockTexture(tip);
            rb.renderStandardBlock(block, x, y, z);
            if (!override) rb.clearOverrideBlockTexture();
        }
        rb.setRenderBounds(0, 0, 0, 1, 1, 1);
        return true;
    }

    private static void box(Block block, RenderBlocks rb, double[] b, IIcon top, IIcon side) {
        Tessellator t = Tessellator.instance;
        rb.setRenderBounds(b[0], b[1], b[2], b[3], b[4], b[5]);
        t.startDrawingQuads();
        t.setNormal(0.0F, -1.0F, 0.0F);
        rb.renderFaceYNeg(block, 0, 0, 0, side);
        t.setNormal(0.0F, 1.0F, 0.0F);
        rb.renderFaceYPos(block, 0, 0, 0, top);
        t.setNormal(0.0F, 0.0F, -1.0F);
        rb.renderFaceZNeg(block, 0, 0, 0, side);
        t.setNormal(0.0F, 0.0F, 1.0F);
        rb.renderFaceZPos(block, 0, 0, 0, side);
        t.setNormal(-1.0F, 0.0F, 0.0F);
        rb.renderFaceXNeg(block, 0, 0, 0, side);
        t.setNormal(1.0F, 0.0F, 0.0F);
        rb.renderFaceXPos(block, 0, 0, 0, side);
        t.draw();
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks rb) {
        if (!(block instanceof BlockWireless w)) return;
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        box(block, rb, new double[]{0, 0, 0, 1, 2 * P, 1}, block.getIcon(1, 0), block.getIcon(2, 0));
        box(block, rb, new double[]{7.5 * P, 2 * P, 7.5 * P, 8.5 * P, 11 * P, 8.5 * P}, w.rodIcon(), w.rodIcon());
        box(block, rb, new double[]{7 * P, 11 * P, 7 * P, 9 * P, 13 * P, 9 * P}, w.tipIcon(false), w.tipIcon(false));
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
