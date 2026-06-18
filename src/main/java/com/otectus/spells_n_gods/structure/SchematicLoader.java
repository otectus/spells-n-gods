package com.otectus.spells_n_gods.structure;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.config.ShrineConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads custom .nbt schematics from config/spells_n_gods/schematics/ and maps them
 * to god IDs. Schematics use the naming convention: {god_id}.nbt
 * (e.g., deus.nbt maps to spells_n_gods:deus).
 *
 * Initialized on server start, cleared on server stop.
 */
public final class SchematicLoader {

    private static final Map<String, StructureTemplate> CACHE = new HashMap<>();

    private SchematicLoader() {
    }

    /**
     * Scans the schematics folder and loads all .nbt files into StructureTemplate objects.
     * Called during ServerStartingEvent.
     */
    public static void init(MinecraftServer server) {
        CACHE.clear();

        if (!ShrineConfig.INSTANCE.useCustomSchematics.get()) {
            SpellsNGodsMod.LOGGER.debug("[SpellsNGods] Custom schematics disabled — skipping schematic load");
            return;
        }

        String folderName = ShrineConfig.INSTANCE.schematicFolder.get();
        // Resolve under the mod's own config dir (config/spells_n_gods/), matching where the mod's
        // config files are actually registered — not the legacy "runic_gods" path.
        Path schematicsDir = FMLPaths.CONFIGDIR.get().resolve(SpellsNGodsMod.MODID).resolve(folderName);

        if (!Files.isDirectory(schematicsDir)) {
            SpellsNGodsMod.LOGGER.info("[SpellsNGods] Schematics directory not found: {}", schematicsDir);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(schematicsDir, "*.nbt")) {
            for (Path nbtFile : stream) {
                String fileName = nbtFile.getFileName().toString();
                // Strip .nbt extension to get the god ID
                String rawId = fileName.substring(0, fileName.length() - 4);
                // Convert to full resource location format:
                //   deus.nbt → spells_n_gods:deus
                //   mygods_zeus.nbt → mygods:zeus (underscore before first _ becomes namespace)
                String godId = toGodId(rawId);

                try (InputStream is = Files.newInputStream(nbtFile)) {
                    CompoundTag tag = NbtIo.readCompressed(is);
                    StructureTemplate template = new StructureTemplate();
                    template.load(server.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
                    CACHE.put(godId, template);
                    SpellsNGodsMod.LOGGER.info("[SpellsNGods] Loaded schematic '{}' for god '{}'", fileName, godId);
                } catch (IOException e) {
                    SpellsNGodsMod.LOGGER.error("[SpellsNGods] Failed to load schematic '{}': {}", fileName, e.getMessage());
                }
            }
        } catch (IOException e) {
            SpellsNGodsMod.LOGGER.error("[SpellsNGods] Failed to scan schematics directory: {}", e.getMessage());
        }

        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Loaded {} custom schematic(s)", CACHE.size());
    }

    /**
     * Clears the schematic cache. Called during ServerStoppingEvent.
     */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * Returns the custom schematic for a god, if one was loaded.
     *
     * @param godId the full god ID (e.g., "spells_n_gods:deus")
     * @return the loaded StructureTemplate, or empty if no custom schematic exists
     */
    public static Optional<StructureTemplate> getSchematic(String godId) {
        return Optional.ofNullable(CACHE.get(godId));
    }

    /**
     * Converts a schematic filename (without extension) to a god ID.
     * If the name contains an underscore, the part before the first underscore
     * is the namespace: "mygods_zeus" → "mygods:zeus".
     * Otherwise, defaults to "spells_n_gods:" namespace: "deus" → "spells_n_gods:deus".
     */
    private static String toGodId(String rawName) {
        int underscoreIdx = rawName.indexOf('_');
        if (underscoreIdx > 0 && underscoreIdx < rawName.length() - 1) {
            // Check if this is a known spells_n_gods god name — if so, don't split
            if (isKnownGod(rawName)) {
                return "spells_n_gods:" + rawName;
            }
            String namespace = rawName.substring(0, underscoreIdx);
            String path = rawName.substring(underscoreIdx + 1);
            return namespace + ":" + path;
        }
        return "spells_n_gods:" + rawName;
    }

    private static boolean isKnownGod(String name) {
        return switch (name) {
            case "deus", "velox", "celia", "bella", "bricoleur",
                 "ingenium", "venatas", "magnus", "glacia" -> true;
            default -> false;
        };
    }
}
