package com.otectus.spells_n_gods.offering;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Records server-side player behaviors used by {@code action_rule} offering validators (see
 * {@link com.otectus.spells_n_gods.offering.validators.OfferingRuleChecks}). Each handler stamps a
 * timestamp on the player's {@link PlayerDivinityData}; validators check those against a recency window.
 *
 * <p>Signals: replanting a crop/sapling (clean), engaging a villager trade (heuristic — Forge 1.20.1 has
 * no trade-completed event), dropping to low health (heuristic "risk"), and getting a kill credit (covers
 * {@code requires_kill_credit}/{@code requires_deathbound}). Mana spend is fed externally via
 * {@link #recordManaSpend(Player)} since it depends on the (optional, non-compile) Iron's Spellbooks mod.
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public final class ActionTracker {

    /** Health fraction at/below which taking damage counts as a "risk". */
    private static final float RISK_HEALTH_FRACTION = 0.3f;

    private ActionTracker() {}

    /** Replanting: placing a crop, stem, or sapling. */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Block block = event.getPlacedBlock().getBlock();
        if (block instanceof CropBlock || block instanceof StemBlock || block instanceof SaplingBlock) {
            PlayerDivinityCapability.getOrCreate(player).setLastReplantMs(System.currentTimeMillis());
        }
    }

    /** Trade cycle: interacting with a villager/merchant (heuristic — no trade-completed event exists). */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof AbstractVillager) {
            PlayerDivinityCapability.getOrCreate(player).setLastTradeCycleMs(System.currentTimeMillis());
        }
    }

    /** Risk: dropping to low health from a hit. */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        float postHealth = player.getHealth() - event.getAmount();
        if (postHealth <= RISK_HEALTH_FRACTION * player.getMaxHealth()) {
            PlayerDivinityCapability.getOrCreate(player).setLastRiskMs(System.currentTimeMillis());
        }
    }

    /** Kill credit (also satisfies deathbound): the player gets credit for a kill. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer
                && event.getEntity() instanceof LivingEntity) {
            PlayerDivinityCapability.getOrCreate(killer).setLastKillCreditMs(System.currentTimeMillis());
        }
    }

    /**
     * Bridge point for the optional Iron's Spellbooks mana system. A compat shim (e.g. via reflection or
     * a KubeJS event) should call this when a follower spends mana; it is intentionally not wired to an
     * Iron's Spellbooks event here because that API is not a compile dependency.
     */
    public static void recordManaSpend(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerDivinityCapability.getOrCreate(serverPlayer).setLastManaSpendMs(System.currentTimeMillis());
        }
    }
}
