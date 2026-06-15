package com.otectus.spells_n_gods.spawning;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/**
 * Built-in domain {@link TagKey}s for the deity structure-spawning system. Structures opt into
 * deity spawning by being members of these tags; the tag contents live in datapack JSON under
 * {@code data/spells_n_gods/tags/worldgen/structure/} so that <em>modded structures can opt in
 * without any code changes</em>.
 *
 * <p>Deity definitions reference these tag ids directly (e.g. {@code "#spells_n_gods:ocean_structures"}).
 */
public final class DomainStructureTags {
    private DomainStructureTags() {
    }

    public static final TagKey<Structure> OCEAN = create("ocean_structures");
    public static final TagKey<Structure> FORGE = create("forge_structures");
    public static final TagKey<Structure> DEATH = create("death_structures");
    public static final TagKey<Structure> WISDOM = create("wisdom_structures");
    public static final TagKey<Structure> WAR = create("war_structures");
    public static final TagKey<Structure> NATURE = create("nature_structures");
    public static final TagKey<Structure> UNDERWORLD = create("underworld_structures");

    /** Tier hint tags so structures can be categorised by datapack instead of only by config. */
    public static final TagKey<Structure> TIER_COMMON = create("tier/common");
    public static final TagKey<Structure> TIER_UNCOMMON = create("tier/uncommon");
    public static final TagKey<Structure> TIER_RARE = create("tier/rare");
    public static final TagKey<Structure> TIER_LEGENDARY = create("tier/legendary");

    public static final List<TagKey<Structure>> DOMAIN_TAGS = List.of(
            OCEAN, FORGE, DEATH, WISDOM, WAR, NATURE, UNDERWORLD);

    private static TagKey<Structure> create(String path) {
        return TagKey.create(Registries.STRUCTURE, new ResourceLocation(SpellsNGodsMod.MODID, path));
    }

    /** Build a {@link TagKey} from a {@code "#namespace:path"} reference, or {@code null} if malformed. */
    public static TagKey<Structure> fromReference(String tagReference) {
        if (tagReference == null || !tagReference.startsWith("#")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(tagReference.substring(1));
        if (id == null) {
            return null;
        }
        return TagKey.create(Registries.STRUCTURE, id);
    }
}
