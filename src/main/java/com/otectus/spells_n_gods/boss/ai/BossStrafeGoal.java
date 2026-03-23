package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Makes the boss circle-strafe around the player between melee attacks.
 * Enhanced with sprint bursts, damage-reactive dodging, and speed variation
 * for a more dynamic, player-like combat feel.
 */
public class BossStrafeGoal extends Goal {
    private final GodBossEntity boss;
    private LivingEntity target;
    private int strafeDirection; // 1 = clockwise, -1 = counter-clockwise
    private int directionChangeTimer;
    private boolean strafingBackwards;

    private static final double STRAFE_SPEED = 1.0;
    private static final double PREFERRED_DISTANCE_SQ = 25.0; // 5 blocks — closer preferred range
    private static final double TOO_CLOSE_SQ = 4.0; // 2 blocks
    private static final double TOO_FAR_SQ = 100.0; // 10 blocks

    // Sprint burst
    private int sprintBurstCooldown = 0;
    private boolean sprinting = false;
    private int sprintBurstTimer = 0;
    private static final int SPRINT_BURST_COOLDOWN = 50; // 2.5 seconds
    private static final int SPRINT_BURST_DURATION = 20;

    // Reactive dodge
    private boolean dodgeRequested = false;

    public BossStrafeGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** Called from GodBossEntity.hurt() when hit by a player. */
    public void requestDodge() {
        dodgeRequested = true;
    }

    @Override
    public boolean canUse() {
        target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = boss.distanceToSqr(target);
        return distSq < TOO_FAR_SQ && boss.getCurrentPhase().isInCombat();
    }

    @Override
    public void start() {
        strafeDirection = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        directionChangeTimer = 20 + ThreadLocalRandom.current().nextInt(25);
        strafingBackwards = false;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && boss.getCurrentPhase().isInCombat();
    }

    @Override
    public void tick() {
        if (target == null) return;

        // --- Sprint burst cooldown ---
        if (sprintBurstCooldown > 0) sprintBurstCooldown--;

        // --- Reactive dodge on being hit ---
        if (dodgeRequested) {
            dodgeRequested = false;
            Vec3 away = boss.position().subtract(target.position()).normalize().scale(0.6);
            boss.setDeltaMovement(away.x, 0.2, away.z);
            boss.hasImpulse = true;
            return;
        }

        // --- Sprint burst: occasionally rush the target ---
        if (sprinting) {
            sprintBurstTimer--;
            if (sprintBurstTimer <= 0) {
                sprinting = false;
            } else {
                boss.getNavigation().moveTo(target, 1.5);
                boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
                return;
            }
        }
        if (sprintBurstCooldown <= 0 && ThreadLocalRandom.current().nextFloat() < 0.08f) {
            sprinting = true;
            sprintBurstTimer = SPRINT_BURST_DURATION;
            sprintBurstCooldown = SPRINT_BURST_COOLDOWN;
            return;
        }

        // --- Normal strafe behavior ---
        double distSq = boss.distanceToSqr(target);

        if (distSq > PREFERRED_DISTANCE_SQ) {
            strafingBackwards = false;
        } else if (distSq < TOO_CLOSE_SQ) {
            strafingBackwards = true;
        }

        directionChangeTimer--;
        if (directionChangeTimer <= 0) {
            strafeDirection = -strafeDirection;
            directionChangeTimer = 15 + ThreadLocalRandom.current().nextInt(30);

            // Occasional feint pause
            if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
                directionChangeTimer = 10;
                return;
            }
        }

        boss.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Speed variation per tick (not constant)
        float currentStrafeSpeed = (float) (STRAFE_SPEED
                * (0.8 + ThreadLocalRandom.current().nextDouble() * 0.4));
        float forward = strafingBackwards ? -0.6f : 0.6f;
        float strafe = strafeDirection * currentStrafeSpeed;

        boss.getMoveControl().strafe(forward, strafe);
    }

    @Override
    public void stop() {
        target = null;
        sprinting = false;
    }
}
