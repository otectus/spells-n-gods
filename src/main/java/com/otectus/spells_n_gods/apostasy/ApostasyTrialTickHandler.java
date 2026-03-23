package com.otectus.spells_n_gods.apostasy;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles server tick processing for apostasy trials.
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class ApostasyTrialTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Check if player is in a trial
        if (!ApostasyTrialManager.isInTrial(player.getUUID())) {
            return;
        }

        // Verify capability state matches
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        if (!data.isInApostasyTrial()) {
            // Trial state mismatch - clean up
            ApostasyTrialManager.completeTrial(player, false);
            return;
        }

        // Tick the trial
        ApostasyTrialManager.tickAllTrials(player);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Check if player was in a trial
        if (ApostasyTrialManager.isInTrial(player.getUUID())) {
            ApostasyTrialManager.onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Clean up all trials on server shutdown
        ApostasyTrialManager.cleanup();
    }
}
