package com.otectus.spells_n_gods.rivalry;

import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.*;

/**
 * Calculates rivalry pressure between gods based on player populations.
 * Used to trigger divine events when one god becomes too dominant.
 */
public class RivalPressureCalculator {

    /**
     * Result of pressure calculation for a god.
     */
    public record PressureResult(
            ResourceLocation godId,
            int followerCount,
            float dominanceRatio,
            float pressureLevel,
            List<ResourceLocation> pressuredRivals
    ) {}

    /**
     * Calculate pressure levels for all gods based on current player population.
     */
    public static Map<ResourceLocation, PressureResult> calculateAllPressure(MinecraftServer server) {
        Map<ResourceLocation, PressureResult> results = new HashMap<>();
        Map<ResourceLocation, Integer> followerCounts = countFollowers(server);
        int totalFollowers = followerCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (totalFollowers == 0) {
            return results;
        }

        Map<ResourceLocation, GodDefinition> gods = SpellsNGodsDataManager.getGods();

        for (Map.Entry<ResourceLocation, Integer> entry : followerCounts.entrySet()) {
            ResourceLocation godId = entry.getKey();
            int count = entry.getValue();
            float dominance = (float) count / totalFollowers;

            GodDefinition god = gods.get(godId);
            if (god == null) continue;

            // Calculate pressure on rivals
            List<ResourceLocation> pressuredRivals = new ArrayList<>();
            float pressureLevel = 0f;

            GodDefinition.RivalDefinition rivals = god.rivals();
            if (rivals != null) {
                // Check primary rival (full pressure weight)
                if (!rivals.primary().isEmpty()) {
                    ResourceLocation rivalId = ResourceLocation.tryParse(rivals.primary());
                    if (rivalId != null) {
                        pressureLevel += calculateRivalPressure(
                                rivalId, dominance, followerCounts, totalFollowers, pressuredRivals, 1.0f);
                    }
                }
                // Check secondary rival (half pressure weight)
                if (!rivals.secondary().isEmpty()) {
                    ResourceLocation rivalId = ResourceLocation.tryParse(rivals.secondary());
                    if (rivalId != null) {
                        pressureLevel += calculateRivalPressure(
                                rivalId, dominance, followerCounts, totalFollowers, pressuredRivals, 0.5f);
                    }
                }
            }

            results.put(godId, new PressureResult(godId, count, dominance, pressureLevel, pressuredRivals));
        }

        return results;
    }

    private static float calculateRivalPressure(
            ResourceLocation rivalId,
            float dominance,
            Map<ResourceLocation, Integer> followerCounts,
            int totalFollowers,
            List<ResourceLocation> pressuredRivals,
            float weight) {
        int rivalCount = followerCounts.getOrDefault(rivalId, 0);
        float rivalDominance = totalFollowers > 0 ? (float) rivalCount / totalFollowers : 0f;

        // Pressure increases when this god has more followers than rival
        if (dominance > rivalDominance) {
            pressuredRivals.add(rivalId);
            return (dominance - rivalDominance) * weight;
        }
        return 0f;
    }

    /**
     * Count followers of each god on the server.
     */
    public static Map<ResourceLocation, Integer> countFollowers(MinecraftServer server) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
            String godIdStr = data.getChosenGodId();
            if (godIdStr != null && !godIdStr.isEmpty()) {
                ResourceLocation godId = ResourceLocation.tryParse(godIdStr);
                if (godId != null) {
                    counts.merge(godId, 1, Integer::sum);
                }
            }
        }

        return counts;
    }

    /**
     * Count followers of each god at each tier.
     */
    public static Map<ResourceLocation, Map<DivineTier, Integer>> countFollowersByTier(MinecraftServer server) {
        Map<ResourceLocation, Map<DivineTier, Integer>> counts = new HashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
            String godIdStr = data.getChosenGodId();
            if (godIdStr != null && !godIdStr.isEmpty()) {
                ResourceLocation godId = ResourceLocation.tryParse(godIdStr);
                if (godId != null) {
                    counts.computeIfAbsent(godId, k -> new EnumMap<>(DivineTier.class))
                          .merge(data.getCurrentTier(), 1, Integer::sum);
                }
            }
        }

        return counts;
    }

    /**
     * Calculate the weighted power of a god's followers (higher tiers count more).
     */
    public static float calculateWeightedPower(MinecraftServer server, ResourceLocation godId) {
        float power = 0f;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
            String playerGodId = data.getChosenGodId();
            if (playerGodId != null && godId.toString().equals(playerGodId)) {
                // Weight by tier level
                power += data.getCurrentTier().getLevel() * 1.5f;
            }
        }

        return power;
    }

    /**
     * Determine if a god is dominant enough to trigger rival pressure events.
     */
    public static boolean isDominant(PressureResult result, float threshold) {
        return result.dominanceRatio() >= threshold;
    }

    /**
     * Get the most dominant god on the server.
     */
    public static Optional<PressureResult> getMostDominant(MinecraftServer server) {
        Map<ResourceLocation, PressureResult> all = calculateAllPressure(server);
        return all.values().stream()
                .max(Comparator.comparing(PressureResult::dominanceRatio));
    }

    /**
     * Get gods that are under pressure from rivals.
     */
    public static List<ResourceLocation> getGodsUnderPressure(MinecraftServer server, float pressureThreshold) {
        List<ResourceLocation> underPressure = new ArrayList<>();
        Map<ResourceLocation, PressureResult> all = calculateAllPressure(server);

        for (PressureResult result : all.values()) {
            if (result.pressureLevel() >= pressureThreshold) {
                underPressure.addAll(result.pressuredRivals());
            }
        }

        return underPressure;
    }
}
