package com.otectus.spells_n_gods.offering.validators;

import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.offering.OfferingValidator;
import com.otectus.spells_n_gods.offering.ValidationResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AlwaysValidValidator implements OfferingValidator {
    private static final float DEFAULT_VALUE = 5.0f;

    @Override
    public ValidationResult validate(ItemStack stack, Player player, GodDefinition god) {
        if (stack.isEmpty()) {
            return ValidationResult.fail("spells_n_gods.offering.empty");
        }
        return ValidationResult.ok();
    }

    @Override
    public float computeValue(ItemStack stack, Player player, GodDefinition god) {
        return DEFAULT_VALUE * stack.getCount();
    }

    @Override
    public String getValidatorId() {
        return "always_valid";
    }
}
