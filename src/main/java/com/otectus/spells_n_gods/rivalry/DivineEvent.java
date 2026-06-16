package com.otectus.spells_n_gods.rivalry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Represents a scheduled divine event triggered by rival pressure.
 */
public record DivineEvent(
        UUID eventId,
        ResourceLocation sourceGodId,
        ResourceLocation targetGodId,
        ResourceLocation eventDefId,
        EventType type,
        long scheduledTimeMs,
        long expiresTimeMs,
        EventSeverity severity,
        List<UUID> affectedPlayers
) {
    /** True when this event is driven by a loaded {@code EventDefinition} rather than the legacy enum. */
    public boolean isDataDriven() {
        return eventDefId != null;
    }

    public enum EventType {
        // Positive events (for the source god's followers)
        DIVINE_BLESSING,        // Temporary buff to source god followers
        INSPIRATION,            // Bonus favor gain for source followers

        // Negative events (for the target god's followers)
        DIVINE_TRIAL,           // Debuff or challenge for target followers
        FAITH_TEST,             // Temporary reduction in blessing effects
        RIVAL_CURSE,            // Minor curse effect

        // Neutral/World events
        OMEN,                   // Warning message, no mechanical effect
        DIVINE_MANIFESTATION,   // Visual/audio effect in the world
        SACRED_SITE_SPAWN       // Spawn a temporary structure
    }

    public enum EventSeverity {
        MINOR(0.1f),      // Small effect, common
        MODERATE(0.3f),   // Noticeable effect
        MAJOR(0.5f),      // Significant effect, rare
        LEGENDARY(1.0f);  // Dramatic effect, very rare

        private final float intensity;

        EventSeverity(float intensity) {
            this.intensity = intensity;
        }

        public float getIntensity() {
            return intensity;
        }
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresTimeMs;
    }

    public boolean isReady() {
        return System.currentTimeMillis() >= scheduledTimeMs && !isExpired();
    }

    public boolean affectsPlayer(ServerPlayer player) {
        return affectedPlayers.contains(player.getUUID());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID eventId = UUID.randomUUID();
        private ResourceLocation sourceGodId;
        private ResourceLocation targetGodId;
        private ResourceLocation eventDefId = null;
        private EventType type;
        private long scheduledTimeMs;
        private long expiresTimeMs;
        private EventSeverity severity = EventSeverity.MINOR;
        private List<UUID> affectedPlayers = List.of();

        public Builder eventId(UUID id) {
            this.eventId = id;
            return this;
        }

        public Builder sourceGod(ResourceLocation id) {
            this.sourceGodId = id;
            return this;
        }

        public Builder targetGod(ResourceLocation id) {
            this.targetGodId = id;
            return this;
        }

        public Builder eventDef(ResourceLocation id) {
            this.eventDefId = id;
            return this;
        }

        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        public Builder scheduledAt(long timeMs) {
            this.scheduledTimeMs = timeMs;
            return this;
        }

        public Builder expiresAt(long timeMs) {
            this.expiresTimeMs = timeMs;
            return this;
        }

        public Builder severity(EventSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder affectedPlayers(List<UUID> players) {
            this.affectedPlayers = List.copyOf(players);
            return this;
        }

        public DivineEvent build() {
            return new DivineEvent(
                    eventId,
                    sourceGodId,
                    targetGodId,
                    eventDefId,
                    type,
                    scheduledTimeMs,
                    expiresTimeMs,
                    severity,
                    affectedPlayers
            );
        }
    }
}
