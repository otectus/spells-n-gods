package com.otectus.spells_n_gods.item.ability.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.item.ability.DivineAbility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

public class SummonAbility implements DivineAbility {

    @Override
    public boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters) {
        String entityTypeId = parameters.has("entity_type") ? parameters.get("entity_type").getAsString() : "minecraft:vex";
        int count = parameters.has("count") ? parameters.get("count").getAsInt() : 3;
        int lifetimeTicks = parameters.has("lifetime_ticks") ? parameters.get("lifetime_ticks").getAsInt() : 600;
        double spread = parameters.has("spread") ? parameters.get("spread").getAsDouble() : 3.0;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(entityTypeId));
        if (type == null) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level);
            if (entity == null) continue;

            double offsetX = (level.random.nextDouble() - 0.5) * 2 * spread;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2 * spread;

            entity.moveTo(
                    player.getX() + offsetX,
                    player.getY(),
                    player.getZ() + offsetZ,
                    level.random.nextFloat() * 360.0f,
                    0.0f
            );

            // Set limited lifetime
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
                mob.setNoAi(false);
            }
            entity.getPersistentData().putInt("DivineLifetime", lifetimeTicks);

            level.addFreshEntity(entity);
        }

        // Apply self-effects to the player
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
        return "summon";
    }
}
