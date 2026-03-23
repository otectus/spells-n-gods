package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class PotionTierEffect implements TierEffect {
    private final MobEffect effect;
    private final int amplifier;
    private final String effectId;

    public PotionTierEffect(MobEffect effect, int amplifier) {
        this.effect = effect;
        this.amplifier = amplifier;
        this.effectId = "potion_" + effect.getDescriptionId();
    }

    @Override
    public EffectType getType() {
        return EffectType.POTION_EFFECT;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Apply infinite duration effect (hidden, no particles)
        player.addEffect(new MobEffectInstance(
                effect,
                MobEffectInstance.INFINITE_DURATION,
                amplifier,
                true,  // ambient
                false, // visible
                true   // show icon
        ));
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeEffect(effect);
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    public MobEffect getEffect() {
        return effect;
    }

    public int getAmplifier() {
        return amplifier;
    }
}
