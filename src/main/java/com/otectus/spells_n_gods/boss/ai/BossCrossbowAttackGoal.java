package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Arrow;

import java.util.EnumSet;

/**
 * Ranged attack goal for crossbow-wielding bosses (Venatas).
 * Boss charges the crossbow, then fires a bolt with a nature-snare effect.
 * Fires faster when enraged.
 */
public class BossCrossbowAttackGoal extends Goal {
    private final GodBossEntity boss;
    private LivingEntity target;
    private int cooldown = 0;
    private int chargeTicks = 0;
    private boolean charging = false;

    private static final double MIN_RANGE_SQ = 36.0;  // 6 blocks
    private static final double MAX_RANGE_SQ = 324.0;  // 18 blocks
    private static final int CHARGE_DURATION = 20;      // 1 second charge
    private static final float BOLT_DAMAGE = 11.0f;

    public BossCrossbowAttackGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
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
        if (!boss.getCurrentPhase().isInCombat()) return false;

        double distSq = boss.distanceToSqr(target);
        return distSq >= MIN_RANGE_SQ && distSq <= MAX_RANGE_SQ;
    }

    @Override
    public void start() {
        charging = true;
        chargeTicks = 0;
        boss.getNavigation().stop();
        boss.setAggressive(true);
    }

    @Override
    public boolean canContinueToUse() {
        return charging && target != null && target.isAlive()
                && boss.getCurrentPhase().isInCombat();
    }

    @Override
    public void tick() {
        if (target == null) return;

        boss.getLookControl().setLookAt(target, 360.0F, 90.0F);
        chargeTicks++;

        double distSq = boss.distanceToSqr(target);
        if (distSq < MIN_RANGE_SQ) {
            charging = false;
            return;
        }

        // Nature particles while charging (Venatas theme)
        if (boss.level() instanceof ServerLevel sl && chargeTicks % 4 == 0) {
            sl.sendParticles(ParticleTypes.COMPOSTER,
                    boss.getX(), boss.getY() + 1.2, boss.getZ(),
                    2, 0.2, 0.2, 0.2, 0.01);
        }

        if (chargeTicks >= CHARGE_DURATION) {
            fireBolt();
            charging = false;
        }
    }

    private void fireBolt() {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;

        Arrow bolt = new Arrow(serverLevel, boss);
        double dx = target.getX() - boss.getX();
        double dy = target.getY(0.3333) - bolt.getY();
        double dz = target.getZ() - boss.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        bolt.shoot(dx, dy + dist * 0.03, dz, 3.15f, 1.5f);
        bolt.setBaseDamage(BOLT_DAMAGE + bossDef().rangedBonusDamage());

        if (boss.getCurrentPhase().isEnraged()) {
            bolt.setBaseDamage(bolt.getBaseDamage() * 1.3);
            bolt.setCritArrow(true);
        }

        // Tag for Slowness on-hit (handled by event or via arrow's own effects)
        bolt.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)); // Brief snare
        bolt.setOwner(boss);
        serverLevel.addFreshEntity(bolt);

        // Effects
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                boss.getX(), boss.getY() + 1.2, boss.getZ(),
                8, 0.3, 0.3, 0.3, 0.05);
        serverLevel.playSound(null, boss.blockPosition(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F);

        cooldown = boss.getCurrentPhase().isEnraged()
                ? bossDef().rangedEnragedCooldown()
                : bossDef().rangedAttackCooldown();
    }

    @Override
    public void stop() {
        charging = false;
        chargeTicks = 0;
        target = null;
        boss.setAggressive(false);
        if (boss.getRepositionGoal() != null) {
            boss.getRepositionGoal().triggerReposition();
        }
    }
}
