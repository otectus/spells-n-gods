package com.otectus.spells_n_gods.item.ability;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for data-driven divine weapon abilities.
 * Implementations are registered via {@link DivineAbilityRegistry} and
 * referenced by type name in god JSON definitions.
 */
public interface DivineAbility {

    /**
     * Execute the ability.
     * @return true if the ability was successfully used (triggers cooldown and durability loss)
     */
    boolean execute(ServerLevel level, ServerPlayer player, ItemStack weapon, JsonObject parameters);

    /**
     * @return the type identifier for this ability (e.g., "aoe_burst", "dash", "teleport")
     */
    String getType();
}
