package pl.geesio.redcircuit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.fluid.FluidTankBlockEntity;

/** Rysuje ciecz w zbiorniku (prostopadłościan o wysokości zależnej od wypełnienia). */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State> {
    private static final float MIN = 1.02f / 16f, MAX = 14.98f / 16f;

    public static class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite sprite;
        int color;
        int light;
        float fill;
    }

    public FluidTankRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity be, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        FluidStack fs = be.getTank().getFluid();
        state.sprite = null;
        if (fs.isEmpty()) return;
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fs.getFluid().defaultFluidState());
        state.sprite = model.stillMaterial().sprite();
        int color = model.fluidTintSource() != null ? model.fluidTintSource().colorAsStack(fs) : 0xFFFFFFFF;
        if ((color >>> 24) == 0) color |= 0xFF000000;
        state.color = color;
        int fluidLight = fs.getFluidType().getLightLevel(fs);
        state.light = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(state.lightCoords), fluidLight), LightCoordsUtil.sky(state.lightCoords));
        state.fill = be.fill();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null) return;
        int color = state.color, lightUv = state.light;
        float h = Math.max(0.02f, state.fill);
        float y0 = 1.02f / 16f, y1 = y0 + (MAX - y0) * h;
        float v0 = 1 - h;
        collector.submitCustomGeometry(pose, Sheets.translucentBlockSheet(), (p, vc) -> {
            // góra
            quad(vc, p, sprite, color, lightUv, 0, 1, 0,
                    MIN, y1, MIN, MIN, y1, MAX, MAX, y1, MAX, MAX, y1, MIN, 0, 0, 1, 1);
            // dół
            quad(vc, p, sprite, color, lightUv, 0, -1, 0,
                    MIN, y0, MAX, MIN, y0, MIN, MAX, y0, MIN, MAX, y0, MAX, 0, 0, 1, 1);
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
        });
    }

    /** Czworokąt: wierzchołki (x,y,z) x4 w kolejności przeciwnej do ruchu wskazówek, UV jako ułamki sprite'a. */
    private static void quad(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite s, int color, int light,
                             float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float u0, float v0, float u1, float v1) {
        float a = s.getU(u0), b = s.getU(u1), c = s.getV(v0), d = s.getV(v1);
        vc.addVertex(p, x1, y1, z1).setColor(color).setUv(a, c).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x2, y2, z2).setColor(color).setUv(a, d).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x3, y3, z3).setColor(color).setUv(b, d).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x4, y4, z4).setColor(color).setUv(b, c).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
