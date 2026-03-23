package com.otectus.spells_n_gods.data.loader;

import com.google.gson.Gson;
import com.otectus.spells_n_gods.data.EffectDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.Optional;

public class EffectsReloadListener extends SpellsNGodsJsonLoader<EffectDefinition> {
    public EffectsReloadListener(Gson gson) {
        super(gson, "spells_n_gods/effects", "effect");
    }

    @Override
    protected Optional<EffectDefinition> parse(ResourceLocation location, com.google.gson.JsonObject json) {
        return EffectDefinition.fromJson(location, json);
    }

    @Override
    protected void onApply(Map<ResourceLocation, EffectDefinition> data) {
        SpellsNGodsDataManager.setEffects(data);
    }
}
