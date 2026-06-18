package com.otectus.spells_n_gods.spawning;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.boss.BossSpawnAnimationHandler;
import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.config.StructureSpawnConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.spawning.logic.DeityStructureProfile;
import com.otectus.spells_n_gods.spawning.logic.SpawnChanceCalculator;
import com.otectus.spells_n_gods.spawning.logic.TierSettings;
import com.otectus.spells_n_gods.spawning.logic.WeightedSelection;
import com.otectus.spells_n_gods.worldstate.EncounterRecord;
import com.otectus.spells_n_gods.worldstate.StructureEncounterState;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Server-side orchestrator for data-driven deity spawning at detected structures.
 *
 * <p>Forge 1.20.1 exposes no structure spawn-list hook (the 1.16-era
 * {@code StructureSpawnListGatherEvent} no longer exists; structure mob spawns now live in
 * datapack {@code spawn_overrides}). Rather than spamming {@code ChunkEvent} listeners, this
 * manager polls online players on a throttled server tick and queries
 * {@link net.minecraft.world.level.StructureManager#getAllStructuresAt(BlockPos)} /
 * {@code getStructureAt} to learn which structure (and bounding box) the player currently
 * stands in. All cooldown/budget/exhaustion state is persisted in {@link StructureEncounterState}.
 *
 * <p>The decision follows the weighted condition stack: base chance → tier modifier →
 * biome/dimension modifier → deity weight → difficulty scalar → progression gates, with a
 * deterministic per-structure-per-day roll for debuggability.
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public final class StructureSpawnManager {
    private static final int BOSS_ENTITY_HEIGHT = 3;
    private static final long TICKS_PER_DAY = 24000L;
    private static int tickCounter = 0;
    private static int cyclesSincePrune = 0;

    private StructureSpawnManager() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!StructureSpawnConfig.GENERAL.enabled.get()) {
            return;
        }
        int interval = StructureSpawnConfig.GENERAL.detectionIntervalTicks.get();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        StructureEncounterState state = StructureEncounterState.get(server);
        long now = server.overworld().getGameTime();

        // Build the deity profiles once per cycle (small set, ~9 deities).
        Map<String, GodDefinition> gods = new HashMap<>();
        List<DeityStructureProfile> profiles = new ArrayList<>();
        for (Map.Entry<ResourceLocation, GodDefinition> e : SpellsNGodsDataManager.getGods().entrySet()) {
            String id = e.getKey().toString();
            gods.put(id, e.getValue());
            profiles.add(e.getValue().structure().toProfile(id));
        }
        if (profiles.isEmpty()) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                tryDetectAndSpawn(level, player, state, gods, profiles, now);
            }
        }

        if (++cyclesSincePrune >= 20) {
            cyclesSincePrune = 0;
            state.prune(now);
        }
    }

    private static void tryDetectAndSpawn(ServerLevel level, ServerPlayer player,
                                          StructureEncounterState state,
                                          Map<String, GodDefinition> gods,
                                          List<DeityStructureProfile> profiles, long now) {
        double difficultyScalar = difficultyScalar(level.getDifficulty());
        if (difficultyScalar <= 0.0) {
            return; // e.g. peaceful disables spawning by default
        }

        BlockPos pos = player.blockPosition();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Map<Structure, LongSet> here = level.structureManager().getAllStructuresAt(pos);
        if (here.isEmpty()) {
            return;
        }

        String dimension = level.dimension().location().toString();
        boolean allowAny = StructureSpawnConfig.GENERAL.allowAnyStructure.get();
        boolean debug = StructureSpawnConfig.GENERAL.debugLogging.get();

        for (Structure structure : here.keySet()) {
            StructureStart start = level.structureManager().getStructureAt(pos, structure);
            if (start == null || !start.isValid()) {
                continue; // player is referenced by but not actually inside this structure
            }
            ResourceLocation structureKey = registry.getKey(structure);
            if (structureKey == null) {
                continue;
            }
            String structureId = structureKey.toString();
            BoundingBox bounds = start.getBoundingBox();
            Holder<Structure> holder = registry.wrapAsHolder(structure);

            // --- Build the weighted candidate set for this structure ---
            List<WeightedSelection.Weighted<DeityStructureProfile>> candidates = new ArrayList<>();
            Map<String, List<String>> matchedTagsByDeity = new HashMap<>();
            for (DeityStructureProfile profile : profiles) {
                List<String> matchedTags = matchedTags(holder, profile);
                boolean matchesTag = !matchedTags.isEmpty();
                if (!profile.appliesTo(structureId, matchesTag, allowAny)) {
                    continue;
                }
                int weight = profile.weightFor(structureId, matchesTag);
                if (weight <= 0) {
                    continue;
                }
                candidates.add(new WeightedSelection.Weighted<>(profile, weight));
                matchedTagsByDeity.put(profile.deityId(), matchedTags);
            }
            if (candidates.isEmpty()) {
                continue;
            }

            // --- Stable identity + budget/cooldown gate ---
            // Peek without creating: only structures that actually host a deity get persisted,
            // which keeps the saved data bounded (no record per village/mineshaft walked through).
            String key = StructureEncounterState.makeKey(dimension, structureId, bounds);
            EncounterRecord record = state.get(key);
            long dayIndex = now / TICKS_PER_DAY;
            Random rng = deterministicRng(key, dayIndex);

            // Deity selection is part of the weighted stack; pick before the chance roll so the
            // chosen deity's weight/tier feed the chance computation.
            DeityStructureProfile chosen = WeightedSelection.pick(candidates, rng);
            if (chosen == null) {
                continue;
            }

            StructureTier tier = resolveTier(holder, structureId, matchedTagsByDeity.get(chosen.deityId()),
                    chosen.defaultTier());
            TierSettings settings = StructureSpawnConfig.settings(tier);

            // A missing record means this structure has never hosted a deity -> free to spawn.
            if (record != null && !record.canSpawn(now, settings.maxActiveDeities())) {
                if (debug) {
                    SpellsNGodsMod.LOGGER.info("[Sng/Spawn] {} at {} blocked: budget/cooldown "
                                    + "(active={}, permExhausted={})",
                            structureId, record.key(), record.activeDeityCount(),
                            record.isPermanentlyExhausted());
                }
                continue;
            }

            SpawnGateEvaluator.Result gate = SpawnGateEvaluator.evaluate(player, chosen.deityId(), settings);
            int weight = chosen.weightFor(structureId,
                    !matchedTagsByDeity.get(chosen.deityId()).isEmpty());
            SpawnChanceCalculator.Result chance = SpawnChanceCalculator.compute(
                    settings.baseChance(),
                    1.0,
                    dimensionModifier(dimension),
                    weight,
                    difficultyScalar,
                    gate.passed());

            double roll = rng.nextDouble();
            boolean spawn = roll < chance.finalChance();

            if (debug) {
                SpellsNGodsMod.LOGGER.info("[Sng/Spawn] deity={} structure={} tier={} | {} | roll={} (gate: {}) => {}",
                        chosen.deityId(), structureId, tier.configKey(), chance.breakdown(),
                        String.format(java.util.Locale.ROOT, "%.4f", roll), gate.reason(),
                        spawn ? "SPAWN" : "no-spawn");
            }

            if (!spawn) {
                continue;
            }

            doSpawn(level, player, state, dimension, chosen, gods.get(chosen.deityId()),
                    structureId, bounds, matchedTagsByDeity.get(chosen.deityId()), settings, now);
            // One spawn attempt per player per cycle is plenty.
            return;
        }
    }

    private static void doSpawn(ServerLevel level, ServerPlayer player, StructureEncounterState state,
                                String dimension, DeityStructureProfile chosen, GodDefinition god,
                                String structureId, BoundingBox bounds, List<String> matchedTags,
                                TierSettings settings, long now) {
        SpawnPlacement placement = god.structure().resolvePlacement(defaultPlacement());
        var placed = PlacementResolver.resolve(level, bounds, placement, BOSS_ENTITY_HEIGHT, null);
        if (placed.isEmpty()) {
            // No record is committed here (keeps saved data bounded), so a player lingering in a
            // structure with no safe placement re-evaluates it each cycle; keep this at debug to
            // avoid log spam (many structures legitimately offer no safe boss placement).
            SpellsNGodsMod.LOGGER.debug("[Sng/Spawn] No safe placement for {} in {} ({}); skipping",
                    chosen.deityId(), structureId, placement);
            return;
        }
        BlockPos spawnPos = placed.get();
        SpawnAlignment alignment = god.structure().resolveAlignment(structureId, matchedTags);

        // Now that we are committing to a spawn, materialise (and persist) the encounter record.
        EncounterRecord record = state.getOrCreate(dimension, structureId, bounds);
        GodBossEntity boss = BossSpawnAnimationHandler.spawnGodAt(level, chosen.deityId(), spawnPos,
                alignment, spawned -> state.recordSpawn(record, spawned.getUUID(), now, settings.cooldownDays()));
        if (boss == null) {
            return;
        }

        SpellsNGodsMod.LOGGER.info("[Sng/Spawn] Spawned {} ({}) at {} in {} [tier={}, cooldownDays={}]",
                chosen.deityId(), alignment, spawnPos, structureId, settings.tier().configKey(),
                settings.cooldownDays());

        // --- First-discovery polish ---
        if (StructureSpawnConfig.GENERAL.firstDiscoveryAnnouncements.get()
                && state.markDiscovered(player.getUUID(), chosen.deityId(), structureId)) {
            String name = god.displayName();
            player.sendSystemMessage(Component.literal("You sense " + name + " stirring within this place...")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.6F, 1.4F);
        }
    }

    // --- Budget release on death ---

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof GodBossEntity boss)) {
            return;
        }
        if (boss.level().isClientSide()) {
            return;
        }
        MinecraftServer server = boss.getServer();
        if (server == null) {
            return;
        }
        // Releases the budget slot if this boss was spawned by the tag/tier system; harmless
        // (returns false) for legacy dedicated-temple bosses.
        StructureEncounterState.get(server).releaseDeity(boss.getUUID());
    }

    // --- Helpers ---

    /** Which of a deity's domain tags this structure belongs to (returns the matched tag references). */
    private static List<String> matchedTags(Holder<Structure> holder, DeityStructureProfile profile) {
        List<String> matched = new ArrayList<>();
        for (String ref : profile.domainTags()) {
            var tagKey = DomainStructureTags.fromReference(ref);
            if (tagKey != null && holder.is(tagKey)) {
                matched.add(ref);
            }
        }
        return matched;
    }

    /**
     * Resolve a structure's effective tier: per-structure config override → datapack tier tag →
     * the candidate deity's configured default tier.
     */
    private static StructureTier resolveTier(Holder<Structure> holder, String structureId,
                                             List<String> matchedDomainTags, StructureTier deityDefault) {
        StructureTier override = overrideTier(structureId, matchedDomainTags);
        if (override != null) {
            return override;
        }
        if (holder.is(DomainStructureTags.TIER_LEGENDARY)) return StructureTier.LEGENDARY;
        if (holder.is(DomainStructureTags.TIER_RARE)) return StructureTier.RARE;
        if (holder.is(DomainStructureTags.TIER_UNCOMMON)) return StructureTier.UNCOMMON;
        if (holder.is(DomainStructureTags.TIER_COMMON)) return StructureTier.COMMON;
        return deityDefault;
    }

    private static StructureTier overrideTier(String structureId, List<String> matchedDomainTags) {
        for (Object raw : StructureSpawnConfig.GENERAL.structureTierOverrides.get()) {
            String entry = String.valueOf(raw);
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String id = entry.substring(0, eq).trim();
            String tierStr = entry.substring(eq + 1).trim();
            boolean match = id.equals(structureId)
                    || (matchedDomainTags != null && matchedDomainTags.contains(id));
            if (match && StructureTier.isValid(tierStr)) {
                return StructureTier.fromString(tierStr, null);
            }
        }
        return null;
    }

    private static double dimensionModifier(String dimension) {
        for (Object raw : StructureSpawnConfig.GENERAL.dimensionModifiers.get()) {
            String entry = String.valueOf(raw);
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if (entry.substring(0, eq).trim().equals(dimension)) {
                try {
                    return Double.parseDouble(entry.substring(eq + 1).trim());
                } catch (NumberFormatException e) {
                    SpellsNGodsMod.LOGGER.warn("[Sng/Spawn] Invalid dimension modifier '{}'", entry);
                    return 1.0;
                }
            }
        }
        return 1.0;
    }

    private static double difficultyScalar(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> StructureSpawnConfig.GENERAL.difficultyScalarPeaceful.get();
            case EASY -> StructureSpawnConfig.GENERAL.difficultyScalarEasy.get();
            case NORMAL -> StructureSpawnConfig.GENERAL.difficultyScalarNormal.get();
            case HARD -> StructureSpawnConfig.GENERAL.difficultyScalarHard.get();
        };
    }

    private static SpawnPlacement defaultPlacement() {
        return SpawnPlacement.fromString(StructureSpawnConfig.GENERAL.defaultPlacement.get(),
                SpawnPlacement.NEAREST_SAFE_FLOOR);
    }

    private static Random deterministicRng(String stableKey, long dayIndex) {
        long seed = ((long) stableKey.hashCode() << 20) ^ (dayIndex * 0x9E3779B97F4A7C15L);
        return new Random(seed);
    }
}
