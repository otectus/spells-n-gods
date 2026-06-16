package com.otectus.spells_n_gods.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Resolves the data-authored {@code particle}/{@code ambient_sound} names on monument definitions to
 * concrete, already-registered {@link ParticleOptions}/{@link SoundEvent}.
 *
 * <p>Resolution order for each: (1) the registry, so a real {@code spells_n_gods:*} asset is honored if
 * one is ever registered; (2) a mapping of the authored flavor names onto fitting vanilla equivalents,
 * so the feature works today with zero new binary assets; (3) a sensible fallback (the school particle /
 * no sound). Custom {@code .ogg}/textures can later replace the vanilla mappings with no code change.
 */
public final class AmbientResolver {

    private AmbientResolver() {}

    /** Resolve an authored particle id to a usable particle, falling back to the school particle. */
    public static ParticleOptions resolveParticle(String id, String school) {
        if (id != null && !id.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                ParticleType<?> registered = BuiltInRegistries.PARTICLE_TYPE.get(rl);
                if (registered instanceof SimpleParticleType simple) {
                    return simple;
                }
                ParticleOptions byName = particleByFlavorName(rl.getPath());
                if (byName != null) {
                    return byName;
                }
            }
        }
        return SchoolColors.getSchoolParticle(school == null ? "" : school);
    }

    private static ParticleOptions particleByFlavorName(String path) {
        return switch (path) {
            case "embers"        -> ParticleTypes.FLAME;
            case "holy_glow"     -> ParticleTypes.END_ROD;
            case "blood_drip"    -> ParticleTypes.DAMAGE_INDICATOR;
            case "snowflake"     -> ParticleTypes.SNOWFLAKE;
            case "fracture"      -> ParticleTypes.REVERSE_PORTAL;
            case "arcane_scrape" -> ParticleTypes.WITCH;
            case "order_sigil"   -> ParticleTypes.ENCHANT;
            case "spark"         -> ParticleTypes.ELECTRIC_SPARK;
            case "spores"        -> ParticleTypes.SPORE_BLOSSOM_AIR;
            default              -> null;
        };
    }

    /** Resolve an authored ambient-sound id to a {@link SoundEvent}, or {@code null} if unmappable. */
    public static SoundEvent resolveSound(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(rl);
        if (registered != null) {
            return registered;
        }
        return switch (rl.getPath()) {
            case "war_drum"       -> SoundEvents.WITHER_AMBIENT;
            case "heartbeat"      -> SoundEvents.WARDEN_HEARTBEAT;
            case "divine_choir"   -> SoundEvents.BEACON_AMBIENT;
            case "frost_wind"     -> SoundEvents.WEATHER_RAIN;
            case "rift_whisper"   -> SoundEvents.PORTAL_AMBIENT;
            case "arcane_hum"     -> SoundEvents.ENCHANTMENT_TABLE_USE;
            case "thunder_rumble" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case "divine_gavel"   -> SoundEvents.ANVIL_LAND;
            case "forest_breath"  -> SoundEvents.AMBIENT_CAVE.value();
            default               -> null;
        };
    }
}
