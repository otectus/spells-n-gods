package com.otectus.spells_n_gods.data.loader;

import com.google.gson.Gson;
import com.otectus.spells_n_gods.data.EventDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.Optional;

public class EventsReloadListener extends SpellsNGodsJsonLoader<EventDefinition> {
    public EventsReloadListener(Gson gson) {
        super(gson, "spells_n_gods/events", "event");
    }

    @Override
    protected Optional<EventDefinition> parse(ResourceLocation location, com.google.gson.JsonObject json) {
        return EventDefinition.fromJson(location, json);
    }

    @Override
    protected void onApply(Map<ResourceLocation, EventDefinition> data) {
        SpellsNGodsDataManager.setEvents(data);
    }
}
