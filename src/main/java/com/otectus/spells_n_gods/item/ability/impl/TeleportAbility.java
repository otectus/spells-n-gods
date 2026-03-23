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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TeleportAbility implements DivineAbility {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters) {
        double maxDistance = parameters.has("max_distance") ? parameters.get("max_distance").getAsDouble() : 16.0;
        float arrivalDamage = parameters.has("arrival_damage") ? parameters.get("arrival_damage").getAsFloat() : 4.0f;
        double arrivalRadius = parameters.has("arrival_radius") ? parameters.get("arrival_radius").getAsDouble() : 3.0;

        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        // Raycast for destination
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, eyePos.add(look.scale(maxDistance)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 destination;
        if (blockHit.getType() != HitResult.Type.MISS) {
            // Step back slightly from the wall
            destination = blockHit.getLocation().subtract(look.scale(0.5));
        } else {
            destination = eyePos.add(look.scale(maxDistance));
        }
        // Snap to ground level
        destination = new Vec3(destination.x, destination.y - player.getEyeHeight() + 0.1, destination.z);

        // Resolve particle type
        SimpleParticleType particle = ParticleTypes.PORTAL;
        if (parameters.has("particle")) {
            SimpleParticleType resolved = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE.get(
                    ResourceLocation.tryParse(parameters.get("particle").getAsString()));
            if (resolved != null) particle = resolved;
        }

        // Particles at origin
        Vec3 origin = player.position();
        level.sendParticles(particle, origin.x, origin.y + 1.0, origin.z, 20, 0.3, 0.5, 0.3, 0.05);

        // Teleport player
        player.teleportTo(destination.x, destination.y, destination.z);

        // Particles at destination
        level.sendParticles(particle, destination.x, destination.y + 1.0, destination.z, 20, 0.3, 0.5, 0.3, 0.05);

        // AoE damage at arrival point
        if (arrivalDamage > 0) {
            final double arrivalRadiusSqr = arrivalRadius * arrivalRadius;
            final Vec3 dest = destination;
            AABB area = new AABB(destination, destination).inflate(arrivalRadius);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive() && e.distanceToSqr(dest) <= arrivalRadiusSqr);

            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), arrivalDamage);

                // Apply arrival effects
                if (parameters.has("arrival_effects")) {
                    applyEffects(parameters.getAsJsonArray("arrival_effects"), target);
                }
            }
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
        return "teleport";
    }
}
