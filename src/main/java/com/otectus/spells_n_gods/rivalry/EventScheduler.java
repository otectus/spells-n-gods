package com.otectus.spells_n_gods.rivalry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.rivalry.DivineEvent.EventSeverity;
import com.otectus.spells_n_gods.rivalry.DivineEvent.EventType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Schedules divine events based on rival pressure calculations.
 * Runs periodically to check pressure levels and queue appropriate events.
 */
public class EventScheduler {

    private static final Queue<DivineEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private static final Queue<DivineEvent> activeEvents = new ConcurrentLinkedQueue<>();

    // Pressure thresholds for triggering events
    private static final float MINOR_PRESSURE_THRESHOLD = 0.1f;
    private static final float MODERATE_PRESSURE_THRESHOLD = 0.25f;
    private static final float MAJOR_PRESSURE_THRESHOLD = 0.4f;
    private static final float LEGENDARY_PRESSURE_THRESHOLD = 0.6f;

    // Minimum time between events (in milliseconds)
    private static final long MIN_EVENT_INTERVAL_MS = 30 * 60 * 1000; // 30 minutes
    private static long lastEventTimeMs = 0;

    private static final Random RANDOM = new Random();

    /**
     * Called periodically (e.g., every 5 minutes) to check pressure and schedule events.
     */
    public static void tick(MinecraftServer server) {
        // Clean up expired events
        cleanupExpiredEvents();

        // Process ready events
        processReadyEvents(server);

        // Check if we should schedule new events
        long now = System.currentTimeMillis();
        if (now - lastEventTimeMs < MIN_EVENT_INTERVAL_MS) {
            return;
        }

        // Calculate pressure and potentially schedule events
        Map<ResourceLocation, RivalPressureCalculator.PressureResult> pressure =
                RivalPressureCalculator.calculateAllPressure(server);

        for (RivalPressureCalculator.PressureResult result : pressure.values()) {
            if (result.pressureLevel() >= MINOR_PRESSURE_THRESHOLD) {
                scheduleEventsForPressure(server, result);
            }
        }
    }

    private static void scheduleEventsForPressure(MinecraftServer server,
                                                   RivalPressureCalculator.PressureResult result) {
        float pressure = result.pressureLevel();
        EventSeverity severity;
        float eventChance;

        if (pressure >= LEGENDARY_PRESSURE_THRESHOLD) {
            severity = EventSeverity.LEGENDARY;
            eventChance = 0.3f;
        } else if (pressure >= MAJOR_PRESSURE_THRESHOLD) {
            severity = EventSeverity.MAJOR;
            eventChance = 0.4f;
        } else if (pressure >= MODERATE_PRESSURE_THRESHOLD) {
            severity = EventSeverity.MODERATE;
            eventChance = 0.5f;
        } else {
            severity = EventSeverity.MINOR;
            eventChance = 0.6f;
        }

        // Random chance to actually trigger
        if (RANDOM.nextFloat() > eventChance) {
            return;
        }

        // Schedule event for each pressured rival
        for (ResourceLocation rivalGodId : result.pressuredRivals()) {
            DivineEvent event = createEvent(server, result.godId(), rivalGodId, severity);
            if (event != null) {
                pendingEvents.add(event);
                lastEventTimeMs = System.currentTimeMillis();
                SpellsNGodsMod.LOGGER.info("Scheduled divine event: {} -> {}, severity: {}",
                        result.godId(), rivalGodId, severity);
            }
        }
    }

    private static DivineEvent createEvent(MinecraftServer server,
                                            ResourceLocation sourceGodId,
                                            ResourceLocation targetGodId,
                                            EventSeverity severity) {
        // Collect affected players (followers of target god)
        List<UUID> affectedPlayers = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
            String godId = data.getChosenGodId();
            if (godId != null && targetGodId.toString().equals(godId)) {
                affectedPlayers.add(player.getUUID());
            }
        }

        if (affectedPlayers.isEmpty()) {
            return null;
        }

        // Choose event type based on severity
        EventType type = chooseEventType(severity);

        long now = System.currentTimeMillis();
        long delay = RANDOM.nextLong(60_000, 300_000); // 1-5 minutes delay
        long duration = getEventDuration(severity);

        return DivineEvent.builder()
                .sourceGod(sourceGodId)
                .targetGod(targetGodId)
                .type(type)
                .severity(severity)
                .scheduledAt(now + delay)
                .expiresAt(now + delay + duration)
                .affectedPlayers(affectedPlayers)
                .build();
    }

    private static EventType chooseEventType(EventSeverity severity) {
        return switch (severity) {
            case LEGENDARY -> {
                EventType[] types = {EventType.DIVINE_TRIAL, EventType.RIVAL_CURSE, EventType.DIVINE_MANIFESTATION};
                yield types[RANDOM.nextInt(types.length)];
            }
            case MAJOR -> {
                EventType[] types = {EventType.FAITH_TEST, EventType.DIVINE_TRIAL};
                yield types[RANDOM.nextInt(types.length)];
            }
            case MODERATE -> {
                EventType[] types = {EventType.FAITH_TEST, EventType.OMEN};
                yield types[RANDOM.nextInt(types.length)];
            }
            case MINOR -> EventType.OMEN;
        };
    }

    private static long getEventDuration(EventSeverity severity) {
        return switch (severity) {
            case LEGENDARY -> 30 * 60 * 1000; // 30 minutes
            case MAJOR -> 15 * 60 * 1000;     // 15 minutes
            case MODERATE -> 10 * 60 * 1000;  // 10 minutes
            case MINOR -> 5 * 60 * 1000;      // 5 minutes
        };
    }

    private static void processReadyEvents(MinecraftServer server) {
        Iterator<DivineEvent> iterator = pendingEvents.iterator();
        while (iterator.hasNext()) {
            DivineEvent event = iterator.next();
            if (event.isReady()) {
                iterator.remove();
                activeEvents.add(event);
                EventExecutor.execute(server, event);
            }
        }
    }

    private static void cleanupExpiredEvents() {
        pendingEvents.removeIf(DivineEvent::isExpired);
        activeEvents.removeIf(event -> {
            if (event.isExpired()) {
                EventExecutor.cleanup(event);
                return true;
            }
            return false;
        });
    }

    public static Queue<DivineEvent> getPendingEvents() {
        return new ConcurrentLinkedQueue<>(pendingEvents);
    }

    public static Queue<DivineEvent> getActiveEvents() {
        return new ConcurrentLinkedQueue<>(activeEvents);
    }

    public static void clearAllEvents() {
        pendingEvents.clear();
        activeEvents.clear();
    }

    public static int getPendingCount() {
        return pendingEvents.size();
    }

    public static int getActiveCount() {
        return activeEvents.size();
    }
}
