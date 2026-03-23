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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AoeBurstAbility implements DivineAbility {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters) {
        double radius = parameters.has("radius") ? parameters.get("radius").getAsDouble() : 5.0;
        float damage = parameters.has("damage") ? parameters.get("damage").getAsFloat() : 6.0f;
        double knockback = parameters.has("knockback") ? parameters.get("knockback").getAsDouble() : 1.5;

        // Find all living entities in radius
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && e.distanceTo(player) <= radius);

        // Damage and knockback
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), damage);

            Vec3 direction = target.position().subtract(player.position()).normalize().scale(knockback);
            target.setDeltaMovement(target.getDeltaMovement().add(direction.x, 0.3, direction.z));
            target.hurtMarked = true;

            // Apply effects to targets
            if (parameters.has("effects")) {
                applyEffects(parameters.getAsJsonArray("effects"), target);
            }
        }

        // Spawn particles in ring pattern
        spawnRingParticles(level, player.position(), radius, parameters);

        // Play sound
        if (parameters.has("sound")) {
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(parameters.get("sound").getAsString()));
            if (sound != null) {
                level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        // Apply self-effects
        if (parameters.has("self_effects")) {
            applyEffects(parameters.getAsJsonArray("self_effects"), player);
        }

        return true;
    }

    private void spawnRingParticles(ServerLevel level, Vec3 center, double radius, JsonObject parameters) {
        SimpleParticleType particle = ParticleTypes.EXPLOSION;
        if (parameters.has("particle")) {
            SimpleParticleType resolved = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE.get(
                    ResourceLocation.tryParse(parameters.get("particle").getAsString()));
            if (resolved != null) particle = resolved;
        }

        int points = (int) (radius * 8);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y + 0.5, z, 1, 0, 0, 0, 0);
        }
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
        return "aoe_burst";
    }
}
