package com.otectus.spells_n_gods.state;

import com.otectus.spells_n_gods.capability.BlessingState;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;

public class BlessingStateMachine {

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

    public static void onOfferingAccepted(PlayerDivinityData data, float favorGain) {
        long now = System.currentTimeMillis();

        data.setLastOfferingEpochMs(now);
        data.addFavor(favorGain);
        data.setLastFavorUpdateMs(now);

        if (data.getBlessingState() == BlessingState.HALTED) {
            data.setBlessingState(BlessingState.ACTIVE);
        }

        GodDefinition god = getGod(data);
        if (god != null) {
            DivineTier newTier = DivineTier.computeFromFavor(data.getFavor(), god);
            data.setCurrentTier(newTier);
        }
    }

    public static void onPrayerCompleted(PlayerDivinityData data, float favorGain) {
        long now = System.currentTimeMillis();

        data.setLastPrayerEpochMs(now);
        data.addFavor(favorGain);
        data.setLastFavorUpdateMs(now);

        GodDefinition god = getGod(data);
        if (god != null) {
            DivineTier newTier = DivineTier.computeFromFavor(data.getFavor(), god);
            data.setCurrentTier(newTier);
        }
    }

    public static StateTransitionResult tryTransition(PlayerDivinityData data, BlessingState target) {
        BlessingState current = data.getBlessingState();

        // Define valid transitions
        return switch (current) {
            case UNBOUND -> {
                if (target == BlessingState.ACTIVE) {
                    data.setBlessingState(target);
                    yield StateTransitionResult.ok();
                }
                yield StateTransitionResult.error("Cannot transition from UNBOUND to " + target);
            }
            case ACTIVE -> {
                if (target == BlessingState.HALTED || target == BlessingState.UNBOUND_COOLDOWN) {
                    data.setBlessingState(target);
                    yield StateTransitionResult.ok();
                }
                yield StateTransitionResult.error("Cannot transition from ACTIVE to " + target);
            }
            case HALTED -> {
                if (target == BlessingState.ACTIVE || target == BlessingState.UNBOUND_COOLDOWN) {
                    data.setBlessingState(target);
                    yield StateTransitionResult.ok();
                }
                yield StateTransitionResult.error("Cannot transition from HALTED to " + target);
            }
            case UNBOUND_COOLDOWN -> {
                if (target == BlessingState.UNBOUND) {
                    long now = System.currentTimeMillis();
                    if (now >= data.getApostasyCooldownEndMs()) {
                        data.setBlessingState(target);
                        yield StateTransitionResult.ok();
                    }
                    yield StateTransitionResult.error("Apostasy cooldown not yet expired");
                }
                yield StateTransitionResult.error("Cannot transition from UNBOUND_COOLDOWN to " + target);
            }
            case MISSING_DEITY -> {
                if (target == BlessingState.ACTIVE || target == BlessingState.HALTED) {
                    GodDefinition god = getGod(data);
                    if (god != null) {
                        data.setBlessingState(target);
                        yield StateTransitionResult.ok();
                    }
                    yield StateTransitionResult.error("God definition still missing");
                }
                yield StateTransitionResult.error("Cannot transition from MISSING_DEITY to " + target);
            }
        };
    }

    public static boolean isEffectsActive(PlayerDivinityData data) {
        return data.getBlessingState() == BlessingState.ACTIVE;
    }

    public static boolean canReceiveBlessings(PlayerDivinityData data) {
        return data.getBlessingState() == BlessingState.ACTIVE;
    }

    public static boolean isSubjectToRivalPressure(PlayerDivinityData data) {
        return data.getBlessingState() == BlessingState.ACTIVE ||
               data.getBlessingState() == BlessingState.HALTED;
    }

    private static GodDefinition getGod(PlayerDivinityData data) {
        if (data.getChosenGodId() == null) {
            return null;
        }
        return SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
    }

    public record StateTransitionResult(boolean successful, String message) {
        public static StateTransitionResult ok() {
            return new StateTransitionResult(true, "");
        }

        public static StateTransitionResult error(String message) {
            return new StateTransitionResult(false, message);
        }
    }
}
