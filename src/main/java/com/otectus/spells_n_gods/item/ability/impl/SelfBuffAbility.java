package com.otectus.spells_n_gods.item.ability.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.item.ability.DivineAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

public class SelfBuffAbility implements DivineAbility {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters) {
        float sacrificeHealth = parameters.has("sacrifice_health") ? parameters.get("sacrifice_health").getAsFloat() : 0.0f;

        // Sacrifice health if configured
        if (sacrificeHealth > 0) {
            if (player.getHealth() <= sacrificeHealth) {
                // Don't kill the player; require at least 1 HP remaining
                return false;
            }
            player.setHealth(player.getHealth() - sacrificeHealth);
        }

        // Apply potion effects
        if (parameters.has("effects")) {
            JsonArray effects = parameters.getAsJsonArray("effects");
            for (JsonElement element : effects) {
                JsonObject effectObj = element.getAsJsonObject();
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(
                        ResourceLocation.tryParse(effectObj.get("id").getAsString()));
                if (effect != null) {
                    int duration = effectObj.has("duration") ? effectObj.get("duration").getAsInt() : 200;
                    int amplifier = effectObj.has("amplifier") ? effectObj.get("amplifier").getAsInt() : 0;
                    player.addEffect(new MobEffectInstance(effect, duration, amplifier));
                }
            }
        }

        // Set NBT flags on weapon for conditional on-hit passives
        if (parameters.has("nbt_flags")) {
            CompoundTag tag = weapon.getOrCreateTag();
            CompoundTag divineFlags = tag.contains("DivineFlags") ? tag.getCompound("DivineFlags") : new CompoundTag();

            JsonArray flags = parameters.getAsJsonArray("nbt_flags");
            for (JsonElement element : flags) {
                JsonObject flagObj = element.getAsJsonObject();
                String key = flagObj.get("key").getAsString();
                String value = flagObj.has("value") ? flagObj.get("value").getAsString() : "true";
                int durationTicks = flagObj.has("duration_ticks") ? flagObj.get("duration_ticks").getAsInt() : 200;

                divineFlags.putString(key, value);
                divineFlags.putLong(key + "_expiry", level.getGameTime() + durationTicks);
            }

            tag.put("DivineFlags", divineFlags);
            weapon.setTag(tag);
        }

        // Spawn particles around player
        SimpleParticleType particle = ParticleTypes.HEART;
        if (parameters.has("particle")) {
            SimpleParticleType resolved = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE.get(
                    ResourceLocation.tryParse(parameters.get("particle").getAsString()));
            if (resolved != null) particle = resolved;
        }

        level.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(),
                15, 0.5, 0.5, 0.5, 0.02);

        return true;
    }

    @Override
    public String getType() {
        return "self_buff";
    }
}
