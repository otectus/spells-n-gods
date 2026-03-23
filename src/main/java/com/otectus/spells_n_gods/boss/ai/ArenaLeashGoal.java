package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import com.otectus.spells_n_gods.worldstate.StructureRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Optional;

public class ArenaLeashGoal extends Goal {
    private final GodBossEntity boss;
    private BlockPos arenaCenter;
    private double leashRadius;

    public ArenaLeashGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (boss.getServer() == null) return false;
        if (boss.getGodId().isEmpty()) return false;

        GodWorldState state = GodWorldState.get(boss.getServer());
        Optional<StructureRecord> recordOpt = state.getStructure(boss.getGodId());
        if (recordOpt.isEmpty()) return false;

        arenaCenter = recordOpt.get().center();
        GodDefinition god = boss.getGodDefinition();
        leashRadius = god != null ? god.boss().leashRadius() : 32.0;

        double distSq = boss.blockPosition().distSqr(arenaCenter);
        return distSq > leashRadius * leashRadius;
    }

    @Override
    public void start() {
        double distSq = boss.blockPosition().distSqr(arenaCenter);
        // If extremely far (2x radius), teleport directly
        if (distSq > (leashRadius * 2) * (leashRadius * 2)) {
            boss.teleportTo(
                    arenaCenter.getX() + 0.5,
                    arenaCenter.getY() + GodBossEntity.ALTAR_STANDING_Y_OFFSET,
                    arenaCenter.getZ() + 0.5
            );
        } else {
            boss.getNavigation().moveTo(
                    arenaCenter.getX() + 0.5,
                    arenaCenter.getY() + GodBossEntity.ALTAR_STANDING_Y_OFFSET,
                    arenaCenter.getZ() + 0.5,
                    1.5
            );
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !boss.getNavigation().isDone()
                && boss.blockPosition().distSqr(arenaCenter) > 4.0;
    }

    @Override
    public void stop() {
        boss.getNavigation().stop();
    }
}
