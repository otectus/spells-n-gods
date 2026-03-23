package com.otectus.spells_n_gods.boss.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders a translucent school-colored sphere when the boss has its shield active.
 * Uses a separate shield_bubble geo model overlaid on the boss.
 */
public class GodBossShieldLayer extends GeoRenderLayer<GodBossEntity> {

    private static final ResourceLocation SHIELD_TEXTURE =
            new ResourceLocation(SpellsNGodsMod.MODID, "textures/entity/shield_bubble.png");
    private static final float SHIELD_SCALE = 2.5f;

    public GodBossShieldLayer(GeoRenderer<GodBossEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, GodBossEntity entity, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {

        if (!entity.isShielding()) return;

        GodDefinition god = entity.getGodDefinition();
        String school = god != null ? god.magicSchool() : "";
        Vector3f color = SchoolColors.getSchoolColor(school);

        // Pulsing alpha for shield effect
        float alpha = 0.25f + (float) Math.sin((entity.tickCount + partialTick) * 0.2) * 0.1f;

        RenderType shieldType = RenderType.entityTranslucent(SHIELD_TEXTURE);
        VertexConsumer shieldBuffer = bufferSource.getBuffer(shieldType);

        poseStack.pushPose();
        // Center the shield sphere on the boss
        poseStack.translate(0, 1.0, 0);
        poseStack.scale(SHIELD_SCALE, SHIELD_SCALE, SHIELD_SCALE);

        // Render the boss model itself as the shield shape (slightly scaled up)
        // This creates a protective shell effect around the boss
        getRenderer().actuallyRender(poseStack, entity, bakedModel, shieldType,
                bufferSource, shieldBuffer, true, partialTick,
                packedLight, OverlayTexture.NO_OVERLAY,
                color.x, color.y, color.z, alpha);

        poseStack.popPose();
    }
}
