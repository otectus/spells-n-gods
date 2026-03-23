package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom melee attack goal that simulates player-like approach behavior:
 * - Speed ramps up gradually (like WASD acceleration)
 * - Adds lateral offset to avoid perfect beeline approaches
 * - Recalculates paths less aggressively to avoid stop-start stutter
 * - Brief "sizing up" pause before first swing on entering melee range
 */
public class BossMeleeGoal extends Goal {
    private final GodBossEntity boss;
    private LivingEntity target;
    private int attackCooldown;
    private int decisionPauseTicks;
    private boolean hasEnteredRange;

    // Path recalculation control
    private Vec3 lastPathTarget;
    private int pathRecalcCooldown;
    private static final double REPATH_THRESHOLD_SQ = 4.0; // repath if target moves >2 blocks
    private static final int REPATH_MIN_INTERVAL = 8;

    // Speed ramping
    private int moveTicksElapsed;
    private static final int RAMP_UP_TICKS = 5;
    private static final double BASE_SPEED_MULT = 1.4;

    // Attack parameters
    private static final double ATTACK_REACH_SQ = 4.5; // ~2.1 blocks

    public BossMeleeGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private GodDefinition.BossDefinition bossDef() {
        GodDefinition god = boss.getGodDefinition();
        return god != null ? god.boss() : GodDefinition.BossDefinition.defaultBoss();
    }

    @Override
    public boolean canUse() {
        target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        return boss.getCurrentPhase().isInCombat();
    }

    @Override
    public void start() {
        moveTicksElapsed = 0;
        hasEnteredRange = false;
        decisionPauseTicks = 0;
        lastPathTarget = null;
        pathRecalcCooldown = 0;
        attackCooldown = 0;
        navigateToTargetWithOffset(0.7); // Start with momentum
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && boss.getCurrentPhase().isInCombat();
    }

    @Override
    public void tick() {
        target = boss.getTarget();
        if (target == null) return;

        boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distSq = boss.distanceToSqr(target);

        if (pathRecalcCooldown > 0) pathRecalcCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        moveTicksElapsed++;

        // Speed ramp: 0.7 → BASE_SPEED_MULT over RAMP_UP_TICKS
        double speedFraction = Math.min(1.0, (double) moveTicksElapsed / RAMP_UP_TICKS);
        double currentSpeedMult = 0.7 + (BASE_SPEED_MULT - 0.7) * speedFraction;

        if (distSq > ATTACK_REACH_SQ) {
            hasEnteredRange = false;

            boolean shouldRepath = lastPathTarget == null
                    || boss.getNavigation().isDone()
                    || (pathRecalcCooldown <= 0
                        && target.position().distanceToSqr(lastPathTarget) > REPATH_THRESHOLD_SQ);

            if (shouldRepath) {
                navigateToTargetWithOffset(currentSpeedMult);
                pathRecalcCooldown = REPATH_MIN_INTERVAL;
            }
        } else {
            // In melee range
            if (!hasEnteredRange) {
                hasEnteredRange = true;
                // Brief sizing-up pause before first swing
                decisionPauseTicks = 2 + ThreadLocalRandom.current().nextInt(4); // 2-5 ticks
                boss.getNavigation().stop();
                moveTicksElapsed = 0;
            }

            if (decisionPauseTicks > 0) {
                decisionPauseTicks--;
                return;
            }

            if (attackCooldown <= 0) {
                boss.swing(InteractionHand.MAIN_HAND);
                boss.doHurtTarget(target);
                int minInterval = bossDef().meleeAttackIntervalMin();
                int maxInterval = bossDef().meleeAttackIntervalMax();
                attackCooldown = minInterval
                        + ThreadLocalRandom.current().nextInt(
                                Math.max(1, maxInterval - minInterval));
                if (boss.isEnraged()) {
                    attackCooldown = (int) (attackCooldown * 0.5); // 50% faster swings when enraged
                }
            }
        }
    }

    private void navigateToTargetWithOffset(double speed) {
        if (target == null) return;
        // Add slight random lateral offset to avoid perfect beeline
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double offsetX = (rng.nextDouble() - 0.5) * 2.0;
        double offsetZ = (rng.nextDouble() - 0.5) * 2.0;
        Vec3 targetPos = target.position().add(offsetX, 0, offsetZ);
        lastPathTarget = target.position();
        boss.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, speed);
    }

    @Override
    public void stop() {
        target = null;
        boss.getNavigation().stop();
    }
}
