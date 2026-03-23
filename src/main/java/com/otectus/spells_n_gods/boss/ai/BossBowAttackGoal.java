package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Arrow;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ranged attack goal for bow-wielding bosses (Velox).
 * Boss draws the bow visually, then fires a high-damage arrow at the target.
 * Fires faster when enraged. Falls back to melee when target is too close.
 */
public class BossBowAttackGoal extends Goal {
    private final GodBossEntity boss;
    private LivingEntity target;
    private int cooldown = 0;
    private int drawTicks = 0;
    private boolean drawing = false;

    private static final double MIN_RANGE_SQ = 36.0;  // 6 blocks — switch to melee if closer
    private static final double MAX_RANGE_SQ = 400.0;  // 20 blocks
    private static final int DRAW_DURATION = 15;        // ~0.75s draw time
    private static final float ARROW_DAMAGE = 9.0f;

    public BossBowAttackGoal(GodBossEntity boss) {
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
        drawing = true;
        drawTicks = 0;
        boss.getNavigation().stop();
        // Start using item animation (bow draw)
        boss.setAggressive(true);
    }

    @Override
    public boolean canContinueToUse() {
        return drawing && target != null && target.isAlive()
                && boss.getCurrentPhase().isInCombat();
    }

    @Override
    public void tick() {
        if (target == null) return;

        boss.getLookControl().setLookAt(target, 360.0F, 90.0F);
        drawTicks++;

        // Slight strafe while drawing
        double distSq = boss.distanceToSqr(target);
        if (distSq < MIN_RANGE_SQ) {
            // Target got too close, abort and let melee take over
            drawing = false;
            return;
        }

        // Electric particles while drawing (Velox theme)
        if (boss.level() instanceof ServerLevel sl && drawTicks % 3 == 0) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    boss.getX(), boss.getY() + 1.5, boss.getZ(),
                    3, 0.2, 0.3, 0.2, 0.05);
        }

        if (drawTicks >= DRAW_DURATION) {
            fireArrow();
            drawing = false;
        }
    }

    private void fireArrow() {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;

        Arrow arrow = new Arrow(serverLevel, boss);
        double dx = target.getX() - boss.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - boss.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + dist * 0.05, dz, 2.5f, 2.0f);
        arrow.setBaseDamage(ARROW_DAMAGE + bossDef().rangedBonusDamage());

        if (boss.getCurrentPhase().isEnraged()) {
            arrow.setBaseDamage(arrow.getBaseDamage() * 1.4);
            arrow.setCritArrow(true);
        }

        arrow.setOwner(boss);
        serverLevel.addFreshEntity(arrow);

        // Effects
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                boss.getX(), boss.getY() + 1.5, boss.getZ(),
                10, 0.3, 0.3, 0.3, 0.1);
        serverLevel.playSound(null, boss.blockPosition(),
                SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F);

        // Set cooldown
        cooldown = boss.getCurrentPhase().isEnraged()
                ? bossDef().rangedEnragedCooldown()
                : bossDef().rangedAttackCooldown();
    }

    @Override
    public void stop() {
        drawing = false;
        drawTicks = 0;
        target = null;
        boss.setAggressive(false);
        // Trigger tactical reposition after shooting
        if (boss.getRepositionGoal() != null) {
            boss.getRepositionGoal().triggerReposition();
        }
    }
}
