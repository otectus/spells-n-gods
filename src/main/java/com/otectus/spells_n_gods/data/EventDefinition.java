package com.otectus.spells_n_gods.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A data-driven rival divine event. The raw JSON is retained and exposed through typed accessors so the
 * runtime ({@code rivalry.EventScheduler}/{@code rivalry.EventExecutor}) can drive event selection,
 * presentation, and effects from datapack content rather than hardcoded enums.
 */
public record EventDefinition(int schema, ResourceLocation id, JsonObject raw) {
    public static Optional<EventDefinition> fromJson(ResourceLocation location, JsonObject json) {
        int schema = SpellsNGodsJsonUtil.getInt(json, "schema", 1);
        String idRaw = SpellsNGodsJsonUtil.getString(json, "id", location.toString());
        ResourceLocation id = SpellsNGodsJsonUtil.parseId(idRaw, location);
        return Optional.of(new EventDefinition(schema, id, json.deepCopy()));
    }

    public String type() {
        return SpellsNGodsJsonUtil.getString(raw, "type", "instant");
    }

    public int durationSeconds() {
        return SpellsNGodsJsonUtil.getInt(raw, "duration_seconds", 30);
    }

    public int radius() {
        return SpellsNGodsJsonUtil.getInt(raw, "radius", 16);
    }

    /** The rival god this event targets; should equal the event's target god (consistency assertion). */
    public String requiresRival() {
        JsonObject conditions = SpellsNGodsJsonUtil.getObject(raw, "conditions");
        return conditions == null ? "" : SpellsNGodsJsonUtil.getString(conditions, "requires_rival", "");
    }

    /** Minimum follower tier (e.g. {@code "devout"}) that feels the mechanical effects; {@code ""} = any. */
    public String tierAtLeast() {
        JsonObject conditions = SpellsNGodsJsonUtil.getObject(raw, "conditions");
        return conditions == null ? "" : SpellsNGodsJsonUtil.getString(conditions, "tier_at_least", "");
    }

    public String titleKey() {
        JsonObject presentation = SpellsNGodsJsonUtil.getObject(raw, "presentation");
        return presentation == null ? "" : SpellsNGodsJsonUtil.getString(presentation, "title_key", "");
    }

    public String particles() {
        JsonObject presentation = SpellsNGodsJsonUtil.getObject(raw, "presentation");
        return presentation == null ? "" : SpellsNGodsJsonUtil.getString(presentation, "particles", "");
    }

    public String sound() {
        JsonObject presentation = SpellsNGodsJsonUtil.getObject(raw, "presentation");
        return presentation == null ? "" : SpellsNGodsJsonUtil.getString(presentation, "sound", "");
    }

    public List<EffectEntry> effects() {
        List<EffectEntry> out = new ArrayList<>();
        JsonElement element = raw.get("effects");
        if (element != null && element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry.isJsonObject()) {
                    out.add(EffectEntry.fromJson(entry.getAsJsonObject()));
                }
            }
        }
        return out;
    }

    /**
     * A single effect entry. The numeric magnitude lives under a type-specific key
     * ({@code value}/{@code chance}/{@code amplifier}/{@code multiplier}/{@code strength}/{@code range}/
     * {@code amount}/{@code duration_seconds}), so read it via {@link #number(String, double)} rather than
     * assuming a single field.
     */
    public record EffectEntry(String type, JsonObject params) {
        public static EffectEntry fromJson(JsonObject obj) {
            return new EffectEntry(SpellsNGodsJsonUtil.getString(obj, "type", ""), obj.deepCopy());
        }

        public double number(String key, double fallback) {
            return SpellsNGodsJsonUtil.getDouble(params, key, fallback);
        }

        public int amplifier() {
            return SpellsNGodsJsonUtil.getInt(params, "amplifier", 0);
        }

        public int durationSeconds(int fallback) {
            return SpellsNGodsJsonUtil.getInt(params, "duration_seconds", fallback);
        }
    }
}
