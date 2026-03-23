package com.otectus.spells_n_gods.item.ability.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.item.ability.DivineAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class ProjectileBarrageAbility implements DivineAbility {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters) {
        double targetRange = parameters.has("target_range") ? parameters.get("target_range").getAsDouble() : 16.0;
        int hitCount = parameters.has("hit_count") ? parameters.get("hit_count").getAsInt() : 5;
        float damagePerHit = parameters.has("damage_per_hit") ? parameters.get("damage_per_hit").getAsFloat() : 3.0f;
        boolean requiresHostile = !parameters.has("requires_hostile") || parameters.get("requires_hostile").getAsBoolean();

        // Find closest valid target
        AABB searchArea = player.getBoundingBox().inflate(targetRange);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e != player && e.isAlive() && e.distanceTo(player) <= targetRange
                        && (!requiresHostile || e instanceof Monster));

        if (candidates.isEmpty()) {
            return false;
        }

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceTo(player)))
                .orElse(null);

        if (target == null) {
            return false;
        }

        // Resolve particle type
        SimpleParticleType particle = ParticleTypes.ENCHANTED_HIT;
        if (parameters.has("particle")) {
            SimpleParticleType resolved = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE.get(
                    ResourceLocation.tryParse(parameters.get("particle").getAsString()));
            if (resolved != null) particle = resolved;
        }

        // Deal damage in multiple hits with particle trails
        Vec3 playerPos = player.getEyePosition();
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);

        for (int i = 0; i < hitCount; i++) {
            target.hurt(level.damageSources().playerAttack(player), damagePerHit);

            // Spawn particle trail from player to target
            int trailSteps = 8;
            for (int s = 0; s < trailSteps; s++) {
                double t = (double) s / trailSteps;
                Vec3 pos = playerPos.lerp(targetPos, t);
                double spread = 0.15;
                level.sendParticles(particle,
                        pos.x + (level.random.nextDouble() - 0.5) * spread,
                        pos.y + (level.random.nextDouble() - 0.5) * spread,
                        pos.z + (level.random.nextDouble() - 0.5) * spread,
                        1, 0, 0, 0, 0);
            }
        }

        // Apply self-effects
        if (parameters.has("self_effects")) {
            applyEffects(parameters.getAsJsonArray("self_effects"), player);
        }

        return true;
    }

    private void applyEffects(JsonArray effects, LivingEntity target) {
        for (JsonElement element : effects) {
            JsonObject effectObj = element.getAsJsonObject();
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(
                    ResourceLocation.tryParse(effectObj.get("id").getAsString()));
            if (effect != null) {
                int duration = effectObj.has("duration") ? effectObj.get("duration").getAsInt() : 100;
                int amplifier = effectObj.has("amplifier") ? effectObj.get("amplifier").getAsInt() : 0;
                target.addEffect(new MobEffectInstance(effect, duration, amplifier));
            }
        }
    }

    @Override
    public String getType() {
        return "projectile_barrage";
    }
}
