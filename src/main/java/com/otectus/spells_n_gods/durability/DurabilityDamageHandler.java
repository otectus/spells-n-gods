package com.otectus.spells_n_gods.durability;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.apostasy.ScarEffectHandler;
import com.otectus.spells_n_gods.durability.logic.DurabilityModifierService;
import com.otectus.spells_n_gods.effect.EffectProfile;
import com.otectus.spells_n_gods.effect.EffectProfileCache;
import com.otectus.spells_n_gods.effect.EffectType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft-facing service that the {@code ItemStackMixin} delegates to. It owns all of the
 * gameplay logic for durability scaling so the mixin body stays trivial:
 *
 * <ol>
 *   <li>applies the safety guards (server-only via {@link ServerPlayer}, creative, empty/unbreakable
 *       /non-durable stacks, non-positive amounts);</li>
 *   <li>resolves the two existing hook points — the apostasy-scar penalty
 *       ({@link ScarEffectHandler#getDurabilityPenaltyMultiplier}) and the Aurex/Permanence blessing
 *       ({@link EffectProfile#getMultiplier} for {@link EffectType#DURABILITY_MULTIPLIER});</li>
 *   <li>delegates the deterministic arithmetic to {@link DurabilityModifierService}.</li>
 * </ol>
 *
 * <p>Vanilla behavior is preserved whenever no modifier applies (the method returns the input
 * {@code amount} untouched), so Unbreaking and every other durability interaction are unaffected.
 */
public final class DurabilityDamageHandler {

    private DurabilityDamageHandler() {}

    /**
     * Scale a single {@code ItemStack.hurt} durability-damage amount by the player's scar/blessing
     * multipliers. Returns {@code amount} unchanged when no modifier applies.
     *
     * @param stack  the item being damaged (the mixin's {@code this})
     * @param amount the vanilla durability damage about to be applied
     * @param random the {@link RandomSource} vanilla passes to {@code hurt}, reused for the
     *               probabilistic-rounding draw
     * @param player the damaging player; {@code null} for mob/non-player wear (then a no-op)
     */
    public static int modifyDurabilityDamage(ItemStack stack, int amount, RandomSource random, ServerPlayer player) {
        // Server-only (ServerPlayer guarantees it), and skip the cases where a modifier must not apply.
        if (player == null || amount <= 0 || player.getAbilities().instabuild) {
            return amount;
        }
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return amount;
        }

        float scarMultiplier = ScarEffectHandler.getDurabilityPenaltyMultiplier(player); // >= 1.0

        float blessingMultiplier = 1.0f; // default = no change
        EffectProfile profile = EffectProfileCache.get(player);
        if (profile != null) {
            blessingMultiplier = profile.getMultiplier(EffectType.DURABILITY_MULTIPLIER); // <= 1.0
        }

        float combined = DurabilityModifierService.combineMultipliers(scarMultiplier, blessingMultiplier);
        if (combined == 1.0f) {
            return amount; // nothing to do — preserve vanilla exactly
        }

        int modified = DurabilityModifierService.applyMultiplier(amount, combined, random.nextDouble());

        if (modified != amount) {
            SpellsNGodsMod.LOGGER.debug(
                    "Durability damage for {} scaled {}->{} (scar x{}, blessing x{})",
                    player.getName().getString(), amount, modified, scarMultiplier, blessingMultiplier);
        }
        return modified;
    }
}
