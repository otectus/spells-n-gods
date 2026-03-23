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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.List;

public class DashAbility implements DivineAbility {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters) {
        double distance = parameters.has("distance") ? parameters.get("distance").getAsDouble() : 8.0;
        float damage = parameters.has("damage") ? parameters.get("damage").getAsFloat() : 5.0f;
        int fireSeconds = parameters.has("fire_seconds") ? parameters.get("fire_seconds").getAsInt() : 0;

        Vec3 start = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(distance));

        // Raycast for wall collision
        BlockHitResult blockHit = level.clip(new ClipContext(
                player.getEyePosition(), player.getEyePosition().add(look.scale(distance)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        double actualDistance = distance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            actualDistance = Math.max(0, blockHit.getLocation().distanceTo(player.getEyePosition()) - 1.0);
        }

        Vec3 destination = start.add(look.scale(actualDistance));

        // Find entities in the dash path
        AABB pathBox = player.getBoundingBox().expandTowards(look.scale(actualDistance)).inflate(1.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pathBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), damage);
            if (fireSeconds > 0) {
                target.setSecondsOnFire(fireSeconds);
            }
        }

        // Spawn trail particles
        SimpleParticleType particle = ParticleTypes.FLAME;
        if (parameters.has("particle")) {
            SimpleParticleType resolved = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE.get(
                    ResourceLocation.tryParse(parameters.get("particle").getAsString()));
            if (resolved != null) particle = resolved;
        }

        int steps = (int) (actualDistance * 4);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            Vec3 pos = start.add(look.scale(actualDistance * t));
            level.sendParticles(particle, pos.x, pos.y + 1.0, pos.z, 2, 0.1, 0.1, 0.1, 0.01);
        }

        // Teleport player
        player.teleportTo(destination.x, destination.y, destination.z);

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
        return "dash";
    }
}
