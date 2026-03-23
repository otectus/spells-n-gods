package com.otectus.spells_n_gods.compat;

import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Custom events fired by Spells n' Gods for mod compatibility.
 * Other mods (KubeJS, FTB Quests, etc.) can listen to these events.
 */
public class SpellsNGodsEvents {

    /**
     * Fired when a player binds to a god.
     */
    public static class PlayerBoundEvent extends Event {
        private final ServerPlayer player;
        private final GodDefinition god;

        public PlayerBoundEvent(ServerPlayer player, GodDefinition god) {
            this.player = player;
            this.god = god;
        }

        public ServerPlayer getPlayer() { return player; }
        public GodDefinition getGod() { return god; }
        public String getGodId() { return god.id().toString(); }
    }

    /**
     * Fired when a player's divine tier changes.
     */
    public static class TierChangeEvent extends Event {
        private final ServerPlayer player;
        private final DivineTier oldTier;
        private final DivineTier newTier;
        private final GodDefinition god;

        public TierChangeEvent(ServerPlayer player, DivineTier oldTier, DivineTier newTier, GodDefinition god) {
            this.player = player;
            this.oldTier = oldTier;
            this.newTier = newTier;
            this.god = god;
        }

        public ServerPlayer getPlayer() { return player; }
        public DivineTier getOldTier() { return oldTier; }
        public DivineTier getNewTier() { return newTier; }
        public GodDefinition getGod() { return god; }
        public boolean isPromotion() { return newTier.ordinal() > oldTier.ordinal(); }
        public boolean isDemotion() { return newTier.ordinal() < oldTier.ordinal(); }
    }

    /**
     * Fired when a player completes apostasy.
     */
    public static class ApostasyCompleteEvent extends Event {
        private final ServerPlayer player;
        private final String abandonedGodId;
        private final int scarCount;

        public ApostasyCompleteEvent(ServerPlayer player, String abandonedGodId, int scarCount) {
            this.player = player;
            this.abandonedGodId = abandonedGodId;
            this.scarCount = scarCount;
        }

        public ServerPlayer getPlayer() { return player; }
        public String getAbandonedGodId() { return abandonedGodId; }
        public int getScarCount() { return scarCount; }
    }

    /**
     * Fired when a player fails apostasy trials.
     */
    public static class ApostasyFailedEvent extends Event {
        private final ServerPlayer player;
        private final String godId;
        private final int failedPhase;

        public ApostasyFailedEvent(ServerPlayer player, String godId, int failedPhase) {
            this.player = player;
            this.godId = godId;
            this.failedPhase = failedPhase;
        }

        public ServerPlayer getPlayer() { return player; }
        public String getGodId() { return godId; }
        public int getFailedPhase() { return failedPhase; }
    }

    /**
     * Fired when a player completes a prayer.
     */
    public static class PrayerCompleteEvent extends Event {
        private final ServerPlayer player;
        private final GodDefinition god;
        private final float favorGained;

        public PrayerCompleteEvent(ServerPlayer player, GodDefinition god, float favorGained) {
            this.player = player;
            this.god = god;
            this.favorGained = favorGained;
        }

        public ServerPlayer getPlayer() { return player; }
        public GodDefinition getGod() { return god; }
        public float getFavorGained() { return favorGained; }
    }

    /**
     * Fired when a player makes an offering.
     * Can be cancelled to reject the offering.
     */
    @Cancelable
    public static class OfferingEvent extends Event {
        private final ServerPlayer player;
        private final GodDefinition god;
        private final net.minecraft.world.item.ItemStack offering;
        private float favorValue;

        public OfferingEvent(ServerPlayer player, GodDefinition god, net.minecraft.world.item.ItemStack offering, float favorValue) {
            this.player = player;
            this.god = god;
            this.offering = offering;
            this.favorValue = favorValue;
        }

        public ServerPlayer getPlayer() { return player; }
        public GodDefinition getGod() { return god; }
        public net.minecraft.world.item.ItemStack getOffering() { return offering; }
        public float getFavorValue() { return favorValue; }
        public void setFavorValue(float value) { this.favorValue = value; }
    }

    /**
     * Fired when a latent curse expires.
     */
    public static class CurseExpiredEvent extends Event {
        private final ServerPlayer player;
        private final String sourceGodId;

        public CurseExpiredEvent(ServerPlayer player, String sourceGodId) {
            this.player = player;
            this.sourceGodId = sourceGodId;
        }

        public ServerPlayer getPlayer() { return player; }
        public String getSourceGodId() { return sourceGodId; }
    }

    /**
     * Fired when a god boss is defeated.
     */
    public static class BossDefeatedEvent extends Event {
        private final String godId;
        private final ServerPlayer killer;

        public BossDefeatedEvent(String godId, ServerPlayer killer) {
            this.godId = godId;
            this.killer = killer;
        }

        public String getGodId() { return godId; }
        public ServerPlayer getKiller() { return killer; }
    }

    /**
     * Fired when a god boss spawns (initial or respawn).
     */
    public static class BossSpawnedEvent extends Event {
        private final String godId;
        private final net.minecraft.core.BlockPos location;

        public BossSpawnedEvent(String godId, net.minecraft.core.BlockPos location) {
            this.godId = godId;
            this.location = location;
        }

        public String getGodId() { return godId; }
        public net.minecraft.core.BlockPos getLocation() { return location; }
    }
}
