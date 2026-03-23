package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * NYXARA - Transgression
 * Rule-bending effects with side effects. Power at a cost.
 * Can include night vision with light weakness, invisibility with noise, etc.
 */
public class TransgressionEffect implements TierEffect {

    public enum TransgressionType {
        SHADOW_SIGHT,      // Night vision but weakness in bright light
        PHASE_WALK,        // No fall damage but hunger cost
        SOUL_LEECH,        // Lifesteal but visible health decay particles
        VOID_TOUCH,        // Extra damage but self-damage risk
        ETHEREAL_FORM      // Invisibility with drawbacks
    }

    private final TransgressionType type;
    private final int amplifier;
    private final float sideEffectIntensity;
    private final String effectId;

    public TransgressionEffect(TransgressionType type, int amplifier, float sideEffectIntensity) {
        this.type = type;
        this.amplifier = amplifier;
        this.sideEffectIntensity = sideEffectIntensity;
        this.effectId = "transgression_" + type.name().toLowerCase() + "_" + amplifier;
    }

    @Override
    public EffectType getType() {
        return EffectType.TRANSGRESSION;
    }

    @Override
    public void apply(ServerPlayer player) {
        switch (type) {
            case SHADOW_SIGHT -> {
                player.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        MobEffectInstance.INFINITE_DURATION,
                        amplifier,
                        true,
                        false,
                        true
                ));
            }
            case PHASE_WALK -> {
                player.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING,
                        MobEffectInstance.INFINITE_DURATION,
                        amplifier,
                        true,
                        false,
                        true
                ));
            }
            case ETHEREAL_FORM -> {
                player.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY,
                        MobEffectInstance.INFINITE_DURATION,
                        amplifier,
                        true,
                        false,
                        true
                ));
            }
            case SOUL_LEECH, VOID_TOUCH -> {
                // These are applied via damage event handlers
            }
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        switch (type) {
            case SHADOW_SIGHT -> player.removeEffect(MobEffects.NIGHT_VISION);
            case PHASE_WALK -> player.removeEffect(MobEffects.SLOW_FALLING);
            case ETHEREAL_FORM -> player.removeEffect(MobEffects.INVISIBILITY);
            case SOUL_LEECH, VOID_TOUCH -> {
                // Nothing to remove
            }
        }
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    public TransgressionType getTransgressionType() {
        return type;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public float getSideEffectIntensity() {
        return sideEffectIntensity;
    }
}
