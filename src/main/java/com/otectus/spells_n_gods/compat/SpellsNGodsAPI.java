package com.otectus.spells_n_gods.compat;

import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Public API for Spells n' Gods mod compatibility.
 * Use this for KubeJS scripts, addon mods, or other integrations.
 *
 * Example KubeJS usage:
 * <pre>
 * // Listen to binding event
 * ForgeEvents.onEvent('com.otectus.spells_n_gods.compat.SpellsNGodsEvents$PlayerBoundEvent', event => {
 *     let player = event.getPlayer();
 *     let godId = event.getGodId();
 *     console.log(`${player.getName()} bound to ${godId}`);
 * });
 *
 * // Check player's god
 * let godId = SpellsNGodsAPI.getPlayerGodId(player);
 * let tier = SpellsNGodsAPI.getPlayerTier(player);
 * </pre>
 */
public class SpellsNGodsAPI {

    /**
     * Get the god ID a player is bound to.
     * @return The god ID string, or null if unbound
     */
    @Nullable
    public static String getPlayerGodId(ServerPlayer player) {
        return PlayerDivinityCapability.getOrCreate(player).getChosenGodId();
    }

    /**
     * Get a player's current divine tier.
     */
    public static DivineTier getPlayerTier(ServerPlayer player) {
        return PlayerDivinityCapability.getOrCreate(player).getCurrentTier();
    }

    /**
     * Get a player's current favor value.
     */
    public static float getPlayerFavor(ServerPlayer player) {
        return PlayerDivinityCapability.getOrCreate(player).getFavor();
    }

    /**
     * Check if a player is bound to any god.
     */
    public static boolean isPlayerBound(ServerPlayer player) {
        return PlayerDivinityCapability.getOrCreate(player).isBound();
    }

    /**
     * Check if a player is bound to a specific god.
     */
    public static boolean isPlayerBoundTo(ServerPlayer player, String godId) {
        String currentGod = getPlayerGodId(player);
        return currentGod != null && currentGod.equals(godId);
    }

    /**
     * Check if a player has an active curse.
     */
    public static boolean hasActiveCurse(ServerPlayer player) {
        return PlayerDivinityCapability.getOrCreate(player).hasLatentCurse();
    }

    /**
     * Get a player's apostasy scar count.
     */
    public static int getScarCount(ServerPlayer player) {
        return PlayerDivinityCapability.getOrCreate(player).getScarData().getScarCount();
    }

    /**
     * Check if a player has a specific tier or higher.
     */
    public static boolean hasTierOrHigher(ServerPlayer player, String tierName) {
        if (tierName == null) {
            return false;
        }
        DivineTier required;
        try {
            required = DivineTier.valueOf(tierName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Unknown tier name from a script/integration — treat as "requirement not met".
            return false;
        }
        DivineTier current = getPlayerTier(player);
        return current.ordinal() >= required.ordinal();
    }

    /**
     * Get all loaded god IDs.
     */
    public static Collection<ResourceLocation> getAllGodIds() {
        return SpellsNGodsDataManager.getGods().keySet();
    }

    /**
     * Get a god definition by ID.
     */
    public static Optional<GodDefinition> getGod(String godId) {
        ResourceLocation id = ResourceLocation.tryParse(godId);
        if (id == null) return Optional.empty();
        return Optional.ofNullable(SpellsNGodsDataManager.getGods().get(id));
    }

    /**
     * Get the full player divinity data (for advanced usage).
     */
    public static PlayerDivinityData getPlayerData(ServerPlayer player) {
        return PlayerDivinityCapability.getOrCreate(player);
    }

    /**
     * Add favor to a player (with bounds checking).
     * @param amount Amount to add (can be negative)
     */
    public static void addFavor(ServerPlayer player, float amount) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        data.addFavor(amount);
    }

    /**
     * Set a player's favor (clamped to 0-100).
     */
    public static void setFavor(ServerPlayer player, float favor) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        data.setFavor(Math.max(0, Math.min(100, favor)));
    }
}
