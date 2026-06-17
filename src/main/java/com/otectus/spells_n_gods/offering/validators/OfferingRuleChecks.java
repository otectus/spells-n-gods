package com.otectus.spells_n_gods.offering.validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.compat.ModCompatHandler;
import com.otectus.spells_n_gods.offering.ValidationResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared parsing and checking for offering-validator {@code rules}: the {@code allow_items} allowlist and
 * the {@code requires_*} action requirements. Used by both {@link ItemRuleValidator} and
 * {@link ActionRuleValidator} so every validator honors the same authored rules with one implementation.
 *
 * <p>Action requirements are satisfied by player behaviors recorded on {@link PlayerDivinityData} by
 * {@code offering.ActionTracker} within {@link #ACTION_WINDOW_MS}.
 */
public final class OfferingRuleChecks {

    /** How recently a tracked action must have happened to satisfy a {@code requires_*} flag. */
    public static final long ACTION_WINDOW_MS = 5 * 60 * 1000L; // 5 minutes

    private OfferingRuleChecks() {}

    /**
     * Parse the offering allowlist. Item-rule validators author it as {@code accept_tags} and
     * action-rule validators as {@code allow_items}; both are merged into one allowlist here so a
     * present-but-unmatched list correctly rejects off-theme items.
     */
    public static List<TagKey<Item>> parseAllowList(JsonObject rules) {
        List<TagKey<Item>> tags = parseTagList(rules, "allow_items");
        tags.addAll(parseTagList(rules, "accept_tags"));
        return tags;
    }

    /** Parse a JSON tag array ({@code allow_items}/{@code deny_tags}), tolerating a leading {@code '#'}. */
    public static List<TagKey<Item>> parseTagList(JsonObject rules, String key) {
        List<TagKey<Item>> tags = new ArrayList<>();
        if (rules.has(key) && rules.get(key).isJsonArray()) {
            JsonArray arr = rules.getAsJsonArray(key);
            for (JsonElement elem : arr) {
                String raw = elem.getAsString();
                if (raw.startsWith("#")) {
                    raw = raw.substring(1);
                }
                ResourceLocation tagId = ResourceLocation.tryParse(raw);
                if (tagId != null) {
                    tags.add(TagKey.create(Registries.ITEM, tagId));
                }
            }
        }
        return tags;
    }

    /** Fail when an allowlist is present and the stack matches none of it; no-op when the list is empty. */
    public static ValidationResult checkAllowItems(List<TagKey<Item>> allowTags, ItemStack stack) {
        if (allowTags.isEmpty()) {
            return ValidationResult.ok();
        }
        for (TagKey<Item> tag : allowTags) {
            if (stack.is(tag)) {
                return ValidationResult.ok();
            }
        }
        return ValidationResult.fail("spells_n_gods.offering.not_allowed");
    }

    /**
     * Enforce the {@code requires_*} flags present in {@code rules} against the player's tracked action
     * timestamps. Returns the first failure, or {@link ValidationResult#ok()} when all are satisfied (or
     * none are required).
     */
    public static ValidationResult checkActionRequirements(JsonObject rules, Player player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        long now = System.currentTimeMillis();

        if (flag(rules, "requires_replant") && !recent(data.getLastReplantMs(), now)) {
            return ValidationResult.fail("spells_n_gods.offering.requires_replant");
        }
        if (flag(rules, "requires_trade_cycle") && !recent(data.getLastTradeCycleMs(), now)) {
            return ValidationResult.fail("spells_n_gods.offering.requires_trade_cycle");
        }
        if (flag(rules, "requires_risk") && !recent(data.getLastRiskMs(), now)) {
            return ValidationResult.fail("spells_n_gods.offering.requires_risk");
        }
        if ((flag(rules, "requires_kill_credit") || flag(rules, "requires_deathbound"))
                && !recent(data.getLastKillCreditMs(), now)) {
            return ValidationResult.fail("spells_n_gods.offering.requires_kill_credit");
        }
        if (flag(rules, "requires_mana_spend")) {
            // Mana is an Iron's Spellbooks concept, fed via ActionTracker.recordManaSpend(...). Only
            // enforce recency once a mana-spend has actually been recorded (i.e. a bridge is active);
            // with no bridge the timestamp stays 0 and the requirement is treated as unenforceable so the
            // companion allow_items ("scrolls") path remains the intended route.
            if (ModCompatHandler.IRONS_SPELLS_LOADED
                    && data.getLastManaSpendMs() > 0
                    && !recent(data.getLastManaSpendMs(), now)) {
                return ValidationResult.fail("spells_n_gods.offering.requires_mana_spend");
            }
        }
        if (flag(rules, "requires_timer")) {
            int minSeconds = rules.has("min_seconds") ? rules.get("min_seconds").getAsInt() : 0;
            long elapsed = now - data.getLastOfferingEpochMs();
            // First-ever offering (no prior timestamp) is exempt; otherwise require the configured gap.
            if (data.getLastOfferingEpochMs() > 0 && elapsed < minSeconds * 1000L) {
                return ValidationResult.fail("spells_n_gods.offering.requires_timer", minSeconds);
            }
        }
        return ValidationResult.ok();
    }

    private static boolean flag(JsonObject rules, String key) {
        return rules.has(key) && rules.get(key).getAsBoolean();
    }

    private static boolean recent(long timestampMs, long now) {
        return timestampMs > 0 && (now - timestampMs) <= ACTION_WINDOW_MS;
    }
}
