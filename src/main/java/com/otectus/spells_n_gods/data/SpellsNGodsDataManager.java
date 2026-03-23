package com.otectus.spells_n_gods.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.data.loader.EffectsReloadListener;
import com.otectus.spells_n_gods.data.loader.EventsReloadListener;
import com.otectus.spells_n_gods.data.loader.GodsReloadListener;
import com.otectus.spells_n_gods.data.loader.MonumentsReloadListener;
import com.otectus.spells_n_gods.data.loader.OfferingValidatorsReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class SpellsNGodsDataManager {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final AtomicReference<Map<ResourceLocation, GodDefinition>> GODS = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<ResourceLocation, EventDefinition>> EVENTS = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<ResourceLocation, OfferingValidatorDefinition>> OFFERING_VALIDATORS = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<ResourceLocation, MonumentDefinition>> MONUMENTS = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<ResourceLocation, EffectDefinition>> EFFECTS = new AtomicReference<>(Map.of());

    private SpellsNGodsDataManager() {
    }

    public static void registerReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new GodsReloadListener(GSON));
        event.addListener(new EventsReloadListener(GSON));
        event.addListener(new OfferingValidatorsReloadListener(GSON));
        event.addListener(new MonumentsReloadListener(GSON));
        event.addListener(new EffectsReloadListener(GSON));
    }

    public static Map<ResourceLocation, GodDefinition> getGods() {
        return GODS.get();
    }

    public static Map<ResourceLocation, EventDefinition> getEvents() {
        return EVENTS.get();
    }

    public static Map<ResourceLocation, OfferingValidatorDefinition> getOfferingValidators() {
        return OFFERING_VALIDATORS.get();
    }

    public static Map<ResourceLocation, MonumentDefinition> getMonuments() {
        return MONUMENTS.get();
    }

    public static Map<ResourceLocation, EffectDefinition> getEffects() {
        return EFFECTS.get();
    }

    public static void setGods(Map<ResourceLocation, GodDefinition> data) {
        GODS.set(Map.copyOf(data));
        SpellsNGodsMod.LOGGER.info("[SpellsNGods] DataManager: stored {} god definitions. Keys: {}",
                data.size(), data.keySet());
    }

    public static void setEvents(Map<ResourceLocation, EventDefinition> data) {
        EVENTS.set(Map.copyOf(data));
    }

    public static void setOfferingValidators(Map<ResourceLocation, OfferingValidatorDefinition> data) {
        OFFERING_VALIDATORS.set(Map.copyOf(data));
    }

    public static void setMonuments(Map<ResourceLocation, MonumentDefinition> data) {
        MONUMENTS.set(Map.copyOf(data));
    }

    public static void setEffects(Map<ResourceLocation, EffectDefinition> data) {
        EFFECTS.set(Map.copyOf(data));
    }
}
