package pl.geesio.redcircuit.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sieć bezprzewodowa: częstotliwości 1..5000, wspólne dla wszystkich wymiarów.
 * Częstotliwość jest aktywna, gdy nadaje na niej choć jeden zasilony nadajnik albo wciśnięty pilot.
 * Aktywne nadajniki są zapisywane w świecie (działają też w niezaładowanych chunkach).
 */
public class WirelessNetwork extends SavedData {
    public static final int MIN_FREQ = 1, MAX_FREQ = 5000;
    private static final String NAME = "redcircuit_wireless";

    private final Map<Integer, Set<GlobalPos>> transmitters = new HashMap<>();
    /** Piloty: częstotliwość -> (gracz -> czas wygaśnięcia). Nie są zapisywane. */
    private final Map<Integer, Map<UUID, Long>> remotes = new HashMap<>();

    public static WirelessNetwork get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WirelessNetwork::new, WirelessNetwork::load, null), NAME);
    }

    public static int clampFreq(int f) {
        return Math.clamp(f, MIN_FREQ, MAX_FREQ);
    }

    public boolean isActive(MinecraftServer server, int freq) {
        Set<GlobalPos> t = transmitters.get(freq);
        if (t != null && !t.isEmpty()) return true;
        Map<UUID, Long> r = remotes.get(freq);
        if (r == null || r.isEmpty()) return false;
        long now = server.overworld().getGameTime();
        r.values().removeIf(exp -> exp < now);
        return !r.isEmpty();
    }

    public void setTransmitter(Level level, BlockPos pos, int freq, boolean active) {
        GlobalPos gp = GlobalPos.of(level.dimension(), pos.immutable());
        boolean changed;
        if (active) {
            changed = transmitters.computeIfAbsent(freq, k -> new HashSet<>()).add(gp);
        } else {
            Set<GlobalPos> s = transmitters.get(freq);
            changed = s != null && s.remove(gp);
            if (s != null && s.isEmpty()) transmitters.remove(freq);
        }
        if (changed) setDirty();
    }

    /** Usuwa nadajnik ze wszystkich częstotliwości (np. po zniszczeniu bloku). */
    public void removeTransmitter(Level level, BlockPos pos) {
        GlobalPos gp = GlobalPos.of(level.dimension(), pos.immutable());
        boolean changed = false;
        for (Set<GlobalPos> s : transmitters.values()) changed |= s.remove(gp);
        transmitters.values().removeIf(Set::isEmpty);
        if (changed) setDirty();
    }

    /** Pilot trzymany (PPM) - częstotliwość aktywna przez kilka ticków. */
    public void pulseRemote(MinecraftServer server, int freq, UUID player) {
        remotes.computeIfAbsent(freq, k -> new HashMap<>()).put(player, server.overworld().getGameTime() + 3);
    }

    // ------------------------------------------------------------------ zapis

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        transmitters.forEach((freq, set) -> {
            for (GlobalPos gp : set) {
                CompoundTag e = new CompoundTag();
                e.putInt("Freq", freq);
                e.putString("Dim", gp.dimension().location().toString());
                e.putLong("Pos", gp.pos().asLong());
                list.add(e);
            }
        });
        tag.put("Transmitters", list);
        return tag;
    }

    private static WirelessNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        WirelessNetwork net = new WirelessNetwork();
        ListTag list = tag.getList("Transmitters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(e.getString("Dim"));
            if (dim == null) continue;
            GlobalPos gp = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(e.getLong("Pos")));
            net.transmitters.computeIfAbsent(e.getInt("Freq"), k -> new HashSet<>()).add(gp);
        }
        return net;
    }
}
