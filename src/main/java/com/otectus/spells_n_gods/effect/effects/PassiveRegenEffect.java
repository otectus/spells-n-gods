package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * VIREN - Sustainability
 * Ambient regeneration that scales with environmental harmony.
 * Stronger when in nature, weaker when in artificial structures.
 */
public class PassiveRegenEffect implements TierEffect {
    private final float baseRegenRate;
    private final boolean scalesWithEnvironment;
    private final boolean requiresSatiation;
    private final String effectId;

    public PassiveRegenEffect(float baseRegenRate, boolean scalesWithEnvironment, boolean requiresSatiation) {
        this.baseRegenRate = baseRegenRate;
        this.scalesWithEnvironment = scalesWithEnvironment;
        this.requiresSatiation = requiresSatiation;
        this.effectId = "passive_regen_" + baseRegenRate;
    }

    @Override
    public EffectType getType() {
        return EffectType.POTION_EFFECT;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Apply regeneration effect
        int amplifier = calculateAmplifier(player);
        if (amplifier >= 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    MobEffectInstance.INFINITE_DURATION,
                    amplifier,
                    true,
                    false,
                    true
            ));
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.REGENERATION);
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    private int calculateAmplifier(ServerPlayer player) {
        if (requiresSatiation && player.getFoodData().getFoodLevel() < 18) {
            return -1; // No regen if not well-fed
        }

        int baseAmplifier = (int) (baseRegenRate - 1);

        if (scalesWithEnvironment) {
            // Check if player is in natural environment
            if (isInNature(player)) {
                baseAmplifier += 1;
            }
        }

        return Math.max(0, baseAmplifier);
    }

    private boolean isInNature(ServerPlayer player) {
        // Check if player can see sky and is on natural blocks
        return player.level().canSeeSky(player.blockPosition());
    }

    public float getBaseRegenRate() {
        return baseRegenRate;
    }
}
