package com.otectus.spells_n_gods.item;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge event handler for divine weapon projectile impacts and kill-based passives.
 * <p>
 * Handles:
 * - Stormstrike arrow impacts (DivineBowItem): spawns lightning on hit
 * - Ensnaring bolt impacts (DivineCrossbowItem): roots + poisons target
 * - Kill passives (DivineWeaponItem): Celia heal-on-kill, Magnus XP-on-kill
 */
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class DivineWeaponEventHandler {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity hitEntity)) return;

        // Stormstrike arrow (Velox's Bow of Agility)
        if (arrow.getTags().contains("stormstrike_arrow")) {
            DivineBowItem.onStormstrikeArrowHit(arrow, hitEntity);
        }

        // Ensnaring bolt (Venatas's Crossbow of the Wild)
        if (arrow.getTags().contains("ensnaring_bolt")) {
            DivineCrossbowItem.onEnsnaringBoltHit(arrow, hitEntity);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;

        LivingEntity killed = event.getEntity();

        // Divine melee weapon kill passives (Celia heal, Magnus XP)
        DivineWeaponItem.onKillWithDivineWeapon(player, killed);
    }
}
