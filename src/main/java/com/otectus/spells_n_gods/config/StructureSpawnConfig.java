package com.otectus.spells_n_gods.config;

import com.otectus.spells_n_gods.spawning.StructureTier;
import com.otectus.spells_n_gods.spawning.logic.TierSettings;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the data-driven deity structure-spawning system: tiered structure
 * categories, global toggles, world-difficulty scalars, per-structure tier overrides, and
 * per-dimension modifiers.
 *
 * <p>Per-<em>deity</em> structure config (default tier, domain tags, weighted whitelist) lives
 * in the deity JSON ({@code GodDefinition.structure}) rather than here, because ForgeConfigSpec
 * cannot express arbitrary per-deity arrays of arbitrary structure ids. This split keeps deity
 * data datapack-driven (and modpack/datapack overridable) while global tuning stays in TOML.
 */
public final class StructureSpawnConfig {
    public static final ForgeConfigSpec SPEC;
    public static final General GENERAL;
    private static final Map<StructureTier, Tier> TIERS = new EnumMap<>(StructureTier.class);

    static {
        var builder = new ForgeConfigSpec.Builder();
        GENERAL = new General(builder);
        for (StructureTier tier : StructureTier.values()) {
            TIERS.put(tier, new Tier(builder, tier));
        }
        SPEC = builder.build();
    }

    private StructureSpawnConfig() {
    }

    public static Tier tier(StructureTier tier) {
        return TIERS.get(tier);
    }

    /** Resolve a tier's live config into the pure-logic {@link TierSettings} used by the engine. */
    public static TierSettings settings(StructureTier tier) {
        Tier t = TIERS.get(tier);
        return new TierSettings(
                tier,
                t.baseChance.get(),
                t.cooldownDays.get(),
                t.maxActiveDeities.get(),
                t.requiresDragonKilled.get(),
                t.requiredAdvancement.get().trim(),
                t.requiredDimension.get().trim(),
                t.minFavor.get());
    }

    public static final class General {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.BooleanValue allowAnyStructure;
        public final ForgeConfigSpec.BooleanValue debugLogging;
        public final ForgeConfigSpec.IntValue detectionIntervalTicks;
        public final ForgeConfigSpec.ConfigValue<String> defaultPlacement;
        public final ForgeConfigSpec.BooleanValue firstDiscoveryAnnouncements;

        public final ForgeConfigSpec.DoubleValue difficultyScalarPeaceful;
        public final ForgeConfigSpec.DoubleValue difficultyScalarEasy;
        public final ForgeConfigSpec.DoubleValue difficultyScalarNormal;
        public final ForgeConfigSpec.DoubleValue difficultyScalarHard;

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> structureTierOverrides;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionModifiers;

        private General(ForgeConfigSpec.Builder builder) {
            builder.comment(
                    "Data-driven deity structure spawning.",
                    "Deities can appear at vanilla or modded structures based on domain tags,",
                    "tiered categories, deity weights, cooldowns and per-structure spawn budgets.").push("general");

            builder.comment("Master toggle for tag/tier-based deity spawning at detected structures.",
                    "Does NOT affect the legacy dedicated god-temple system.");
            enabled = builder.define("enabled", true);

            builder.comment("If true, deities may spawn at ANY detected structure (subject to weights/gates),",
                    "not only those matching a domain tag or whitelist. Default false.");
            allowAnyStructure = builder.define("allow_any_structure", false);

            builder.comment("Log a full decision breakdown explaining why a deity did or did not spawn.");
            debugLogging = builder.define("debug_logging", false);

            builder.comment("How often (in ticks) each online player is checked against nearby structures.",
                    "Higher = cheaper but slower to react. 100 = every 5 seconds.");
            detectionIntervalTicks = builder.defineInRange("detection_interval_ticks", 100, 20, 12000);

            builder.comment("Default spawn placement when a deity/structure does not specify one.",
                    "One of: center_of_structure, nearest_safe_floor, nearest_air_above_floor,",
                    "random_valid_position, shrine_anchor.");
            defaultPlacement = builder.define("default_placement", "nearest_safe_floor");

            builder.comment("Announce to a player the first time they discover a deity at a structure type.");
            firstDiscoveryAnnouncements = builder.define("first_discovery_announcements", true);

            builder.comment("World difficulty scalars applied to the final spawn chance.");
            difficultyScalarPeaceful = builder.defineInRange("difficulty_scalar_peaceful", 0.0, 0.0, 5.0);
            difficultyScalarEasy = builder.defineInRange("difficulty_scalar_easy", 0.75, 0.0, 5.0);
            difficultyScalarNormal = builder.defineInRange("difficulty_scalar_normal", 1.0, 0.0, 5.0);
            difficultyScalarHard = builder.defineInRange("difficulty_scalar_hard", 1.25, 0.0, 5.0);

            builder.comment("Per-structure tier overrides that beat a deity's default tier.",
                    "Format: 'namespace:structure_id=tier' or '#namespace:tag=tier'.",
                    "Example: [\"minecraft:stronghold=rare\", \"minecraft:ancient_city=legendary\"]");
            structureTierOverrides = builder.defineList("structure_tier_overrides",
                    new ArrayList<>(List.of(
                            "minecraft:ancient_city=legendary",
                            "minecraft:end_city=legendary",
                            "minecraft:stronghold=rare",
                            "minecraft:monument=rare",
                            "minecraft:fortress=uncommon",
                            "minecraft:bastion_remnant=uncommon",
                            "minecraft:pillager_outpost=common",
                            "minecraft:village_plains=common")),
                    o -> o instanceof String);

            builder.comment("Per-dimension multiplicative chance modifiers.",
                    "Format: 'namespace:dimension=multiplier'. Example: [\"minecraft:the_nether=1.2\"]");
            dimensionModifiers = builder.defineList("dimension_modifiers",
                    new ArrayList<String>(), o -> o instanceof String);

            builder.pop();
        }
    }

    public static final class Tier {
        public final ForgeConfigSpec.DoubleValue baseChance;
        public final ForgeConfigSpec.IntValue cooldownDays;
        public final ForgeConfigSpec.IntValue maxActiveDeities;
        public final ForgeConfigSpec.BooleanValue requiresDragonKilled;
        public final ForgeConfigSpec.ConfigValue<String> requiredAdvancement;
        public final ForgeConfigSpec.ConfigValue<String> requiredDimension;
        public final ForgeConfigSpec.IntValue minFavor;

        private Tier(ForgeConfigSpec.Builder builder, StructureTier tier) {
            TierSettings def = TierSettings.defaultFor(tier);
            builder.comment("Tier '" + tier.configKey() + "' defaults.").push("tier_" + tier.configKey());

            builder.comment("Base spawn chance [0..1] for structures of this tier.");
            baseChance = builder.defineInRange("base_chance", def.baseChance(), 0.0, 1.0);

            builder.comment("In-game days before a structure of this tier may roll again.",
                    "Use -1 for permanent exhaustion (one deity ever).");
            cooldownDays = builder.defineInRange("cooldown_days", def.cooldownDays(), -1, 100000);

            builder.comment("Maximum simultaneously-active deity encounters at one structure instance.");
            maxActiveDeities = builder.defineInRange("max_active_deities", def.maxActiveDeities(), 1, 16);

            builder.comment("Progression gate: require the player to have killed the Ender Dragon.");
            requiresDragonKilled = builder.define("requires_dragon_killed", def.requiresDragonKilled());

            builder.comment("Progression gate: require a completed advancement id (blank = none).",
                    "Unknown/invalid ids fail safe (block the spawn) with a warning.");
            requiredAdvancement = builder.define("required_advancement", def.requiredAdvancement());

            builder.comment("Progression gate: require the player to currently be in this dimension (blank = none).");
            requiredDimension = builder.define("required_dimension", def.requiredDimension());

            builder.comment("Progression gate: minimum favor the player must hold with the candidate deity.");
            minFavor = builder.defineInRange("min_favor", def.minFavor(), 0, 100000);

            builder.pop();
        }
    }
}
