package com.otectus.spells_n_gods.favor;

import com.otectus.spells_n_gods.capability.BlessingState;
import com.otectus.spells_n_gods.capability.CapabilityHandler;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class FavorCalculator {
    private static final long MS_PER_DAY = 86_400_000L;
    private static final long PRAYER_WINDOW_MS = 3_600_000L; // 1 hour

    public static float computeCurrentFavor(PlayerDivinityData data) {
        if (data.getChosenGodId() == null) {
            return 0f;
        }

        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
        if (god == null) {
            return data.getFavor();
        }

        double decayPerDay = god.favor().decayPerDay();
        long now = System.currentTimeMillis();

        long effectiveLastUpdate = data.getLastFavorUpdateMs();
        if (effectiveLastUpdate <= 0) {
            effectiveLastUpdate = now;
        }

        // Decay pauses during prayer window if configured
        if (SpellsNGodsConfig.COMMON.favorDecayPausedWhilePraying.get()) {
            if (data.getLastPrayerEpochMs() > 0 && (now - data.getLastPrayerEpochMs()) < PRAYER_WINDOW_MS) {
                // Recent prayer - reduce effective elapsed time
                effectiveLastUpdate = Math.max(effectiveLastUpdate, data.getLastPrayerEpochMs());
            }
        }

        long elapsedMs = now - effectiveLastUpdate;
        if (elapsedMs <= 0) {
            return data.getFavor();
        }

        double elapsedDays = (double) elapsedMs / MS_PER_DAY;
        double decayed = decayPerDay * elapsedDays;

        return Math.max(0f, data.getFavor() - (float) decayed);
    }

    public static void updateOnLogin(ServerPlayer player) {
        Optional<PlayerDivinityData> dataOpt = PlayerDivinityCapability.get(player);
        if (dataOpt.isEmpty()) {
            return;
        }

        PlayerDivinityData data = dataOpt.get();
        if (data.getBlessingState() == BlessingState.UNBOUND) {
            return;
        }

        // Update favor with decay
        float newFavor = computeCurrentFavor(data);
        data.setFavor(newFavor);
        data.setLastFavorUpdateMs(System.currentTimeMillis());

        // Update tier
        GodDefinition god = getGod(data);
        if (god != null) {
            DivineTier newTier = DivineTier.computeFromFavor(newFavor, god);
            data.setCurrentTier(newTier);
        }

        // Check blessing state (offering lapse)
        updateBlessingState(data);

        // Sync to client
        CapabilityHandler.syncToClient(player);
    }

    public static void updateBlessingState(PlayerDivinityData data) {
        if (data.getBlessingState() == BlessingState.UNBOUND ||
            data.getBlessingState() == BlessingState.UNBOUND_COOLDOWN) {
            return;
        }

        GodDefinition god = getGod(data);
        if (god == null) {
            data.setBlessingState(BlessingState.MISSING_DEITY);
            return;
        }

        long now = System.currentTimeMillis();
        int graceHours = god.worship().offering().graceHours();
        long graceMs = graceHours * 3_600_000L;
        long timeSinceOffering = now - data.getLastOfferingEpochMs();

        if (timeSinceOffering > graceMs) {
            if (data.getBlessingState() == BlessingState.ACTIVE) {
                data.setBlessingState(BlessingState.HALTED);
            }
        }
    }

    public static DivineTier computeTier(PlayerDivinityData data, float favor) {
        GodDefinition god = getGod(data);
        if (god == null) {
            return DivineTier.NONE;
        }
        return DivineTier.computeFromFavor(favor, god);
    }

    public static GodDefinition getGod(PlayerDivinityData data) {
        if (data.getChosenGodId() == null) {
            return null;
        }
        return SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
    }

    public static long getHoursUntilHalted(PlayerDivinityData data) {
        GodDefinition god = getGod(data);
        if (god == null) {
            return 0;
        }

        int graceHours = god.worship().offering().graceHours();
        long graceMs = graceHours * 3_600_000L;
        long timeSinceOffering = System.currentTimeMillis() - data.getLastOfferingEpochMs();
        long remainingMs = graceMs - timeSinceOffering;

        if (remainingMs <= 0) {
            return 0;
        }

        return remainingMs / 3_600_000L;
    }
}
