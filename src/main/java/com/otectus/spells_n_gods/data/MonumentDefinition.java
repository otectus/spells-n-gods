package com.otectus.spells_n_gods.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

public record MonumentDefinition(int schema, ResourceLocation id, JsonObject raw) {
    public static Optional<MonumentDefinition> fromJson(ResourceLocation location, JsonObject json) {
        int schema = SpellsNGodsJsonUtil.getInt(json, "schema", 1);
        String idRaw = SpellsNGodsJsonUtil.getString(json, "variant", location.getPath());
        ResourceLocation id = SpellsNGodsJsonUtil.parseId(idRaw, location);
        return Optional.of(new MonumentDefinition(schema, id, json.deepCopy()));
    }

    /** Authored ambient particle id (e.g. {@code spells_n_gods:embers}); {@code ""} if unset. */
    public String particle() {
        return SpellsNGodsJsonUtil.getString(raw, "particle", "");
    }

    /** Authored ambient loop-sound id (e.g. {@code spells_n_gods:war_drum}); {@code ""} if unset. */
    public String ambientSound() {
        return SpellsNGodsJsonUtil.getString(raw, "ambient_sound", "");
    }
}
