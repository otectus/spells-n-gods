package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import com.otectus.spells_n_gods.worldstate.StructureRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * When the boss has no target, it wanders naturally within the arena
 * rather than standing perfectly still. Picks random points weighted
 * toward the center, walks at variable speed, and pauses between destinations.
 */
public class BossIdleWanderGoal extends Goal {
    private final GodBossEntity boss;
    private BlockPos arenaCenter;
    private double arenaRadius;

    private enum WanderState { CHOOSING, WALKING, PAUSING }
    private WanderState state = WanderState.CHOOSING;
    private int pauseTimer;
    private int chooseDelay;

    private static final int MIN_PAUSE_TICKS = 40;  // 2 seconds
    private static final int MAX_PAUSE_TICKS = 100;  // 5 seconds
    private static final int CHOOSE_DELAY_MIN = 20;
    private static final int CHOOSE_DELAY_MAX = 60;

    public BossIdleWanderGoal(GodBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (boss.getTarget() != null) return false;
        if (boss.isEmerging()) return false;
        if (boss.getServer() == null || boss.getGodId().isEmpty()) return false;

        GodWorldState worldState = GodWorldState.get(boss.getServer());
        Optional<StructureRecord> opt = worldState.getStructure(boss.getGodId());
        if (opt.isEmpty()) return false;

        arenaCenter = opt.get().center();
        GodDefinition god = boss.getGodDefinition();
        arenaRadius = god != null
                ? god.boss().leashRadius() * 0.4
                : 12.0;

        return true;
    }

    @Override
    public void start() {
        state = WanderState.CHOOSING;
        chooseDelay = CHOOSE_DELAY_MIN
                + ThreadLocalRandom.current().nextInt(CHOOSE_DELAY_MAX - CHOOSE_DELAY_MIN);
    }

    @Override
    public void tick() {
        switch (state) {
            case CHOOSING -> {
                if (--chooseDelay <= 0) {
                    pickWanderTarget();
                    state = WanderState.WALKING;
                }
            }
            case WALKING -> {
                if (boss.getNavigation().isDone()) {
                    state = WanderState.PAUSING;
                    pauseTimer = MIN_PAUSE_TICKS
                            + ThreadLocalRandom.current().nextInt(
                                    MAX_PAUSE_TICKS - MIN_PAUSE_TICKS);
                }
            }
            case PAUSING -> {
                if (--pauseTimer <= 0) {
                    state = WanderState.CHOOSING;
                    chooseDelay = CHOOSE_DELAY_MIN
                            + ThreadLocalRandom.current().nextInt(
                                    CHOOSE_DELAY_MAX - CHOOSE_DELAY_MIN);
                }
            }
        }
    }

    private void pickWanderTarget() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // sqrt distribution weights destinations toward center
        double dist = Math.sqrt(rng.nextDouble()) * arenaRadius;
        double angle = rng.nextDouble() * Math.PI * 2;
        double tx = arenaCenter.getX() + 0.5 + Math.cos(angle) * dist;
        double tz = arenaCenter.getZ() + 0.5 + Math.sin(angle) * dist;
        double ty = arenaCenter.getY() + GodBossEntity.ALTAR_STANDING_Y_OFFSET;

        // Variable walk speed: 0.6 to 0.9 of base
        double speedMult = 0.6 + rng.nextDouble() * 0.3;
        boss.getNavigation().moveTo(tx, ty, tz, speedMult);
    }

    @Override
    public boolean canContinueToUse() {
        return boss.getTarget() == null && !boss.isEmerging();
    }

    @Override
    public void stop() {
        boss.getNavigation().stop();
    }
}
