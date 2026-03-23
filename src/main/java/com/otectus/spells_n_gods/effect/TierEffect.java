package com.otectus.spells_n_gods.effect;

import net.minecraft.server.level.ServerPlayer;

public interface TierEffect {
    EffectType getType();

    void apply(ServerPlayer player);

    void remove(ServerPlayer player);

    default String getEffectId() {
        return getType().name().toLowerCase();
    }
}
