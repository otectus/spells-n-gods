package com.otectus.spells_n_gods.data.loader;

import com.google.gson.Gson;
import com.otectus.spells_n_gods.data.OfferingValidatorDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.Optional;

public class OfferingValidatorsReloadListener extends SpellsNGodsJsonLoader<OfferingValidatorDefinition> {
    public OfferingValidatorsReloadListener(Gson gson) {
        super(gson, "spells_n_gods/offering_validators", "offering validator");
    }

    @Override
    protected Optional<OfferingValidatorDefinition> parse(ResourceLocation location, com.google.gson.JsonObject json) {
        return OfferingValidatorDefinition.fromJson(location, json);
    }

    @Override
    protected void onApply(Map<ResourceLocation, OfferingValidatorDefinition> data) {
        SpellsNGodsDataManager.setOfferingValidators(data);
    }
}
