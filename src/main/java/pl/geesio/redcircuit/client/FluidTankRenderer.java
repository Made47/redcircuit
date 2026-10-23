package pl.geesio.redcircuit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import pl.geesio.redcircuit.platform.fluids.FluidStack;
import pl.geesio.redcircuit.fluid.FluidTankBlockEntity;

/** Rysuje ciecz w zbiorniku (prostopadłościan o wysokości zależnej od wypełnienia). */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {
    private static final float MIN = 1.02f / 16f, MAX = 14.98f / 16f;

    public FluidTankRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(FluidTankBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fs = be.getTank().getFluid();
        if (fs.isEmpty()) return;
        FluidVariant variant = fs.toVariant();
        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(variant);
        if (sprite == null) return;
        int color = FluidVariantRendering.getColor(variant, be.getLevel(), be.getBlockPos());
        if ((color >>> 24) == 0) color |= 0xFF000000;
        int fluidLight = fs.getFluidType().getLightLevel(fs);
        int lightUv = LightTexture.pack(Math.max(LightTexture.block(light), fluidLight), LightTexture.sky(light));

        float h = Math.max(0.02f, be.fill());
        float y0 = 1.02f / 16f, y1 = y0 + (MAX - y0) * h;
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose p = pose.last();

        // góra
        quad(vc, p, sprite, color, lightUv, 0, 1, 0,
                MIN, y1, MIN, MIN, y1, MAX, MAX, y1, MAX, MAX, y1, MIN, 0, 0, 1, 1);
        // dół
        quad(vc, p, sprite, color, lightUv, 0, -1, 0,
                MIN, y0, MAX, MIN, y0, MIN, MAX, y0, MIN, MAX, y0, MAX, 0, 0, 1, 1);
        float v0 = 1 - h;
        // północ (z = MIN)
        quad(vc, p, sprite, color, lightUv, 0, 0, -1,
                MAX, y1, MIN, MAX, y0, MIN, MIN, y0, MIN, MIN, y1, MIN, 0, v0, 1, 1);
        // południe (z = MAX)
        quad(vc, p, sprite, color, lightUv, 0, 0, 1,
                MIN, y1, MAX, MIN, y0, MAX, MAX, y0, MAX, MAX, y1, MAX, 0, v0, 1, 1);
        // zachód (x = MIN)
        quad(vc, p, sprite, color, lightUv, -1, 0, 0,
                MIN, y1, MIN, MIN, y0, MIN, MIN, y0, MAX, MIN, y1, MAX, 0, v0, 1, 1);
        // wschód (x = MAX)
        quad(vc, p, sprite, color, lightUv, 1, 0, 0,
                MAX, y1, MAX, MAX, y0, MAX, MAX, y0, MIN, MAX, y1, MIN, 0, v0, 1, 1);
    }

    /** Czworokąt: wierzchołki (x,y,z) x4 w kolejności przeciwnej do ruchu wskazówek, UV jako ułamki sprite'a. */
    private static void quad(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite s, int color, int light,
                             float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float u0, float v0, float u1, float v1) {
        float a = s.getU(u0), b = s.getU(u1), c = s.getV(v0), d = s.getV(v1);
        vc.addVertex(p, x1, y1, z1).setColor(color).setUv(a, c).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x2, y2, z2).setColor(color).setUv(a, d).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x3, y3, z3).setColor(color).setUv(b, d).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x4, y4, z4).setColor(color).setUv(b, c).setLight(light).setNormal(p, nx, ny, nz);
    }
}
