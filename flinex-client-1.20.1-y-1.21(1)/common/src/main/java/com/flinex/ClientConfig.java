package com.flinex;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.*;

public class ClientConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("flinex.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int[] PRESETS = {0xFF5CC8FF, 0xFFFF7A59, 0xFF7CFF8A, 0xFFC084FC, 0xFFFF5C8A};
    public static int accent = PRESETS[0];
    public static boolean rainbow = false;

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject o = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (o.has("accent")) accent = o.get("accent").getAsInt();
            if (o.has("rainbow")) rainbow = o.get("rainbow").getAsBoolean();
            JsonObject m = o.has("modules") ? o.getAsJsonObject("modules") : null;
            if (m == null) return;
            for (Module mod : Modules.ALL) {
                if (!m.has(mod.name)) continue;
                JsonObject j = m.getAsJsonObject(mod.name);
                if (j.has("on")) mod.enabled = j.get("on").getAsBoolean();
                if (j.has("x")) mod.x = j.get("x").getAsInt();
                if (j.has("y")) mod.y = j.get("y").getAsInt();
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("accent", accent);
            o.addProperty("rainbow", rainbow);
            JsonObject m = new JsonObject();
            for (Module mod : Modules.ALL) {
                JsonObject j = new JsonObject();
                j.addProperty("on", mod.enabled);
                j.addProperty("x", mod.x);
                j.addProperty("y", mod.y);
                m.add(mod.name, j);
            }
            o.add("modules", m);
            Files.writeString(FILE, GSON.toJson(o));
        } catch (Exception ignored) {}
    }
}
