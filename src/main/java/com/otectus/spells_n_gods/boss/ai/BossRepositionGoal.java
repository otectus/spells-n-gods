package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * After completing major attacks (leap, shield, spell), the boss briefly disengages
 * and repositions to a tactical position: behind the player, flanking, or at optimal
 * spell-casting range. This gives the fight breathing room and makes the boss feel
 * like a thinking opponent.
 */
public class BossRepositionGoal extends Goal {
    private final GodBossEntity boss;
    private LivingEntity target;
    private Vec3 repositionTarget;
    private int ticksActive;

    private boolean shouldReposition;

    private static final int REPOSITION_DURATION = 15; // 0.75 seconds max — quick repositioning
    private static final double REPOSITION_SPEED = 1.6;
    private static final double FLANK_DISTANCE = 4.5; // Tighter flanking = back in melee range faster

    public BossRepositionGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** Called by other goals (leap, shield, spellcast) in their stop() to trigger. */
    public void triggerReposition() {
        this.shouldReposition = true;
    }

    @Override
    public boolean canUse() {
        if (!shouldReposition) return false;
        target = boss.getTarget();
        return target != null && target.isAlive() && boss.getCurrentPhase().isInCombat();
    }

    @Override
    public void start() {
        shouldReposition = false;
        ticksActive = 0;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Vec3 targetLook = target.getLookAngle().normalize();

        // Choose: behind target (60%) or flank (40%)
        Vec3 offset;
        if (rng.nextDouble() < 0.6) {
            // Behind target
            offset = targetLook.scale(-FLANK_DISTANCE);
        } else {
            // Perpendicular flank
            int side = rng.nextBoolean() ? 1 : -1;
            offset = new Vec3(-targetLook.z * side, 0, targetLook.x * side)
                    .normalize().scale(FLANK_DISTANCE);
        }

        repositionTarget = target.position().add(offset);
        boss.getNavigation().moveTo(
                repositionTarget.x, repositionTarget.y, repositionTarget.z,
                REPOSITION_SPEED);
    }

    @Override
    public void tick() {
        ticksActive++;
        if (target != null) {
            boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return ticksActive < REPOSITION_DURATION
                && !boss.getNavigation().isDone()
                && target != null && target.isAlive();
    }

    @Override
    public void stop() {
        target = null;
        repositionTarget = null;
        boss.getNavigation().stop();
    }
}
