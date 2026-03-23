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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class TargetedDebuffAbility implements DivineAbility {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters) {
        double range = parameters.has("range") ? parameters.get("range").getAsDouble() : 12.0;
        float damage = parameters.has("damage") ? parameters.get("damage").getAsFloat() : 4.0f;
        int markDuration = parameters.has("mark_duration_ticks") ? parameters.get("mark_duration_ticks").getAsInt() : 0;

        // Find nearest hostile
        AABB searchArea = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e != player && e.isAlive() && e instanceof Monster && e.distanceTo(player) <= range);

        if (candidates.isEmpty()) {
            return false;
        }

        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceTo(player)))
                .orElse(null);

        if (target == null) {
            return false;
        }

        // Apply damage
        target.hurt(level.damageSources().playerAttack(player), damage);

        // Apply debuffs
        if (parameters.has("effects")) {
            JsonArray effects = parameters.getAsJsonArray("effects");
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

        // Mark target with Glowing
        if (markDuration > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, markDuration, 0, false, false));
        }

        // Spawn particles at target
        SimpleParticleType particle = ParticleTypes.WITCH;
        if (parameters.has("particle")) {
            SimpleParticleType resolved = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE.get(
                    ResourceLocation.tryParse(parameters.get("particle").getAsString()));
            if (resolved != null) particle = resolved;
        }

        level.sendParticles(particle,
                target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                12, 0.3, 0.3, 0.3, 0.02);

        return true;
    }

    @Override
    public String getType() {
        return "targeted_debuff";
    }
}
