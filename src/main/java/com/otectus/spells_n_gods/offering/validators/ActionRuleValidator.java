package com.otectus.spells_n_gods.offering.validators;

import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.offering.OfferingValidator;
import com.otectus.spells_n_gods.offering.ValidationResult;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Validator for {@code "type": "action_rule"} offering definitions. Accepts an offering only when the
 * stack matches the {@code allow_items} allowlist (if any) AND the player has performed the authored
 * {@code requires_*} actions recently (tracked on {@code PlayerDivinityData} by {@code ActionTracker}).
 */
public class ActionRuleValidator implements OfferingValidator {

    private final String id;
    private final JsonObject rules;
    private final List<TagKey<Item>> allowTags;
    private final float baseValue;

    public ActionRuleValidator(String id, JsonObject definition) {
        this.id = id;
        this.rules = definition.has("rules") ? definition.getAsJsonObject("rules") : new JsonObject();
        this.allowTags = OfferingRuleChecks.parseAllowList(rules);

        JsonObject valueScoring = definition.has("value_scoring")
                ? definition.getAsJsonObject("value_scoring") : new JsonObject();
        this.baseValue = valueScoring.has("base_value") ? valueScoring.get("base_value").getAsFloat() : 12.0f;
    }

    @Override
    public ValidationResult validate(ItemStack stack, Player player, GodDefinition god) {
        if (stack.isEmpty()) {
            return ValidationResult.fail("spells_n_gods.offering.empty");
        }

        ValidationResult allow = OfferingRuleChecks.checkAllowItems(allowTags, stack);
        if (!allow.valid()) {
            return allow;
        }

        return OfferingRuleChecks.checkActionRequirements(rules, player);
    }

    @Override
    public float computeValue(ItemStack stack, Player player, GodDefinition god) {
        // Action-gated offerings derive their worth from the action, not item rarity scoring.
        return baseValue;
    }

    @Override
    public String getValidatorId() {
        return id;
    }
}
