package com.otectus.spells_n_gods.data.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class SpellsNGodsJsonLoader<T> extends SimpleJsonResourceReloadListener {
    private final String typeName;

    protected SpellsNGodsJsonLoader(Gson gson, String folder, String typeName) {
        super(gson, folder);
        this.typeName = typeName;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        SpellsNGodsMod.LOGGER.info("[SpellsNGods] {} reload listener applying — found {} raw JSON entries", typeName, jsonMap.size());

        Map<ResourceLocation, T> parsed = new HashMap<>();
        int errors = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement element = entry.getValue();

            if (!element.isJsonObject()) {
                SpellsNGodsMod.LOGGER.warn("Skipping {} {}: not a JSON object", typeName, location);
                errors++;
                continue;
            }
            JsonObject json = element.getAsJsonObject();
            try {
                Optional<T> value = parse(location, json);
                if (value.isPresent()) {
                    parsed.put(location, value.get());
                    SpellsNGodsMod.LOGGER.info("[SpellsNGods] Parsed {} '{}'", typeName, location);
                } else {
                    SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Parsing {} '{}' returned empty", typeName, location);
                }
            } catch (Exception ex) {
                SpellsNGodsMod.LOGGER.error("[SpellsNGods] Failed to parse {} {}: {}", typeName, location, ex.getMessage(), ex);
                errors++;
            }
        }

        onApply(Map.copyOf(parsed));
        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Loaded {} {} definitions ({} errors)", parsed.size(), typeName, errors);
    }

    protected abstract Optional<T> parse(ResourceLocation location, JsonObject json);

    protected abstract void onApply(Map<ResourceLocation, T> data);
}
