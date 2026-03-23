package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.network.BossVisualEffectPacket;
import com.otectus.spells_n_gods.network.ModNetwork;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Boss dramatically leaps toward the target player, dealing AoE damage on landing.
 * Cooldown-based: won't leap again for a while after use.
 * More frequent when enraged.
 */
public class BossLeapGoal extends Goal {
    private final GodBossEntity boss;
    private LivingEntity target;
    private int cooldown = 0;
    private boolean inAir = false;
    private int airTicks = 0;

    private static final double MIN_RANGE_SQ = 16.0; // 4 blocks — don't leap if too close
    private static final double MAX_RANGE_SQ = 225.0; // 15 blocks
    private static final double LEAP_STRENGTH = 1.2;
    private static final double LEAP_HEIGHT = 0.65;

    public BossLeapGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    private GodDefinition.BossDefinition bossDef() {
        GodDefinition god = boss.getGodDefinition();
        return god != null ? god.boss() : GodDefinition.BossDefinition.defaultBoss();
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = boss.distanceToSqr(target);
        if (distSq < MIN_RANGE_SQ || distSq > MAX_RANGE_SQ) return false;

        // 50% chance each eligible tick to add variability
        return ThreadLocalRandom.current().nextFloat() < 0.50f;
    }

    @Override
    public void start() {
        if (target == null) return;

        Vec3 direction = target.position().subtract(boss.position()).normalize();
        double dist = boss.distanceTo(target);
        double horizStrength = Math.min(LEAP_STRENGTH, dist * 0.1);

        boss.setDeltaMovement(
                direction.x * horizStrength,
                LEAP_HEIGHT,
                direction.z * horizStrength
        );
        boss.hasImpulse = true;
        boss.setLeaping(true);
        inAir = true;
        airTicks = 0;

        // Launch sound
        boss.level().playSound(null, boss.blockPosition(),
                SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    @Override
    public boolean canContinueToUse() {
        return inAir && airTicks < 40; // Max 2 seconds in air
    }

    @Override
    public void tick() {
        airTicks++;

        // Keep looking at target while leaping
        if (target != null && target.isAlive()) {
            boss.getLookControl().setLookAt(target, 360.0F, 90.0F);
        }

        // Check if landed (on ground after being in air)
        if (airTicks > 3 && boss.onGround()) {
            onLand();
            inAir = false;
        }
    }

    private void onLand() {
        GodDefinition.BossDefinition def = bossDef();
        double landingRadius = def.leapLandingRadius();

        // AoE damage and knockback on landing
        AABB aoe = boss.getBoundingBox().inflate(landingRadius, 1.0, landingRadius);
        List<LivingEntity> nearby = boss.level().getEntitiesOfClass(
                LivingEntity.class, aoe,
                e -> e != boss && e.isAlive()
        );

        float damage = (float) def.leapLandingDamage();
        if (boss.getCurrentPhase().isEnraged()) {
            damage *= 1.5f;
        }

        for (LivingEntity entity : nearby) {
            entity.hurt(boss.damageSources().mobAttack(boss), damage);

            // Knockback away from boss
            Vec3 knockback = entity.position().subtract(boss.position()).normalize().scale(1.2);
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.4, knockback.z));
            entity.hurtMarked = true;
        }

        // Landing sound
        boss.level().playSound(null, boss.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.6F);

        // Visual: ground slam particles with school-colored dust
        if (boss.level() instanceof ServerLevel serverLevel) {
            GodDefinition god = boss.getGodDefinition();
            String school = god != null ? god.magicSchool() : "";

            // School-colored dust ring
            DustParticleOptions schoolDust = new DustParticleOptions(
                    SchoolColors.getSchoolColor(school), 1.5f);
            serverLevel.sendParticles(schoolDust,
                    boss.getX(), boss.getY() + 0.2, boss.getZ(),
                    25, landingRadius, 0.1, landingRadius, 0.02);
            // School-themed particle
            serverLevel.sendParticles(SchoolColors.getSchoolParticle(school),
                    boss.getX(), boss.getY() + 0.3, boss.getZ(),
                    10, landingRadius * 0.5, 0.2, landingRadius * 0.5, 0.03);
            // Smoke for ground impact
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    boss.getX(), boss.getY() + 0.2, boss.getZ(),
                    15, landingRadius * 0.5, 0.1, landingRadius * 0.5, 0.02);

            // Camera shake for nearby players
            BossVisualEffectPacket packet = new BossVisualEffectPacket(
                    BossVisualEffectPacket.EffectType.CAMERA_SHAKE_HEAVY,
                    boss.getX(), boss.getY(), boss.getZ());
            for (ServerPlayer sp : serverLevel.players()) {
                if (sp.distanceToSqr(boss) < 2500) {
                    ModNetwork.sendToPlayer(sp, packet);
                }
            }
        }

        cooldown = boss.getCurrentPhase().isEnraged() ? def.leapEnragedCooldown() : def.leapBaseCooldown();
    }

    @Override
    public void stop() {
        inAir = false;
        airTicks = 0;
        target = null;
        boss.setLeaping(false);
        // Trigger tactical reposition after landing
        if (boss.getRepositionGoal() != null) {
            boss.getRepositionGoal().triggerReposition();
        }
    }
}
