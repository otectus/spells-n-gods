package com.otectus.spells_n_gods.prayer;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public class PrayerSession {
    private final UUID playerId;
    private final BlockPos monumentPos;
    private final long startTimeMs;
    private final int requiredSeconds;
    private boolean interrupted;

    public PrayerSession(UUID playerId, BlockPos monumentPos, long startTimeMs, int requiredSeconds) {
        this.playerId = playerId;
        this.monumentPos = monumentPos;
        this.startTimeMs = startTimeMs;
        this.requiredSeconds = requiredSeconds;
        this.interrupted = false;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public BlockPos getMonumentPos() {
        return monumentPos;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public int getRequiredSeconds() {
        return requiredSeconds;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    public float getProgress() {
        long elapsedMs = getElapsedMs();
        long requiredMs = requiredSeconds * 1000L;
        return Math.min(1.0f, (float) elapsedMs / requiredMs);
    }

    public boolean isComplete() {
        if (interrupted) {
            return false;
        }
        return getElapsedMs() >= requiredSeconds * 1000L;
    }

    public int getRemainingSeconds() {
        long remainingMs = (requiredSeconds * 1000L) - getElapsedMs();
        return (int) Math.max(0, (remainingMs + 999) / 1000);
    }
}
