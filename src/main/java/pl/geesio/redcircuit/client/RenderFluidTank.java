package pl.geesio.redcircuit.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.fluid.TileFluidTank;

/** Rysuje ciecz w zbiorniku (prostopadłościan o wysokości zależnej od wypełnienia). */
public class RenderFluidTank extends TileEntitySpecialRenderer {
    private static final double MIN = 1.02 / 16, MAX = 14.98 / 16;

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTick) {
        if (!(te instanceof TileFluidTank tank)) return;
        FluidStack fs = tank.tank.getFluid();
        if (fs == null || fs.amount <= 0 || fs.getFluid() == null) return;
        IIcon icon = fs.getFluid().getStillIcon();
        if (icon == null) icon = fs.getFluid().getIcon(fs);
        if (icon == null) return;
        int color = fs.getFluid().getColor(fs);
        double h = Math.max(0.02, tank.fill());
        double y0 = 1.02 / 16, y1 = y0 + (MAX - y0) * h;

        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        boolean glow = fs.getFluid().getLuminosity(fs) > 0;
        if (glow) GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1F);

        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();
        double vs = icon.getInterpolatedV(16 - h * 16);   // boki: tekstura od dołu do poziomu cieczy
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        if (glow) t.setBrightness(0xF000F0);
        // góra
        t.setNormal(0, 1, 0);
        t.addVertexWithUV(MIN, y1, MIN, u0, v0);
        t.addVertexWithUV(MIN, y1, MAX, u0, v1);
        t.addVertexWithUV(MAX, y1, MAX, u1, v1);
        t.addVertexWithUV(MAX, y1, MIN, u1, v0);
        // dół
        t.setNormal(0, -1, 0);
        t.addVertexWithUV(MIN, y0, MAX, u0, v1);
        t.addVertexWithUV(MIN, y0, MIN, u0, v0);
        t.addVertexWithUV(MAX, y0, MIN, u1, v0);
        t.addVertexWithUV(MAX, y0, MAX, u1, v1);
        // północ (z = MIN)
        t.setNormal(0, 0, -1);
        t.addVertexWithUV(MAX, y1, MIN, u0, vs);
        t.addVertexWithUV(MAX, y0, MIN, u0, v1);
        t.addVertexWithUV(MIN, y0, MIN, u1, v1);
        t.addVertexWithUV(MIN, y1, MIN, u1, vs);
        // południe (z = MAX)
        t.setNormal(0, 0, 1);
        t.addVertexWithUV(MIN, y1, MAX, u0, vs);
        t.addVertexWithUV(MIN, y0, MAX, u0, v1);
        t.addVertexWithUV(MAX, y0, MAX, u1, v1);
        t.addVertexWithUV(MAX, y1, MAX, u1, vs);
        // zachód (x = MIN)
        t.setNormal(-1, 0, 0);
        t.addVertexWithUV(MIN, y1, MIN, u0, vs);
        t.addVertexWithUV(MIN, y0, MIN, u0, v1);
        t.addVertexWithUV(MIN, y0, MAX, u1, v1);
        t.addVertexWithUV(MIN, y1, MAX, u1, vs);
        // wschód (x = MAX)
        t.setNormal(1, 0, 0);
        t.addVertexWithUV(MAX, y1, MAX, u0, vs);
        t.addVertexWithUV(MAX, y0, MAX, u0, v1);
        t.addVertexWithUV(MAX, y0, MIN, u1, v1);
        t.addVertexWithUV(MAX, y1, MIN, u1, vs);
        t.draw();

        if (glow) GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
