package com.otectus.spells_n_gods.spawning;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.config.StructureSpawnConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.spawning.logic.ConfigValidation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates the structure-spawn configuration (deity JSON profiles, tier settings, and
 * per-structure overrides) and logs clear warnings for anything malformed. Validation never
 * throws — invalid config warns but does not crash, per the system's requirements.
 */
public final class StructureSpawnValidator {
    private StructureSpawnValidator() {
    }

    public static void validateAll() {
        Map<?, GodDefinition> gods = SpellsNGodsDataManager.getGods();
        Set<String> knownDeities = new HashSet<>();
        for (GodDefinition god : gods.values()) {
            knownDeities.add(god.id().toString());
        }

        int warnings = 0;

        for (GodDefinition god : gods.values()) {
            String deityId = god.id().toString();
            List<String> profileWarnings = ConfigValidation.validateProfile(
                    god.structure().toProfile(deityId), knownDeities);
            for (String w : profileWarnings) {
                SpellsNGodsMod.LOGGER.warn("[Sng/Spawn][config] {}", w);
                warnings++;
            }
        }

        for (StructureTier tier : StructureTier.values()) {
            List<String> tierWarnings = ConfigValidation.validateTier(StructureSpawnConfig.settings(tier));
            for (String w : tierWarnings) {
                SpellsNGodsMod.LOGGER.warn("[Sng/Spawn][config] {}", w);
                warnings++;
            }
        }

        for (Object raw : StructureSpawnConfig.GENERAL.structureTierOverrides.get()) {
            for (String w : ConfigValidation.validateOverride(String.valueOf(raw))) {
                SpellsNGodsMod.LOGGER.warn("[Sng/Spawn][config] {}", w);
                warnings++;
            }
        }

        if (warnings == 0) {
            SpellsNGodsMod.LOGGER.info("[Sng/Spawn] Structure-spawn config validated: no issues "
                    + "({} deities, allow_any_structure={}).", gods.size(),
                    StructureSpawnConfig.GENERAL.allowAnyStructure.get());
        } else {
            SpellsNGodsMod.LOGGER.warn("[Sng/Spawn] Structure-spawn config validated with {} warning(s).",
                    warnings);
        }
    }
}
