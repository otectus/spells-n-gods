package com.otectus.spells_n_gods.spawning.logic;

/**
 * Pure, immutable cooldown + spawn-budget state for a single structure instance. This is the
 * testable core of the persisted {@code EncounterRecord}; it knows nothing about NBT or
 * Minecraft and is driven entirely by game-tick arithmetic.
 *
 * @param lastSpawnGameTime    tick of the most recent spawn, or {@code -1} if never
 * @param cooldownEndsGameTime tick at which the structure may roll again;
 *                             {@link #PERMANENT} means permanently exhausted
 * @param spawnedCount         total deities ever spawned here (monotonic)
 * @param activeDeityCount     deities currently alive/active here (the spawn budget consumer)
 */
public record CooldownState(
        long lastSpawnGameTime,
        long cooldownEndsGameTime,
        int spawnedCount,
        int activeDeityCount
) {
    /** Default Minecraft day length in ticks. */
    public static final long TICKS_PER_DAY = 24000L;
    /** Sentinel for permanent exhaustion. */
    public static final long PERMANENT = Long.MAX_VALUE;

    public CooldownState {
        if (activeDeityCount < 0) activeDeityCount = 0;
        if (spawnedCount < 0) spawnedCount = 0;
    }

    public static CooldownState fresh() {
        return new CooldownState(-1L, 0L, 0, 0);
    }

    public boolean isPermanentlyExhausted() {
        return cooldownEndsGameTime == PERMANENT;
    }

    public boolean isOnCooldown(long now) {
        return isPermanentlyExhausted() || now < cooldownEndsGameTime;
    }

    /**
     * @return {@code true} if a new deity may spawn here right now, considering both the
     *         per-structure budget and the cooldown/exhaustion window.
     */
    public boolean canSpawn(long now, int maxActiveDeities) {
        if (activeDeityCount >= Math.max(1, maxActiveDeities)) {
            return false;
        }
        return !isOnCooldown(now);
    }

    /** Record a spawn, consuming one budget slot and (re)arming the cooldown. */
    public CooldownState withSpawn(long now, int cooldownDays) {
        return withSpawn(now, cooldownDays, TICKS_PER_DAY);
    }

    public CooldownState withSpawn(long now, int cooldownDays, long ticksPerDay) {
        long ends = cooldownDays < 0 ? PERMANENT : now + (long) cooldownDays * ticksPerDay;
        return new CooldownState(now, ends, spawnedCount + 1, activeDeityCount + 1);
    }

    /** Free a budget slot when an active deity dies or despawns. */
    public CooldownState withDeityRemoved() {
        return new CooldownState(lastSpawnGameTime, cooldownEndsGameTime,
                spawnedCount, Math.max(0, activeDeityCount - 1));
    }

    /**
     * Whether this record carries no useful information and can be safely pruned from saved
     * data: no active deities, and either never spawned or its (non-permanent) cooldown has
     * fully elapsed. Permanently-exhausted records are kept so the structure never re-rolls.
     */
    public boolean isPrunable(long now) {
        if (activeDeityCount > 0 || isPermanentlyExhausted()) {
            return false;
        }
        return now >= cooldownEndsGameTime;
    }
}
