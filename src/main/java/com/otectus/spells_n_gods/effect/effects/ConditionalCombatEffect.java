package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * KHELR - Pressure
 * Combat buffs that scale with danger or the kind of foe being fought. The condition type
 * determines when the buff activates. Player-state conditions evaluate from the player alone;
 * target conditions ({@code *_TARGET}) only resolve when a victim is supplied (offensive bonuses).
 */
public class ConditionalCombatEffect implements TierEffect {
    public enum Condition {
        // --- player-state conditions (work for both bonus and resistance) ---
        LOW_HEALTH,      // Below 50% health
        CRITICAL_HEALTH, // Below 25% health
        OUTNUMBERED,     // Multiple hostile mobs nearby
        BLOODIED,        // Recently took damage
        KILL_STREAK,     // Recently killed enemies
        LAST_STAND,      // Below 20% health and in combat
        SPRINTING,       // Player is sprinting
        ENDERMAN_NEARBY, // An enderman is within 16 blocks
        MELEE_HIT,       // Landing a direct hit on any target
        // --- target conditions (offensive bonus only; need a victim) ---
        ON_FIRE_TARGET,  // The victim is on fire
        ANIMAL_TARGET,   // The victim is a passive animal
        UNDEAD_TARGET,   // The victim is undead
        WITHER_TARGET    // The victim is a wither / wither skeleton
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

    /** Evaluate a player-state condition (no victim available, e.g. the damage-resistance path). */
    public boolean isConditionMet(ServerPlayer player) {
        return isConditionMet(player, null);
    }

    /** Evaluate the condition, using {@code target} for {@code *_TARGET} / {@code MELEE_HIT} cases. */
    public boolean isConditionMet(ServerPlayer player, @Nullable LivingEntity target) {
        return switch (condition) {
            case LOW_HEALTH -> player.getHealth() < player.getMaxHealth() * 0.5f;
            case CRITICAL_HEALTH -> player.getHealth() < player.getMaxHealth() * 0.25f;
            case LAST_STAND -> player.getHealth() < player.getMaxHealth() * 0.2f && isInCombat(player);
            case OUTNUMBERED -> countNearbyHostiles(player) >= 3;
            case BLOODIED -> player.hurtTime > 0;
            case KILL_STREAK -> false; // Would need tracking
            case SPRINTING -> player.isSprinting();
            case ENDERMAN_NEARBY -> hasEndermanNearby(player);
            case MELEE_HIT -> target != null;
            case ON_FIRE_TARGET -> target != null && target.isOnFire();
            case ANIMAL_TARGET -> target instanceof Animal;
            case UNDEAD_TARGET -> target != null && target.getMobType() == MobType.UNDEAD;
            case WITHER_TARGET -> target instanceof WitherBoss || target instanceof WitherSkeleton;
        };
    }

    private boolean hasEndermanNearby(ServerPlayer player) {
        return !player.level().getEntitiesOfClass(
                EnderMan.class, player.getBoundingBox().inflate(16)).isEmpty();
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
