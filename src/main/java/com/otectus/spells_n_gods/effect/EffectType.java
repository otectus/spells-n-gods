package com.otectus.spells_n_gods.effect;

public enum EffectType {
    // Core effect types
    POTION_EFFECT,
    ATTRIBUTE_MODIFIER,

    // Aurex - Permanence
    DURABILITY_MULTIPLIER,
    CRAFT_QUALITY_ROLL,

    // Nyxara - Transgression
    TRANSGRESSION,

    // Khelr - Pressure
    CONDITIONAL_COMBAT,

    // Viren - Sustainability (uses POTION_EFFECT)

    // Mortyss - Finality
    DEATH_PENALTY,

    // Chronyx - Momentum
    COOLDOWN_MODIFIER,

    // Meridian - Stability
    TRADE_MODIFIER,

    // Umbriel - Volatility
    LUCK_MODIFIER,
    DROP_BONUS,
    PROC_RULE,

    // Misc
    COOLDOWN_MULTIPLIER,
    MANA_COST_MULTIPLIER,
    ZONE_AURA,
    EVENT_TRIGGER
}
