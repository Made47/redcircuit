package pl.geesio.redcircuit.wireless;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

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
public class WirelessNetwork extends WorldSavedData {
    public static final int MIN_FREQ = 1, MAX_FREQ = 5000;
    private static final String NAME = "redcircuit_wireless";

    /** Pozycja w danym wymiarze. */
    private static final class Loc {
        final int dim, x, y, z;

        Loc(int dim, int x, int y, int z) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Loc l && l.dim == dim && l.x == x && l.y == y && l.z == z;
        }

        @Override
        public int hashCode() {
            return ((dim * 31 + x) * 31 + y) * 31 + z;
        }
    }

    private final Map<Integer, Set<Loc>> transmitters = new HashMap<>();
    /** Piloty: częstotliwość -> (gracz -> czas wygaśnięcia). Nie są zapisywane. */
    private final Map<Integer, Map<UUID, Long>> remotes = new HashMap<>();

    public WirelessNetwork(String name) {
        super(name);
    }

    /** Sieć zapisana przy świecie głównym (tylko serwer). */
    public static WirelessNetwork get() {
        World w = MinecraftServer.getServer().worldServerForDimension(0);
        WirelessNetwork net = (WirelessNetwork) w.mapStorage.loadData(WirelessNetwork.class, NAME);
        if (net == null) {
            net = new WirelessNetwork(NAME);
            w.mapStorage.setData(NAME, net);
        }
        return net;
    }

    private static long now() {
        return MinecraftServer.getServer().worldServerForDimension(0).getTotalWorldTime();
    }

    public static int clampFreq(int f) {
        return Math.max(MIN_FREQ, Math.min(MAX_FREQ, f));
    }

    public boolean isActive(int freq) {
        Set<Loc> t = transmitters.get(freq);
        if (t != null && !t.isEmpty()) return true;
        Map<UUID, Long> r = remotes.get(freq);
        if (r == null || r.isEmpty()) return false;
        long now = now();
        r.values().removeIf(exp -> exp < now);
        return !r.isEmpty();
    }

    public void setTransmitter(World world, int x, int y, int z, int freq, boolean active) {
        Loc l = new Loc(world.provider.dimensionId, x, y, z);
        boolean changed;
        if (active) {
            changed = transmitters.computeIfAbsent(freq, k -> new HashSet<>()).add(l);
        } else {
            Set<Loc> s = transmitters.get(freq);
            changed = s != null && s.remove(l);
            if (s != null && s.isEmpty()) transmitters.remove(freq);
        }
        if (changed) markDirty();
    }

    /** Usuwa nadajnik ze wszystkich częstotliwości (np. po zniszczeniu bloku). */
    public void removeTransmitter(World world, int x, int y, int z) {
        Loc l = new Loc(world.provider.dimensionId, x, y, z);
        boolean changed = false;
        for (Set<Loc> s : transmitters.values()) changed |= s.remove(l);
        transmitters.values().removeIf(Set::isEmpty);
        if (changed) markDirty();
    }

    /** Pilot trzymany (PPM) - częstotliwość aktywna przez kilka ticków. */
    public void pulseRemote(int freq, UUID player) {
        remotes.computeIfAbsent(freq, k -> new HashMap<>()).put(player, now() + 3);
    }

    // ------------------------------------------------------------------ zapis

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        transmitters.forEach((freq, set) -> {
            for (Loc l : set) {
                NBTTagCompound e = new NBTTagCompound();
                e.setInteger("Freq", freq);
                e.setInteger("Dim", l.dim);
                e.setInteger("X", l.x);
                e.setInteger("Y", l.y);
                e.setInteger("Z", l.z);
                list.appendTag(e);
            }
        });
        tag.setTag("Transmitters", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        transmitters.clear();
        NBTTagList list = tag.getTagList("Transmitters", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            Loc l = new Loc(e.getInteger("Dim"), e.getInteger("X"), e.getInteger("Y"), e.getInteger("Z"));
            transmitters.computeIfAbsent(e.getInteger("Freq"), k -> new HashSet<>()).add(l);
        }
    }
}
