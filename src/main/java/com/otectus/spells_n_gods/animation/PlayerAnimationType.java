package com.otectus.spells_n_gods.animation;

import net.minecraft.resources.ResourceLocation;

public enum PlayerAnimationType {
    PRAYER_KNEEL("prayer_kneel", true),
    BINDING_CEREMONY("binding_ceremony", false),
    OFFERING_PRESENT("offering_present", false),
    TIER_UP_CELEBRATE("tier_up_celebrate", false),
    APOSTASY_STRUGGLE("apostasy_struggle", false),
    CAST_FIRE("cast_fire", false),
    CAST_ICE("cast_ice", false),
    CAST_LIGHTNING("cast_lightning", false),
    CAST_HOLY("cast_holy", false),
    CAST_DARK("cast_dark", false),
    CAST_NATURE("cast_nature", false);

    private final String animationId;
    private final boolean looping;

    PlayerAnimationType(String animationId, boolean looping) {
        this.animationId = animationId;
        this.looping = looping;
    }

    public String getAnimationId() {
        return animationId;
    }

    public boolean isLooping() {
        return looping;
    }

    public ResourceLocation getResourceLocation() {
        return new ResourceLocation("spells_n_gods", "animation/player/" + animationId + ".json");
    }
}
