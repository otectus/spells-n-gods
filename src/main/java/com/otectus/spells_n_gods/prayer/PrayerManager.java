package com.otectus.spells_n_gods.prayer;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.CapabilityHandler;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.content.MonumentBlockEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.animation.PlayerAnimationType;
import com.otectus.spells_n_gods.network.DivineVfxPacket;
import com.otectus.spells_n_gods.network.ModNetwork;
import com.otectus.spells_n_gods.network.PlayerAnimationPacket;
import com.otectus.spells_n_gods.network.PrayerProgressPacket;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class PrayerManager {
    private static final Map<UUID, PrayerSession> activeSessions = new ConcurrentHashMap<>();
    private static final double MAX_DISTANCE_SQUARED = 16.0;

    public static boolean startPrayer(ServerPlayer player, MonumentBlockEntity monument) {
        UUID playerId = player.getUUID();

        if (activeSessions.containsKey(playerId)) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.already_praying"));
            return false;
        }

        Optional<PlayerDivinityData> dataOpt = PlayerDivinityCapability.get(player);
        if (dataOpt.isEmpty()) {
            return false;
        }

        PlayerDivinityData data = dataOpt.get();
        if (data.getChosenGodId() == null) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.not_bound"));
            return false;
        }

        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
        if (god == null) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.unknown_god"));
            return false;
        }

        long now = System.currentTimeMillis();
        int cooldownSeconds = god.worship().prayer().cooldownSeconds();
        long cooldownMs = cooldownSeconds * 1000L;

        if (data.getLastPrayerEpochMs() > 0 && (now - data.getLastPrayerEpochMs()) < cooldownMs) {
            long remainingMs = cooldownMs - (now - data.getLastPrayerEpochMs());
            int remainingSeconds = (int) ((remainingMs + 999) / 1000);
            player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.on_cooldown", remainingSeconds));
            return false;
        }

        int minSeconds = god.worship().prayer().minSeconds();
        PrayerSession session = new PrayerSession(playerId, monument.getBlockPos(), now, minSeconds);
        activeSessions.put(playerId, session);

        ModNetwork.sendToPlayer(player, new PrayerProgressPacket(true, 0.0f, minSeconds));

        player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.started", god.displayName()));

        // Play kneeling animation visible to all nearby players
        ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                player.getUUID(), PlayerAnimationType.PRAYER_KNEEL, PlayerAnimationPacket.Action.PLAY));

        SpellsNGodsMod.LOGGER.debug("Player {} started praying to {}", player.getName().getString(), god.displayName());

        return true;
    }

    public static void cancelPrayer(ServerPlayer player) {
        PrayerSession session = activeSessions.remove(player.getUUID());
        if (session != null) {
            ModNetwork.sendToPlayer(player, new PrayerProgressPacket(false, 0.0f, 0));
            ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                    player.getUUID(), PlayerAnimationType.PRAYER_KNEEL, PlayerAnimationPacket.Action.STOP));
            player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.interrupted"));
        }
    }

    public static boolean isPlayerPraying(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public static Optional<PrayerSession> getSession(UUID playerId) {
        return Optional.ofNullable(activeSessions.get(playerId));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Iterator<Map.Entry<UUID, PrayerSession>> it = activeSessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PrayerSession> entry = it.next();
            PrayerSession session = entry.getValue();

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }

            if (session.isInterrupted()) {
                ModNetwork.sendToPlayer(player, new PrayerProgressPacket(false, 0.0f, 0));
                ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                        player.getUUID(), PlayerAnimationType.PRAYER_KNEEL, PlayerAnimationPacket.Action.STOP));
                it.remove();
                continue;
            }

            double distSq = player.blockPosition().distSqr(session.getMonumentPos());
            if (distSq > MAX_DISTANCE_SQUARED) {
                session.setInterrupted(true);
                ModNetwork.sendToPlayer(player, new PrayerProgressPacket(false, 0.0f, 0));
                ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                        player.getUUID(), PlayerAnimationType.PRAYER_KNEEL, PlayerAnimationPacket.Action.STOP));
                player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.too_far"));
                it.remove();
                continue;
            }

            if (session.isComplete()) {
                completePrayer(player, session);
                it.remove();
            } else {
                ModNetwork.sendToPlayer(player, new PrayerProgressPacket(true, session.getProgress(), session.getRemainingSeconds()));

                // Spawn ambient prayer particles every 4 ticks
                if (player.tickCount % 4 == 0) {
                    spawnPrayerAmbientParticles(player);
                }
            }
        }
    }

    private static void completePrayer(ServerPlayer player, PrayerSession session) {
        Optional<PlayerDivinityData> dataOpt = PlayerDivinityCapability.get(player);
        if (dataOpt.isEmpty()) {
            return;
        }

        PlayerDivinityData data = dataOpt.get();
        long now = System.currentTimeMillis();

        data.setLastPrayerEpochMs(now);
        data.addFavor(1.0f);
        data.setLastFavorUpdateMs(now);

        CapabilityHandler.syncToClient(player);

        ModNetwork.sendToPlayer(player, new PrayerProgressPacket(false, 1.0f, 0));

        GodDefinition god = null;
        if (data.getChosenGodId() != null) {
            god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
        }

        String godName = god != null ? god.displayName() : "the divine";
        player.sendSystemMessage(Component.translatable("spells_n_gods.prayer.complete", godName));

        // Stop kneeling animation
        ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                player.getUUID(), PlayerAnimationType.PRAYER_KNEEL, PlayerAnimationPacket.Action.STOP));

        // Send prayer completion VFX
        String school = god != null ? god.magicSchool() : "";
        ModNetwork.sendToPlayer(player, new DivineVfxPacket(
                DivineVfxPacket.VfxType.PRAYER_COMPLETE,
                player.getX(), player.getY(), player.getZ(),
                school, 0));

        SpellsNGodsMod.LOGGER.debug("Player {} completed prayer, favor now {}", player.getName().getString(), data.getFavor());
    }

    private static void spawnPrayerAmbientParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Optional<PlayerDivinityData> dataOpt = PlayerDivinityCapability.get(player);
        if (dataOpt.isEmpty() || dataOpt.get().getChosenGodId() == null) return;

        GodDefinition god = SpellsNGodsDataManager.getGods().get(
                new ResourceLocation(dataOpt.get().getChosenGodId()));
        if (god == null) return;

        String school = god.magicSchool();
        ParticleOptions particle = SchoolColors.getSchoolParticle(school);

        double x = player.getX();
        double y = player.getY() + 0.5;
        double z = player.getZ();

        // School-themed particles rising around the praying player
        for (int i = 0; i < 5; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = 0.6 + player.getRandom().nextDouble() * 0.3;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            serverLevel.sendParticles(particle, px, y + player.getRandom().nextDouble() * 1.5, pz,
                    1, 0, 0.02, 0, 0.0);
        }

        // School-colored dust ring at feet (slow rotating)
        org.joml.Vector3f color = SchoolColors.getSchoolColor(school);
        DustParticleOptions dust = new DustParticleOptions(color, 0.8f);
        double ringAngle = (player.tickCount * 0.05) % (Math.PI * 2);
        for (int i = 0; i < 3; i++) {
            double a = ringAngle + (i * Math.PI * 2 / 3);
            double ringR = 0.8;
            serverLevel.sendParticles(dust,
                    x + Math.cos(a) * ringR, y + 0.1, z + Math.sin(a) * ringR,
                    1, 0, 0.01, 0, 0.0);
        }
    }
}
