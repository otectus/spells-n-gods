package com.otectus.spells_n_gods.offering.validators;

import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.offering.OfferingValidator;
import com.otectus.spells_n_gods.offering.ValidationResult;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.List;

public class ItemRuleValidator implements OfferingValidator {
    private final String id;
    private final boolean mustBeCrafted;
    private final boolean mustBeUnused;
    private final Rarity minRarity;
    private final List<TagKey<Item>> denyTags;
    private final List<TagKey<Item>> allowTags;
    private final JsonObject rules;
    private final float baseValue;
    private final float rarityMultiplierCommon;
    private final float rarityMultiplierUncommon;
    private final float rarityMultiplierRare;
    private final float rarityMultiplierEpic;
    private final float enchantWeight;

    public ItemRuleValidator(String id, JsonObject definition) {
        this.id = id;

        this.rules = definition.has("rules") ? definition.getAsJsonObject("rules") : new JsonObject();

        this.mustBeCrafted = rules.has("must_be_crafted") && rules.get("must_be_crafted").getAsBoolean();
        this.mustBeUnused = rules.has("must_be_unused") && rules.get("must_be_unused").getAsBoolean();

        String minRarityStr = rules.has("min_rarity") ? rules.get("min_rarity").getAsString() : null;
        this.minRarity = parseRarity(minRarityStr);

        this.denyTags = OfferingRuleChecks.parseTagList(rules, "deny_tags");
        this.allowTags = OfferingRuleChecks.parseTagList(rules, "allow_items");

        JsonObject valueScoring = definition.has("value_scoring") ? definition.getAsJsonObject("value_scoring") : new JsonObject();
        this.baseValue = valueScoring.has("base_value") ? valueScoring.get("base_value").getAsFloat() : 10.0f;

        JsonObject rarityMult = valueScoring.has("rarity_multiplier") ? valueScoring.getAsJsonObject("rarity_multiplier") : new JsonObject();
        this.rarityMultiplierCommon = rarityMult.has("common") ? rarityMult.get("common").getAsFloat() : 1.0f;
        this.rarityMultiplierUncommon = rarityMult.has("uncommon") ? rarityMult.get("uncommon").getAsFloat() : 1.2f;
        this.rarityMultiplierRare = rarityMult.has("rare") ? rarityMult.get("rare").getAsFloat() : 1.6f;
        this.rarityMultiplierEpic = rarityMult.has("epic") ? rarityMult.get("epic").getAsFloat() : 2.5f;

        this.enchantWeight = valueScoring.has("enchant_weight") ? valueScoring.get("enchant_weight").getAsFloat() : 0.3f;
    }

    private Rarity parseRarity(String rarityStr) {
        if (rarityStr == null) {
            return null;
        }
        return switch (rarityStr.toLowerCase()) {
            case "common" -> Rarity.COMMON;
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> null;
        };
    }

    @Override
    public ValidationResult validate(ItemStack stack, Player player, GodDefinition god) {
        if (stack.isEmpty()) {
            return ValidationResult.fail("spells_n_gods.offering.empty");
        }

        if (mustBeUnused && stack.isDamaged()) {
            return ValidationResult.fail("spells_n_gods.offering.must_be_unused");
        }

        if (mustBeCrafted && !wasCraftedByPlayer(stack, player)) {
            return ValidationResult.fail("spells_n_gods.offering.must_be_crafted");
        }

        if (minRarity != null && !meetsRarity(stack, minRarity)) {
            return ValidationResult.fail("spells_n_gods.offering.rarity_too_low");
        }

        for (TagKey<Item> tag : denyTags) {
            if (stack.is(tag)) {
                return ValidationResult.fail("spells_n_gods.offering.denied_item");
            }
        }

        ValidationResult allow = OfferingRuleChecks.checkAllowItems(allowTags, stack);
        if (!allow.valid()) {
            return allow;
        }

        ValidationResult actions = OfferingRuleChecks.checkActionRequirements(rules, player);
        if (!actions.valid()) {
            return actions;
        }

        return ValidationResult.ok();
    }

    @Override
    public float computeValue(ItemStack stack, Player player, GodDefinition god) {
        float value = baseValue;

        // Apply rarity multiplier
        Rarity rarity = stack.getRarity();
        float rarityMult = switch (rarity) {
            case COMMON -> rarityMultiplierCommon;
            case UNCOMMON -> rarityMultiplierUncommon;
            case RARE -> rarityMultiplierRare;
            case EPIC -> rarityMultiplierEpic;
        };
        value *= rarityMult;

        // Add enchantment bonus
        if (stack.isEnchanted()) {
            int enchantCount = stack.getEnchantmentTags().size();
            value += enchantCount * enchantWeight * baseValue;
        }

        // Stack size bonus (diminishing)
        if (stack.getCount() > 1) {
            value *= (1.0f + 0.1f * Math.log(stack.getCount()));
        }

        return value;
    }

    @Override
    public String getValidatorId() {
        return id;
    }

    private boolean wasCraftedByPlayer(ItemStack stack, Player player) {
        if (!stack.hasTag()) {
            return false;
        }
        String craftedBy = stack.getTag().getString("CraftedBy");
        return player.getUUID().toString().equals(craftedBy);
    }

    private boolean meetsRarity(ItemStack stack, Rarity minRarity) {
        Rarity itemRarity = stack.getRarity();
        return itemRarity.ordinal() >= minRarity.ordinal();
    }
}
