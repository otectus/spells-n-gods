package com.otectus.spells_n_gods.rivalry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure (Minecraft-free) guards over the data-driven rival-event layer:
 * <ol>
 *   <li>every god {@code rival_pressure.*.event_pool} id resolves to an {@code events/<id>.json}, and</li>
 *   <li>every event {@code effects[].type} is handled by {@code EventExecutor}'s dispatcher.</li>
 * </ol>
 * These lock the magnus/velox missing-event bug fixed in this phase and catch future pool/effect typos.
 */
class EventPoolReconciliationTest {

    private static final Path DATA = Path.of("src/main/resources/data/spells_n_gods/spells_n_gods");
    private static final Path EVENTS = DATA.resolve("events");
    private static final Path GODS = DATA.resolve("gods");

    /** Effect type strings handled by {@code EventExecutor.applyEffect}. Keep in sync with that switch. */
    private static final Set<String> KNOWN_EFFECTS = Set.of(
            "blindness", "weakness", "slowness", "reduced_movement", "movement_slow", "hunger_drain",
            "mining_fatigue", "nausea", "darkness", "reduced_visibility", "ignite", "fire_tick",
            "teleport_random", "unstable_teleport_chance", "knockback_burst", "thorns_damage",
            "poison_chance", "increase_cooldown_multiplier", "ritual_failure_chance",
            "fire_damage_vulnerability", "reduced_lifesteal", "reduced_healing", "reduced_attack_speed",
            "crop_wither", "structure_instability", "trade_disable");

    @Test
    void everyEventPoolIdResolvesToAnEventFile() throws IOException {
        try (Stream<Path> gods = Files.list(GODS)) {
            for (Path godFile : (Iterable<Path>) gods.filter(EventPoolReconciliationTest::isJson)::iterator) {
                JsonObject god = parse(godFile);
                if (!god.has("rival_pressure")) {
                    continue;
                }
                JsonObject rivalPressure = god.getAsJsonObject("rival_pressure");
                for (String slot : List.of("primary", "secondary")) {
                    if (!rivalPressure.has(slot)) {
                        continue;
                    }
                    JsonObject slotObj = rivalPressure.getAsJsonObject(slot);
                    if (!slotObj.has("event_pool")) {
                        continue;
                    }
                    for (JsonElement entry : slotObj.getAsJsonArray("event_pool")) {
                        String id = entry.getAsString();
                        assertTrue(Files.exists(EVENTS.resolve(id + ".json")),
                                godFile.getFileName() + " event_pool references missing event '" + id + "'");
                    }
                }
            }
        }
    }

    @Test
    void everyEventEffectTypeIsHandled() throws IOException {
        try (Stream<Path> events = Files.list(EVENTS)) {
            for (Path eventFile : (Iterable<Path>) events.filter(EventPoolReconciliationTest::isJson)::iterator) {
                JsonObject event = parse(eventFile);
                if (!event.has("effects")) {
                    continue;
                }
                JsonArray effects = event.getAsJsonArray("effects");
                for (JsonElement entry : effects) {
                    String type = entry.getAsJsonObject().get("type").getAsString();
                    assertTrue(KNOWN_EFFECTS.contains(type),
                            eventFile.getFileName() + " has unhandled effect type '" + type + "'");
                }
            }
        }
    }

    private static boolean isJson(Path path) {
        return path.toString().endsWith(".json");
    }

    private static JsonObject parse(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
