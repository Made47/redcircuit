package pl.geesio.redcircuit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pl.geesio.redcircuit.energy.GeneratorBlockEntity;
import pl.geesio.redcircuit.energy.KineticGeneratorBlock;

/**
 * Trójwymiarowy wiatrak (ok. 5x5) i koło wodne (3x3) przed generatorem.
 * Model składa się z prostopadłościanów liczonych w pikselach (1/16 bloku) i jest oteksturowany
 * teksturami bloków vanilla z atlasu (drewno, wełna, żelazo) - bez kopiowania plików.
 */
public class RotorRenderer implements BlockEntityRenderer<GeneratorBlockEntity, RotorRenderer.State> {
    public static class State extends BlockEntityRenderState {
        KineticGeneratorBlock.Rotor rotor = KineticGeneratorBlock.Rotor.NONE;
        Direction facing = Direction.NORTH;
        float angle;
        int frontLight;
    }

    private static final Identifier PLANKS = Identifier.withDefaultNamespace("block/oak_planks");
    private static final Identifier BEAM = Identifier.withDefaultNamespace("block/stripped_spruce_log");
    private static final Identifier DARK = Identifier.withDefaultNamespace("block/spruce_planks");
    private static final Identifier CLOTH = Identifier.withDefaultNamespace("block/white_wool");
    private static final Identifier METAL = Identifier.withDefaultNamespace("block/iron_block");

    public RotorRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GeneratorBlockEntity be, State st, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, st, partialTick, camera, breakProgress);
        BlockState state = be.getBlockState();
        st.rotor = KineticGeneratorBlock.Rotor.NONE;
        if (!(state.getBlock() instanceof KineticGeneratorBlock) || be.getLevel() == null) return;
        st.rotor = state.getValue(KineticGeneratorBlock.ROTOR);
        if (st.rotor == KineticGeneratorBlock.Rotor.NONE) return;
        st.facing = state.getValue(KineticGeneratorBlock.FACING);
        float speed = st.rotor == KineticGeneratorBlock.Rotor.WINDMILL ? 2.0f : 3.5f;   // stopnie na tick
        st.angle = state.getValue(KineticGeneratorBlock.RUNNING)
                ? ((be.getLevel().getGameTime() % 3600) + partialTick) * speed : 0f;
        st.frontLight = LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos().relative(st.facing));
    }

    @Override
    public void submit(State st, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (st.rotor == KineticGeneratorBlock.Rotor.NONE) return;
        boolean windmill = st.rotor == KineticGeneratorBlock.Rotor.WINDMILL;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-st.facing.toYRot()));   // lokalne +Z = przód generatora
        pose.translate(0, 0, 0.5);                                    // płaszczyzna przedniej ściany
        pose.scale(1 / 16f, 1 / 16f, 1 / 16f);                        // dalej wszystko w pikselach
        // Wiatrak obraca się w prawo (patrząc od przodu), koło wodne - z nurtem
        pose.mulPose(Axis.ZP.rotationDegrees(windmill ? -st.angle : st.angle));
        int light = st.frontLight;
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (last, vc) -> {
            PoseStack ps = new PoseStack();
            ps.last().set(last);
            Parts p = new Parts(ps, vc, light, OverlayTexture.NO_OVERLAY);
            if (windmill) windmill(p);
            else waterWheel(p);
        });
        pose.popPose();
    }

    // ------------------------------------------------------------------ modele

    private static void windmill(Parts p) {
        TextureAtlasSprite beam = sprite(BEAM), metal = sprite(METAL), cloth = sprite(CLOTH), planks = sprite(PLANKS);
        p.box(beam, -2, -2, 0, 2, 2, 9);                   // wał wychodzący z generatora
        p.box(metal, -5, -5, 6, 5, 5, 12);                 // piasta
        p.box(metal, -3, -3, 12, 3, 3, 14);                // kołpak
        for (int k = 0; k < 4; k++) {
            p.pose.pushPose();
            p.pose.mulPose(Axis.ZP.rotationDegrees(k * 90));
            p.box(beam, -1.5f, 4, 7.5f, 1.5f, 38, 10.5f);    // ramię
            // Skrzydło: płótno na ramie, lekko skręcone jak w prawdziwym wiatraku
            p.pose.pushPose();
            p.pose.translate(1.5f, 0, 9);
            p.pose.mulPose(Axis.YP.rotationDegrees(-14));
            p.box(cloth, 0, 9, -0.5f, 12.5f, 37, 0.5f);
            for (float y : new float[]{9, 16, 23, 30, 36}) {
                p.box(planks, 0, y, 0.5f, 12.5f, y + 1, 1.5f);   // poprzeczki
            }
            p.box(planks, 11.5f, 9, 0.5f, 12.5f, 37, 1.5f);      // listwa zewnętrzna
            p.pose.popPose();
            p.pose.popPose();
        }
    }

    private static void waterWheel(Parts p) {
        TextureAtlasSprite beam = sprite(BEAM), metal = sprite(METAL), planks = sprite(PLANKS), dark = sprite(DARK);
        p.box(beam, -2, -2, 0, 2, 2, 16);                  // oś
        p.box(metal, -4, -4, 2, 4, 4, 14);                 // piasta
        float[][] rims = {{2, 4}, {12, 14}};
        for (float[] rim : rims) {
            // Obręcz z 16 segmentów
            for (int i = 0; i < 16; i++) {
                p.pose.pushPose();
                p.pose.mulPose(Axis.ZP.rotationDegrees(i * 22.5f));
                p.box(dark, -4.4f, 20, rim[0], 4.4f, 23, rim[1]);
                p.pose.popPose();
            }
            // Szprychy
            for (int i = 0; i < 8; i++) {
                p.pose.pushPose();
                p.pose.mulPose(Axis.ZP.rotationDegrees(i * 45 + 22.5f));
                p.box(beam, -1, 4, rim[0] + 0.5f, 1, 20, rim[1] - 0.5f);
                p.pose.popPose();
            }
        }
        // Łopatki między obręczami, wystające nieco poza nie
        for (int i = 0; i < 12; i++) {
            p.pose.pushPose();
            p.pose.mulPose(Axis.ZP.rotationDegrees(i * 30));
            p.box(planks, -0.75f, 13, 2, 0.75f, 24, 14);
            p.pose.popPose();
        }
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
    }

    // ------------------------------------------------------------------ prostopadłościany

    /** Rysowanie prostopadłościanów w pikselach; tekstura powtarza się co 16 pikseli (bez rozciągania). */
    private record Parts(PoseStack pose, VertexConsumer vc, int light, int overlay) {

        void box(TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1) {
            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            // przód (+Z) i tył (-Z)
            face(s, x0, y0, z1, 1, 0, 0, dx, 0, 1, 0, dy, 0, 0, 1);
            face(s, x1, y0, z0, -1, 0, 0, dx, 0, 1, 0, dy, 0, 0, -1);
            // prawo (+X) i lewo (-X)
            face(s, x1, y0, z1, 0, 0, -1, dz, 0, 1, 0, dy, 1, 0, 0);
            face(s, x0, y0, z0, 0, 0, 1, dz, 0, 1, 0, dy, -1, 0, 0);
            // góra (+Y) i dół (-Y)
            face(s, x0, y1, z1, 1, 0, 0, dx, 0, 0, -1, dz, 0, 1, 0);
            face(s, x0, y0, z0, 1, 0, 0, dx, 0, 0, 1, dz, 0, -1, 0);
        }

        /** Ściana od punktu o, wzdłuż osi u (długość lu) i v (długość lv), z normalną n; dzielona na kafle 16x16. */
        private void face(TextureAtlasSprite s, float ox, float oy, float oz,
                          float ux, float uy, float uz, float lu,
                          float vx, float vy, float vz, float lv,
                          float nx, float ny, float nz) {
            PoseStack.Pose last = pose.last();
            for (float a = 0; a < lu - 1e-4f; a += 16) {
                float a1 = Math.min(lu, a + 16);
                for (float b = 0; b < lv - 1e-4f; b += 16) {
                    float b1 = Math.min(lv, b + 16);
                    float u0 = s.getU((a % 16) / 16f), u1 = s.getU((a % 16 + (a1 - a)) / 16f);
                    float v0 = s.getV(1 - (b % 16 + (b1 - b)) / 16f), v1 = s.getV(1 - (b % 16) / 16f);
                    vertex(last, ox + ux * a + vx * b, oy + uy * a + vy * b, oz + uz * a + vz * b, u0, v1, nx, ny, nz);
                    vertex(last, ox + ux * a1 + vx * b, oy + uy * a1 + vy * b, oz + uz * a1 + vz * b, u1, v1, nx, ny, nz);
                    vertex(last, ox + ux * a1 + vx * b1, oy + uy * a1 + vy * b1, oz + uz * a1 + vz * b1, u1, v0, nx, ny, nz);
                    vertex(last, ox + ux * a + vx * b1, oy + uy * a + vy * b1, oz + uz * a + vz * b1, u0, v0, nx, ny, nz);
                }
            }
        }

        private void vertex(PoseStack.Pose last, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            vc.addVertex(last, x, y, z)
                    .setColor(255, 255, 255, 255)
                    .setUv(u, v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(last, nx, ny, nz);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
