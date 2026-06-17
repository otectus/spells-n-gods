package com.otectus.spells_n_gods.mixin;

import com.otectus.spells_n_gods.durability.DurabilityDamageHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Scales item durability damage for apostasy scars (faster wear) and Aurex/Permanence blessings
 * (slower wear).
 *
 * <p><b>Why a mixin?</b> Forge 1.20.1 exposes no event for per-point {@link ItemStack} durability
 * damage, so there is no clean hook to multiply the amount. We therefore intercept the single
 * durability chokepoint, {@link ItemStack#hurt(int, RandomSource, ServerPlayer)} — which
 * {@code hurtAndBreak} (and thus tool/armor/elytra wear) all route through — and adjust the incoming
 * damage amount at {@code HEAD}, before vanilla's Unbreaking reduction runs.
 *
 * <p>The {@code ServerPlayer} parameter is {@code null} for non-player wear, which makes this
 * effectively server-and-player only. <b>All gameplay logic is delegated</b> to
 * {@link DurabilityDamageHandler}; this mixin contains no decision-making of its own.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    /**
     * Modify the {@code int amount} (ordinal 0, args only) of {@code hurt}. The handler captures the
     * full target argument list per Mixin's {@code @ModifyVariable} arg-capture rule, so {@code value}
     * and {@code amount} are the same int; {@code player} may be {@code null}.
     */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int spells_n_gods$scaleDurabilityDamage(int value, int amount, RandomSource random, ServerPlayer player) {
        return DurabilityDamageHandler.modifyDurabilityDamage((ItemStack) (Object) this, value, random, player);
    }
}
