package com.otectus.spells_n_gods.compat;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraftforge.fml.ModList;

/**
 * Handles optional mod compatibility integrations.
 * Checks if mods are loaded before attempting to use their APIs.
 */
public class ModCompatHandler {

    public static final boolean JADE_LOADED = ModList.get().isLoaded("jade");
    public static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");
    public static final boolean JEI_LOADED = ModList.get().isLoaded("jei");
    public static final boolean ORIGINS_LOADED = ModList.get().isLoaded("origins");
    public static final boolean APOTHEOSIS_LOADED = ModList.get().isLoaded("apotheosis");
    public static final boolean KUBEJS_LOADED = ModList.get().isLoaded("kubejs");
    public static final boolean IRONS_SPELLS_LOADED = ModList.get().isLoaded("irons_spellbooks");
    public static final boolean SIMPLY_SWORDS_LOADED = ModList.get().isLoaded("simplyswords");

    public static void init() {
        if (JADE_LOADED) {
            SpellsNGodsMod.LOGGER.info("Jade detected - monument tooltips enabled");
        }
        if (CURIOS_LOADED) {
            SpellsNGodsMod.LOGGER.info("Curios detected - divine trinket support enabled");
        }
        if (JEI_LOADED) {
            SpellsNGodsMod.LOGGER.info("JEI detected - recipe integration enabled");
        }
        if (ORIGINS_LOADED) {
            SpellsNGodsMod.LOGGER.info("Origins detected - power compatibility enabled");
        }
        if (APOTHEOSIS_LOADED) {
            SpellsNGodsMod.LOGGER.info("Apotheosis detected - attribute compatibility enabled");
        }
        if (KUBEJS_LOADED) {
            SpellsNGodsMod.LOGGER.info("KubeJS detected - scripting bindings enabled");
        }
        if (IRONS_SPELLS_LOADED) {
            SpellsNGodsMod.LOGGER.info("Iron's Spells n' Spellbooks detected - spellcasting integration enabled");
        }
        if (SIMPLY_SWORDS_LOADED) {
            SpellsNGodsMod.LOGGER.info("SimplySwords detected - boss weapon integration enabled");
        }
    }
}
