package pl.geesio.redcircuit.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.tube.BlockTube;

import java.util.List;

/** Rura: środek (w kolorze farby) i ramiona do połączonych sąsiadów. */
public class RenderTube implements ISimpleBlockRenderingHandler {
    private final int id;

    public RenderTube(int id) {
        this.id = id;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks rb) {
        if (!(block instanceof BlockTube tube)) return false;
        List<float[]> boxes = BlockTube.boxes(world, x, y, z);
        boolean override = rb.hasOverrideBlockTexture();
        for (int i = 0; i < boxes.size(); i++) {
            float[] b = boxes.get(i);
            if (i == 0 && !override) rb.setOverrideBlockTexture(tube.centerIcon(BlockTube.paint(world, x, y, z)));
            rb.setRenderBounds(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0);
            rb.renderStandardBlock(block, x, y, z);
            if (i == 0 && !override) rb.clearOverrideBlockTexture();
        }
        rb.setRenderBounds(0, 0, 0, 1, 1, 1);
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks rb) {
        IIcon icon = block.getIcon(0, meta);
        Tessellator t = Tessellator.instance;
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        for (float[] b : BlockTube.boxes(null, 0, 0, 0)) {
            rb.setRenderBounds(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0);
            t.startDrawingQuads();
            t.setNormal(0.0F, -1.0F, 0.0F);
            rb.renderFaceYNeg(block, 0, 0, 0, icon);
            t.setNormal(0.0F, 1.0F, 0.0F);
            rb.renderFaceYPos(block, 0, 0, 0, icon);
            t.setNormal(0.0F, 0.0F, -1.0F);
            rb.renderFaceZNeg(block, 0, 0, 0, icon);
            t.setNormal(0.0F, 0.0F, 1.0F);
            rb.renderFaceZPos(block, 0, 0, 0, icon);
            t.setNormal(-1.0F, 0.0F, 0.0F);
            rb.renderFaceXNeg(block, 0, 0, 0, icon);
            t.setNormal(1.0F, 0.0F, 0.0F);
            rb.renderFaceXPos(block, 0, 0, 0, icon);
            t.draw();
        }
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
