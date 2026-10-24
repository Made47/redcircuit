package pl.geesio.redcircuit.client;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.RedWireBlock;
import pl.geesio.redcircuit.wire.WireConnections;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model przewodu przyklejonego do ścian - geometria jest generowana w locie
 * na podstawie połączeń z sąsiadami (odczytywanych ze świata podczas budowania siatki chunka).
 */
public class FaceWireModel implements DynamicBlockStateModel {
    private final FaceWireBlock block;
    private final Material.Baked material;
    @Nullable
    private final Material.Baked poweredMaterial;
    private final Map<CacheKey, BlockStateModelPart> cache = new ConcurrentHashMap<>();

    private record CacheKey(BlockState state, WireConnections conn) {}

    public FaceWireModel(FaceWireBlock block, TextureAtlasSprite sprite, @Nullable TextureAtlasSprite poweredSprite) {
        this.block = block;
        this.material = new Material.Baked(sprite, false);
        this.poweredMaterial = poweredSprite == null ? null : new Material.Baked(poweredSprite, false);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        if (!(state.getBlock() instanceof FaceWireBlock wire)) return;
        WireConnections conn = level == BlockAndTintGetter.EMPTY ? WireConnections.EMPTY : wire.computeConnections(level, pos, state);
        parts.add(cache.computeIfAbsent(new CacheKey(state, conn), k -> build(k.state(), k.conn())));
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (!(state.getBlock() instanceof FaceWireBlock wire)) return null;
        return new CacheKey(state, wire.computeConnections(level, pos, state));
    }

    private BlockStateModelPart build(BlockState state, WireConnections conn) {
        Material.Baked tex = material;
        if (poweredMaterial != null && state.hasProperty(RedWireBlock.POWER) && state.getValue(RedWireBlock.POWER) > 0) {
            tex = poweredMaterial;
        }
        QuadCollection.Builder quads = new QuadCollection.Builder();
        for (Direction face : Direction.values()) {
            if (!FaceWireBlock.hasFace(state, face)) continue;
            for (float[] b : block.boxes(face, conn)) {
                addBox(quads, b, tex);
            }
        }
        return new SimpleModelWrapper(quads.build(), true, tex);
    }

    private static void addBox(QuadCollection.Builder out, float[] b, Material.Baked tex) {
        float x0 = b[0] / 16f, y0 = b[1] / 16f, z0 = b[2] / 16f;
        float x1 = b[3] / 16f, y1 = b[4] / 16f, z1 = b[5] / 16f;
        for (Direction side : Direction.values()) {
            BakedQuad quad = new MutableQuad()
                    .setCubeFace(side, x0, y0, z0, x1, y1, z1)
                    .setSprite(tex)
                    .bakeUvsFromPosition()
                    .setShade(true)
                    .toBakedQuad();
            out.addUnculledFace(quad);
        }
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
