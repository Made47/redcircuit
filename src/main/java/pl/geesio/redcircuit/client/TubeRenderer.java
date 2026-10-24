package pl.geesio.redcircuit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.tube.TubeBlockEntity;
import pl.geesio.redcircuit.tube.TubeItem;

import java.util.ArrayList;
import java.util.List;

/** Rysuje przedmioty jadące przez rurę. */
public class TubeRenderer implements BlockEntityRenderer<TubeBlockEntity, TubeRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public static class State extends BlockEntityRenderState {
        final List<Entry> items = new ArrayList<>();
    }

    record Entry(ItemStackRenderState item, float x, float y, float z) {}

    public TubeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TubeBlockEntity be, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.items.clear();
        int seed = (int) be.getBlockPos().asLong();
        int i = 0;
        for (TubeItem item : be.getItems()) {
            float limit = item.to == null ? 0.5f : 1.0f;
            float p = Math.min(limit, item.progress + item.speed * partialTick);
            Direction dir;
            float dist;
            if (p < 0.5f || item.to == null) {
                dir = item.from;
                dist = 0.5f - p;
            } else {
                dir = item.to;
                dist = p - 0.5f;
            }
            ItemStackRenderState rs = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(rs, item.stack, ItemDisplayContext.FIXED, be.getLevel(), null, seed + i++);
            state.items.add(new Entry(rs, 0.5f + dir.getStepX() * dist, 0.5f + dir.getStepY() * dist, 0.5f + dir.getStepZ() * dist));
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Entry e : state.items) {
            if (e.item().isEmpty()) continue;
            pose.pushPose();
            pose.translate(e.x(), e.y(), e.z());
            pose.scale(0.35f, 0.35f, 0.35f);
            e.item().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
