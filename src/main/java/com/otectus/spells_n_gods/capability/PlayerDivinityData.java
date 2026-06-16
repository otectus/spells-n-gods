package com.otectus.spells_n_gods.capability;

import com.otectus.spells_n_gods.offering.OfferingHistory;
import net.minecraft.nbt.CompoundTag;

public class PlayerDivinityData {
    private String chosenGodId;
    private float favor;
    private long lastPrayerEpochMs;
    private long lastOfferingEpochMs;
    private long lastFavorUpdateMs;
    private BlessingState blessingState;
    private DivineTier currentTier;
    private long apostasyCooldownEndMs;
    private long latentCurseEndMs;
    private OfferingHistory offeringHistory;

    // Action-tracking timestamps (epoch ms) consumed by action_rule offering validators
    private long lastManaSpendMs;
    private long lastReplantMs;
    private long lastTradeCycleMs;
    private long lastRiskMs;
    private long lastKillCreditMs;

    // Transient rival-event prayer-cooldown modifier (multiplier active until eventCooldownEndMs)
    private float eventCooldownMultiplier;
    private long eventCooldownEndMs;

    // Apostasy system fields
    private ScarData scarData;
    private String lastAbandonedGodId;
    private boolean inApostasyTrial;
    private int currentTrialPhase;
    private long trialPhaseStartMs;
    private String apostasyMonumentDimension;
    private int apostasyMonumentX;
    private int apostasyMonumentY;
    private int apostasyMonumentZ;

    public PlayerDivinityData() {
        this.chosenGodId = null;
        this.favor = 0.0f;
        this.lastPrayerEpochMs = 0L;
        this.lastOfferingEpochMs = 0L;
        this.lastFavorUpdateMs = System.currentTimeMillis();
        this.blessingState = BlessingState.UNBOUND;
        this.currentTier = DivineTier.NONE;
        this.apostasyCooldownEndMs = 0L;
        this.latentCurseEndMs = 0L;
        this.offeringHistory = new OfferingHistory();
        this.lastManaSpendMs = 0L;
        this.lastReplantMs = 0L;
        this.lastTradeCycleMs = 0L;
        this.lastRiskMs = 0L;
        this.lastKillCreditMs = 0L;
        this.eventCooldownMultiplier = 1.0f;
        this.eventCooldownEndMs = 0L;
        this.scarData = new ScarData();
        this.lastAbandonedGodId = null;
        this.inApostasyTrial = false;
        this.currentTrialPhase = 0;
        this.trialPhaseStartMs = 0L;
    }

    // Getters and setters
    public String getChosenGodId() {
        return chosenGodId;
    }

    public void setChosenGodId(String chosenGodId) {
        this.chosenGodId = chosenGodId;
    }

    public float getFavor() {
        return favor;
    }

    public void setFavor(float favor) {
        this.favor = Math.max(0.0f, favor);
    }

    public void addFavor(float amount) {
        this.favor = Math.max(0.0f, this.favor + amount);
    }

    public long getLastPrayerEpochMs() {
        return lastPrayerEpochMs;
    }

    public void setLastPrayerEpochMs(long lastPrayerEpochMs) {
        this.lastPrayerEpochMs = lastPrayerEpochMs;
    }

    public long getLastOfferingEpochMs() {
        return lastOfferingEpochMs;
    }

    public void setLastOfferingEpochMs(long lastOfferingEpochMs) {
        this.lastOfferingEpochMs = lastOfferingEpochMs;
    }

    public long getLastFavorUpdateMs() {
        return lastFavorUpdateMs;
    }

    public void setLastFavorUpdateMs(long lastFavorUpdateMs) {
        this.lastFavorUpdateMs = lastFavorUpdateMs;
    }

    public BlessingState getBlessingState() {
        return blessingState;
    }

    public void setBlessingState(BlessingState blessingState) {
        this.blessingState = blessingState;
    }

    public DivineTier getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(DivineTier currentTier) {
        this.currentTier = currentTier;
    }

    public long getApostasyCooldownEndMs() {
        return apostasyCooldownEndMs;
    }

    public void setApostasyCooldownEndMs(long apostasyCooldownEndMs) {
        this.apostasyCooldownEndMs = apostasyCooldownEndMs;
    }

    public long getLatentCurseEndMs() {
        return latentCurseEndMs;
    }

    public void setLatentCurseEndMs(long latentCurseEndMs) {
        this.latentCurseEndMs = latentCurseEndMs;
    }

    public boolean isBound() {
        return chosenGodId != null && blessingState != BlessingState.UNBOUND;
    }

    public boolean canBind() {
        if (blessingState == BlessingState.UNBOUND) {
            return true;
        }
        if (blessingState == BlessingState.UNBOUND_COOLDOWN) {
            return System.currentTimeMillis() > apostasyCooldownEndMs;
        }
        return false;
    }

    public boolean hasActiveBlessings() {
        return blessingState == BlessingState.ACTIVE;
    }

    public boolean hasLatentCurse() {
        return latentCurseEndMs > System.currentTimeMillis();
    }

    public OfferingHistory getOfferingHistory() {
        return offeringHistory;
    }

    // Action-tracking getters/setters (timestamps used by action_rule offering validators)
    public long getLastManaSpendMs() { return lastManaSpendMs; }
    public void setLastManaSpendMs(long ms) { this.lastManaSpendMs = ms; }
    public long getLastReplantMs() { return lastReplantMs; }
    public void setLastReplantMs(long ms) { this.lastReplantMs = ms; }
    public long getLastTradeCycleMs() { return lastTradeCycleMs; }
    public void setLastTradeCycleMs(long ms) { this.lastTradeCycleMs = ms; }
    public long getLastRiskMs() { return lastRiskMs; }
    public void setLastRiskMs(long ms) { this.lastRiskMs = ms; }
    public long getLastKillCreditMs() { return lastKillCreditMs; }
    public void setLastKillCreditMs(long ms) { this.lastKillCreditMs = ms; }

    /** Effective prayer-cooldown multiplier from an active rival event (1.0 when none/expired). */
    public float getEventCooldownMultiplier() {
        return System.currentTimeMillis() < eventCooldownEndMs ? eventCooldownMultiplier : 1.0f;
    }

    public void setEventCooldownModifier(float multiplier, long endMs) {
        this.eventCooldownMultiplier = multiplier;
        this.eventCooldownEndMs = endMs;
    }

    // Apostasy system getters/setters
    public ScarData getScarData() {
        return scarData;
    }

    public String getLastAbandonedGodId() {
        return lastAbandonedGodId;
    }

    public void setLastAbandonedGodId(String lastAbandonedGodId) {
        this.lastAbandonedGodId = lastAbandonedGodId;
    }

    public boolean isInApostasyTrial() {
        return inApostasyTrial;
    }

    public void setInApostasyTrial(boolean inApostasyTrial) {
        this.inApostasyTrial = inApostasyTrial;
    }

    public int getCurrentTrialPhase() {
        return currentTrialPhase;
    }

    public void setCurrentTrialPhase(int currentTrialPhase) {
        this.currentTrialPhase = currentTrialPhase;
    }

    public long getTrialPhaseStartMs() {
        return trialPhaseStartMs;
    }

    public void setTrialPhaseStartMs(long trialPhaseStartMs) {
        this.trialPhaseStartMs = trialPhaseStartMs;
    }

    public boolean hasScars() {
        return scarData.getScarCount() > 0;
    }

    // Apostasy monument position
    public void setApostasyMonumentPos(String dimension, int x, int y, int z) {
        this.apostasyMonumentDimension = dimension;
        this.apostasyMonumentX = x;
        this.apostasyMonumentY = y;
        this.apostasyMonumentZ = z;
    }

    public String getApostasyMonumentDimension() {
        return apostasyMonumentDimension;
    }

    public int getApostasyMonumentX() {
        return apostasyMonumentX;
    }

    public int getApostasyMonumentY() {
        return apostasyMonumentY;
    }

    public int getApostasyMonumentZ() {
        return apostasyMonumentZ;
    }

    public boolean hasApostasyMonumentPos() {
        return apostasyMonumentDimension != null;
    }

    public void clearApostasyMonumentPos() {
        this.apostasyMonumentDimension = null;
        this.apostasyMonumentX = 0;
        this.apostasyMonumentY = 0;
        this.apostasyMonumentZ = 0;
    }

    // NBT serialization
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (chosenGodId != null) {
            tag.putString("ChosenGodId", chosenGodId);
        }
        tag.putFloat("Favor", favor);
        tag.putLong("LastPrayerEpochMs", lastPrayerEpochMs);
        tag.putLong("LastOfferingEpochMs", lastOfferingEpochMs);
        tag.putLong("LastFavorUpdateMs", lastFavorUpdateMs);
        tag.putString("BlessingState", blessingState.name());
        tag.putString("CurrentTier", currentTier.name());
        tag.putLong("ApostasyCooldownEndMs", apostasyCooldownEndMs);
        tag.putLong("LatentCurseEndMs", latentCurseEndMs);
        tag.putLong("LastManaSpendMs", lastManaSpendMs);
        tag.putLong("LastReplantMs", lastReplantMs);
        tag.putLong("LastTradeCycleMs", lastTradeCycleMs);
        tag.putLong("LastRiskMs", lastRiskMs);
        tag.putLong("LastKillCreditMs", lastKillCreditMs);
        tag.putFloat("EventCooldownMult", eventCooldownMultiplier);
        tag.putLong("EventCooldownEndMs", eventCooldownEndMs);
        tag.put("OfferingHistory", offeringHistory.save());
        tag.put("ScarData", scarData.save());
        if (lastAbandonedGodId != null) {
            tag.putString("LastAbandonedGodId", lastAbandonedGodId);
        }
        tag.putBoolean("InApostasyTrial", inApostasyTrial);
        tag.putInt("CurrentTrialPhase", currentTrialPhase);
        tag.putLong("TrialPhaseStartMs", trialPhaseStartMs);
        if (apostasyMonumentDimension != null) {
            tag.putString("ApostasyMonumentDim", apostasyMonumentDimension);
            tag.putInt("ApostasyMonumentX", apostasyMonumentX);
            tag.putInt("ApostasyMonumentY", apostasyMonumentY);
            tag.putInt("ApostasyMonumentZ", apostasyMonumentZ);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("ChosenGodId")) {
            chosenGodId = tag.getString("ChosenGodId");
        } else {
            chosenGodId = null;
        }
        setFavor(tag.getFloat("Favor"));
        lastPrayerEpochMs = tag.getLong("LastPrayerEpochMs");
        lastOfferingEpochMs = tag.getLong("LastOfferingEpochMs");
        lastFavorUpdateMs = tag.getLong("LastFavorUpdateMs");
        if (lastFavorUpdateMs == 0L) {
            lastFavorUpdateMs = System.currentTimeMillis();
        }
        try {
            blessingState = BlessingState.valueOf(tag.getString("BlessingState"));
        } catch (IllegalArgumentException e) {
            blessingState = BlessingState.UNBOUND;
        }
        try {
            currentTier = DivineTier.valueOf(tag.getString("CurrentTier"));
        } catch (IllegalArgumentException e) {
            currentTier = DivineTier.NONE;
        }
        apostasyCooldownEndMs = tag.getLong("ApostasyCooldownEndMs");
        latentCurseEndMs = tag.getLong("LatentCurseEndMs");
        lastManaSpendMs = tag.getLong("LastManaSpendMs");
        lastReplantMs = tag.getLong("LastReplantMs");
        lastTradeCycleMs = tag.getLong("LastTradeCycleMs");
        lastRiskMs = tag.getLong("LastRiskMs");
        lastKillCreditMs = tag.getLong("LastKillCreditMs");
        eventCooldownMultiplier = tag.contains("EventCooldownMult") ? tag.getFloat("EventCooldownMult") : 1.0f;
        eventCooldownEndMs = tag.getLong("EventCooldownEndMs");
        if (tag.contains("OfferingHistory")) {
            offeringHistory.load(tag.getCompound("OfferingHistory"));
        }
        if (tag.contains("ScarData")) {
            scarData.load(tag.getCompound("ScarData"));
        }
        if (tag.contains("LastAbandonedGodId")) {
            lastAbandonedGodId = tag.getString("LastAbandonedGodId");
        } else {
            lastAbandonedGodId = null;
        }
        inApostasyTrial = tag.getBoolean("InApostasyTrial");
        currentTrialPhase = tag.getInt("CurrentTrialPhase");
        trialPhaseStartMs = tag.getLong("TrialPhaseStartMs");
        if (tag.contains("ApostasyMonumentDim")) {
            apostasyMonumentDimension = tag.getString("ApostasyMonumentDim");
            apostasyMonumentX = tag.getInt("ApostasyMonumentX");
            apostasyMonumentY = tag.getInt("ApostasyMonumentY");
            apostasyMonumentZ = tag.getInt("ApostasyMonumentZ");
        } else {
            apostasyMonumentDimension = null;
        }
    }

    public void copyFrom(PlayerDivinityData other) {
        this.chosenGodId = other.chosenGodId;
        this.favor = other.favor;
        this.lastPrayerEpochMs = other.lastPrayerEpochMs;
        this.lastOfferingEpochMs = other.lastOfferingEpochMs;
        this.lastFavorUpdateMs = other.lastFavorUpdateMs;
        this.blessingState = other.blessingState;
        this.currentTier = other.currentTier;
        this.apostasyCooldownEndMs = other.apostasyCooldownEndMs;
        this.latentCurseEndMs = other.latentCurseEndMs;
        this.lastManaSpendMs = other.lastManaSpendMs;
        this.lastReplantMs = other.lastReplantMs;
        this.lastTradeCycleMs = other.lastTradeCycleMs;
        this.lastRiskMs = other.lastRiskMs;
        this.lastKillCreditMs = other.lastKillCreditMs;
        this.eventCooldownMultiplier = other.eventCooldownMultiplier;
        this.eventCooldownEndMs = other.eventCooldownEndMs;
        this.offeringHistory.load(other.offeringHistory.save());
        this.scarData.copyFrom(other.scarData);
        this.lastAbandonedGodId = other.lastAbandonedGodId;
        this.inApostasyTrial = other.inApostasyTrial;
        this.currentTrialPhase = other.currentTrialPhase;
        this.trialPhaseStartMs = other.trialPhaseStartMs;
        this.apostasyMonumentDimension = other.apostasyMonumentDimension;
        this.apostasyMonumentX = other.apostasyMonumentX;
        this.apostasyMonumentY = other.apostasyMonumentY;
        this.apostasyMonumentZ = other.apostasyMonumentZ;
    }

    public void reset() {
        this.chosenGodId = null;
        this.favor = 0.0f;
        this.lastPrayerEpochMs = 0L;
        this.lastOfferingEpochMs = 0L;
        this.lastFavorUpdateMs = System.currentTimeMillis();
        this.blessingState = BlessingState.UNBOUND;
        this.currentTier = DivineTier.NONE;
        this.apostasyCooldownEndMs = 0L;
        this.latentCurseEndMs = 0L;
        this.eventCooldownMultiplier = 1.0f;
        this.eventCooldownEndMs = 0L;
        this.offeringHistory.clear();
        // Note: scarData is NOT reset - scars are permanent
        this.lastAbandonedGodId = null;
        this.inApostasyTrial = false;
        this.currentTrialPhase = 0;
        this.trialPhaseStartMs = 0L;
        this.apostasyMonumentDimension = null;
        this.apostasyMonumentX = 0;
        this.apostasyMonumentY = 0;
        this.apostasyMonumentZ = 0;
    }

    /**
     * Full reset including scars (admin/debug only).
     */
    public void fullReset() {
        reset();
        scarData.clear();
    }
}
