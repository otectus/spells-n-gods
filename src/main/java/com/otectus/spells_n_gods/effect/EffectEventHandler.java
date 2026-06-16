package com.otectus.spells_n_gods.effect;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.effect.effects.*;
import com.otectus.spells_n_gods.favor.FavorCalculator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class EffectEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Update favor and state first
            FavorCalculator.updateOnLogin(player);
            // Then recompute effects
            EffectProfileCache.recompute(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Restore retained items from death
            CompoundTag persistentData = player.getPersistentData();
            if (persistentData.contains("spells_n_gods:retained_items", Tag.TAG_LIST)) {
                ListTag retainedItems = persistentData.getList("spells_n_gods:retained_items", Tag.TAG_COMPOUND);
                for (int i = 0; i < retainedItems.size(); i++) {
                    ItemStack stack = ItemStack.of(retainedItems.getCompound(i));
                    if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                        // Inventory full - drop remaining items at player's feet
                        player.drop(stack, false);
                    }
                }
                persistentData.remove("spells_n_gods:retained_items");
            }

            // Recompute effects after respawn
            EffectProfileCache.recompute(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Clean up cache entry
            EffectProfileCache.invalidate(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Reapply effects after dimension change
            EffectProfileCache.recompute(player);
        }
    }

    public static void onTierChange(ServerPlayer player) {
        EffectProfileCache.recompute(player);
    }

    public static void onBlessingStateChange(ServerPlayer player) {
        EffectProfileCache.recompute(player);
    }

    // ==================== COMBAT EFFECTS (Khelr - Pressure) ====================

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerDamaged(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        EffectProfile profile = EffectProfileCache.get(player);
        if (profile == null) {
            return;
        }

        // Apply conditional combat damage resistance
        for (TierEffect effect : profile.getActiveEffects()) {
            if (effect instanceof ConditionalCombatEffect combatEffect) {
                if (combatEffect.isConditionMet(player)) {
                    float resistance = combatEffect.getDamageResistance();
                    if (resistance > 0) {
                        float newDamage = event.getAmount() * (1f - resistance);
                        event.setAmount(newDamage);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerAttack(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        EffectProfile profile = EffectProfileCache.get(player);
        if (profile == null) {
            return;
        }

        // Apply conditional combat damage bonus
        for (TierEffect effect : profile.getActiveEffects()) {
            if (effect instanceof ConditionalCombatEffect combatEffect) {
                if (combatEffect.isConditionMet(player)) {
                    float bonus = combatEffect.getDamageBonus();
                    if (bonus > 0) {
                        float newDamage = event.getAmount() * (1f + bonus);
                        event.setAmount(newDamage);
                    }
                }
            }
        }
    }

    // ==================== DEATH EFFECTS (Mortyss - Finality) ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        EffectProfile profile = EffectProfileCache.get(player);
        if (profile == null) {
            return;
        }

        // Store death penalty effects for use in drops event
        for (TierEffect effect : profile.getActiveEffects()) {
            if (effect instanceof DeathPenaltyReductionEffect deathEffect) {
                // Store in player's persistent data for respawn handling
                player.getPersistentData().putFloat("spells_n_gods:xp_retention", deathEffect.getXpRetentionPercent());
                player.getPersistentData().putFloat("spells_n_gods:item_retention", deathEffect.getItemRetentionChance());
                player.getPersistentData().putBoolean("spells_n_gods:keep_hotbar", deathEffect.shouldKeepHotbar());
                player.getPersistentData().putBoolean("spells_n_gods:keep_armor", deathEffect.shouldKeepArmor());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float itemRetention = player.getPersistentData().getFloat("spells_n_gods:item_retention");
        boolean keepHotbar = player.getPersistentData().getBoolean("spells_n_gods:keep_hotbar");
        boolean keepArmor = player.getPersistentData().getBoolean("spells_n_gods:keep_armor");

        if (itemRetention > 0 || keepHotbar || keepArmor) {
            ListTag retainedItems = new ListTag();

            event.getDrops().removeIf(itemEntity -> {
                ItemStack stack = itemEntity.getItem();

                if (player.getRandom().nextFloat() < itemRetention) {
                    // Serialize item for restoration on respawn
                    retainedItems.add(stack.save(new CompoundTag()));
                    return true;
                }
                return false;
            });

            if (!retainedItems.isEmpty()) {
                player.getPersistentData().put("spells_n_gods:retained_items", retainedItems);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerXpDrop(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Check if this is XP loss from death (negative change)
        if (event.getAmount() < 0) {
            float xpRetention = serverPlayer.getPersistentData().getFloat("spells_n_gods:xp_retention");
            if (xpRetention > 0) {
                // Reduce XP loss
                int reduced = (int) (event.getAmount() * (1f - xpRetention));
                event.setAmount(reduced);
            }
        }
    }

    // ==================== LOOT EFFECTS (Umbriel - Volatility) ====================

    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        EffectProfile profile = EffectProfileCache.get(player);
        if (profile == null) {
            return;
        }

        for (TierEffect effect : profile.getActiveEffects()) {
            if (effect instanceof LuckManipulationEffect luckEffect) {
                float bonusChance = luckEffect.getBonusDropChance();
                if (bonusChance > 0 && player.getRandom().nextFloat() < bonusChance) {
                    // Duplicate random drops
                    event.getDrops().forEach(drop -> {
                        if (player.getRandom().nextFloat() < 0.5f) {
                            ItemStack bonus = drop.getItem().copy();
                            drop.getItem().grow(bonus.getCount());
                        }
                    });
                }
            }
        }
    }

    // ==================== DURABILITY EFFECTS (Aurex - Permanence) ====================

    // Note: Durability multiplier is applied via item damage event
    // This requires a mixin or custom item handling for full implementation
    // The multiplier can be retrieved from EffectProfileCache.get(player).getMultiplier(EffectType.DURABILITY_MULTIPLIER)
}
