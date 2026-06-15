package com.otectus.spells_n_gods.spawning.logic;

import com.otectus.spells_n_gods.spawning.StructureTier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The structure-spawning configuration for a single deity, distilled from its JSON
 * {@code structure} block into a pure-data form (no Minecraft dependencies).
 *
 * @param deityId        fully-qualified deity id, e.g. {@code "spells_n_gods:deus"}
 * @param defaultTier    tier applied to structures matched only via domain tags
 * @param disableDefault if {@code true}, the deity never spawns via domain tags / any-structure;
 *                       only its explicit whitelist counts
 * @param domainTags     structure tag ids this deity opts into (e.g. {@code "#spells_n_gods:wisdom_structures"})
 * @param whitelistWeights explicit per-structure weights keyed by structure id
 *                         (e.g. {@code "minecraft:stronghold" -> 25})
 */
public record DeityStructureProfile(
        String deityId,
        StructureTier defaultTier,
        boolean disableDefault,
        List<String> domainTags,
        Map<String, Integer> whitelistWeights
) {
    /** Default weight used for tag-matched structures that have no explicit whitelist entry. */
    public static final int DEFAULT_TAG_WEIGHT = 10;

    public DeityStructureProfile {
        domainTags = domainTags == null ? List.of() : List.copyOf(domainTags);
        // Preserve insertion order (nice for deterministic logs) and copy defensively.
        Map<String, Integer> copy = new LinkedHashMap<>();
        if (whitelistWeights != null) {
            copy.putAll(whitelistWeights);
        }
        whitelistWeights = Map.copyOf(copy);
    }

    public boolean hasWhitelistEntry(String structureId) {
        return whitelistWeights.containsKey(structureId);
    }

    /**
     * Resolve this deity's selection weight for a structure.
     *
     * @param structureId  the structure being evaluated
     * @param matchesTag   whether the structure matched one of this deity's domain tags
     * @return the weight, or {@code 0} if this deity does not apply to the structure at all
     */
    public int weightFor(String structureId, boolean matchesTag) {
        Integer explicit = whitelistWeights.get(structureId);
        if (explicit != null) {
            return Math.max(0, explicit);
        }
        if (matchesTag && !disableDefault) {
            return DEFAULT_TAG_WEIGHT;
        }
        return 0;
    }

    /** Whether this deity applies to a structure given tag-match status and the any-structure toggle. */
    public boolean appliesTo(String structureId, boolean matchesTag, boolean allowAnyStructure) {
        if (hasWhitelistEntry(structureId)) {
            return whitelistWeights.get(structureId) > 0;
        }
        if (disableDefault) {
            return false;
        }
        return matchesTag || allowAnyStructure;
    }
}
