package com.otectus.spells_n_gods.boss;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.registry.ModEntities;
import com.otectus.spells_n_gods.util.SchoolColors;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import com.otectus.spells_n_gods.worldstate.StructureRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Manages the dramatic "rising from the ground" spawn animation for god bosses.
 * When triggered, the boss entity is created below the altar and smoothly rises
 * over several seconds with themed particles, sounds, and a GeckoLib animation.
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class BossSpawnAnimationHandler {

    private static final List<SpawnSequence> activeSequences = new ArrayList<>();
    private static final Set<GodBossEntity> activeBosses = Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private static final int TOTAL_DURATION = 120;        // 6 seconds
    private static final double START_Y_OFFSET = -2.5;     // Start 2.5 blocks below altar surface
    private static final double END_Y_OFFSET = GodBossEntity.ALTAR_STANDING_Y_OFFSET;
    private static final int VISIBILITY_TICK = 50;          // Become visible at ~2.5 seconds
    private static final int AI_ENABLE_TICK = TOTAL_DURATION;

    // --- Public API ---

    public static void beginSpawnSequence(ServerLevel level, GodWorldState state,
                                           StructureRecord record) {
        GodDefinition god = SpellsNGodsDataManager.getGods()
                .get(new ResourceLocation(record.godId()));
        if (god == null) {
            SpellsNGodsMod.LOGGER.warn("Cannot spawn boss: god def not found for {}", record.godId());
            return;
        }

        GodBossEntity boss = ModEntities.GOD_BOSS.get().create(level);
        if (boss == null) {
            SpellsNGodsMod.LOGGER.error("Failed to create boss entity for {}", record.godId());
            return;
        }

        BlockPos center = record.center();

        // If spawn animation is disabled, place the boss instantly at final position
        if (!SpellsNGodsConfig.COMMON.bossSpawnAnimationEnabled.get()) {
            boss.setGodId(record.godId());
            boss.moveTo(center.getX() + 0.5, center.getY() + END_Y_OFFSET,
                        center.getZ() + 0.5, 0.0F, 0.0F);
            boss.applyGodStats();
            level.addFreshEntity(boss);
            state.setStructure(record.godId(), record.withBossSpawned(boss.getUUID()));
            SpellsNGodsMod.LOGGER.info("Instant spawn (animation disabled) for {} at {}",
                                     record.godId(), center);
            return;
        }

        double startY = center.getY() + START_Y_OFFSET;

        boss.setGodId(record.godId());
        boss.moveTo(center.getX() + 0.5, startY, center.getZ() + 0.5, 0.0F, 0.0F);
        boss.applyGodStats();
        boss.setNoAi(true);
        boss.setInvulnerable(true);
        boss.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, TOTAL_DURATION + 40,
                0, false, false));
        boss.setEmerging(true);

        // Register the spawn sequence BEFORE adding the entity to the world.
        // This prevents a race where the entity's first tick checks hasActiveSequence()
        // before the sequence is registered, which would immediately cancel the animation.
        String magicSchool = god.magicSchool();
        activeSequences.add(new SpawnSequence(boss, center, level, magicSchool));
        activeBosses.add(boss);

        level.addFreshEntity(boss);

        // Mark boss as spawned in world state (clears awaitingPlayerSpawn)
        state.setStructure(record.godId(), record.withBossSpawned(boss.getUUID()));

        // Initial rumble sound
        level.playSound(null, center, SoundEvents.WARDEN_EMERGE,
                SoundSource.HOSTILE, 1.5F, 0.5F);

        SpellsNGodsMod.LOGGER.info("Started spawn sequence for {} at {}", record.godId(), center);
    }

    public static boolean hasActiveSequence(GodBossEntity boss) {
        return activeBosses.contains(boss);
    }

    // --- Tick Handler ---

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (activeSequences.isEmpty()) return;

        Iterator<SpawnSequence> it = activeSequences.iterator();
        while (it.hasNext()) {
            SpawnSequence seq = it.next();
            if (!seq.boss.isAlive()) {
                activeBosses.remove(seq.boss);
                it.remove();
                continue;
            }

            seq.ticksElapsed++;
            tickSequence(seq);

            if (seq.ticksElapsed >= TOTAL_DURATION) {
                finalizeSpawn(seq);
                activeBosses.remove(seq.boss);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            activeSequences.removeIf(seq -> {
                if (seq.level == serverLevel) {
                    activeBosses.remove(seq.boss);
                    return true;
                }
                return false;
            });
        }
    }

    // --- Animation Tick ---

    private static void tickSequence(SpawnSequence seq) {
        double progress = (double) seq.ticksElapsed / TOTAL_DURATION; // 0.0 to 1.0

        // Smooth cubic ease-out for Y position
        double easedProgress = 1.0 - Math.pow(1.0 - progress, 3);
        double currentY = seq.center.getY() + START_Y_OFFSET
                + (END_Y_OFFSET - START_Y_OFFSET) * easedProgress;

        seq.boss.teleportTo(
                seq.center.getX() + 0.5,
                currentY,
                seq.center.getZ() + 0.5);

        // Prevent fall damage during rise
        seq.boss.fallDistance = 0.0F;

        // --- Visibility transition ---
        if (seq.ticksElapsed == VISIBILITY_TICK) {
            seq.boss.removeEffect(MobEffects.INVISIBILITY);
            seq.level.playSound(null, seq.center, SoundEvents.TOTEM_USE,
                    SoundSource.HOSTILE, 1.2F, 0.7F);
        }

        // --- Particles ---
        spawnParticles(seq, progress);

        // --- Sound stages ---
        if (seq.ticksElapsed == 30) {
            seq.level.playSound(null, seq.center, SoundEvents.STONE_BREAK,
                    SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        if (seq.ticksElapsed == 60) {
            seq.level.playSound(null, seq.center, SoundEvents.ANVIL_LAND,
                    SoundSource.HOSTILE, 0.8F, 0.4F);
        }
        if (seq.ticksElapsed == 90) {
            seq.level.playSound(null, seq.center, SoundEvents.WARDEN_ROAR,
                    SoundSource.HOSTILE, 0.7F, 0.5F);
        }
    }

    private static void spawnParticles(SpawnSequence seq, double progress) {
        double cx = seq.center.getX() + 0.5;
        double cy = seq.center.getY() + 2.0; // altar surface level
        double cz = seq.center.getZ() + 0.5;

        // Block break particles from the altar accent block
        if (seq.ticksElapsed % 4 == 0) {
            BlockState altarBlock = seq.level.getBlockState(seq.center.above()); // accent block at cy+1
            if (!altarBlock.isAir()) {
                seq.level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, altarBlock),
                        cx, cy, cz,
                        5, 0.5, 0.3, 0.5, 0.05);
            }
        }

        // Rising smoke
        if (seq.ticksElapsed % 3 == 0) {
            seq.level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    cx, cy + 0.5, cz,
                    3, 0.4, 0.1, 0.4, 0.02);
        }

        // School-specific particles (every 5 ticks)
        if (seq.ticksElapsed % 5 == 0) {
            spawnSchoolParticles(seq.level, cx, cy, cz, seq.magicSchool, progress);
        }

        // Intensifying spiral ring as boss rises
        if (progress > 0.25 && seq.ticksElapsed % 6 == 0) {
            int count = (int) (8 + progress * 16);
            double radius = 2.0 - progress * 0.5;
            for (int i = 0; i < count; i++) {
                double angle = (i / (double) count) * Math.PI * 2
                        + seq.ticksElapsed * 0.15;
                seq.level.sendParticles(ParticleTypes.ENCHANT,
                        cx + Math.cos(angle) * radius,
                        cy + progress * 2.0,
                        cz + Math.sin(angle) * radius,
                        2, 0.05, 0.2, 0.05, 0.1);
            }
        }
    }

    static void spawnSchoolParticles(ServerLevel level, double cx, double cy,
                                              double cz, String school, double progress) {
        int count = (int) (3 + progress * 8);
        double spread = 1.0 + progress * 0.5;
        double speed = school.equalsIgnoreCase("lightning") || school.equalsIgnoreCase("ender") ? 0.1 : 0.02;

        level.sendParticles(SchoolColors.getSchoolParticle(school),
                cx, cy, cz, count, spread, 0.5, spread, speed);
    }

    private static void finalizeSpawn(SpawnSequence seq) {
        GodBossEntity boss = seq.boss;

        // Snap to final position
        boss.teleportTo(
                seq.center.getX() + 0.5,
                seq.center.getY() + END_Y_OFFSET,
                seq.center.getZ() + 0.5);

        // Enable AI and combat
        boss.setNoAi(false);
        boss.setInvulnerable(false);
        boss.setEmerging(false);
        boss.removeEffect(MobEffects.INVISIBILITY);

        // Dramatic reveal burst
        double cx = seq.center.getX() + 0.5;
        double cy = seq.center.getY() + END_Y_OFFSET;
        double cz = seq.center.getZ() + 0.5;

        seq.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                cx, cy + 1.0, cz, 1, 0, 0, 0, 0);
        spawnSchoolParticles(seq.level, cx, cy, cz, seq.magicSchool, 1.0);

        seq.level.playSound(null, seq.center, SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 1.0F, 0.6F);

        // Knockback pulse for impact
        for (ServerPlayer player : seq.level.players()) {
            if (player.distanceToSqr(boss) < 400) { // 20 blocks
                Vec3 push = player.position().subtract(boss.position()).normalize().scale(0.3);
                player.push(push.x, 0.15, push.z);
                player.hurtMarked = true;
            }
        }

        SpellsNGodsMod.LOGGER.info("Spawn sequence complete for {} at {}",
                boss.getGodId(), seq.center);
    }

    // --- Internal State ---

    private static class SpawnSequence {
        final GodBossEntity boss;
        final BlockPos center;
        final ServerLevel level;
        final String magicSchool;
        int ticksElapsed;

        SpawnSequence(GodBossEntity boss, BlockPos center, ServerLevel level,
                      String magicSchool) {
            this.boss = boss;
            this.center = center;
            this.level = level;
            this.magicSchool = magicSchool;
            this.ticksElapsed = 0;
        }
    }
}
