package com.otectus.spells_n_gods.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines apostasy configuration for a god, including curse effects,
 * scar modifiers, and trial configuration.
 */
public record ApostasyDefinition(
        CurseDefinition latentCurse,
        ScarModifiers scarModifiers,
        TrialDefinition trials
) {
    /**
     * Default apostasy definition with moderate penalties.
     */
    public static ApostasyDefinition defaultDefinition() {
        return new ApostasyDefinition(
                CurseDefinition.defaultCurse(),
                ScarModifiers.defaultModifiers(),
                TrialDefinition.defaultTrials()
        );
    }

    public static ApostasyDefinition fromJson(JsonObject json) {
        if (json == null) {
            return defaultDefinition();
        }

        JsonObject curseObj = SpellsNGodsJsonUtil.getObject(json, "latent_curse");
        CurseDefinition curse = CurseDefinition.fromJson(curseObj);

        JsonObject scarObj = SpellsNGodsJsonUtil.getObject(json, "scar_modifiers");
        ScarModifiers scars = ScarModifiers.fromJson(scarObj);

        JsonObject trialsObj = SpellsNGodsJsonUtil.getObject(json, "trials");
        TrialDefinition trials = TrialDefinition.fromJson(trialsObj);

        return new ApostasyDefinition(curse, scars, trials);
    }

    /**
     * Curse effects applied during the latent curse period after apostasy.
     */
    public record CurseDefinition(
            List<CurseEffect> effects,
            float damageMultiplier,
            float hungerDrain,
            int durationHours,
            String curseMessageKey
    ) {
        public static CurseDefinition defaultCurse() {
            return new CurseDefinition(
                    List.of(new CurseEffect("minecraft:weakness", 0)),
                    1.25f,
                    1.5f,
                    72,
                    "spells_n_gods.curse.default.active"
            );
        }

        public static CurseDefinition fromJson(JsonObject json) {
            if (json == null) {
                return defaultCurse();
            }

            List<CurseEffect> effects = new ArrayList<>();
            if (json.has("effects")) {
                JsonArray effectsArray = json.getAsJsonArray("effects");
                for (JsonElement elem : effectsArray) {
                    if (elem.isJsonObject()) {
                        JsonObject effectObj = elem.getAsJsonObject();
                        // Support both "effect_id" and "effect" for backwards compatibility
                        String effect = SpellsNGodsJsonUtil.getString(effectObj, "effect_id", "");
                        if (effect.isEmpty()) {
                            effect = SpellsNGodsJsonUtil.getString(effectObj, "effect", "");
                        }
                        int amplifier = SpellsNGodsJsonUtil.getInt(effectObj, "amplifier", 0);
                        if (!effect.isEmpty()) {
                            effects.add(new CurseEffect(effect, amplifier));
                        }
                    }
                }
            }

            float damageMultiplier = (float) SpellsNGodsJsonUtil.getDouble(json, "damage_multiplier", 1.25);
            float hungerDrain = (float) SpellsNGodsJsonUtil.getDouble(json, "hunger_drain", 1.5);
            int durationHours = SpellsNGodsJsonUtil.getInt(json, "duration_hours", 72);
            // Support both "curse_message_key" and "curse_message" for flexibility
            String messageKey = SpellsNGodsJsonUtil.getString(json, "curse_message_key", "");
            if (messageKey.isEmpty()) {
                messageKey = SpellsNGodsJsonUtil.getString(json, "curse_message", "spells_n_gods.curse.default.active");
            }

            return new CurseDefinition(effects, damageMultiplier, hungerDrain, durationHours, messageKey);
        }

        public long getDurationMs() {
            return durationHours * 3600_000L;
        }
    }

    /**
     * A single curse potion effect.
     */
    public record CurseEffect(String effectId, int amplifier) {}

    /**
     * Modifiers applied to permanent scars from apostasy.
     */
    public record ScarModifiers(
            float healthReduction,
            float xpPenaltyBase,
            float xpPenaltyPerScar,
            float deathPenaltyIncrease,
            float luckReduction,
            float durabilityPenalty
    ) {
        public static ScarModifiers defaultModifiers() {
            return new ScarModifiers(
                    0.05f,   // 5% max health reduction per scar
                    0.1f,    // 10% base XP penalty
                    0.05f,   // 5% additional XP penalty per scar
                    0.15f,   // 15% increased death penalty
                    0.05f,   // 5% luck reduction
                    0.1f     // 10% durability penalty
            );
        }

        public static ScarModifiers fromJson(JsonObject json) {
            if (json == null) {
                return defaultModifiers();
            }

            // Support both "xp_penalty" and "xp_penalty_base" for flexibility
            double xpPenalty = SpellsNGodsJsonUtil.getDouble(json, "xp_penalty", -1);
            if (xpPenalty < 0) {
                xpPenalty = SpellsNGodsJsonUtil.getDouble(json, "xp_penalty_base", 0.1);
            }

            return new ScarModifiers(
                    (float) SpellsNGodsJsonUtil.getDouble(json, "health_reduction", 0.05),
                    (float) xpPenalty,
                    (float) SpellsNGodsJsonUtil.getDouble(json, "xp_penalty_per_scar", 0.05),
                    (float) SpellsNGodsJsonUtil.getDouble(json, "death_penalty_increase", 0.15),
                    (float) SpellsNGodsJsonUtil.getDouble(json, "luck_reduction", 0.05),
                    (float) SpellsNGodsJsonUtil.getDouble(json, "durability_penalty", 0.1)
            );
        }
    }

    /**
     * Configuration for the 3-phase apostasy trial system.
     */
    public record TrialDefinition(
            PhaseConfig phase1,
            PhaseConfig phase2,
            PhaseConfig phase3
    ) {
        public static TrialDefinition defaultTrials() {
            return new TrialDefinition(
                    new PhaseConfig(60, "marked_for_wrath", null, 0, false),
                    new PhaseConfig(120, "divine_pursuit", "minecraft:vindicator", 3, false),
                    new PhaseConfig(180, "final_judgment", "minecraft:iron_golem", 1, true)
            );
        }

        public static TrialDefinition fromJson(JsonObject json) {
            if (json == null) {
                return defaultTrials();
            }

            JsonObject phase1Obj = SpellsNGodsJsonUtil.getObject(json, "phase_1");
            JsonObject phase2Obj = SpellsNGodsJsonUtil.getObject(json, "phase_2");
            JsonObject phase3Obj = SpellsNGodsJsonUtil.getObject(json, "phase_3");

            return new TrialDefinition(
                    PhaseConfig.fromJson(phase1Obj, 1),
                    PhaseConfig.fromJson(phase2Obj, 2),
                    PhaseConfig.fromJson(phase3Obj, 3)
            );
        }

        public PhaseConfig getPhase(int phase) {
            return switch (phase) {
                case 1 -> phase1;
                case 2 -> phase2;
                case 3 -> phase3;
                default -> null;
            };
        }
    }

    /**
     * Configuration for a single trial phase.
     */
    public record PhaseConfig(
            int durationSeconds,
            String effect,
            String pursuerType,
            int pursuerCount,
            boolean isBoss
    ) {
        public static PhaseConfig fromJson(JsonObject json, int defaultPhase) {
            if (json == null) {
                return switch (defaultPhase) {
                    case 1 -> new PhaseConfig(60, "marked_for_wrath", null, 0, false);
                    case 2 -> new PhaseConfig(120, "divine_pursuit", "minecraft:vindicator", 3, false);
                    case 3 -> new PhaseConfig(180, "final_judgment", "minecraft:iron_golem", 1, true);
                    default -> new PhaseConfig(60, "unknown", null, 0, false);
                };
            }

            int duration = SpellsNGodsJsonUtil.getInt(json, "duration_seconds", 60);
            String effect = SpellsNGodsJsonUtil.getString(json, "effect", "");
            String pursuerType = SpellsNGodsJsonUtil.getString(json, "pursuer_type", "");
            int pursuerCount = SpellsNGodsJsonUtil.getInt(json, "pursuer_count", 0);
            boolean isBoss = json.has("boss_spawn") && json.get("boss_spawn").getAsBoolean();

            return new PhaseConfig(
                    duration,
                    effect.isEmpty() ? null : effect,
                    pursuerType.isEmpty() ? null : pursuerType,
                    pursuerCount,
                    isBoss
            );
        }

        public long getDurationMs() {
            return durationSeconds * 1000L;
        }
    }
}
