package pl.geesio.redcircuit.wireless;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
    private record Entry(int freq, GlobalPos pos) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("Freq").forGetter(Entry::freq),
                GlobalPos.CODEC.fieldOf("Pos").forGetter(Entry::pos)
        ).apply(i, Entry::new));
    }

    private static final Codec<WirelessNetwork> CODEC = Entry.CODEC.listOf().fieldOf("Transmitters").codec().xmap(
            WirelessNetwork::fromEntries, WirelessNetwork::entries);

    public static final SavedDataType<WirelessNetwork> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(pl.geesio.redcircuit.RedCircuit.MODID, "wireless"), WirelessNetwork::new, CODEC);

    private final Map<Integer, Set<GlobalPos>> transmitters = new HashMap<>();
    /** Piloty: częstotliwość -> (gracz -> czas wygaśnięcia). Nie są zapisywane. */
    private final Map<Integer, Map<UUID, Long>> remotes = new HashMap<>();

    public static WirelessNetwork get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        transmitters.forEach((freq, set) -> set.forEach(gp -> list.add(new Entry(freq, gp))));
        return list;
    }

    private static WirelessNetwork fromEntries(List<Entry> list) {
        WirelessNetwork net = new WirelessNetwork();
        for (Entry e : list) net.transmitters.computeIfAbsent(e.freq(), k -> new HashSet<>()).add(e.pos());
        return net;
    }
}
