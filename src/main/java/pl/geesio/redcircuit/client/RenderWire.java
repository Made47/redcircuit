package pl.geesio.redcircuit.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.wire.BlockEncasedWire;
import pl.geesio.redcircuit.wire.BlockFaceWire;

import java.util.List;

/**
 * Rysowanie przewodów (przyklejanych do ścian i w obudowie) z prostopadłościanów wyliczonych przez blok -
 * zamiast dynamicznego modelu z wersji 1.21.
 */
public class RenderWire implements ISimpleBlockRenderingHandler {
    private final int id;
    private final boolean inventory3d;

    public RenderWire(int id, boolean inventory3d) {
        this.id = id;
        this.inventory3d = inventory3d;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks rb) {
        List<float[]> boxes;
        if (block instanceof BlockFaceWire wire) boxes = wire.allBoxes(world, x, y, z);
        else if (block instanceof BlockEncasedWire encased) boxes = encased.boxes(world, x, y, z);
        else return false;
        for (float[] b : boxes) {
            rb.setRenderBounds(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0, b[5] / 16.0);
            rb.renderStandardBlock(block, x, y, z);
        }
        rb.setRenderBounds(0, 0, 0, 1, 1, 1);
        return !boxes.isEmpty();
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks rb) {
        List<float[]> boxes;
        if (block instanceof BlockEncasedWire encased) boxes = encased.boxes(null, 0, 0, 0);
        else return;
        IIcon icon = block.getIcon(0, meta);
        Tessellator t = Tessellator.instance;
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        for (float[] b : boxes) {
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
        return inventory3d;
    }

    @Override
    public int getRenderId() {
        return id;
    }
}
