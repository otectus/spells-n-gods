package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.compat.ModIntegrationLayer;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SpellcastingGoal extends Goal {
    private final GodBossEntity boss;
    private LivingEntity target;
    private int castTimer;
    private GodDefinition.SpellEntry chosenSpell;

    private static final int CAST_TIME_TICKS = 15; // 0.75 seconds

    public SpellcastingGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!boss.canCastSpell()) return false;

        target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!boss.getSensing().hasLineOfSight(target)) return false;

        GodDefinition god = boss.getGodDefinition();
        if (god == null) return false;

        double distSqr = boss.distanceToSqr(target);

        List<GodDefinition.SpellEntry> pool = god.boss().spellPool();
        if (pool == null || pool.isEmpty()) return false;

        // Filter spells by per-spell range
        List<GodDefinition.SpellEntry> eligible = pool.stream()
                .filter(s -> s.range() * s.range() >= distSqr)
                .toList();
        if (eligible.isEmpty()) return false;

        // Weighted random selection from eligible spells
        int totalWeight = eligible.stream().mapToInt(GodDefinition.SpellEntry::weight).sum();
        if (totalWeight <= 0) return false;

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        chosenSpell = null;
        for (GodDefinition.SpellEntry entry : eligible) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                chosenSpell = entry;
                break;
            }
        }

        return chosenSpell != null && !chosenSpell.spellId().isEmpty();
    }

    @Override
    public void start() {
        castTimer = CAST_TIME_TICKS;
        boss.getNavigation().stop();
        boss.setCasting(true);
    }

    @Override
    public boolean canContinueToUse() {
        // Commit-to-cast: once casting begins, finish regardless of range
        return castTimer > 0 && target != null && target.isAlive();
    }

    @Override
    public void tick() {
        if (target != null) {
            boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        // Spawn runic casting VFX during windup
        if (castTimer > 0) {
            spawnCastingVFX();
        }

        castTimer--;
        if (castTimer <= 0) {
            executeCast();
        }
    }

    private void spawnCastingVFX() {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;

        GodDefinition god = boss.getGodDefinition();
        if (god == null) return;

        Vector3f color = getSchoolColor(god.magicSchool());
        DustParticleOptions dust = new DustParticleOptions(color, 1.2f);

        double cx = boss.getX();
        double cy = boss.getY() + 1.5;
        double cz = boss.getZ();

        // Progress fraction (0.0 = just started, 1.0 = about to fire)
        float progress = 1.0f - ((float) castTimer / CAST_TIME_TICKS);

        // Expanding spiral ring of school-colored dust
        int particlesPerTick = 4 + (int) (progress * 8);
        double radius = 0.5 + progress * 1.5;
        double baseAngle = boss.tickCount * 0.3;

        for (int i = 0; i < particlesPerTick; i++) {
            double angle = baseAngle + (i / (double) particlesPerTick) * Math.PI * 2;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = cy + progress * 0.8;
            serverLevel.sendParticles(dust, px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
        }

        // Inner enchant glow intensifies past 50% progress
        if (progress > 0.5) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    cx, cy, cz, 3, 0.2, 0.3, 0.2, 0.5);
        }
    }

    private static Vector3f getSchoolColor(String school) {
        return SchoolColors.getSchoolColor(school);
    }

    private void executeCast() {
        if (chosenSpell == null || target == null) return;

        // Clamp so malformed datapack data (minLevel > maxLevel) can't crash nextInt.
        int hi = Math.max(chosenSpell.minLevel(), chosenSpell.maxLevel());
        int level = ThreadLocalRandom.current().nextInt(chosenSpell.minLevel(), hi + 1);

        GodDefinition god = boss.getGodDefinition();
        String school = god != null ? god.magicSchool() : "";

        ModIntegrationLayer.castSpell(boss, target, chosenSpell.spellId(), level, school);

        // Cast sound
        boss.level().playSound(null, boss.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.6F,
                1.2F + (float)(Math.random() * 0.4 - 0.2));

        // Cast-completion burst in school color
        if (boss.level() instanceof ServerLevel serverLevel && god != null) {
            Vector3f color = getSchoolColor(god.magicSchool());
            DustParticleOptions dust = new DustParticleOptions(color, 2.0f);
            serverLevel.sendParticles(dust,
                    boss.getX(), boss.getY() + 1.5, boss.getZ(),
                    20, 0.8, 0.8, 0.8, 0.1);
        }

        int cooldown = god != null ? god.boss().spellCooldownTicks() : 60;
        if (boss.getCurrentPhase().isEnraged()) {
            cooldown = (int) (cooldown * 0.6); // Faster casts when enraged
        }
        boss.setSpellCooldown(cooldown);
    }

    @Override
    public void stop() {
        target = null;
        chosenSpell = null;
        castTimer = 0;
        boss.setCasting(false);
        // Trigger tactical reposition after spell cast
        if (boss.getRepositionGoal() != null) {
            boss.getRepositionGoal().triggerReposition();
        }
    }
}
