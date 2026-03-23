package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;

/**
 * KHELR - Pressure
 * Combat buffs that scale with danger. Stronger when low health, outnumbered, etc.
 * The condition type determines when the buff activates.
 */
public class ConditionalCombatEffect implements TierEffect {
    public enum Condition {
        LOW_HEALTH,      // Below 50% health
        CRITICAL_HEALTH, // Below 25% health
        OUTNUMBERED,     // Multiple hostile mobs nearby
        BLOODIED,        // Recently took damage
        KILL_STREAK,     // Recently killed enemies
        LAST_STAND       // Below 20% health and in combat
    }

    private final Condition condition;
    private final float damageBonus;
    private final float damageResistance;
    private final float speedBonus;
    private final String effectId;

    public ConditionalCombatEffect(Condition condition, float damageBonus, float damageResistance, float speedBonus) {
        this.condition = condition;
        this.damageBonus = damageBonus;
        this.damageResistance = damageResistance;
        this.speedBonus = speedBonus;
        this.effectId = "conditional_combat_" + condition.name().toLowerCase();
    }

    @Override
    public EffectType getType() {
        return EffectType.CONDITIONAL_COMBAT;
    }

    @Override
    public void apply(ServerPlayer player) {
        // Applied via combat event hooks
    }

    @Override
    public void remove(ServerPlayer player) {
        // Nothing to remove - event-based
    }

    @Override
    public String getEffectId() {
        return effectId;
    }

    public Condition getCondition() {
        return condition;
    }

    public float getDamageBonus() {
        return damageBonus;
    }

    public float getDamageResistance() {
        return damageResistance;
    }

    public float getSpeedBonus() {
        return speedBonus;
    }

    public boolean isConditionMet(ServerPlayer player) {
        return switch (condition) {
            case LOW_HEALTH -> player.getHealth() < player.getMaxHealth() * 0.5f;
            case CRITICAL_HEALTH -> player.getHealth() < player.getMaxHealth() * 0.25f;
            case LAST_STAND -> player.getHealth() < player.getMaxHealth() * 0.2f && isInCombat(player);
            case OUTNUMBERED -> countNearbyHostiles(player) >= 3;
            case BLOODIED -> player.hurtTime > 0;
            case KILL_STREAK -> false; // Would need tracking
        };
    }

    private boolean isInCombat(ServerPlayer player) {
        return player.getLastHurtByMob() != null &&
               player.tickCount - player.getLastHurtByMobTimestamp() < 200; // 10 seconds
    }

    private int countNearbyHostiles(ServerPlayer player) {
        return (int) player.level().getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class,
                player.getBoundingBox().inflate(16),
                mob -> mob.getTarget() == player
        ).size();
    }
}
