package com.otectus.spells_n_gods.effect;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.BlessingState;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EffectProfileCache {
    private static final Map<UUID, EffectProfile> cache = new ConcurrentHashMap<>();

    public static EffectProfile get(Player player) {
        return cache.get(player.getUUID());
    }

    public static void invalidate(Player player) {
        EffectProfile old = cache.remove(player.getUUID());
        if (old != null && player instanceof ServerPlayer serverPlayer) {
            for (TierEffect effect : old.getActiveEffects()) {
                effect.remove(serverPlayer);
            }
        }
    }

    public static void invalidateAll() {
        cache.clear();
    }

    public static void recompute(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        // Remove old effects first
        EffectProfile old = cache.get(player.getUUID());
        if (old != null) {
            for (TierEffect effect : old.getActiveEffects()) {
                effect.remove(player);
            }
        }

        // Check if player should have effects
        if (data.getBlessingState() != BlessingState.ACTIVE) {
            cache.remove(player.getUUID());
            SpellsNGodsMod.LOGGER.debug("Player {} has no active blessings, removing effects", player.getName().getString());
            return;
        }

        // Get god definition
        if (data.getChosenGodId() == null) {
            cache.remove(player.getUUID());
            return;
        }

        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
        if (god == null) {
            cache.remove(player.getUUID());
            SpellsNGodsMod.LOGGER.warn("Cannot compute effects for player {} - god {} not found",
                    player.getName().getString(), data.getChosenGodId());
            return;
        }

        // Build effects for current tier
        List<TierEffect> effects = buildEffectsForTier(god, data.getCurrentTier());

        // Create new profile
        EffectProfile newProfile = new EffectProfile(effects);
        cache.put(player.getUUID(), newProfile);

        // Apply new effects
        for (TierEffect effect : effects) {
            effect.apply(player);
        }

        SpellsNGodsMod.LOGGER.debug("Recomputed effects for player {}: {} effects active",
                player.getName().getString(), effects.size());
    }

    private static List<TierEffect> buildEffectsForTier(GodDefinition god, DivineTier tier) {
        List<TierEffect> effects = new ArrayList<>();

        // Get blessing effects for current tier and all lower tiers
        for (DivineTier t : DivineTier.values()) {
            if (t.getLevel() > 0 && t.getLevel() <= tier.getLevel()) {
                List<TierEffect> tierEffects = EffectParser.parseEffectsForTier(god, t);
                effects.addAll(tierEffects);
            }
        }

        return effects;
    }

    public static boolean hasProfile(UUID playerId) {
        return cache.containsKey(playerId);
    }

    public static int getCacheSize() {
        return cache.size();
    }
}
