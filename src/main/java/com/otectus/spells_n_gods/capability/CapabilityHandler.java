package com.otectus.spells_n_gods.capability;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.network.ModNetwork;
import com.otectus.spells_n_gods.network.SyncDivinityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityHandler {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerDivinityCapability.DIVINITY).isPresent()) {
                event.addCapability(PlayerDivinityCapability.ID, new PlayerDivinityCapability());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(PlayerDivinityCapability.DIVINITY).ifPresent(oldData -> {
                event.getEntity().getCapability(PlayerDivinityCapability.DIVINITY).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                });
            });
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        player.getCapability(PlayerDivinityCapability.DIVINITY).ifPresent(data -> {
            ModNetwork.sendToPlayer(player, new SyncDivinityPacket(
                    data.getChosenGodId(),
                    data.getFavor(),
                    data.getBlessingState(),
                    data.getCurrentTier(),
                    data.getLastOfferingEpochMs(),
                    data.getLastPrayerEpochMs(),
                    data.getScarData().getScarCount(),
                    data.getScarData().getTotalHealthReduction()
            ));
        });
    }

    @Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerDivinityData.class);
        }
    }
}
