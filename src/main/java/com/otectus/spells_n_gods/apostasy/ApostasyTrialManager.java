package com.otectus.spells_n_gods.apostasy;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.ApostasyDefinition;
import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the 3-phase apostasy trial system.
 */
public class ApostasyTrialManager {

    private static final Map<UUID, ActiveTrial> activeTrials = new ConcurrentHashMap<>();

    /**
     * Tracks an active apostasy trial.
     */
    private static class ActiveTrial {
        final UUID playerId;
        final GodDefinition god;
        int currentPhase;
        long phaseStartMs;
        final List<Entity> spawnedEntities = new ArrayList<>();
        boolean failed;

        ActiveTrial(UUID playerId, GodDefinition god) {
            this.playerId = playerId;
            this.god = god;
            this.currentPhase = 1;
            this.phaseStartMs = System.currentTimeMillis();
            this.failed = false;
        }
    }

    /**
     * Start apostasy trials for a player.
     */
    public static void startTrials(ServerPlayer player, GodDefinition god) {
        ActiveTrial trial = new ActiveTrial(player.getUUID(), god);
        activeTrials.put(player.getUUID(), trial);

        applyPhaseEffects(player, god, 1);

        ApostasyDefinition.PhaseConfig phase = god.apostasy().trials().getPhase(1);
        if (phase != null) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.trial.phase1.start",
                    phase.durationSeconds()));
        }

        SpellsNGodsMod.LOGGER.info("Started apostasy trial for player {}, god {}",
                player.getName().getString(), god.id());
    }

    /**
     * Check if player is in a trial.
     */
    public static boolean isInTrial(UUID playerId) {
        return activeTrials.containsKey(playerId);
    }

    /**
     * Get the current trial phase for a player.
     */
    public static int getCurrentPhase(UUID playerId) {
        ActiveTrial trial = activeTrials.get(playerId);
        return trial != null ? trial.currentPhase : 0;
    }

    /**
     * Tick all active trials.
     */
    public static void tickAllTrials(ServerPlayer player) {
        ActiveTrial trial = activeTrials.get(player.getUUID());
        if (trial == null || trial.failed) {
            return;
        }

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        if (!data.isInApostasyTrial()) {
            // Trial was cancelled externally
            cleanupTrial(player.getUUID());
            return;
        }

        ApostasyDefinition.PhaseConfig phaseConfig = trial.god.apostasy().trials().getPhase(trial.currentPhase);
        if (phaseConfig == null) {
            // Invalid phase, complete the trial
            completeTrial(player, true);
            return;
        }

        long elapsed = System.currentTimeMillis() - trial.phaseStartMs;
        if (elapsed >= phaseConfig.getDurationMs()) {
            // Phase completed
            advancePhase(player);
        } else {
            // Continue phase effects
            tickPhaseEffects(player, trial);
        }
    }

    /**
     * Advance to the next trial phase.
     */
    private static void advancePhase(ServerPlayer player) {
        ActiveTrial trial = activeTrials.get(player.getUUID());
        if (trial == null) {
            return;
        }

        // Clean up current phase entities
        for (Entity entity : trial.spawnedEntities) {
            if (entity.isAlive()) {
                entity.discard();
            }
        }
        trial.spawnedEntities.clear();

        trial.currentPhase++;
        trial.phaseStartMs = System.currentTimeMillis();

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        data.setCurrentTrialPhase(trial.currentPhase);
        data.setTrialPhaseStartMs(trial.phaseStartMs);

        if (trial.currentPhase > 3) {
            // All phases complete
            completeTrial(player, true);
        } else {
            // Start next phase
            player.sendSystemMessage(Component.translatable("spells_n_gods.trial.phase_complete"));

            applyPhaseEffects(player, trial.god, trial.currentPhase);

            ApostasyDefinition.PhaseConfig phase = trial.god.apostasy().trials().getPhase(trial.currentPhase);
            if (phase != null) {
                String phaseKey = "spells_n_gods.trial.phase" + trial.currentPhase + ".start";
                player.sendSystemMessage(Component.translatable(phaseKey, phase.durationSeconds()));
            }

            SpellsNGodsMod.LOGGER.debug("Player {} advanced to trial phase {}",
                    player.getName().getString(), trial.currentPhase);
        }
    }

    /**
     * Complete the trial (success or failure).
     */
    public static void completeTrial(ServerPlayer player, boolean success) {
        ActiveTrial trial = activeTrials.remove(player.getUUID());
        if (trial != null) {
            // Clean up spawned entities
            for (Entity entity : trial.spawnedEntities) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }

            // Remove trial effects
            player.removeEffect(MobEffects.GLOWING);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }

        if (success) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.trial.complete"));
            ApostasyHandler.completeApostasy(player);
        } else {
            ApostasyHandler.failApostasy(player);
        }
    }

    /**
     * Called when a player dies during a trial.
     */
    public static void onPlayerDeath(ServerPlayer player) {
        ActiveTrial trial = activeTrials.get(player.getUUID());
        if (trial != null) {
            trial.failed = true;
            completeTrial(player, false);
        }
    }

    /**
     * Apply effects for a specific trial phase.
     */
    private static void applyPhaseEffects(ServerPlayer player, GodDefinition god, int phase) {
        ApostasyDefinition.PhaseConfig phaseConfig = god.apostasy().trials().getPhase(phase);
        if (phaseConfig == null) {
            return;
        }

        switch (phase) {
            case 1 -> {
                // Phase 1: Marked for Wrath - glowing, slow
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                        (int)(phaseConfig.getDurationMs() / 50), 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        (int)(phaseConfig.getDurationMs() / 50), 0, true, false, true));
            }
            case 2 -> {
                // Phase 2: Divine Pursuit - spawn pursuers
                spawnPursuers(player, phaseConfig);
            }
            case 3 -> {
                // Phase 3: Final Judgment - spawn boss
                spawnBoss(player, phaseConfig);
            }
        }
    }

    private static void tickPhaseEffects(ServerPlayer player, ActiveTrial trial) {
        // Check if pursuers are alive (Phase 2)
        if (trial.currentPhase == 2) {
            trial.spawnedEntities.removeIf(e -> !e.isAlive());

            // Respawn if all pursuers are dead (optional - makes it harder)
            if (trial.spawnedEntities.isEmpty()) {
                ApostasyDefinition.PhaseConfig phaseConfig = trial.god.apostasy().trials().getPhase(2);
                if (phaseConfig != null && phaseConfig.pursuerCount() > 0) {
                    // Only respawn once
                    // spawnPursuers(player, phaseConfig);
                }
            }
        }

        // Check if boss is dead (Phase 3) - early completion
        if (trial.currentPhase == 3) {
            boolean bossAlive = trial.spawnedEntities.stream().anyMatch(Entity::isAlive);
            if (!bossAlive && !trial.spawnedEntities.isEmpty()) {
                // Boss defeated, advance early
                advancePhase(player);
            }
        }
    }

    private static void spawnPursuers(ServerPlayer player, ApostasyDefinition.PhaseConfig config) {
        if (config.pursuerType() == null || config.pursuerCount() <= 0) {
            return;
        }

        ActiveTrial trial = activeTrials.get(player.getUUID());
        if (trial == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ResourceLocation entityId = ResourceLocation.tryParse(config.pursuerType());
        if (entityId == null) {
            return;
        }

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (entityType == null) {
            SpellsNGodsMod.LOGGER.warn("Unknown pursuer entity type: {}", config.pursuerType());
            return;
        }

        for (int i = 0; i < config.pursuerCount(); i++) {
            BlockPos spawnPos = findSpawnPosition(player, 8 + i * 2);
            Entity entity = entityType.create(level);
            if (entity != null) {
                entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                level.addFreshEntity(entity);
                trial.spawnedEntities.add(entity);

                if (entity instanceof Mob mob) {
                    mob.setTarget(player);
                }
            }
        }

        SpellsNGodsMod.LOGGER.debug("Spawned {} pursuers for player {}",
                config.pursuerCount(), player.getName().getString());
    }

    private static void spawnBoss(ServerPlayer player, ApostasyDefinition.PhaseConfig config) {
        if (config.pursuerType() == null) {
            return;
        }

        ActiveTrial trial = activeTrials.get(player.getUUID());
        if (trial == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        ResourceLocation entityId = ResourceLocation.tryParse(config.pursuerType());
        if (entityId == null) {
            return;
        }

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (entityType == null) {
            SpellsNGodsMod.LOGGER.warn("Unknown boss entity type: {}", config.pursuerType());
            return;
        }

        BlockPos spawnPos = findSpawnPosition(player, 10);
        Entity entity = entityType.create(level);
        if (entity != null) {
            entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

            // Make boss stronger
            if (entity instanceof Mob mob) {
                // Boss has custom name
                mob.setCustomName(Component.translatable("spells_n_gods.trial.boss_name",
                        trial.god.displayName()));
                mob.setCustomNameVisible(true);
                mob.setTarget(player);
            }

            level.addFreshEntity(entity);
            trial.spawnedEntities.add(entity);

            SpellsNGodsMod.LOGGER.debug("Spawned boss for player {}", player.getName().getString());
        }
    }

    private static BlockPos findSpawnPosition(ServerPlayer player, int distance) {
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        RandomSource random = level.getRandom();

        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int x = playerPos.getX() + (int)(Math.cos(angle) * distance);
            int z = playerPos.getZ() + (int)(Math.sin(angle) * distance);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }

        // Fallback: spawn near player
        return playerPos.offset(distance, 0, 0);
    }

    private static void cleanupTrial(UUID playerId) {
        ActiveTrial trial = activeTrials.remove(playerId);
        if (trial != null) {
            for (Entity entity : trial.spawnedEntities) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }
        }
    }

    /**
     * Clean up all trials (for server shutdown).
     */
    public static void cleanup() {
        for (ActiveTrial trial : activeTrials.values()) {
            for (Entity entity : trial.spawnedEntities) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }
        }
        activeTrials.clear();
    }

    public static int getActiveTrialCount() {
        return activeTrials.size();
    }
}
