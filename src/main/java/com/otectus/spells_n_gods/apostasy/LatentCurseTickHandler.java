package com.otectus.spells_n_gods.apostasy;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles server tick processing for latent curses.
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class LatentCurseTickHandler {

    private static final int TICK_INTERVAL = 20; // Check every second

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Only check periodically to reduce overhead
        if (player.tickCount % TICK_INTERVAL != 0) {
            return;
        }

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        if (!data.hasLatentCurse()) {
            return;
        }

        // Check if curse has expired
        long remaining = LatentCurseManager.getRemainingCurseMs(player);
        if (remaining <= 0) {
            LatentCurseManager.onCurseExpired(player);
            return;
        }

        // Tick curse effects
        LatentCurseManager.tickCurseEffects(player);
    }

    /**
     * Cursed apostates take amplified damage. The per-curse multiplier (>= 1.0) comes from the
     * abandoned god's {@code apostasy.latent_curse.damage_multiplier}.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!LatentCurseManager.hasCurse(player)) {
            return;
        }
        float multiplier = LatentCurseManager.getDamageMultiplier(player);
        if (multiplier > 1.0f) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }
}
