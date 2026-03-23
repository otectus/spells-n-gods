package com.otectus.spells_n_gods.effect;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EffectProfile {
    private final List<TierEffect> activeEffects;
    private final Map<EffectType, Float> multipliers;

    public EffectProfile(List<TierEffect> effects) {
        this.activeEffects = List.copyOf(effects);
        this.multipliers = computeMultipliers(effects);
    }

    private static Map<EffectType, Float> computeMultipliers(List<TierEffect> effects) {
        Map<EffectType, Float> result = new EnumMap<>(EffectType.class);

        for (TierEffect effect : effects) {
            if (effect instanceof MultiplierEffect mult) {
                result.merge(effect.getType(), mult.getMultiplier(), (a, b) -> a * b);
            }
        }

        return result;
    }

    public List<TierEffect> getActiveEffects() {
        return activeEffects;
    }

    public float getMultiplier(EffectType type) {
        return multipliers.getOrDefault(type, 1.0f);
    }

    public boolean hasMultiplier(EffectType type) {
        return multipliers.containsKey(type);
    }

    public boolean isEmpty() {
        return activeEffects.isEmpty();
    }

    public int size() {
        return activeEffects.size();
    }

    public interface MultiplierEffect {
        float getMultiplier();
    }
}
