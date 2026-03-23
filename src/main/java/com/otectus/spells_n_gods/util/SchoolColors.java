package com.otectus.spells_n_gods.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Centralized school-to-color and school-to-particle mappings.
 * Used by SpellcastingGoal, BossSpawnAnimationHandler, GodBossEntity, and render layers.
 */
public final class SchoolColors {

    private SchoolColors() {}

    private static final Vector3f COLOR_HOLY      = new Vector3f(1.0f, 0.85f, 0.2f);
    private static final Vector3f COLOR_FIRE      = new Vector3f(1.0f, 0.3f, 0.0f);
    private static final Vector3f COLOR_ICE       = new Vector3f(0.4f, 0.7f, 1.0f);
    private static final Vector3f COLOR_LIGHTNING  = new Vector3f(0.6f, 0.8f, 1.0f);
    private static final Vector3f COLOR_BLOOD     = new Vector3f(0.7f, 0.0f, 0.0f);
    private static final Vector3f COLOR_NATURE    = new Vector3f(0.2f, 0.8f, 0.2f);
    private static final Vector3f COLOR_ENDER     = new Vector3f(0.5f, 0.0f, 0.8f);
    private static final Vector3f COLOR_ELDRITCH  = new Vector3f(0.2f, 0.4f, 0.5f);
    private static final Vector3f COLOR_EVOCATION = new Vector3f(0.9f, 0.9f, 0.9f);
    private static final Vector3f COLOR_DEFAULT   = new Vector3f(0.8f, 0.6f, 1.0f);

    private static final Map<String, Vector3f> COLOR_MAP = Map.of(
            "holy", COLOR_HOLY,
            "fire", COLOR_FIRE,
            "ice", COLOR_ICE,
            "lightning", COLOR_LIGHTNING,
            "blood", COLOR_BLOOD,
            "nature", COLOR_NATURE,
            "ender", COLOR_ENDER,
            "eldritch", COLOR_ELDRITCH,
            "evocation", COLOR_EVOCATION
    );

    /**
     * Returns the RGB color for a given magic school as a Vector3f (0.0-1.0 per channel).
     * The returned vector is shared — callers must NOT mutate it.
     */
    public static Vector3f getSchoolColor(String school) {
        return COLOR_MAP.getOrDefault(school.toLowerCase(), COLOR_DEFAULT);
    }

    /**
     * Returns the ARGB packed color for a given school (alpha = 0xFF).
     */
    public static int getSchoolColorARGB(String school) {
        Vector3f c = getSchoolColor(school);
        int r = (int) (c.x * 255);
        int g = (int) (c.y * 255);
        int b = (int) (c.z * 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * Returns the school-themed ambient particle type.
     */
    public static ParticleOptions getSchoolParticle(String school) {
        return switch (school.toLowerCase()) {
            case "fire"      -> ParticleTypes.FLAME;
            case "ice"       -> ParticleTypes.SNOWFLAKE;
            case "lightning"  -> ParticleTypes.ELECTRIC_SPARK;
            case "blood"     -> ParticleTypes.DAMAGE_INDICATOR;
            case "nature"    -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case "ender"     -> ParticleTypes.REVERSE_PORTAL;
            case "eldritch"  -> ParticleTypes.SCULK_SOUL;
            case "evocation" -> ParticleTypes.WITCH;
            case "holy"      -> ParticleTypes.END_ROD;
            default          -> ParticleTypes.ENCHANT;
        };
    }
}
