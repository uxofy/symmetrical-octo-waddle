package com.pvpt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public enum Mode { ALL, CRITS, SWEEP }

    public static boolean enabled = false;
    public static Mode mode = Mode.ALL;
    public static double distance = 3.0; // 1.0 .. 3.0, шаг 0.5

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvpt.json");

    private static class Data {
        boolean enabled;
        Mode mode = Mode.ALL;
        double distance = 3.0;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Data d = GSON.fromJson(Files.readString(FILE), Data.class);
                if (d != null) {
                    enabled = d.enabled;
                    mode = d.mode == null ? Mode.ALL : d.mode;
                    distance = Math.max(1.0, Math.min(3.0, Math.round(d.distance * 2) / 2.0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.enabled = enabled;
            d.mode = mode;
            d.distance = distance;
            Files.writeString(FILE, GSON.toJson(d));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
