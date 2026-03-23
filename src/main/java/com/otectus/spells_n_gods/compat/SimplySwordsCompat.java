package com.otectus.spells_n_gods.compat;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Isolated compatibility class for SimplySwords.
 * This class is ONLY referenced when ModCompatHandler.SIMPLY_SWORDS_LOADED is true.
 */
public class SimplySwordsCompat {

    /**
     * Resolve a SimplySwords weapon by registry name.
     *
     * @param weaponId the full registry name (e.g., "simplyswords:longsword")
     * @return the weapon ItemStack, or empty if not found
     */
    public static ItemStack getWeapon(String weaponId) {
        try {
            ResourceLocation rl = new ResourceLocation(weaponId);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
            SpellsNGodsMod.LOGGER.warn("SimplySwords weapon not found in registry: {}", weaponId);
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.warn("Failed to resolve SimplySwords weapon '{}': {}", weaponId, e.getMessage());
        }
        return ItemStack.EMPTY;
    }
}
