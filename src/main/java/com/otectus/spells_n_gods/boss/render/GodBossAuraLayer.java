package com.otectus.spells_n_gods.boss.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * Renders a slightly enlarged, translucent, school-colored version of the boss model
 * as a divine aura effect. Active during combat/enraged phases within 32 blocks.
 */
public class GodBossAuraLayer extends GeoRenderLayer<GodBossEntity> {

    private static final float AURA_SCALE = 1.05f;
    private static final float BASE_ALPHA = 0.15f;
    private static final float ENRAGED_ALPHA = 0.3f;
    private static final double MAX_RENDER_DIST_SQ = 32.0 * 32.0;

    public GodBossAuraLayer(GeoRenderer<GodBossEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, GodBossEntity entity, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {

        // Only render during combat/enraged
        if (!entity.isEnraged() && !entity.getCurrentPhase().isInCombat()) {
            return;
        }

        // Distance check: only within 32 blocks
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            double distSq = entity.distanceToSqr(net.minecraft.client.Minecraft.getInstance().player);
            if (distSq > MAX_RENDER_DIST_SQ) return;
        }

        GodDefinition god = entity.getGodDefinition();
        String school = god != null ? god.magicSchool() : "";
        Vector3f color = SchoolColors.getSchoolColor(school);

        // Pulsing alpha
        boolean enraged = entity.isEnraged();
        float pulseSpeed = enraged ? 0.25f : 0.1f;
        float baseAlpha = enraged ? ENRAGED_ALPHA : BASE_ALPHA;
        float alpha = baseAlpha + (float) Math.sin((entity.tickCount + partialTick) * pulseSpeed) * 0.08f;
        alpha = Math.max(0.05f, Math.min(alpha, 0.5f));

        ResourceLocation texture = getRenderer().getTextureLocation(entity);
        RenderType auraType = RenderType.entityTranslucent(texture);
        VertexConsumer auraBuffer = bufferSource.getBuffer(auraType);

        poseStack.pushPose();
        poseStack.scale(AURA_SCALE, AURA_SCALE, AURA_SCALE);
        // Offset to keep the model centered after scaling
        poseStack.translate(0, -0.025, 0);

        getRenderer().actuallyRender(poseStack, entity, bakedModel, auraType,
                bufferSource, auraBuffer, true, partialTick,
                packedLight, OverlayTexture.NO_OVERLAY,
                color.x, color.y, color.z, alpha);

        poseStack.popPose();
    }
}
