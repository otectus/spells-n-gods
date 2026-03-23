package com.otectus.spells_n_gods.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

public record EffectDefinition(int schema, ResourceLocation id, JsonObject raw) {
    public static Optional<EffectDefinition> fromJson(ResourceLocation location, JsonObject json) {
        int schema = SpellsNGodsJsonUtil.getInt(json, "schema", 1);
        String idRaw = SpellsNGodsJsonUtil.getString(json, "id", location.toString());
        ResourceLocation id = SpellsNGodsJsonUtil.parseId(idRaw, location);
        return Optional.of(new EffectDefinition(schema, id, json.deepCopy()));
    }
}
