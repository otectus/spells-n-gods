package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.EffectProfile;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;

/**
 * CHRONYX - Momentum
 * Reduces cooldowns for various actions (ender pearls, shields, etc.).
 * Also provides speed bonuses that scale with consecutive actions.
 */
public class CooldownReductionEffect implements TierEffect, EffectProfile.MultiplierEffect {
    private final float cooldownMultiplier;
    private final float speedBonus;
    private final boolean stacksOnAction;
    private final int maxStacks;
    private final String effectId;

    public CooldownReductionEffect(float cooldownMultiplier, float speedBonus,
                                    boolean stacksOnAction, int maxStacks) {
        this.cooldownMultiplier = cooldownMultiplier;
        this.speedBonus = speedBonus;
        this.stacksOnAction = stacksOnAction;
        this.maxStacks = maxStacks;
        this.effectId = "cooldown_reduction_" + cooldownMultiplier;
    }

    @Override
    public EffectType getType() {
        return EffectType.COOLDOWN_MODIFIER;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Cooldown reduction is applied via event handlers
    }

    @Override
    public void remove(ServerPlayer player) {
        // Nothing to remove directly
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    @Override
    public float getMultiplier() {
        return cooldownMultiplier;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }

    public boolean doesStackOnAction() {
        return stacksOnAction;
    }

    public int getMaxStacks() {
        return maxStacks;
    }
}
