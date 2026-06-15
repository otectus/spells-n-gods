package com.otectus.spells_n_gods.spawning;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.favor.FavorCalculator;
import com.otectus.spells_n_gods.spawning.logic.TierSettings;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Evaluates the progression gates of a {@link TierSettings} against the triggering player.
 *
 * <p>Only gates the current codebase can cleanly support are implemented: Ender-Dragon kill
 * (via the vanilla advancement), an arbitrary completed advancement, the player's current
 * dimension, and minimum favor with the candidate deity. Unknown/invalid gate values
 * <em>fail safe</em> — they block the spawn and emit a warning rather than crashing.
 */
public final class SpawnGateEvaluator {
    private static final ResourceLocation KILL_DRAGON =
            new ResourceLocation("minecraft", "end/kill_dragon");

    private SpawnGateEvaluator() {
    }

    public record Result(boolean passed, String reason) {
        static Result pass() {
            return new Result(true, "ok");
        }

        static Result fail(String reason) {
            return new Result(false, reason);
        }
    }

    public static Result evaluate(ServerPlayer player, String candidateDeityId, TierSettings tier) {
        if (tier == null || !tier.hasProgressionGate()) {
            return Result.pass();
        }

        if (tier.requiresDragonKilled() && !hasCompletedAdvancement(player, KILL_DRAGON)) {
            return Result.fail("dragon not killed");
        }

        if (!tier.requiredAdvancement().isBlank()) {
            ResourceLocation adv = ResourceLocation.tryParse(tier.requiredAdvancement());
            if (adv == null) {
                SpellsNGodsMod.LOGGER.warn("[Sng/Spawn] Tier '{}' has invalid required_advancement '{}'; "
                        + "failing gate safely.", tier.tier().configKey(), tier.requiredAdvancement());
                return Result.fail("invalid required_advancement");
            }
            if (!hasCompletedAdvancement(player, adv)) {
                return Result.fail("advancement '" + adv + "' not completed");
            }
        }

        if (!tier.requiredDimension().isBlank()) {
            String current = player.level().dimension().location().toString();
            if (!current.equals(tier.requiredDimension())) {
                return Result.fail("wrong dimension (in " + current + ", need " + tier.requiredDimension() + ")");
            }
        }

        if (tier.minFavor() > 0) {
            float favor = favorWith(player, candidateDeityId);
            if (favor < tier.minFavor()) {
                return Result.fail("favor " + (int) favor + " < required " + tier.minFavor());
            }
        }

        return Result.pass();
    }

    private static boolean hasCompletedAdvancement(ServerPlayer player, ResourceLocation id) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            SpellsNGodsMod.LOGGER.warn("[Sng/Spawn] Advancement '{}' not found; gate fails safe.", id);
            return false;
        }
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** Player's favor with the candidate deity (0 unless it is their currently-bound deity). */
    private static float favorWith(ServerPlayer player, String candidateDeityId) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        String chosen = data.getChosenGodId();
        if (chosen == null || !chosen.equals(candidateDeityId)) {
            return 0f;
        }
        return FavorCalculator.computeCurrentFavor(data);
    }
}
