package com.otectus.spells_n_gods.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.item.ability.DivineAbilityDefinition;
import com.otectus.spells_n_gods.spawning.SpawnAlignment;
import com.otectus.spells_n_gods.spawning.SpawnPlacement;
import com.otectus.spells_n_gods.spawning.StructureTier;
import com.otectus.spells_n_gods.spawning.logic.DeityStructureProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record GodDefinition(
        int schema,
        ResourceLocation id,
        String displayName,
        String gender,
        List<String> domains,
        String philosophy,
        String magicSchool,
        RivalDefinition rivals,
        BindingDefinition binding,
        FavorDefinition favor,
        WorshipDefinition worship,
        JsonObject blessings,
        JsonObject rivalPressure,
        ApostasyDefinition apostasy,
        BossDefinition boss,
        StructureDefinition structure,
        JsonObject localization,
        DivineAbilityDefinition weaponAbility
) {
    public static Optional<GodDefinition> fromJson(ResourceLocation location, JsonObject json) {
        int schema = SpellsNGodsJsonUtil.getInt(json, "schema", 1);
        String idRaw = SpellsNGodsJsonUtil.getString(json, "id", location.toString());
        ResourceLocation id = SpellsNGodsJsonUtil.parseId(idRaw, location);

        String displayName = SpellsNGodsJsonUtil.getString(json, "display_name", id.getPath());
        String gender = SpellsNGodsJsonUtil.getString(json, "gender", "unknown");
        List<String> domains = SpellsNGodsJsonUtil.getStringList(json, "domains");
        String philosophy = SpellsNGodsJsonUtil.getString(json, "philosophy", "");
        String magicSchool = SpellsNGodsJsonUtil.getString(json, "magic_school", "");

        JsonObject rivalsObj = SpellsNGodsJsonUtil.getObject(json, "rivals");
        RivalDefinition rivals = RivalDefinition.fromJson(rivalsObj);

        JsonObject bindingObj = SpellsNGodsJsonUtil.getObject(json, "binding");
        BindingDefinition binding = BindingDefinition.fromJson(bindingObj);

        JsonObject favorObj = SpellsNGodsJsonUtil.getObject(json, "favor");
        FavorDefinition favor = FavorDefinition.fromJson(favorObj);

        JsonObject worshipObj = SpellsNGodsJsonUtil.getObject(json, "worship");
        WorshipDefinition worship = WorshipDefinition.fromJson(worshipObj);

        JsonObject blessings = SpellsNGodsJsonUtil.getObject(json, "blessings");
        JsonObject rivalPressure = SpellsNGodsJsonUtil.getObject(json, "rival_pressure");

        JsonObject apostasyObj = SpellsNGodsJsonUtil.getObject(json, "apostasy");
        ApostasyDefinition apostasy = ApostasyDefinition.fromJson(apostasyObj);

        JsonObject bossObj = SpellsNGodsJsonUtil.getObject(json, "boss");
        BossDefinition boss = bossObj != null ? BossDefinition.fromJson(bossObj) : BossDefinition.defaultBoss();

        JsonObject structureObj = SpellsNGodsJsonUtil.getObject(json, "structure");
        StructureDefinition structure = structureObj != null
                ? StructureDefinition.fromJson(structureObj) : StructureDefinition.defaultStructure();

        JsonObject localization = SpellsNGodsJsonUtil.getObject(json, "localization");

        JsonObject weaponAbilityObj = SpellsNGodsJsonUtil.getObject(json, "weapon_ability");
        DivineAbilityDefinition weaponAbility = DivineAbilityDefinition.fromJson(weaponAbilityObj);

        return Optional.of(new GodDefinition(
                schema,
                id,
                displayName,
                gender,
                domains,
                philosophy,
                magicSchool,
                rivals,
                binding,
                favor,
                worship,
                blessings != null ? blessings.deepCopy() : new JsonObject(),
                rivalPressure != null ? rivalPressure.deepCopy() : new JsonObject(),
                apostasy,
                boss,
                structure,
                localization != null ? localization.deepCopy() : new JsonObject(),
                weaponAbility
        ));
    }

    // --- Existing sub-records (unchanged) ---

    public record RivalDefinition(String primary, String secondary) {
        public static RivalDefinition fromJson(JsonObject json) {
            if (json == null) {
                return new RivalDefinition("", "");
            }
            return new RivalDefinition(
                    SpellsNGodsJsonUtil.getString(json, "primary", ""),
                    SpellsNGodsJsonUtil.getString(json, "secondary", "")
            );
        }
    }

    public record BindingDefinition(String runeItem, MonumentBinding monument) {
        public static BindingDefinition fromJson(JsonObject json) {
            if (json == null) {
                return new BindingDefinition("", new MonumentBinding("", "", "", ""));
            }
            String runeItem = SpellsNGodsJsonUtil.getString(json, "rune_item", "spells_n_gods:rune");
            JsonObject monumentObj = SpellsNGodsJsonUtil.getObject(json, "monument");
            return new BindingDefinition(runeItem, MonumentBinding.fromJson(monumentObj));
        }
    }

    public record MonumentBinding(String block, String variant, String statueProfile, String fallbackProfile) {
        public static MonumentBinding fromJson(JsonObject json) {
            if (json == null) {
                return new MonumentBinding("spells_n_gods:monument", "default", "skin_owner", "MHF_Steve");
            }
            return new MonumentBinding(
                    SpellsNGodsJsonUtil.getString(json, "block", "spells_n_gods:monument"),
                    SpellsNGodsJsonUtil.getString(json, "variant", "default"),
                    SpellsNGodsJsonUtil.getString(json, "statue_profile", "skin_owner"),
                    SpellsNGodsJsonUtil.getString(json, "fallback_profile", "MHF_Steve")
            );
        }
    }

    public record FavorDefinition(Thresholds thresholds, double decayPerDay) {
        public static FavorDefinition fromJson(JsonObject json) {
            if (json == null) {
                return new FavorDefinition(new Thresholds(10, 35, 70, 95), 2.0);
            }
            JsonObject thresholdsObj = SpellsNGodsJsonUtil.getObject(json, "thresholds");
            double decay = SpellsNGodsJsonUtil.getDouble(json, "decay_per_day", 2.0);
            return new FavorDefinition(Thresholds.fromJson(thresholdsObj), decay);
        }
    }

    public record Thresholds(int initiate, int devout, int exalted, int ascendant) {
        public static Thresholds fromJson(JsonObject json) {
            if (json == null) {
                return new Thresholds(10, 35, 70, 95);
            }
            return new Thresholds(
                    SpellsNGodsJsonUtil.getInt(json, "initiate", 10),
                    SpellsNGodsJsonUtil.getInt(json, "devout", 35),
                    SpellsNGodsJsonUtil.getInt(json, "exalted", 70),
                    SpellsNGodsJsonUtil.getInt(json, "ascendant", 95)
            );
        }
    }

    public record WorshipDefinition(PrayerDefinition prayer, OfferingDefinition offering) {
        public static WorshipDefinition fromJson(JsonObject json) {
            if (json == null) {
                return new WorshipDefinition(new PrayerDefinition(10, 300), new OfferingDefinition(72, true, ""));
            }
            JsonObject prayerObj = SpellsNGodsJsonUtil.getObject(json, "prayer");
            JsonObject offeringObj = SpellsNGodsJsonUtil.getObject(json, "offering");
            return new WorshipDefinition(PrayerDefinition.fromJson(prayerObj), OfferingDefinition.fromJson(offeringObj));
        }
    }

    public record PrayerDefinition(int minSeconds, int cooldownSeconds) {
        public static PrayerDefinition fromJson(JsonObject json) {
            if (json == null) {
                return new PrayerDefinition(10, 300);
            }
            return new PrayerDefinition(
                    SpellsNGodsJsonUtil.getInt(json, "min_seconds", 10),
                    SpellsNGodsJsonUtil.getInt(json, "cooldown_seconds", 300)
            );
        }
    }

    public record OfferingDefinition(int graceHours, boolean diminishingReturns, String validator) {
        public static OfferingDefinition fromJson(JsonObject json) {
            if (json == null) {
                return new OfferingDefinition(72, true, "");
            }
            return new OfferingDefinition(
                    Math.max(0, SpellsNGodsJsonUtil.getInt(json, "grace_hours", 72)),
                    json.has("diminishing_returns") && json.get("diminishing_returns").getAsBoolean(),
                    SpellsNGodsJsonUtil.getString(json, "validator", "")
            );
        }
    }

    // --- New sub-records for boss encounter system ---

    public record BossDefinition(
            double maxHealth,
            double armor,
            double attackDamage,
            double movementSpeed,
            double knockbackResistance,
            String weaponId,
            String fallbackWeaponId,
            String bossBarColor,
            String bossBarOverlay,
            String skinTexture,
            String enragedSkinTexture,
            List<SpellEntry> spellPool,
            int spellCooldownTicks,
            double leashRadius,
            int respawnDelayTicks,
            List<String> accessories,
            JsonObject phaseConfig
    ) {
        public static BossDefinition defaultBoss() {
            return new BossDefinition(
                    300.0, 8.0, 10.0, 0.3, 0.5,
                    "minecraft:iron_sword", "minecraft:iron_sword",
                    "purple", "progress",
                    "spells_n_gods:textures/entity/boss_default.png",
                    "",
                    List.of(), 35, 32.0, 72000,
                    List.of(), new JsonObject()
            );
        }

        public static BossDefinition fromJson(JsonObject json) {
            if (json == null) return defaultBoss();

            double maxHealth = SpellsNGodsJsonUtil.getDouble(json, "max_health", 300.0);
            double armor = SpellsNGodsJsonUtil.getDouble(json, "armor", 8.0);
            double attackDamage = SpellsNGodsJsonUtil.getDouble(json, "attack_damage", 10.0);
            double movementSpeed = SpellsNGodsJsonUtil.getDouble(json, "movement_speed", 0.3);
            double knockbackResistance = SpellsNGodsJsonUtil.getDouble(json, "knockback_resistance", 0.5);
            String weaponId = SpellsNGodsJsonUtil.getString(json, "weapon_id", "minecraft:iron_sword");
            String fallbackWeaponId = SpellsNGodsJsonUtil.getString(json, "fallback_weapon_id", "minecraft:iron_sword");
            String bossBarColor = SpellsNGodsJsonUtil.getString(json, "boss_bar_color", "purple");
            String bossBarOverlay = SpellsNGodsJsonUtil.getString(json, "boss_bar_overlay", "progress");
            String skinTexture = SpellsNGodsJsonUtil.getString(json, "skin_texture",
                    "spells_n_gods:textures/entity/boss_default.png");
            String enragedSkinTexture = SpellsNGodsJsonUtil.getString(json, "enraged_skin_texture", "");

            List<SpellEntry> spells = new ArrayList<>();
            if (json.has("spell_pool") && json.get("spell_pool").isJsonArray()) {
                JsonArray arr = json.getAsJsonArray("spell_pool");
                for (JsonElement elem : arr) {
                    if (elem.isJsonObject()) {
                        spells.add(SpellEntry.fromJson(elem.getAsJsonObject()));
                    }
                }
            }

            int spellCooldownTicks = SpellsNGodsJsonUtil.getInt(json, "spell_cooldown_ticks", 35);
            double leashRadius = SpellsNGodsJsonUtil.getDouble(json, "leash_radius", 32.0);
            int respawnDelayTicks = SpellsNGodsJsonUtil.getInt(json, "respawn_delay_ticks", 72000);
            List<String> accessories = SpellsNGodsJsonUtil.getStringList(json, "accessories");
            JsonObject phaseConfig = SpellsNGodsJsonUtil.getObject(json, "phases");

            return new BossDefinition(
                    maxHealth, armor, attackDamage, movementSpeed, knockbackResistance,
                    weaponId, fallbackWeaponId, bossBarColor, bossBarOverlay, skinTexture,
                    enragedSkinTexture,
                    Collections.unmodifiableList(spells), spellCooldownTicks, leashRadius,
                    respawnDelayTicks,
                    Collections.unmodifiableList(accessories),
                    phaseConfig != null ? phaseConfig.deepCopy() : new JsonObject()
            );
        }

        public double enrageHealthPercent() {
            if (phaseConfig.has("enrage_health_percent")) {
                return phaseConfig.get("enrage_health_percent").getAsDouble();
            }
            return 0.25;
        }

        public double enrageDamageMultiplier() {
            if (phaseConfig.has("enrage_damage_multiplier")) {
                return phaseConfig.get("enrage_damage_multiplier").getAsDouble();
            }
            return 1.5;
        }

        public double enrageSpeedMultiplier() {
            if (phaseConfig.has("enrage_speed_multiplier")) {
                return phaseConfig.get("enrage_speed_multiplier").getAsDouble();
            }
            return 1.3;
        }

        // --- Phase config helpers for per-god combat tuning ---

        private int getPhaseInt(String key, int defaultValue) {
            if (phaseConfig != null && phaseConfig.has(key)) {
                try { return phaseConfig.get(key).getAsInt(); }
                catch (Exception e) { return defaultValue; }
            }
            return defaultValue;
        }

        private double getPhaseDouble(String key, double defaultValue) {
            if (phaseConfig != null && phaseConfig.has(key)) {
                try { return phaseConfig.get(key).getAsDouble(); }
                catch (Exception e) { return defaultValue; }
            }
            return defaultValue;
        }

        // Leap goal tuning
        public int leapBaseCooldown() { return getPhaseInt("leap_base_cooldown", 80); }
        public int leapEnragedCooldown() { return getPhaseInt("leap_enraged_cooldown", 40); }
        public double leapLandingDamage() { return getPhaseDouble("leap_landing_damage", 8.0); }
        public double leapLandingRadius() { return getPhaseDouble("leap_landing_radius", 4.0); }

        // Shield goal tuning
        public int shieldBaseCooldown() { return getPhaseInt("shield_base_cooldown", 160); }
        public int shieldEnragedCooldown() { return getPhaseInt("shield_enraged_cooldown", 80); }
        public int shieldDurationTicks() { return getPhaseInt("shield_duration", 30); }
        public double shieldPushRadius() { return getPhaseDouble("shield_push_radius", 5.0); }
        public double shieldActivationHealthPercent() { return getPhaseDouble("shield_activation_health_percent", 0.85); }

        // Melee goal tuning
        public int meleeAttackIntervalMin() { return getPhaseInt("melee_attack_interval_min", 8); }
        public int meleeAttackIntervalMax() { return getPhaseInt("melee_attack_interval_max", 16); }

        // Spellcasting goal tuning
        public double spellMaxRange() { return getPhaseDouble("spell_max_range", 16.0); }

        // Ranged attack goal tuning (bow/crossbow bosses)
        public int rangedAttackCooldown() { return getPhaseInt("ranged_attack_cooldown", 40); }
        public int rangedEnragedCooldown() { return getPhaseInt("ranged_enraged_cooldown", 25); }
        public double rangedBonusDamage() { return getPhaseDouble("ranged_bonus_damage", 4.0); }
    }

    public record SpellEntry(String spellId, int minLevel, int maxLevel, int weight, double range) {
        public static SpellEntry fromJson(JsonObject json) {
            if (json == null) return new SpellEntry("", 1, 1, 1, 16.0);
            return new SpellEntry(
                    SpellsNGodsJsonUtil.getString(json, "spell_id", ""),
                    SpellsNGodsJsonUtil.getInt(json, "min_level", 1),
                    SpellsNGodsJsonUtil.getInt(json, "max_level", 1),
                    SpellsNGodsJsonUtil.getInt(json, "weight", 1),
                    SpellsNGodsJsonUtil.getDouble(json, "range", 16.0)
            );
        }
    }

    /**
     * Structure-spawning configuration for a deity.
     *
     * <p>The first block of fields ({@code templateId}..{@code size}) drives the legacy dedicated
     * god-temple worldgen and is kept for backward compatibility. The second block powers the
     * data-driven tag/tier spawning system: deities opt into existing (vanilla or modded)
     * structures via domain tags and/or an explicit weighted whitelist, with a default tier,
     * placement strategy, and per-structure/tag alignment overrides.
     *
     * @param defaultTier        tier applied to tag-matched structures (string parsed leniently)
     * @param disableDefault     if true, only the explicit whitelist counts (no tags / any-structure)
     * @param domainTags         structure tag references, e.g. {@code "#spells_n_gods:wisdom_structures"}
     * @param whitelistWeights   explicit structure id -> selection weight
     * @param placement          spawn placement strategy ("" = use config default)
     * @param defaultAlignment   alignment when no override matches
     * @param alignmentOverrides per-tag-or-structure-id alignment overrides
     */
    public record StructureDefinition(
            String templateId,
            String biomeTag,
            int searchRadius,
            int minDistFromSpawn,
            int size,
            String defaultTier,
            boolean disableDefault,
            List<String> domainTags,
            Map<String, Integer> whitelistWeights,
            String placement,
            String defaultAlignment,
            Map<String, String> alignmentOverrides
    ) {
        public StructureDefinition {
            domainTags = domainTags == null ? List.of() : List.copyOf(domainTags);
            whitelistWeights = whitelistWeights == null ? Map.of() : Map.copyOf(whitelistWeights);
            alignmentOverrides = alignmentOverrides == null ? Map.of() : Map.copyOf(alignmentOverrides);
            if (templateId == null) templateId = "";
            if (biomeTag == null) biomeTag = "";
            if (defaultTier == null) defaultTier = "uncommon";
            if (placement == null) placement = "";
            if (defaultAlignment == null) defaultAlignment = "hostile";
        }

        public static StructureDefinition defaultStructure() {
            return new StructureDefinition("", "", 10000, 1000, 16,
                    "uncommon", false, List.of(), Map.of(), "", "hostile", Map.of());
        }

        public static StructureDefinition fromJson(JsonObject json) {
            if (json == null) return defaultStructure();

            // --- Weighted whitelist: supports both an array of {id, weight} objects and a
            //     simple {id: weight} object form. Backward compatible with absent field. ---
            Map<String, Integer> whitelist = new LinkedHashMap<>();
            JsonElement wlElem = json.get("whitelist");
            if (wlElem != null && wlElem.isJsonArray()) {
                for (JsonElement e : wlElem.getAsJsonArray()) {
                    if (e.isJsonObject()) {
                        JsonObject o = e.getAsJsonObject();
                        String id = SpellsNGodsJsonUtil.getString(o, "id", "");
                        int weight = SpellsNGodsJsonUtil.getInt(o, "weight", DeityStructureProfile.DEFAULT_TAG_WEIGHT);
                        if (!id.isBlank()) {
                            whitelist.put(id, weight);
                        }
                    }
                }
            } else if (wlElem != null && wlElem.isJsonObject()) {
                JsonObject o = wlElem.getAsJsonObject();
                for (String key : o.keySet()) {
                    if (o.get(key).isJsonPrimitive()) {
                        whitelist.put(key, o.get(key).getAsInt());
                    }
                }
            }

            // --- Per-tag/structure alignment overrides: { "#tag": "neutral", "ns:struct": "hostile" } ---
            Map<String, String> alignments = new LinkedHashMap<>();
            JsonObject aoObj = SpellsNGodsJsonUtil.getObject(json, "alignment_overrides");
            if (aoObj != null) {
                for (String key : aoObj.keySet()) {
                    if (aoObj.get(key).isJsonPrimitive()) {
                        alignments.put(key, aoObj.get(key).getAsString());
                    }
                }
            }

            return new StructureDefinition(
                    SpellsNGodsJsonUtil.getString(json, "template_id", ""),
                    SpellsNGodsJsonUtil.getString(json, "biome_tag", ""),
                    SpellsNGodsJsonUtil.getInt(json, "search_radius", 10000),
                    SpellsNGodsJsonUtil.getInt(json, "min_dist_from_spawn", 1000),
                    SpellsNGodsJsonUtil.getInt(json, "size", 16),
                    SpellsNGodsJsonUtil.getString(json, "default_tier", "uncommon"),
                    json.has("disable_default") && json.get("disable_default").getAsBoolean(),
                    SpellsNGodsJsonUtil.getStringList(json, "domain_tags"),
                    whitelist,
                    SpellsNGodsJsonUtil.getString(json, "placement", ""),
                    SpellsNGodsJsonUtil.getString(json, "default_alignment", "hostile"),
                    alignments
            );
        }

        /** Distil this definition into the pure-logic profile consumed by the spawn engine. */
        public DeityStructureProfile toProfile(String deityId) {
            return new DeityStructureProfile(
                    deityId,
                    StructureTier.fromString(defaultTier, StructureTier.RARE),
                    disableDefault,
                    domainTags,
                    whitelistWeights);
        }

        public SpawnPlacement resolvePlacement(SpawnPlacement fallback) {
            return SpawnPlacement.fromString(placement, fallback);
        }

        /**
         * Resolve the alignment for a spawn, preferring an exact structure-id override, then any
         * matched-tag override, else the deity default.
         *
         * @param structureId  the structure being spawned at
         * @param matchedTags  domain tag references this structure matched (e.g. "#ns:war_structures")
         */
        public SpawnAlignment resolveAlignment(String structureId, List<String> matchedTags) {
            String byId = alignmentOverrides.get(structureId);
            if (byId != null) {
                return SpawnAlignment.fromString(byId, SpawnAlignment.HOSTILE);
            }
            if (matchedTags != null) {
                for (String tag : matchedTags) {
                    String byTag = alignmentOverrides.get(tag);
                    if (byTag != null) {
                        return SpawnAlignment.fromString(byTag, SpawnAlignment.HOSTILE);
                    }
                }
            }
            return SpawnAlignment.fromString(defaultAlignment, SpawnAlignment.HOSTILE);
        }
    }
}
