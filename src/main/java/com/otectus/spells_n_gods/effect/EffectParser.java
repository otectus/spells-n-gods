package com.otectus.spells_n_gods.effect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.effect.effects.*;
import com.otectus.spells_n_gods.effect.effects.ConditionalCombatEffect.Condition;
import com.otectus.spells_n_gods.effect.effects.TransgressionEffect.TransgressionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class EffectParser {

    public static List<TierEffect> parseEffectsForTier(GodDefinition god, DivineTier tier) {
        List<TierEffect> effects = new ArrayList<>();

        JsonObject blessings = god.blessings();
        if (blessings == null) {
            return effects;
        }

        String tierKey = tier.getTierKey();
        if (!blessings.has(tierKey)) {
            return effects;
        }

        JsonObject tierData = blessings.getAsJsonObject(tierKey);
        if (!tierData.has("effects")) {
            return effects;
        }

        JsonElement effectsElement = tierData.get("effects");
        if (!effectsElement.isJsonArray()) {
            return effects;
        }

        JsonArray effectsArray = effectsElement.getAsJsonArray();
        for (JsonElement elem : effectsArray) {
            if (!elem.isJsonObject()) {
                continue;
            }

            TierEffect effect = parseEffect(elem.getAsJsonObject());
            if (effect != null) {
                effects.add(effect);
            }
        }

        return effects;
    }

    public static TierEffect parseEffect(JsonObject obj) {
        if (!obj.has("type")) {
            SpellsNGodsMod.LOGGER.warn("Effect missing 'type' field");
            return null;
        }

        String type = obj.get("type").getAsString();

        return switch (type) {
            case "potion_effect" -> parsePotionEffect(obj);
            case "attribute_modifier" -> parseAttributeModifier(obj);
            case "durability_multiplier" -> parseDurabilityMultiplier(obj);
            case "conditional_combat" -> parseConditionalCombat(obj);
            case "passive_regen" -> parsePassiveRegen(obj);
            case "death_penalty" -> parseDeathPenalty(obj);
            case "cooldown_reduction" -> parseCooldownReduction(obj);
            case "trade_efficiency" -> parseTradeEfficiency(obj);
            case "luck_manipulation" -> parseLuckManipulation(obj);
            case "transgression" -> parseTransgression(obj);
            default -> {
                SpellsNGodsMod.LOGGER.warn("Unknown effect type: {}", type);
                yield null;
            }
        };
    }

    private static TierEffect parsePotionEffect(JsonObject obj) {
        // Data authoring uses "effect_id"; tolerate the shorter "effect" alias too.
        String effectId = readString(obj, null, "effect_id", "effect");
        if (effectId == null) {
            SpellsNGodsMod.LOGGER.warn("potion_effect missing 'effect_id' field");
            return null;
        }

        int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;

        ResourceLocation effectLoc = ResourceLocation.tryParse(effectId);
        if (effectLoc == null) {
            SpellsNGodsMod.LOGGER.warn("Invalid effect ID: {}", effectId);
            return null;
        }

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectLoc);
        if (effect == null) {
            SpellsNGodsMod.LOGGER.warn("Unknown mob effect: {}", effectId);
            return null;
        }

        return new PotionTierEffect(effect, amplifier);
    }

    private static TierEffect parseAttributeModifier(JsonObject obj) {
        if (!obj.has("attribute")) {
            SpellsNGodsMod.LOGGER.warn("attribute_modifier missing 'attribute' field");
            return null;
        }

        String attributeId = obj.get("attribute").getAsString();
        // Data authoring uses "amount"; tolerate the "value" alias too.
        double value = readDouble(obj, 0.0, "amount", "value");
        String opStr = obj.has("operation") ? obj.get("operation").getAsString() : "add";
        String name = obj.has("name") ? obj.get("name").getAsString() : "spells_n_gods_tier_" + attributeId;

        ResourceLocation attributeLoc = ResourceLocation.tryParse(attributeId);
        if (attributeLoc == null) {
            SpellsNGodsMod.LOGGER.warn("Invalid attribute ID: {}", attributeId);
            return null;
        }

        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeLoc);
        if (attribute == null) {
            SpellsNGodsMod.LOGGER.warn("Unknown attribute: {}", attributeId);
            return null;
        }

        AttributeModifier.Operation operation = switch (opStr.toLowerCase()) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };

        return new AttributeTierEffect(attribute, name, value, operation);
    }

    private static TierEffect parseDurabilityMultiplier(JsonObject obj) {
        float multiplier = obj.has("multiplier") ? obj.get("multiplier").getAsFloat() : 0.5f;
        return new DurabilityMultiplierEffect(multiplier);
    }

    private static TierEffect parseConditionalCombat(JsonObject obj) {
        String conditionStr = obj.has("condition") ? obj.get("condition").getAsString() : "LOW_HEALTH";
        Condition condition = parseEnum(Condition.class, conditionStr, null);
        if (condition == null) {
            SpellsNGodsMod.LOGGER.warn("Unknown conditional_combat condition '{}' — skipping effect", conditionStr);
            return null;
        }
        float damageBonus = obj.has("damage_bonus") ? obj.get("damage_bonus").getAsFloat() : 0f;
        float damageResistance = obj.has("damage_resistance") ? obj.get("damage_resistance").getAsFloat() : 0f;
        float speedBonus = obj.has("speed_bonus") ? obj.get("speed_bonus").getAsFloat() : 0f;
        return new ConditionalCombatEffect(condition, damageBonus, damageResistance, speedBonus);
    }

    private static TierEffect parsePassiveRegen(JsonObject obj) {
        // Data authoring uses "heal_per_second"; tolerate the "base_rate" alias too.
        float baseRate = (float) readDouble(obj, 1.0, "heal_per_second", "base_rate");
        boolean scalesWithEnv = obj.has("scales_with_environment") && obj.get("scales_with_environment").getAsBoolean();
        boolean requiresSatiation = obj.has("requires_satiation") && obj.get("requires_satiation").getAsBoolean();
        return new PassiveRegenEffect(baseRate, scalesWithEnv, requiresSatiation);
    }

    private static TierEffect parseDeathPenalty(JsonObject obj) {
        float xpRetention = obj.has("xp_retention") ? obj.get("xp_retention").getAsFloat() : 0f;
        float itemRetention = obj.has("item_retention") ? obj.get("item_retention").getAsFloat() : 0f;
        boolean keepHotbar = obj.has("keep_hotbar") && obj.get("keep_hotbar").getAsBoolean();
        boolean keepArmor = obj.has("keep_armor") && obj.get("keep_armor").getAsBoolean();
        return new DeathPenaltyReductionEffect(xpRetention, itemRetention, keepHotbar, keepArmor);
    }

    private static TierEffect parseCooldownReduction(JsonObject obj) {
        float multiplier = obj.has("multiplier") ? obj.get("multiplier").getAsFloat() : 1f;
        float speedBonus = obj.has("speed_bonus") ? obj.get("speed_bonus").getAsFloat() : 0f;
        boolean stacks = obj.has("stacks_on_action") && obj.get("stacks_on_action").getAsBoolean();
        int maxStacks = obj.has("max_stacks") ? obj.get("max_stacks").getAsInt() : 1;
        return new CooldownReductionEffect(multiplier, speedBonus, stacks, maxStacks);
    }

    private static TierEffect parseTradeEfficiency(JsonObject obj) {
        float discount = obj.has("trade_discount") ? obj.get("trade_discount").getAsFloat() : 0f;
        float xpBonus = obj.has("experience_bonus") ? obj.get("experience_bonus").getAsFloat() : 0f;
        boolean suppressNegative = obj.has("suppress_negative") && obj.get("suppress_negative").getAsBoolean();
        float efficiency = obj.has("efficiency") ? obj.get("efficiency").getAsFloat() : 1f;
        return new TradeEfficiencyEffect(discount, xpBonus, suppressNegative, efficiency);
    }

    private static TierEffect parseLuckManipulation(JsonObject obj) {
        int luckAmp = obj.has("luck_amplifier") ? obj.get("luck_amplifier").getAsInt() : 0;
        float critChance = obj.has("critical_proc_chance") ? obj.get("critical_proc_chance").getAsFloat() : 0f;
        float dropChance = obj.has("bonus_drop_chance") ? obj.get("bonus_drop_chance").getAsFloat() : 0f;
        float volatility = obj.has("volatility") ? obj.get("volatility").getAsFloat() : 1f;
        return new LuckManipulationEffect(luckAmp, critChance, dropChance, volatility);
    }

    private static TierEffect parseTransgression(JsonObject obj) {
        String typeStr = obj.has("transgression_type") ? obj.get("transgression_type").getAsString() : "SHADOW_SIGHT";
        TransgressionType type = parseEnum(TransgressionType.class, typeStr, null);
        if (type == null) {
            SpellsNGodsMod.LOGGER.warn("Unknown transgression_type '{}' — skipping effect", typeStr);
            return null;
        }
        int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
        float sideEffect = obj.has("side_effect_intensity") ? obj.get("side_effect_intensity").getAsFloat() : 0.5f;
        return new TransgressionEffect(type, amplifier, sideEffect);
    }

    // ---- tolerant readers --------------------------------------------------------------------

    /** Return the first present key's string value, or {@code fallback} if none are present. */
    private static String readString(JsonObject obj, String fallback, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsString();
            }
        }
        return fallback;
    }

    /** Return the first present key's numeric value, or {@code fallback} if none are present. */
    private static double readDouble(JsonObject obj, double fallback, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                try {
                    return obj.get(key).getAsDouble();
                } catch (NumberFormatException ignored) {
                    // try next key
                }
            }
        }
        return fallback;
    }

    /** Case-insensitive enum lookup that returns {@code fallback} instead of throwing on a bad value. */
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static List<TierEffect> parseEffectsArray(JsonArray effectsArray) {
        List<TierEffect> effects = new ArrayList<>();

        for (JsonElement elem : effectsArray) {
            if (!elem.isJsonObject()) {
                continue;
            }

            TierEffect effect = parseEffect(elem.getAsJsonObject());
            if (effect != null) {
                effects.add(effect);
            }
        }

        return effects;
    }
}
