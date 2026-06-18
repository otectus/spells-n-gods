package com.otectus.spells_n_gods.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellsNGodsConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final Common COMMON;
    public static final Server SERVER;

    static {
        var commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        var serverBuilder = new ForgeConfigSpec.Builder();
        SERVER = new Server(serverBuilder);
        SERVER_SPEC = serverBuilder.build();
    }

    private SpellsNGodsConfig() {
    }

    public static final class Common {
        // --- Divinity ---
        public final ForgeConfigSpec.BooleanValue allowApostasy;
        public final ForgeConfigSpec.IntValue apostasyCooldownDays;
        public final ForgeConfigSpec.IntValue unboundDurationHours;
        public final ForgeConfigSpec.BooleanValue bindingRequiresMonument;
        public final ForgeConfigSpec.BooleanValue oneGodPerPlayer;

        // --- Worship ---
        public final ForgeConfigSpec.IntValue offeringGraceHours;
        public final ForgeConfigSpec.IntValue prayerMinSeconds;
        public final ForgeConfigSpec.IntValue prayerCooldownSeconds;
        public final ForgeConfigSpec.DoubleValue favorDecayPerDay;
        public final ForgeConfigSpec.BooleanValue favorDecayPausedWhilePraying;

        // --- Favor Thresholds ---
        public final ForgeConfigSpec.IntValue thresholdInitiate;
        public final ForgeConfigSpec.IntValue thresholdDevout;
        public final ForgeConfigSpec.IntValue thresholdExalted;
        public final ForgeConfigSpec.IntValue thresholdAscendant;

        // --- Rivalry ---
        public final ForgeConfigSpec.DoubleValue primaryRivalPressureMultiplier;
        public final ForgeConfigSpec.DoubleValue secondaryRivalPressureMultiplier;
        public final ForgeConfigSpec.BooleanValue rivalPressureEnabled;

        // --- Monument ---
        public final ForgeConfigSpec.BooleanValue usePlayerSkin;
        public final ForgeConfigSpec.ConfigValue<String> offlineSkinFallback;
        public final ForgeConfigSpec.IntValue chunkAnchorRadius;
        public final ForgeConfigSpec.BooleanValue allowMonumentBreaking;
        public final ForgeConfigSpec.BooleanValue allowMonumentMoving;

        // --- Difficulty ---
        public final ForgeConfigSpec.DoubleValue bossDifficultyMultiplier;
        public final ForgeConfigSpec.DoubleValue bossSpawnTriggerRange;
        public final ForgeConfigSpec.BooleanValue bossSpawnAnimationEnabled;

        // --- VFX ---
        public final ForgeConfigSpec.BooleanValue bossScreenEffectsEnabled;
        public final ForgeConfigSpec.BooleanValue worshipVfxEnabled;
        public final ForgeConfigSpec.BooleanValue monumentAmbientParticles;
        public final ForgeConfigSpec.BooleanValue playerAnimationsEnabled;

        // --- Boss ---
        // Per-boss stats (health, armor, attack, speed, leash, respawn delay, enrage thresholds,
        // spell cooldown) are authored per-god in the gods/*.json datapack; use bossDifficultyMultiplier
        // for global HP/damage scaling. No redundant global stat overrides are exposed here.
        public final ForgeConfigSpec.BooleanValue bossesEnabled;

        // --- Boss Siege (block-breaking) ---
        public final ForgeConfigSpec.BooleanValue bossSiegeEnabled;
        public final ForgeConfigSpec.DoubleValue bossSiegeAwarenessRange;
        public final ForgeConfigSpec.IntValue bossSiegeBreakIntervalTicks;
        public final ForgeConfigSpec.IntValue bossSiegeEscalationSeconds;
        public final ForgeConfigSpec.IntValue bossSiegeSmashRadius;

        // --- Structure ---
        public final ForgeConfigSpec.BooleanValue structuresEnabled;
        public final ForgeConfigSpec.BooleanValue structureProtection;
        public final ForgeConfigSpec.BooleanValue protectFromExplosions;
        public final ForgeConfigSpec.BooleanValue protectFromPistons;
        public final ForgeConfigSpec.IntValue structureSpacing;
        public final ForgeConfigSpec.IntValue structureSeparation;
        public final ForgeConfigSpec.IntValue structureMinDistFromSpawn;

        // --- Loot ---
        public final ForgeConfigSpec.DoubleValue dropSimplySwordsChance;
        public final ForgeConfigSpec.DoubleValue dropIronsScrollChance;
        public final ForgeConfigSpec.DoubleValue dropFragmentChance;
        public final ForgeConfigSpec.IntValue dropFragmentMin;
        public final ForgeConfigSpec.IntValue dropFragmentMax;

        // --- Locator ---
        public final ForgeConfigSpec.IntValue locatorCooldownTicks;
        public final ForgeConfigSpec.BooleanValue locatorCraftingEnabled;

        // --- Debug ---
        public final ForgeConfigSpec.BooleanValue debugBossSpawning;
        public final ForgeConfigSpec.BooleanValue debugStructurePlacement;
        public final ForgeConfigSpec.BooleanValue debugModCompat;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("divinity");
            allowApostasy = builder.define("allowApostasy", true);
            apostasyCooldownDays = builder.defineInRange("apostasyCooldownDays", 21, 0, 3650);
            unboundDurationHours = builder.defineInRange("unboundDurationHours", 72, 0, 7200);
            bindingRequiresMonument = builder.define("bindingRequiresMonument", true);
            oneGodPerPlayer = builder.define("oneGodPerPlayer", true);
            builder.pop();

            builder.push("worship");
            offeringGraceHours = builder.defineInRange("offeringGraceHours", 72, 1, 7200);
            prayerMinSeconds = builder.defineInRange("prayerMinSeconds", 10, 1, 3600);
            prayerCooldownSeconds = builder.defineInRange("prayerCooldownSeconds", 300, 0, 86400);
            favorDecayPerDay = builder.defineInRange("favorDecayPerDay", 2.0, 0.0, 100.0);
            favorDecayPausedWhilePraying = builder.define("favorDecayPausedWhilePraying", true);
            builder.pop();

            builder.push("favorThresholds");
            thresholdInitiate = builder.defineInRange("initiate", 10, 0, 100000);
            thresholdDevout = builder.defineInRange("devout", 35, 0, 100000);
            thresholdExalted = builder.defineInRange("exalted", 70, 0, 100000);
            thresholdAscendant = builder.defineInRange("ascendant", 95, 0, 100000);
            builder.pop();

            builder.push("rivalry");
            primaryRivalPressureMultiplier = builder.defineInRange("primaryRivalPressureMultiplier", 1.0, 0.0, 100.0);
            secondaryRivalPressureMultiplier = builder.defineInRange("secondaryRivalPressureMultiplier", 0.5, 0.0, 100.0);
            rivalPressureEnabled = builder.define("rivalPressureEnabled", true);
            builder.pop();

            builder.push("monument");
            usePlayerSkin = builder.define("usePlayerSkin", true);
            offlineSkinFallback = builder.define("offlineSkinFallback", "MHF_Steve");
            chunkAnchorRadius = builder.defineInRange("chunkAnchorRadius", 1, 0, 8);
            allowMonumentBreaking = builder.define("allowMonumentBreaking", false);
            allowMonumentMoving = builder.define("allowMonumentMoving", false);
            builder.pop();

            builder.push("difficulty");
            builder.comment("Global multiplier applied to boss HP and damage on top of per-god JSON values.",
                            "0.5 = Easy, 1.0 = Normal, 1.5 = Hard, 2.0 = Nightmare.");
            bossDifficultyMultiplier = builder.defineInRange("bossDifficultyMultiplier", 1.0, 0.1, 5.0);
            builder.comment("Distance in blocks a player must be from the altar center to trigger boss spawn.");
            bossSpawnTriggerRange = builder.defineInRange("bossSpawnTriggerRange", 8.0, 4.0, 32.0);
            builder.comment("Whether to play the dramatic rising-from-ground spawn animation.",
                            "Disable for faster spawns or if experiencing performance issues.");
            bossSpawnAnimationEnabled = builder.define("bossSpawnAnimationEnabled", true);
            builder.pop();

            builder.push("vfx");
            builder.comment("Enable camera shake and screen vignette effects during boss fights.",
                            "Disable if these effects cause discomfort.");
            bossScreenEffectsEnabled = builder.define("bossScreenEffectsEnabled", true);
            builder.comment("Enable particle effects for worship actions (prayer, offerings, binding, tier-ups).",
                            "Disable for reduced visual noise or performance.");
            worshipVfxEnabled = builder.define("worshipVfxEnabled", true);
            builder.comment("Enable ambient particles around monument blocks.",
                            "Monuments emit gentle school-themed particles when placed.");
            monumentAmbientParticles = builder.define("monumentAmbientParticles", true);
            builder.comment("Enable player body animations during rituals (requires playerAnimator mod).",
                            "Disable if animations conflict with other mods or cause issues.");
            playerAnimationsEnabled = builder.define("playerAnimationsEnabled", true);
            builder.pop();

            builder.push("boss");
            builder.comment("Master toggle for the boss encounter system.",
                            "Per-god boss stats (health, armor, attack, speed, leash radius, respawn delay,",
                            "enrage thresholds, spell cooldown) are defined in the gods/*.json datapack.",
                            "Use the [difficulty] bossDifficultyMultiplier for global HP/damage scaling.");
            bossesEnabled = builder.define("enabled", true);
            builder.pop();

            builder.push("boss_siege");
            builder.comment("God bosses break through blocks to reach the player, like the Wither or Ender Dragon.",
                            "Protected temple blocks, bedrock, block entities, and anything in the",
                            "#spells_n_gods:boss_unbreakable block tag are always spared.");
            bossSiegeEnabled = builder.define("enabled", true);
            builder.comment("Range in blocks at which a boss tracks the player (through walls) and pursues.");
            bossSiegeAwarenessRange = builder.defineInRange("awarenessRange", 48.0, 8.0, 128.0);
            builder.comment("How often (in ticks) an active siege breaks blocks. Lower = faster digging.");
            bossSiegeBreakIntervalTicks = builder.defineInRange("breakIntervalTicks", 5, 1, 200);
            builder.comment("Seconds a boss must stay walled-out before escalating to a Wither-style area smash.");
            bossSiegeEscalationSeconds = builder.defineInRange("escalationDelaySeconds", 6, 0, 300);
            builder.comment("Half-width (in blocks) of the area smash once a boss has escalated.");
            bossSiegeSmashRadius = builder.defineInRange("smashRadius", 2, 1, 8);
            builder.pop();

            builder.push("structure");
            builder.comment("Master toggle for structure generation.");
            structuresEnabled = builder.define("enabled", true);
            builder.comment("Protect god structures from player modification.");
            structureProtection = builder.define("protection", true);
            protectFromExplosions = builder.define("protectFromExplosions", true);
            protectFromPistons = builder.define("protectFromPistons", true);
            builder.comment("Structure placement spacing in chunks (higher = more rare).");
            structureSpacing = builder.defineInRange("spacing", 384, 32, 4096);
            structureSeparation = builder.defineInRange("separation", 192, 16, 2048);
            structureMinDistFromSpawn = builder.defineInRange("minDistFromSpawn", 1000, 0, 100000);
            builder.pop();

            builder.push("loot");
            builder.comment("Drop chances for compat mod items. Only used if the respective mod is loaded.");
            dropSimplySwordsChance = builder.defineInRange("simplySwordsWeaponDropChance", 0.10, 0.0, 1.0);
            dropIronsScrollChance = builder.defineInRange("ironsSpellsScrollDropChance", 0.15, 0.0, 1.0);
            builder.comment("Runic Fragment drop settings (fallback loot item).");
            dropFragmentChance = builder.defineInRange("runicFragmentDropChance", 0.50, 0.0, 1.0);
            dropFragmentMin = builder.defineInRange("runicFragmentMinCount", 1, 0, 64);
            dropFragmentMax = builder.defineInRange("runicFragmentMaxCount", 3, 1, 64);
            builder.pop();

            builder.push("locator");
            locatorCooldownTicks = builder.defineInRange("cooldownTicks", 100, 0, 72000);
            locatorCraftingEnabled = builder.define("craftingEnabled", true);
            builder.pop();

            builder.push("debug");
            debugBossSpawning = builder.define("debugBossSpawning", false);
            debugStructurePlacement = builder.define("debugStructurePlacement", false);
            debugModCompat = builder.define("debugModCompat", false);
            builder.pop();
        }
    }

    public static final class Server {
        public final ForgeConfigSpec.BooleanValue enableHud;
        public final ForgeConfigSpec.BooleanValue enableApostasyTrials;

        private Server(ForgeConfigSpec.Builder builder) {
            builder.push("ui");
            enableHud = builder.define("enableHud", true);
            builder.pop();

            builder.push("apostasy");
            enableApostasyTrials = builder.define("enableApostasyTrials", true);
            builder.pop();
        }
    }
}
