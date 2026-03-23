package com.otectus.spells_n_gods.data.loader;

import com.google.gson.Gson;
import com.otectus.spells_n_gods.data.MonumentDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.Optional;

public class MonumentsReloadListener extends SpellsNGodsJsonLoader<MonumentDefinition> {
    public MonumentsReloadListener(Gson gson) {
        super(gson, "spells_n_gods/monuments", "monument");
    }

    @Override
    protected Optional<MonumentDefinition> parse(ResourceLocation location, com.google.gson.JsonObject json) {
        return MonumentDefinition.fromJson(location, json);
    }

    @Override
    protected void onApply(Map<ResourceLocation, MonumentDefinition> data) {
        SpellsNGodsDataManager.setMonuments(data);
    }
}
