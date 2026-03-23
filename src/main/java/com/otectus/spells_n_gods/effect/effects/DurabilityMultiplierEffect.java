package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectProfile;
import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;

/**
 * AUREX - Permanence
 * Reduces durability loss on items. The multiplier is applied to damage taken by items.
 * A multiplier of 0.5 means items take half damage.
 */
public class DurabilityMultiplierEffect implements TierEffect, EffectProfile.MultiplierEffect {
    private final float multiplier;
    private final String effectId;

    public DurabilityMultiplierEffect(float multiplier) {
        this.multiplier = multiplier;
        this.effectId = "durability_mult_" + multiplier;
    }

    @Override
    public EffectType getType() {
        return EffectType.DURABILITY_MULTIPLIER;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Effect is applied via event hooks, not directly
    }

    @Override
    public void remove(ServerPlayer player) {
        // Nothing to remove - event-based
    }

    @Override
    public float getMultiplier() {
        return multiplier;
    }

    @Override
    public String getEffectId() {
        return effectId;
    }
}
