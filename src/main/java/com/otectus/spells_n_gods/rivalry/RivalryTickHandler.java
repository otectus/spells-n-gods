package com.otectus.spells_n_gods.rivalry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles server tick events for the rivalry system.
 * Runs pressure calculations and event scheduling periodically.
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class RivalryTickHandler {

    // Check every 5 minutes (6000 ticks at 20 TPS)
    private static final int CHECK_INTERVAL_TICKS = 6000;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        if (server == null || server.getPlayerList().getPlayerCount() == 0) {
            return;
        }

        // Run the event scheduler
        try {
            EventScheduler.tick(server);
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.error("Error in rivalry tick handler", e);
        }
    }

    /**
     * Force an immediate pressure check (for debug commands).
     */
    public static void forceCheck(MinecraftServer server) {
        EventScheduler.tick(server);
    }

    /**
     * Reset the tick counter (e.g., after server restart).
     */
    public static void reset() {
        tickCounter = 0;
        EventScheduler.clearAllEvents();
    }
}
