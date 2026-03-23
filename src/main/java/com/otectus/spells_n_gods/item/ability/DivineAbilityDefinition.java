package com.otectus.spells_n_gods.item.ability;

import com.google.gson.JsonObject;

/**
 * Data-driven definition of a divine weapon ability, loaded from god JSON.
 * Contains the ability type, display info, cooldown, and type-specific parameters.
 */
public record DivineAbilityDefinition(
        String type,
        String name,
        String description,
        int cooldownTicks,
        JsonObject parameters
) {
    public static DivineAbilityDefinition fromJson(JsonObject json) {
        if (json == null) return null;

        String type = json.has("type") ? json.get("type").getAsString() : "";
        String name = json.has("name") ? json.get("name").getAsString() : "Unknown Ability";
        String description = json.has("description") ? json.get("description").getAsString() : "";
        int cooldownTicks = json.has("cooldown_ticks") ? json.get("cooldown_ticks").getAsInt() : 200;
        JsonObject parameters = json.has("parameters") ? json.getAsJsonObject("parameters") : new JsonObject();

        return new DivineAbilityDefinition(type, name, description, cooldownTicks, parameters);
    }
}
