package pl.geesio.redcircuit.client;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.tube.TileTube;
import pl.geesio.redcircuit.tube.TubeItem;

/** Rysuje przedmioty jadące przez rurę. */
public class RenderTubeItems extends TileEntitySpecialRenderer {
    private final RenderItem renderer;
    private EntityItem dummy;

    public RenderTubeItems() {
        renderer = new RenderItem() {
            @Override
            public boolean shouldBob() {
                return false;
            }

            @Override
            public boolean shouldSpreadItems() {
                return false;
            }
        };
        renderer.setRenderManager(RenderManager.instance);
    }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTick) {
        if (!(te instanceof TileTube tube) || tube.getItems().isEmpty()) return;
        if (dummy == null || dummy.worldObj != te.getWorldObj()) dummy = new EntityItem(te.getWorldObj());
        dummy.hoverStart = 0.0F;
        boolean fancy = RenderItem.renderInFrame;
        RenderItem.renderInFrame = true;
        for (TubeItem item : tube.getItems()) {
            float limit = item.to == null ? 0.5F : 1.0F;
            float p = Math.min(limit, item.progress + item.speed * partialTick);
            ForgeDirection dir;
            float dist;
            if (p < 0.5F || item.to == null) {
                dir = item.from;
                dist = 0.5F - p;
            } else {
                dir = item.to;
                dist = p - 0.5F;
            }
            dummy.setEntityItemStack(item.stack);
            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5 + dir.offsetX * dist, y + 0.5 + dir.offsetY * dist - 0.1, z + 0.5 + dir.offsetZ * dist);
            GL11.glScalef(0.7F, 0.7F, 0.7F);
            renderer.doRender(dummy, 0, 0, 0, 0, 0);
            GL11.glPopMatrix();
        }
        RenderItem.renderInFrame = fancy;
    }
}
