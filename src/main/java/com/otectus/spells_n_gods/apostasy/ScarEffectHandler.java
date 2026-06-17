package com.otectus.spells_n_gods.apostasy;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.capability.ScarData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Handles the application of permanent scar effects from apostasy.
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class ScarEffectHandler {

    // Modifier UUIDs for scar effects
    private static final UUID SCAR_HEALTH_MODIFIER_UUID = UUID.fromString("b3e8c5a1-7f2d-4e9a-b8c1-5d6e7f8a9b0c");
    private static final UUID SCAR_LUCK_MODIFIER_UUID = UUID.fromString("c4f9d6b2-8e3f-5a0b-c9d2-6e7f8a9b0c1d");

    /**
     * Apply scar effects on player login.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recomputeScarEffects(player);
        }
    }

    /**
     * Apply scar effects on respawn.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recomputeScarEffects(player);
        }
    }

    /**
     * Apply XP penalty from scars.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(serverPlayer);
        ScarData scarData = data.getScarData();
        if (scarData.getScarCount() == 0) {
            return;
        }

        if (event.getAmount() > 0) {
            // Scars sap XP gains.
            float totalPenalty = scarData.getTotalXpPenalty();
            if (totalPenalty > 0) {
                int originalXp = event.getAmount();
                int reducedXp = (int) (originalXp * (1.0f - Math.min(totalPenalty, 0.9f)));
                event.setAmount(Math.max(1, reducedXp));
            }
        } else if (event.getAmount() < 0) {
            // Scars deepen the penalty of death: amplify XP lost (mirrors the blessing XP-retention
            // path in EffectEventHandler). Stored at the moment of death so scar state is consistent.
            float deathPenalty = serverPlayer.getPersistentData().getFloat("spells_n_gods:scar_death_penalty");
            if (deathPenalty > 0) {
                int amplified = (int) (event.getAmount() * (1.0f + Math.min(deathPenalty, 1.0f)));
                event.setAmount(amplified);
                serverPlayer.getPersistentData().remove("spells_n_gods:scar_death_penalty");
            }
        }
    }

    /**
     * Apply death penalty increase from scars.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        ScarData scarData = data.getScarData();

        if (scarData.getScarCount() == 0) {
            return;
        }

        // Store death penalty modifier in persistent data for use on drop event
        float deathPenalty = scarData.getTotalDeathPenalty();
        player.getPersistentData().putFloat("spells_n_gods:scar_death_penalty", deathPenalty);
    }

    /**
     * Recompute all scar effects for a player.
     */
    public static void recomputeScarEffects(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        ScarData scarData = data.getScarData();

        // Apply health reduction
        applyHealthReduction(player, scarData.getTotalHealthReduction());

        // Apply luck reduction
        applyLuckReduction(player, scarData.getTotalLuckReduction());

        if (scarData.getScarCount() > 0) {
            SpellsNGodsMod.LOGGER.debug("Applied scar effects for player {}: {} scars, {}% health reduction",
                    player.getName().getString(),
                    scarData.getScarCount(),
                    (int)(scarData.getTotalHealthReduction() * 100));
        }
    }

    /**
     * Apply max health reduction from scars.
     */
    private static void applyHealthReduction(ServerPlayer player, float reduction) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) {
            return;
        }

        // Remove existing modifier
        healthAttr.removeModifier(SCAR_HEALTH_MODIFIER_UUID);

        if (reduction > 0) {
            // Cap at 50% health reduction
            float cappedReduction = Math.min(reduction, 0.5f);

            // Apply as multiplicative reduction
            AttributeModifier modifier = new AttributeModifier(
                    SCAR_HEALTH_MODIFIER_UUID,
                    "Apostasy Scar Health Reduction",
                    -cappedReduction,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            healthAttr.addPermanentModifier(modifier);

            // Ensure current health doesn't exceed new max
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /**
     * Apply luck reduction from scars.
     */
    private static void applyLuckReduction(ServerPlayer player, float reduction) {
        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (luckAttr == null) {
            return;
        }

        // Remove existing modifier
        luckAttr.removeModifier(SCAR_LUCK_MODIFIER_UUID);

        if (reduction > 0) {
            // Apply as flat reduction
            AttributeModifier modifier = new AttributeModifier(
                    SCAR_LUCK_MODIFIER_UUID,
                    "Apostasy Scar Luck Reduction",
                    -reduction,
                    AttributeModifier.Operation.ADDITION
            );
            luckAttr.addPermanentModifier(modifier);
        }
    }

    /**
     * Get the durability penalty multiplier for a player.
     * Called from EffectEventHandler when processing item damage.
     */
    public static float getDurabilityPenaltyMultiplier(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        ScarData scarData = data.getScarData();

        if (scarData.getScarCount() == 0) {
            return 1.0f;
        }

        // Return multiplier > 1 to increase durability loss
        return 1.0f + scarData.getTotalDurabilityPenalty();
    }

    /**
     * Spawn subtle dark wisp particles on scarred players.
     * Intensity scales with scar count.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 40 != 0) return; // Every 2 seconds

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        int scarCount = data.getScarData().getScarCount();
        if (scarCount == 0) return;

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Dark wisp particles - intensity scales with scar count (1-3 particles)
        int particleCount = Math.min(scarCount, 3);
        DustParticleOptions darkWisp = new DustParticleOptions(
                new Vector3f(0.15f, 0.05f, 0.2f), 0.8f);

        serverLevel.sendParticles(darkWisp,
                player.getX(), player.getY() + 0.3, player.getZ(),
                particleCount, 0.25, 0.15, 0.25, 0.01);

        // At 3+ scars, also spawn soul particles
        if (scarCount >= 3 && player.tickCount % 80 == 0) {
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    1, 0.2, 0.1, 0.2, 0.01);
        }
    }

    /**
     * Clear all scar effects from a player (admin command).
     */
    public static void clearScarEffects(ServerPlayer player) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(SCAR_HEALTH_MODIFIER_UUID);
        }

        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (luckAttr != null) {
            luckAttr.removeModifier(SCAR_LUCK_MODIFIER_UUID);
        }
    }
}
