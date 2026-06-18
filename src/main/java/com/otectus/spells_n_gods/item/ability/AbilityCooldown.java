package com.otectus.spells_n_gods.item.ability;

import net.minecraft.world.item.ItemStack;

/**
 * Shared NBT-backed ability cooldown for stacks that track their own timer (divine bow/crossbow).
 * ponytail: plain static helpers over a single int tag; no interface or registry needed.
 */
public final class AbilityCooldown {
    private static final String KEY = "AbilityCooldownTicks";

    private AbilityCooldown() {
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt(KEY) > 0;
    }

    public static void start(ItemStack stack, int ticks) {
        stack.getOrCreateTag().putInt(KEY, ticks);
    }

    /** Decrement by one tick; clears the tag when it reaches zero. Call from inventoryTick. */
    public static void tick(ItemStack stack) {
        if (!isActive(stack)) return;
        int remaining = stack.getTag().getInt(KEY) - 1;
        if (remaining <= 0) {
            stack.getTag().remove(KEY);
        } else {
            stack.getTag().putInt(KEY, remaining);
        }
    }

    /** Remaining cooldown rounded up to whole seconds (for tooltips). */
    public static int remainingSeconds(ItemStack stack) {
        if (!isActive(stack)) return 0;
        return (stack.getTag().getInt(KEY) + 19) / 20;
    }
}
