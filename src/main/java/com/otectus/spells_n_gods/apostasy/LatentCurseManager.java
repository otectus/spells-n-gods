package com.otectus.spells_n_gods.apostasy;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.ApostasyDefinition;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.network.DivineVfxPacket;
import com.otectus.spells_n_gods.network.ModNetwork;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

/**
 * Manages the latent curse applied after apostasy.
 * Curses are temporary debuffs that last for a configurable duration.
 */
public class LatentCurseManager {

    /**
     * Apply a latent curse to a player based on their abandoned god.
     */
    public static void applyCurse(ServerPlayer player, GodDefinition abandonedGod) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        ApostasyDefinition apostasy = abandonedGod != null ?
                abandonedGod.apostasy() : ApostasyDefinition.defaultDefinition();

        ApostasyDefinition.CurseDefinition curse = apostasy.latentCurse();

        // Set curse end time
        long curseEnd = System.currentTimeMillis() + curse.getDurationMs();
        data.setLatentCurseEndMs(curseEnd);
        data.setLastAbandonedGodId(abandonedGod != null ? abandonedGod.id().toString() : null);

        // Apply initial curse effects
        applyEffects(player, curse);

        player.sendSystemMessage(Component.translatable(curse.curseMessageKey()));

        SpellsNGodsMod.LOGGER.info("Applied latent curse to player {} from god {}, duration {} hours",
                player.getName().getString(),
                abandonedGod != null ? abandonedGod.id() : "unknown",
                curse.durationHours());
    }

    /**
     * Check if a player has an active latent curse.
     */
    public static boolean hasCurse(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        return data.hasLatentCurse();
    }

    /**
     * Get remaining curse duration in milliseconds.
     */
    public static long getRemainingCurseMs(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        long curseEnd = data.getLatentCurseEndMs();

        if (curseEnd <= 0) {
            return 0;
        }

        long remaining = curseEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Get remaining curse duration in hours.
     */
    public static int getRemainingCurseHours(ServerPlayer player) {
        return (int) (getRemainingCurseMs(player) / 3600_000L);
    }

    /**
     * Tick curse effects for a player.
     * Called every server tick while the curse is active.
     */
    public static void tickCurseEffects(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        if (!data.hasLatentCurse()) {
            return;
        }

        String abandonedGodId = data.getLastAbandonedGodId();
        GodDefinition god = abandonedGodId != null ?
                SpellsNGodsDataManager.getGods().get(new ResourceLocation(abandonedGodId)) : null;

        ApostasyDefinition apostasy = god != null ?
                god.apostasy() : ApostasyDefinition.defaultDefinition();

        ApostasyDefinition.CurseDefinition curse = apostasy.latentCurse();

        // Reapply effects if they're about to expire (every ~30 seconds)
        // Effects are applied for 40 seconds (800 ticks) to ensure overlap
        if (player.tickCount % 600 == 0) {
            applyEffects(player, curse);
        }

        // Apply hunger drain
        if (curse.hungerDrain() > 1.0f && player.tickCount % 100 == 0) {
            float extraDrain = (curse.hungerDrain() - 1.0f) * 0.5f;
            player.causeFoodExhaustion(extraDrain);
        }

        // Curse visual feedback - dark wisps around player
        if (player.tickCount % 60 == 0 && player.level() instanceof ServerLevel serverLevel) {
            DustParticleOptions curseWisp = new DustParticleOptions(
                    new Vector3f(0.25f, 0.05f, 0.15f), 1.0f);
            serverLevel.sendParticles(curseWisp,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    3, 0.3, 0.2, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 0.3, player.getZ(),
                    1, 0.2, 0.1, 0.2, 0.01);
        }

        // Subtle ambient curse sound every ~10 seconds
        if (player.tickCount % 200 == 0) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.3F, 0.6F);
        }
    }

    /**
     * Apply curse potion effects.
     */
    private static void applyEffects(ServerPlayer player, ApostasyDefinition.CurseDefinition curse) {
        for (ApostasyDefinition.CurseEffect effect : curse.effects()) {
            ResourceLocation effectId = ResourceLocation.tryParse(effect.effectId());
            if (effectId == null) {
                continue;
            }

            MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
            if (mobEffect == null) {
                SpellsNGodsMod.LOGGER.warn("Unknown curse effect: {}", effect.effectId());
                continue;
            }

            // Apply for 40 seconds (800 ticks)
            player.addEffect(new MobEffectInstance(
                    mobEffect,
                    800,
                    effect.amplifier(),
                    true,
                    true,
                    true
            ));
        }
    }

    /**
     * Clear the latent curse from a player.
     */
    public static void clearCurse(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        String abandonedGodId = data.getLastAbandonedGodId();
        GodDefinition god = abandonedGodId != null ?
                SpellsNGodsDataManager.getGods().get(new ResourceLocation(abandonedGodId)) : null;

        ApostasyDefinition apostasy = god != null ?
                god.apostasy() : ApostasyDefinition.defaultDefinition();

        // Remove curse effects
        for (ApostasyDefinition.CurseEffect effect : apostasy.latentCurse().effects()) {
            ResourceLocation effectId = ResourceLocation.tryParse(effect.effectId());
            if (effectId != null) {
                MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                if (mobEffect != null) {
                    player.removeEffect(mobEffect);
                }
            }
        }

        data.setLatentCurseEndMs(0L);
        data.setLastAbandonedGodId(null);

        player.sendSystemMessage(Component.translatable("spells_n_gods.curse.cleared"));

        SpellsNGodsMod.LOGGER.info("Cleared latent curse from player {}", player.getName().getString());
    }

    /**
     * Handle curse expiration.
     */
    public static void onCurseExpired(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        // Clear curse effects
        String abandonedGodId = data.getLastAbandonedGodId();
        GodDefinition god = abandonedGodId != null ?
                SpellsNGodsDataManager.getGods().get(new ResourceLocation(abandonedGodId)) : null;

        ApostasyDefinition apostasy = god != null ?
                god.apostasy() : ApostasyDefinition.defaultDefinition();

        for (ApostasyDefinition.CurseEffect effect : apostasy.latentCurse().effects()) {
            ResourceLocation effectId = ResourceLocation.tryParse(effect.effectId());
            if (effectId != null) {
                MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                if (mobEffect != null) {
                    player.removeEffect(mobEffect);
                }
            }
        }

        data.setLatentCurseEndMs(0L);
        data.setLastAbandonedGodId(null);

        // Public event for integrators (KubeJS/FTB Quests).
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new com.otectus.spells_n_gods.compat.SpellsNGodsEvents.CurseExpiredEvent(player, abandonedGodId));

        player.sendSystemMessage(Component.translatable("spells_n_gods.curse.expired"));

        // Curse lifted celebration VFX
        ModNetwork.sendToPlayer(player, new DivineVfxPacket(
                DivineVfxPacket.VfxType.CURSE_LIFTED,
                player.getX(), player.getY(), player.getZ(),
                "", 0));

        SpellsNGodsMod.LOGGER.info("Latent curse expired for player {}", player.getName().getString());
    }

    /**
     * Get the damage multiplier from the curse.
     * Called from combat event handlers.
     */
    public static float getDamageMultiplier(ServerPlayer player) {
        if (!hasCurse(player)) {
            return 1.0f;
        }

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        String abandonedGodId = data.getLastAbandonedGodId();
        GodDefinition god = abandonedGodId != null ?
                SpellsNGodsDataManager.getGods().get(new ResourceLocation(abandonedGodId)) : null;

        ApostasyDefinition apostasy = god != null ?
                god.apostasy() : ApostasyDefinition.defaultDefinition();

        return apostasy.latentCurse().damageMultiplier();
    }
}
