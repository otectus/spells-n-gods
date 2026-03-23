package com.otectus.spells_n_gods.boss;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GodBossModel extends GeoModel<GodBossEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(SpellsNGodsMod.MODID, "geo/god_boss.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(SpellsNGodsMod.MODID, "animations/god_boss.animation.json");
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(SpellsNGodsMod.MODID, "textures/entity/boss_default.png");
    private boolean loggedTexture = false;

    @Override
    public ResourceLocation getModelResource(GodBossEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GodBossEntity entity) {
        ResourceLocation tex = resolveTexture(entity);

        if (!loggedTexture) {
            loggedTexture = true;
            SpellsNGodsMod.LOGGER.info("[BossModel] Resolved texture for '{}': {}", entity.getGodId(), tex);
        }

        return tex;
    }

    private ResourceLocation resolveTexture(GodBossEntity entity) {
        // When enraged, try to use an enraged texture variant
        if (entity.isEnraged()) {
            ResourceLocation enragedTex = getEnragedTexture(entity);
            if (enragedTex != null) {
                return enragedTex;
            }
        }

        GodDefinition god = entity.getGodDefinition();
        if (god != null && !god.boss().skinTexture().isEmpty()) {
            return new ResourceLocation(god.boss().skinTexture());
        }

        // Fallback: derive from god ID
        String godId = entity.getGodId();
        if (!godId.isEmpty()) {
            ResourceLocation rl = new ResourceLocation(godId);
            return new ResourceLocation(rl.getNamespace(), "textures/entity/boss_" + rl.getPath() + ".png");
        }

        return DEFAULT_TEXTURE;
    }

    private ResourceLocation getEnragedTexture(GodBossEntity entity) {
        GodDefinition god = entity.getGodDefinition();

        // Only use an enraged texture if explicitly specified in the god definition
        if (god != null && !god.boss().enragedSkinTexture().isEmpty()) {
            return new ResourceLocation(god.boss().enragedSkinTexture());
        }

        // No enraged texture defined — return null to fall through to the normal texture.
        // The renderer's actuallyRender() applies a red tint for the enrage visual effect.
        return null;
    }

    @Override
    public RenderType getRenderType(GodBossEntity entity, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public ResourceLocation getAnimationResource(GodBossEntity entity) {
        return ANIMATION;
    }
}
