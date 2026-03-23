package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * UMBRIEL - Volatility
 * Manipulates luck and random outcomes. High variance, high reward.
 * Effects can proc bonus drops, critical strikes, or other random benefits.
 */
public class LuckManipulationEffect implements TierEffect {
    private final int luckAmplifier;
    private final float criticalProcChance;
    private final float bonusDropChance;
    private final float volatilityFactor;
    private final String effectId;

    public LuckManipulationEffect(int luckAmplifier, float criticalProcChance,
                                   float bonusDropChance, float volatilityFactor) {
        this.luckAmplifier = luckAmplifier;
        this.criticalProcChance = criticalProcChance;
        this.bonusDropChance = bonusDropChance;
        this.volatilityFactor = volatilityFactor;
        this.effectId = "luck_manipulation_" + luckAmplifier;
    }

    @Override
    public EffectType getType() {
        return EffectType.LUCK_MODIFIER;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Apply vanilla luck effect
        if (luckAmplifier >= 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.LUCK,
                    MobEffectInstance.INFINITE_DURATION,
                    luckAmplifier,
                    true,
                    false,
                    true
            ));
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.LUCK);
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    public int getLuckAmplifier() {
        return luckAmplifier;
    }

    public float getCriticalProcChance() {
        return criticalProcChance;
    }

    public float getBonusDropChance() {
        return bonusDropChance;
    }

    public float getVolatilityFactor() {
        return volatilityFactor;
    }
}
