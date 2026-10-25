package pl.geesio.redcircuit.client;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.RedWireBlock;
import pl.geesio.redcircuit.wire.WireConnections;

import java.util.List;
import java.util.function.Predicate;

/**
 * Model przewodu przyklejonego do ścian - geometria jest generowana w locie
 * na podstawie połączeń z sąsiadami (odczytywanych ze świata podczas budowania siatki chunka).
 * Fabric: quady są emitowane przez QuadEmitter (Fabric Renderer API).
 */
public class FaceWireModel implements BlockStateModel {
    private final FaceWireBlock block;
    private final Material.Baked material;
    @Nullable
    private final Material.Baked poweredMaterial;

    public FaceWireModel(FaceWireBlock block, Material.Baked material, @Nullable Material.Baked poweredMaterial) {
        this.block = block;
        this.material = material;
        this.poweredMaterial = poweredMaterial;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
                          RandomSource random, Predicate<@Nullable Direction> cullTest) {
        if (!(state.getBlock() instanceof FaceWireBlock wire)) return;
        WireConnections conn = level == BlockAndTintGetter.EMPTY ? WireConnections.EMPTY : wire.computeConnections(level, pos, state);
        Material.Baked tex = material;
        if (poweredMaterial != null && state.hasProperty(RedWireBlock.POWER) && state.getValue(RedWireBlock.POWER) > 0) {
            tex = poweredMaterial;
        }
        for (Direction face : Direction.values()) {
            if (!FaceWireBlock.hasFace(state, face)) continue;
            for (float[] b : block.boxes(face, conn)) {
                addBox(emitter, b, tex);
            }
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (!(state.getBlock() instanceof FaceWireBlock wire)) return null;
        return new Key(state, wire.computeConnections(level, pos, state));
    }

    private record Key(BlockState state, WireConnections conn) {}

    private static void addBox(QuadEmitter out, float[] b, Material.Baked tex) {
        float x0 = b[0] / 16f, y0 = b[1] / 16f, z0 = b[2] / 16f;
        float x1 = b[3] / 16f, y1 = b[4] / 16f, z1 = b[5] / 16f;
        for (Direction side : Direction.values()) {
            float[][] v = switch (side) {
                case DOWN -> new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
                case UP -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
                case NORTH -> new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
                case SOUTH -> new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
                case WEST -> new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
                case EAST -> new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
            };
            for (int i = 0; i < 4; i++) out.pos(i, v[i][0], v[i][1], v[i][2]);
            out.nominalFace(side);
            out.color(-1, -1, -1, -1);
            out.materialBake(tex, MutableQuadView.BAKE_LOCK_UV);
            out.emit();
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        // Bez świata (np. spadający blok) - geometria jest tylko w emitQuads.
    }

    @Override
    public Material.Baked particleMaterial() {
        return material;
    }

    @Override
    public int materialFlags() {
        return 0;
    }
}
