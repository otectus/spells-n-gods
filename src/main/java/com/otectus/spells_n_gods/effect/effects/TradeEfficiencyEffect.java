package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;

/**
 * MERIDIAN - Stability
 * Improves trade efficiency and reduces random negative events.
 * Provides consistent, reliable bonuses without variance.
 */
public class TradeEfficiencyEffect implements TierEffect {
    private final float tradeDiscount;
    private final float experienceBonus;
    private final boolean suppressNegativeEvents;
    private final float efficiencyMultiplier;
    private final String effectId;

    public TradeEfficiencyEffect(float tradeDiscount, float experienceBonus,
                                  boolean suppressNegativeEvents, float efficiencyMultiplier) {
        this.tradeDiscount = tradeDiscount;
        this.experienceBonus = experienceBonus;
        this.suppressNegativeEvents = suppressNegativeEvents;
        this.efficiencyMultiplier = efficiencyMultiplier;
        this.effectId = "trade_efficiency_" + tradeDiscount;
    }

    @Override
    public EffectType getType() {
        return EffectType.TRADE_MODIFIER;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Applied via villager trade event handlers
    }

    @Override
    public void remove(ServerPlayer player) {
        // Nothing to remove directly
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    public float getTradeDiscount() {
        return tradeDiscount;
    }

    public float getExperienceBonus() {
        return experienceBonus;
    }

    public boolean shouldSuppressNegativeEvents() {
        return suppressNegativeEvents;
    }

    public float getEfficiencyMultiplier() {
        return efficiencyMultiplier;
    }
}
