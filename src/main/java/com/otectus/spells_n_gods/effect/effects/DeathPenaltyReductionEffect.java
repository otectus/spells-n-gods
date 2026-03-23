package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;

/**
 * MORTYSS - Finality
 * Reduces death penalties such as XP loss and item drops.
 * Higher tiers provide greater protection.
 */
public class DeathPenaltyReductionEffect implements TierEffect {
    private final float xpRetentionPercent;
    private final float itemRetentionChance;
    private final boolean keepHotbar;
    private final boolean keepArmor;
    private final String effectId;

    public DeathPenaltyReductionEffect(float xpRetentionPercent, float itemRetentionChance,
                                        boolean keepHotbar, boolean keepArmor) {
        this.xpRetentionPercent = xpRetentionPercent;
        this.itemRetentionChance = itemRetentionChance;
        this.keepHotbar = keepHotbar;
        this.keepArmor = keepArmor;
        this.effectId = "death_penalty_" + xpRetentionPercent + "_" + itemRetentionChance;
    }

    @Override
    public EffectType getType() {
        return EffectType.DEATH_PENALTY;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Effect is checked during death event, not applied as ongoing effect
    }

    @Override
    public void remove(ServerPlayer player) {
        // Nothing to remove
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    public float getXpRetentionPercent() {
        return xpRetentionPercent;
    }

    public float getItemRetentionChance() {
        return itemRetentionChance;
    }

    public boolean shouldKeepHotbar() {
        return keepHotbar;
    }

    public boolean shouldKeepArmor() {
        return keepArmor;
    }
}
