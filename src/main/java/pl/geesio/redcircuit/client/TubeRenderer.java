package pl.geesio.redcircuit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import pl.geesio.redcircuit.tube.TubeBlockEntity;
import pl.geesio.redcircuit.tube.TubeItem;

/** Rysuje przedmioty jadące przez rurę. */
public class TubeRenderer implements BlockEntityRenderer<TubeBlockEntity> {
    private final ItemRenderer itemRenderer;

    public TubeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(TubeBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
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
            pose.pushPose();
            pose.translate(0.5 + dir.getStepX() * dist, 0.5 + dir.getStepY() * dist, 0.5 + dir.getStepZ() * dist);
            pose.scale(0.35f, 0.35f, 0.35f);
            itemRenderer.renderStatic(item.stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, be.getLevel(), i++);
            pose.popPose();
        }
    }
}
