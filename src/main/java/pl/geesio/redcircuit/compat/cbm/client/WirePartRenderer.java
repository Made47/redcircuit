package pl.geesio.redcircuit.compat.cbm.client;

import codechicken.lib.render.CCRenderState;
import codechicken.multipart.api.part.render.PartRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.RedCircuit;
import pl.geesio.redcircuit.client.FaceWireModel;
import pl.geesio.redcircuit.compat.cbm.RCWirePart;

import java.util.List;

/**
 * Renderuje przewód-część tym samym modelem co blok-przewód (FaceWireModel) z połączeniami części.
 * Obsługuje oba sposoby renderowania CB Multipart: nowszy (getQuads, od 3.5.0.15x)
 * i starszy (renderStatic przez CCRenderState, np. 3.5.0.148).
 */
public class WirePartRenderer implements PartRenderer<RCWirePart> {
    private static boolean errorLogged;

    /** Quady przewodu-części (bez filtrowania warstwy). */
    private static List<BakedQuad> quads(RCWirePart part, RandomSource rand) {
        if (!part.hasLevel()) return List.of();
        try {
            BlockState state = part.renderState();
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            ModelData md = ModelData.builder().with(FaceWireModel.CONNECTIONS, part.connections(part.level())).build();
            return model.getQuads(state, null, rand, md, null);
        } catch (RuntimeException e) {
            if (!errorLogged) {
                errorLogged = true;
                RedCircuit.LOGGER.error("RedCircuit: błąd renderowania przewodu-części CB Multipart", e);
            }
            return List.of();
        }
    }

    /** Przewody rysujemy w jednej warstwie (solid), żeby nie dublować quadów. */
    private static boolean ourLayer(@Nullable RenderType type) {
        return type == null || type == RenderType.solid();
    }

    @Override
    public List<BakedQuad> getQuads(RCWirePart part, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (side != null || !ourLayer(renderType)) return List.of();
        return quads(part, rand);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void renderStatic(RCWirePart part, @Nullable RenderType layer, CCRenderState ccrs) {
        if (layer == null || !ourLayer(layer)) return;
        VertexConsumer consumer = ccrs.getConsumer();
        PoseStack.Pose pose = new PoseStack().last();
        int light = LevelRenderer.getLightColor(part.level(), part.pos());
        for (BakedQuad quad : quads(part, RandomSource.create(42L))) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void renderBreaking(RCWirePart part, CCRenderState ccrs) {
        VertexConsumer consumer = ccrs.getConsumer();
        PoseStack.Pose pose = new PoseStack().last();
        int light = LevelRenderer.getLightColor(part.level(), part.pos());
        for (BakedQuad quad : quads(part, RandomSource.create(42L))) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        }
    }
}
