package com.example.waypointtracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class WaypointDatabase {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path jsonFile;
    private final Path logFile;
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public static class Entry {
        public String uuid;
        public String name;
        public double x, z;
        public String timestamp;
        public String method;
        public double confidence;
    }

    public WaypointDatabase() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("waypointtracker");
        try { Files.createDirectories(dir); } catch (IOException e) { e.printStackTrace(); }
        this.jsonFile = dir.resolve("waypoints.json");
        this.logFile = dir.resolve("history.log");
        load();
    }

    public void load() {
        if (!Files.exists(jsonFile)) return;
        try (Reader r = Files.newBufferedReader(jsonFile)) {
            List<Entry> loaded = GSON.fromJson(r, new TypeToken<List<Entry>>(){}.getType());
            if (loaded != null) {
                entries.clear();
                entries.addAll(loaded);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(jsonFile)) {
            GSON.toJson(entries, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void record(UUID uuid, String name, double x, double z, String method, double confidence) {
        entries.removeIf(e -> e.uuid.equals(uuid.toString()));
        Entry e = new Entry();
        e.uuid = uuid.toString();
        e.name = name;
        e.x = x;
        e.z = z;
        e.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        e.method = method;
        e.confidence = confidence;
        entries.add(e);
        save();

        try {
            String line = String.format("[%s] %s | %s | (%.2f, %.2f) | %.0f%% | %s%n",
                e.timestamp, uuid.toString().substring(0, 8), name, x, z, confidence * 100, method);
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    public List<Entry> getAll() { return new ArrayList<>(entries); }
    public Entry get(UUID uuid) {
        for (Entry e : entries) if (e.uuid.equals(uuid.toString())) return e;
        return null;
    }
}
