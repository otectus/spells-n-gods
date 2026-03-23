package com.otectus.spells_n_gods.offering;

import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface OfferingValidator {
    ValidationResult validate(ItemStack stack, Player player, GodDefinition god);

    float computeValue(ItemStack stack, Player player, GodDefinition god);

    default String getValidatorId() {
        return "unknown";
    }
}
