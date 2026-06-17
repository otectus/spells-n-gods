package com.otectus.spells_n_gods.boss.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, Minecraft-free decision and geometry logic for the god-boss "siege" behaviour
 * (breaking through blocks to reach the player).
 *
 * <p>Kept free of Minecraft classes so it can be unit-tested in isolation, mirroring the
 * {@code spawning/logic} package. World mutation and the breakable-block predicate live in
 * {@link BossSiegeGoal} / {@link BossSiegeRules}.
 */
public final class BossSiegeLogic {
    private BossSiegeLogic() {
    }

    /** Minimum horizontal delta (blocks) before an axis counts as a movement direction. */
    public static final double DIR_EPSILON = 0.3;
    /** A target is considered "not closing" if squared distance fails to shrink by at least this. */
    public static final double PROGRESS_EPSILON_SQ = 0.25;

    /** What the boss should do to the blocks around it this evaluation. */
    public enum Mode {
        /** Do nothing — clear path, no target, or disabled. */
        NONE,
        /** Break the 1-wide column directly ahead toward the target. */
        BREAK_PATH,
        /** Escalated: smash a box of blocks around the boss until it breaks through. */
        BREAK_AREA
    }

    /**
     * Decide the siege mode for this tick.
     *
     * @param hasTarget            whether the boss currently has a living target
     * @param siegeEnabled         the config master toggle
     * @param obstructed           whether the boss is blocked from reaching the target
     * @param stuckTicks           how long the boss has been continuously obstructed-and-not-closing
     * @param escalationDelayTicks ticks of being stuck before escalating to an area smash
     */
    public static Mode decideMode(boolean hasTarget, boolean siegeEnabled, boolean obstructed,
                                  int stuckTicks, int escalationDelayTicks) {
        if (!hasTarget || !siegeEnabled || !obstructed) {
            return Mode.NONE;
        }
        return stuckTicks >= escalationDelayTicks ? Mode.BREAK_AREA : Mode.BREAK_PATH;
    }

    /**
     * Advance the "stuck" counter. Increments while the boss is obstructed and not meaningfully
     * closing distance on its target; resets to zero the moment it is unobstructed or makes progress.
     *
     * @return the new stuck-tick count
     */
    public static int updateStuckTicks(int prev, boolean obstructed, double prevDistSq, double curDistSq) {
        if (!obstructed) {
            return 0;
        }
        boolean closing = curDistSq < prevDistSq - PROGRESS_EPSILON_SQ;
        return closing ? 0 : prev + 1;
    }

    /**
     * Resolve a horizontal delta to a discrete step direction on each axis
     * ({@code -1}, {@code 0}, or {@code 1}). Axes below {@link #DIR_EPSILON} are treated as zero.
     *
     * @return a two-element array {@code [stepX, stepZ]}
     */
    public static int[] stepDirection(double dx, double dz) {
        int sx = Math.abs(dx) > DIR_EPSILON ? (int) Math.signum(dx) : 0;
        int sz = Math.abs(dz) > DIR_EPSILON ? (int) Math.signum(dz) : 0;
        return new int[]{sx, sz};
    }

    /**
     * Block offsets (relative to the boss's foot position) for tunnelling toward the target:
     * the column(s) immediately ahead, {@code height} blocks tall. When moving diagonally, the two
     * axis-aligned neighbours are included as well so the boss does not snag on corners.
     *
     * @param stepX  horizontal step on X ({@code -1..1})
     * @param stepZ  horizontal step on Z ({@code -1..1})
     * @param height number of vertical blocks to clear (e.g. 3 for a 3-tall boss)
     * @return list of {@code [dx, dy, dz]} offsets; empty if there is no direction
     */
    public static List<int[]> pathBreakOffsets(int stepX, int stepZ, int height) {
        List<int[]> offsets = new ArrayList<>();
        if (stepX == 0 && stepZ == 0) {
            return offsets;
        }
        List<int[]> columns = new ArrayList<>();
        columns.add(new int[]{stepX, stepZ});
        if (stepX != 0 && stepZ != 0) {
            columns.add(new int[]{stepX, 0});
            columns.add(new int[]{0, stepZ});
        }
        for (int[] col : columns) {
            for (int y = 0; y < height; y++) {
                offsets.add(new int[]{col[0], y, col[1]});
            }
        }
        return offsets;
    }

    /**
     * Block offsets for an escalated area smash: a box of horizontal half-width {@code radius}
     * and the given {@code height}, centred on the boss's foot position. The boss's own column is
     * skipped (no point breaking the floor it stands on).
     */
    public static List<int[]> areaBreakOffsets(int radius, int height) {
        List<int[]> offsets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                for (int y = 0; y < height; y++) {
                    offsets.add(new int[]{x, y, z});
                }
            }
        }
        return offsets;
    }
}
