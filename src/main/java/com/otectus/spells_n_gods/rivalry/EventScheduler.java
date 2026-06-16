package com.otectus.spells_n_gods.rivalry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.EventDefinition;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.rivalry.DivineEvent.EventSeverity;
import com.otectus.spells_n_gods.rivalry.DivineEvent.EventType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    // Event-pool ids that referenced a missing definition, so we warn only once per session.
    private static final Set<ResourceLocation> WARNED_MISSING_EVENTS = ConcurrentHashMap.newKeySet();

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
        if (server.overworld().getRandom().nextFloat() > eventChance) {
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

        // Prefer a data-driven event from the source god's pool for this rival; the severity-based enum
        // type remains as the fallback when the pool is empty or all its ids lack loaded definitions.
        RandomSource random = server.overworld().getRandom();
        ResourceLocation chosenDefId = chooseEventDefId(sourceGodId, targetGodId, random);
        EventType type = chooseEventType(severity, random);

        long now = System.currentTimeMillis();
        long delay = 60_000L + random.nextInt(240_000); // 1-5 minutes delay
        long duration = getEventDuration(severity);

        return DivineEvent.builder()
                .sourceGod(sourceGodId)
                .targetGod(targetGodId)
                .eventDef(chosenDefId)
                .type(type)
                .severity(severity)
                .scheduledAt(now + delay)
                .expiresAt(now + delay + duration)
                .affectedPlayers(affectedPlayers)
                .build();
    }

    /**
     * Resolve a random, loaded {@link EventDefinition} id from the source god's {@code event_pool} for the
     * pressured rival, or {@code null} when no usable pool entry exists (caller falls back to the enum).
     */
    private static ResourceLocation chooseEventDefId(ResourceLocation sourceGodId,
                                                     ResourceLocation targetGodId,
                                                     RandomSource random) {
        GodDefinition source = SpellsNGodsDataManager.getGods().get(sourceGodId);
        if (source == null) {
            return null;
        }
        String slot = source.rivalSlot(targetGodId.getPath());
        if (slot.isEmpty()) {
            return null;
        }

        Map<ResourceLocation, EventDefinition> events = SpellsNGodsDataManager.getEvents();
        List<ResourceLocation> resolved = new ArrayList<>();
        for (String rawId : source.eventPool(slot)) {
            ResourceLocation defId = new ResourceLocation(SpellsNGodsMod.MODID, rawId);
            if (events.containsKey(defId)) {
                resolved.add(defId);
            } else if (WARNED_MISSING_EVENTS.add(defId)) {
                SpellsNGodsMod.LOGGER.warn("Rival event pool for {} references missing event '{}'",
                        sourceGodId, rawId);
            }
        }
        if (resolved.isEmpty()) {
            return null;
        }
        return resolved.get(random.nextInt(resolved.size()));
    }

    private static EventType chooseEventType(EventSeverity severity, RandomSource random) {
        return switch (severity) {
            case LEGENDARY -> {
                EventType[] types = {EventType.DIVINE_TRIAL, EventType.RIVAL_CURSE, EventType.DIVINE_MANIFESTATION};
                yield types[random.nextInt(types.length)];
            }
            case MAJOR -> {
                EventType[] types = {EventType.FAITH_TEST, EventType.DIVINE_TRIAL};
                yield types[random.nextInt(types.length)];
            }
            case MODERATE -> {
                EventType[] types = {EventType.FAITH_TEST, EventType.OMEN};
                yield types[random.nextInt(types.length)];
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
