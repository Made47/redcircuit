package pl.geesio.redcircuit.platform.common;

import pl.geesio.redcircuit.RedCircuit;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Properties;

/**
 * Prosty zamiennik ModConfigSpec z NeoForge: wartości z domyślnymi i zakresami,
 * zapisywane w pliku config/redcircuit-common.properties (klucz = sekcja.nazwa).
 */
public final class ModConfigSpec {
    private final List<ConfigValue<?>> values;

    private ModConfigSpec(List<ConfigValue<?>> values) {
        this.values = values;
    }

    /** Wczytuje plik (brakujące wartości = domyślne) i zapisuje go z komentarzami. */
    public void load(Path file) {
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException e) {
                RedCircuit.LOGGER.warn("RedCircuit: cannot read config {}", file, e);
            }
        }
        for (ConfigValue<?> v : values) {
            String raw = props.getProperty(v.key);
            if (raw != null) v.parse(raw.trim());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("# RedCircuit - konfiguracja / configuration\n");
                String section = null;
                for (ConfigValue<?> v : values) {
                    if (!v.section.equals(section)) {
                        section = v.section;
                        w.write("\n# [" + section + "]" + (v.sectionComment == null ? "" : " " + v.sectionComment) + "\n");
                    }
                    if (v.comment != null) w.write("# " + v.comment + v.rangeInfo() + "\n");
                    w.write(v.key + "=" + v.get() + "\n");
                }
            }
        } catch (IOException e) {
            RedCircuit.LOGGER.warn("RedCircuit: cannot write config {}", file, e);
        }
    }

    public static final class Builder {
        private final Deque<String> path = new ArrayDeque<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final java.util.Map<String, String> sectionComments = new java.util.HashMap<>();
        private String comment;

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder comment(String... lines) {
            return comment(String.join(" ", lines));
        }

        public Builder push(String name) {
            path.addLast(name);
            if (comment != null) sectionComments.put(section(), comment);
            comment = null;
            return this;
        }

        public Builder pop() {
            path.removeLast();
            return this;
        }

        private String section() {
            return String.join(".", path);
        }

        private <V extends ConfigValue<?>> V add(V v) {
            v.section = section();
            v.sectionComment = sectionComments.get(v.section);
            v.comment = comment;
            comment = null;
            values.add(v);
            return v;
        }

        private String key(String name) {
            return path.isEmpty() ? name : section() + "." + name;
        }

        public BooleanValue define(String name, boolean def) {
            return add(new BooleanValue(key(name), def));
        }

        public IntValue defineInRange(String name, int def, int min, int max) {
            return add(new IntValue(key(name), def, min, max));
        }

        public ModConfigSpec build() {
            return new ModConfigSpec(List.copyOf(values));
        }
    }

    public abstract static class ConfigValue<T> {
        final String key;
        String section = "";
        String sectionComment;
        String comment;
        protected T value;

        ConfigValue(String key, T def) {
            this.key = key;
            this.value = def;
        }

        public T get() {
            return value;
        }

        public T getAsObject() {
            return value;
        }

        abstract void parse(String raw);

        String rangeInfo() {
            return "";
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(String key, boolean def) {
            super(key, def);
        }

        public boolean getAsBoolean() {
            return value;
        }

        @Override
        void parse(String raw) {
            if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) value = Boolean.parseBoolean(raw);
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {
        private final int min, max;

        IntValue(String key, int def, int min, int max) {
            super(key, def);
            this.min = min;
            this.max = max;
        }

        public int getAsInt() {
            return value;
        }

        @Override
        void parse(String raw) {
            try {
                value = Math.max(min, Math.min(max, Integer.parseInt(raw)));
            } catch (NumberFormatException ignored) {
            }
        }

        @Override
        String rangeInfo() {
            return " (" + min + " - " + max + ")";
        }
    }
}
