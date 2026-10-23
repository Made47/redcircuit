package pl.geesio.redcircuit.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import pl.geesio.redcircuit.wire.FaceWireBlock;
import pl.geesio.redcircuit.wire.RedWireBlock;
import pl.geesio.redcircuit.wire.WireConnections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model przewodu przyklejonego do ścian - geometria jest generowana w locie
 * na podstawie połączeń z sąsiadami (ModelData).
 */
public class FaceWireModel implements BakedModel {
    public static final ModelProperty<WireConnections> CONNECTIONS = new ModelProperty<>();

    private final FaceWireBlock block;
    private final TextureAtlasSprite sprite;
    @Nullable
    private final TextureAtlasSprite poweredSprite;
    private final Map<CacheKey, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    private record CacheKey(BlockState state, WireConnections conn) {}

    public FaceWireModel(FaceWireBlock block, TextureAtlasSprite sprite, @Nullable TextureAtlasSprite poweredSprite) {
        this.block = block;
        this.sprite = sprite;
        this.poweredSprite = poweredSprite;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (!(state.getBlock() instanceof FaceWireBlock wire)) return modelData;
        return modelData.derive().with(CONNECTIONS, wire.computeConnections(level, pos, state)).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (side != null || state == null) return List.of();
        WireConnections conn = data.get(CONNECTIONS);
        if (conn == null) conn = WireConnections.EMPTY;
        return cache.computeIfAbsent(new CacheKey(state, conn), k -> build(k.state(), k.conn()));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    private List<BakedQuad> build(BlockState state, WireConnections conn) {
        TextureAtlasSprite tex = sprite;
        if (poweredSprite != null && state.hasProperty(RedWireBlock.POWER) && state.getValue(RedWireBlock.POWER) > 0) {
            tex = poweredSprite;
        }
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction face : Direction.values()) {
            if (!FaceWireBlock.hasFace(state, face)) continue;
            for (float[] b : block.boxes(face, conn)) {
                addBox(quads, b, tex);
            }
        }
        return quads;
    }

    // ------------------------------------------------------------------ geometria quadów

    private static void addBox(List<BakedQuad> out, float[] b, TextureAtlasSprite tex) {
        float x0 = b[0] / 16f, y0 = b[1] / 16f, z0 = b[2] / 16f;
        float x1 = b[3] / 16f, y1 = b[4] / 16f, z1 = b[5] / 16f;
        out.add(quad(Direction.DOWN, tex, new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}}));
        out.add(quad(Direction.UP, tex, new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}}));
        out.add(quad(Direction.NORTH, tex, new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}}));
        out.add(quad(Direction.SOUTH, tex, new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}}));
        out.add(quad(Direction.WEST, tex, new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}}));
        out.add(quad(Direction.EAST, tex, new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}}));
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static BakedQuad quad(Direction face, TextureAtlasSprite tex, float[][] v) {
        int[] data = new int[32];
        int normal = (((int) (face.getStepX() * 127)) & 0xFF)
                | ((((int) (face.getStepY() * 127)) & 0xFF) << 8)
                | ((((int) (face.getStepZ() * 127)) & 0xFF) << 16);
        for (int i = 0; i < 4; i++) {
            float x = v[i][0], y = v[i][1], z = v[i][2];
            // Automatyczne UV jak w modelach JSON (rzut na płaszczyznę ściany).
            float u, w;
            switch (face) {
                case DOWN -> { u = x; w = 1 - z; }
                case UP -> { u = x; w = z; }
                case NORTH -> { u = 1 - x; w = 1 - y; }
                case SOUTH -> { u = x; w = 1 - y; }
                case WEST -> { u = z; w = 1 - y; }
                default -> { u = 1 - z; w = 1 - y; }
            }
            u = clamp01(u);
            w = clamp01(w);
            float su = tex.getU0() + (tex.getU1() - tex.getU0()) * u;
            float sv = tex.getV0() + (tex.getV1() - tex.getV0()) * w;
            int o = i * 8;
            data[o] = Float.floatToRawIntBits(x);
            data[o + 1] = Float.floatToRawIntBits(y);
            data[o + 2] = Float.floatToRawIntBits(z);
            data[o + 3] = 0xFFFFFFFF; // kolor
            data[o + 4] = Float.floatToRawIntBits(su);
            data[o + 5] = Float.floatToRawIntBits(sv);
            data[o + 6] = 0; // światło (liczone przez silnik)
            data[o + 7] = normal;
        }
        return new BakedQuad(data, -1, face, tex, true);
    }

    // ------------------------------------------------------------------ BakedModel

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return sprite;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
