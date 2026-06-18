package com.otectus.spells_n_gods.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Shrine-specific configuration for Spells n' Gods.
 * Saved to config/spells_n_gods/shrines.toml.
 *
 * Controls custom schematic loading and shrine generation behavior.
 * Modpack creators can drop .nbt files into config/spells_n_gods/schematics/
 * using the naming convention: {god_id}.nbt (e.g., deus.nbt, velox.nbt).
 */
public final class ShrineConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ShrineConfig INSTANCE;

    // --- Schematics ---
    public final ForgeConfigSpec.BooleanValue useCustomSchematics;
    public final ForgeConfigSpec.ConfigValue<String> schematicFolder;
    public final ForgeConfigSpec.BooleanValue fallbackToProcedural;

    static {
        var builder = new ForgeConfigSpec.Builder();
        INSTANCE = new ShrineConfig(builder);
        SPEC = builder.build();
    }

    private ShrineConfig(ForgeConfigSpec.Builder builder) {
        builder.push("schematics");
        builder.comment(
                "Custom schematic support for god shrines.",
                "Place .nbt structure files in config/spells_n_gods/schematics/ using the naming convention:",
                "  <god_id>.nbt  (e.g., deus.nbt, velox.nbt, celia.nbt)",
                "For datapack-added gods, use their namespace-stripped ID (e.g., mygods_zeus.nbt for mygods:zeus)."
        );

        useCustomSchematics = builder
                .comment("Master toggle for loading custom schematics from the schematics folder.",
                        "When disabled, all shrines use the default procedural generation.")
                .define("useCustomSchematics", false);

        schematicFolder = builder
                .comment("Subfolder name under config/spells_n_gods/ where .nbt schematics are stored.")
                .define("schematicFolder", "schematics");

        fallbackToProcedural = builder
                .comment("If a god has no matching .nbt schematic, fall back to procedural generation.",
                        "When false, gods without a schematic will not generate a shrine at all.")
                .define("fallbackToProcedural", true);

        builder.pop();
    }

    private ShrineConfig() {
        throw new UnsupportedOperationException();
    }
}
