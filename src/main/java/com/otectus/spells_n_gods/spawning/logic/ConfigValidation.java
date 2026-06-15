package com.otectus.spells_n_gods.spawning.logic;

import com.otectus.spells_n_gods.spawning.StructureTier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure validation helpers for the structure-spawn configuration. Each method returns a list
 * of human-readable warnings rather than throwing, so invalid config <em>warns but never
 * crashes</em> (a hard requirement of the system).
 *
 * <p>No Minecraft dependencies — id syntax is validated structurally (the resource-system
 * loaders perform the authoritative existence checks at runtime).
 */
public final class ConfigValidation {
    private ConfigValidation() {
    }

    /** Loose structural check for a {@code namespace:path} resource id. */
    public static boolean isValidResourceId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            return false;
        }
        String namespace = id.substring(0, colon);
        String path = id.substring(colon + 1);
        return namespace.matches("[a-z0-9_.-]+") && path.matches("[a-z0-9/._-]+");
    }

    /** A structure tag reference is a resource id prefixed with {@code #}. */
    public static boolean isValidTagReference(String tag) {
        return tag != null && tag.startsWith("#") && isValidResourceId(tag.substring(1));
    }

    /**
     * Validate one deity's structure profile, collecting warnings for malformed ids/tags,
     * negative weights, and unknown deity references.
     *
     * @param knownDeityIds set of loaded deity ids (for the disable/whitelist sanity check),
     *                      or {@code null} to skip that cross-check
     */
    public static List<String> validateProfile(DeityStructureProfile profile, Set<String> knownDeityIds) {
        List<String> warnings = new ArrayList<>();
        if (profile == null) {
            return warnings;
        }
        if (knownDeityIds != null && !knownDeityIds.contains(profile.deityId())) {
            warnings.add("Unknown deity id '" + profile.deityId() + "' referenced by a structure profile.");
        }
        for (String tag : profile.domainTags()) {
            if (!isValidTagReference(tag)) {
                warnings.add("Deity '" + profile.deityId() + "' has malformed domain tag '" + tag
                        + "' (expected '#namespace:path').");
            }
        }
        for (Map.Entry<String, Integer> e : profile.whitelistWeights().entrySet()) {
            if (!isValidResourceId(e.getKey())) {
                warnings.add("Deity '" + profile.deityId() + "' whitelist has malformed structure id '"
                        + e.getKey() + "'.");
            }
            if (e.getValue() == null || e.getValue() < 0) {
                warnings.add("Deity '" + profile.deityId() + "' whitelist weight for '" + e.getKey()
                        + "' is negative; treating as 0 (never spawns).");
            }
        }
        if (profile.disableDefault() && profile.whitelistWeights().isEmpty()) {
            warnings.add("Deity '" + profile.deityId() + "' sets disable_default=true but has no whitelist; "
                    + "it will never spawn at any structure.");
        }
        return warnings;
    }

    /** Validate one tier's settings. */
    public static List<String> validateTier(TierSettings settings) {
        List<String> warnings = new ArrayList<>();
        if (settings == null) {
            return warnings;
        }
        String t = settings.tier().configKey();
        if (settings.baseChance() <= 0.0) {
            warnings.add("Tier '" + t + "' base_chance is " + settings.baseChance()
                    + "; deities will never spawn at this tier.");
        }
        if (settings.cooldownDays() < TierSettings.PERMANENT_COOLDOWN) {
            warnings.add("Tier '" + t + "' cooldown_days " + settings.cooldownDays()
                    + " is invalid; use -1 for permanent or a non-negative number.");
        }
        if (settings.maxActiveDeities() < 1) {
            warnings.add("Tier '" + t + "' max_active_deities < 1; clamped to 1.");
        }
        if (!settings.requiredAdvancement().isBlank() && !isValidResourceId(settings.requiredAdvancement())) {
            warnings.add("Tier '" + t + "' required_advancement '" + settings.requiredAdvancement()
                    + "' is not a valid id; gate will fail safe (block spawn).");
        }
        if (!settings.requiredDimension().isBlank() && !isValidResourceId(settings.requiredDimension())) {
            warnings.add("Tier '" + t + "' required_dimension '" + settings.requiredDimension()
                    + "' is not a valid id; gate will fail safe (block spawn).");
        }
        return warnings;
    }

    /** Validate a per-structure {@code id=tier} override mapping string. */
    public static List<String> validateOverride(String raw) {
        List<String> warnings = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return warnings;
        }
        int eq = raw.indexOf('=');
        if (eq <= 0) {
            warnings.add("Structure tier override '" + raw + "' is malformed (expected 'namespace:id=tier').");
            return warnings;
        }
        String id = raw.substring(0, eq).trim();
        String tier = raw.substring(eq + 1).trim();
        if (!isValidResourceId(id) && !isValidTagReference(id)) {
            warnings.add("Structure tier override id '" + id + "' is malformed.");
        }
        if (!StructureTier.isValid(tier)) {
            warnings.add("Structure tier override '" + raw + "' has unknown tier '" + tier
                    + "' (expected common/uncommon/rare/legendary).");
        }
        return warnings;
    }
}
