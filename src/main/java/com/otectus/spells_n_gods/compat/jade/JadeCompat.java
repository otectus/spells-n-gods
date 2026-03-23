package com.otectus.spells_n_gods.compat.jade;

import com.otectus.spells_n_gods.SpellsNGodsMod;

/**
 * Jade/WAILA integration for displaying monument and ruined idol information.
 * <p>
 * NOTE: Actual Jade API integration is disabled until the Jade compile dependency
 * is available. When re-enabled, this class should implement IWailaPlugin and
 * register block component providers for MonumentBlock and RuinedIdolBlock.
 */
public class JadeCompat {

    public static void init() {
        SpellsNGodsMod.LOGGER.info("Jade compat stub loaded - full integration pending Jade API dependency");
    }
}
