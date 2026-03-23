package com.otectus.spells_n_gods.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public final class SpellsNGodsJsonUtil {
    private SpellsNGodsJsonUtil() {
    }

    public static String getString(JsonObject obj, String key, String fallback) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    public static int getInt(JsonObject obj, String key, int fallback) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
    }

    public static double getDouble(JsonObject obj, String key, double fallback) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsDouble() : fallback;
    }

    public static JsonObject getObject(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public static List<String> getStringList(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<String> values = new ArrayList<>();
        for (JsonElement entry : array) {
            if (entry.isJsonPrimitive()) {
                values.add(entry.getAsString());
            }
        }
        return List.copyOf(values);
    }

    public static net.minecraft.resources.ResourceLocation parseId(String raw, net.minecraft.resources.ResourceLocation fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        net.minecraft.resources.ResourceLocation parsed = net.minecraft.resources.ResourceLocation.tryParse(raw);
        if (parsed == null) {
            return fallback;
        }
        if (!raw.contains(":") && fallback != null) {
            return new net.minecraft.resources.ResourceLocation(fallback.getNamespace(), raw);
        }
        return parsed;
    }
}
