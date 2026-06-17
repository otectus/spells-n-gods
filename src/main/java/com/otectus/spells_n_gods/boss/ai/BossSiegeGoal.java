package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Makes a god boss break through blocks to reach its target, like the Wither or Ender Dragon.
 *
 * <p>This is a <strong>flagless</strong> goal: it claims no {@link Goal.Flag}, so it runs
 * concurrently with the movement/attack goals instead of competing with them. Each tick it judges
 * whether the boss is obstructed; on a fixed cadence it breaks the column of blocks ahead, and if the
 * boss stays stuck long enough it escalates to a Wither-style area smash. All destruction is filtered
 * through {@link BossSiegeRules} so bedrock, tagged blocks, block entities, and temples are spared.
 *
 * <p>Decision and geometry logic lives in {@link BossSiegeLogic} (pure, unit-tested); this class only
 * wires it to the live world.
 */
public class BossSiegeGoal extends Goal {
    /** Below this squared distance the boss is effectively on the player — no need to dig. */
    private static final double APPROACH_MIN_SQ = 9.0; // 3 blocks

    private final GodBossEntity boss;

    private int breakTimer;
    private int stuckTicks;
    private double lastDistSq = Double.MAX_VALUE;

    public BossSiegeGoal(GodBossEntity boss) {
        this.boss = boss;
        // No flags: this goal augments movement, it does not replace it.
    }

    private static boolean enabled() {
        return SpellsNGodsConfig.COMMON.bossSiegeEnabled.get();
    }

    @Override
    public boolean canUse() {
        if (!enabled()) {
            return false;
        }
        LivingEntity target = boss.getTarget();
        return target != null && target.isAlive() && boss.getCurrentPhase().isInCombat();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        breakTimer = 0;
        stuckTicks = 0;
        lastDistSq = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
        stuckTicks = 0;
        lastDistSq = Double.MAX_VALUE;
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        if (target == null) {
            return;
        }

        double distSq = boss.distanceToSqr(target);
        boolean obstructed = distSq > APPROACH_MIN_SQ
                && (boss.horizontalCollision || boss.getNavigation().isDone());

        stuckTicks = BossSiegeLogic.updateStuckTicks(stuckTicks, obstructed, lastDistSq, distSq);
        lastDistSq = distSq;

        int interval = Math.max(1, SpellsNGodsConfig.COMMON.bossSiegeBreakIntervalTicks.get());
        if (++breakTimer < interval) {
            return;
        }
        breakTimer = 0;

        int escalationTicks = SpellsNGodsConfig.COMMON.bossSiegeEscalationSeconds.get() * 20;
        BossSiegeLogic.Mode mode = BossSiegeLogic.decideMode(true, true, obstructed, stuckTicks, escalationTicks);
        if (mode == BossSiegeLogic.Mode.NONE) {
            return;
        }

        int height = Math.max(2, Mth.ceil(boss.getBbHeight()));
        List<int[]> offsets;
        if (mode == BossSiegeLogic.Mode.BREAK_AREA) {
            int radius = SpellsNGodsConfig.COMMON.bossSiegeSmashRadius.get();
            offsets = BossSiegeLogic.areaBreakOffsets(radius, height);
        } else {
            int[] dir = BossSiegeLogic.stepDirection(target.getX() - boss.getX(), target.getZ() - boss.getZ());
            offsets = BossSiegeLogic.pathBreakOffsets(dir[0], dir[1], height);
        }

        breakOffsets(offsets);
    }

    private void breakOffsets(List<int[]> offsets) {
        Level level = boss.level();
        BlockPos foot = boss.blockPosition();
        for (int[] o : offsets) {
            BlockPos pos = foot.offset(o[0], o[1], o[2]);
            if (BossSiegeRules.canBreak(level, pos)) {
                // dropBlock = false: annihilate (like the Wither) without littering the arena with items.
                level.destroyBlock(pos, false, boss);
            }
        }
    }
}
