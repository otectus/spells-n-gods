package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import com.otectus.spells_n_gods.worldstate.StructureRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Softer version of ArenaLeashGoal -- gently returns boss toward center
 * when it's outside the arena but still within the hard leash.
 * Only activates when boss has no target.
 */
public class ReturnToArenaGoal extends Goal {
    private final GodBossEntity boss;
    private BlockPos arenaCenter;

    public ReturnToArenaGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (boss.getTarget() != null) return false;
        if (boss.getServer() == null) return false;
        if (boss.getGodId().isEmpty()) return false;

        GodWorldState state = GodWorldState.get(boss.getServer());
        Optional<StructureRecord> recordOpt = state.getStructure(boss.getGodId());
        if (recordOpt.isEmpty()) return false;

        arenaCenter = recordOpt.get().center();
        GodDefinition god = boss.getGodDefinition();
        double softRadius = (god != null ? god.boss().leashRadius() : 32.0) * 0.5;

        double distSq = boss.blockPosition().distSqr(arenaCenter);
        return distSq > softRadius * softRadius;
    }

    @Override
    public void start() {
        boss.getNavigation().moveTo(
                arenaCenter.getX() + 0.5,
                arenaCenter.getY() + GodBossEntity.ALTAR_STANDING_Y_OFFSET,
                arenaCenter.getZ() + 0.5,
                1.0
        );
    }

    @Override
    public boolean canContinueToUse() {
        return !boss.getNavigation().isDone()
                && boss.blockPosition().distSqr(arenaCenter) > 9.0
                && boss.getTarget() == null;
    }

    @Override
    public void stop() {
        boss.getNavigation().stop();
    }
}
