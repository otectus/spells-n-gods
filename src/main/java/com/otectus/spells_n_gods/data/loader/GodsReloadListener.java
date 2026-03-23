package com.otectus.spells_n_gods.data.loader;

import com.google.gson.Gson;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.Optional;

public class GodsReloadListener extends SpellsNGodsJsonLoader<GodDefinition> {
    public GodsReloadListener(Gson gson) {
        super(gson, "spells_n_gods/gods", "god");
    }

    @Override
    protected Optional<GodDefinition> parse(ResourceLocation location, com.google.gson.JsonObject json) {
        return GodDefinition.fromJson(location, json);
    }

    @Override
    protected void onApply(Map<ResourceLocation, GodDefinition> data) {
        SpellsNGodsDataManager.setGods(data);
    }
}
