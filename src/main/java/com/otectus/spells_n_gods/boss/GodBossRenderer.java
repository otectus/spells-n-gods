package com.otectus.spells_n_gods.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.otectus.spells_n_gods.boss.render.GodBossAuraLayer;
import com.otectus.spells_n_gods.boss.render.GodBossShieldLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import javax.annotation.Nullable;

public class GodBossRenderer extends GeoEntityRenderer<GodBossEntity> {

    private static final float BOSS_SCALE = 1.5F;
    private static final String RIGHT_HAND_BONE = "right_item";
    private static final String LEFT_HAND_BONE = "left_item";

    public GodBossRenderer(EntityRendererProvider.Context context) {
        super(context, new GodBossModel());
        withScale(BOSS_SCALE);
        this.shadowRadius = 0.7F;

        // Emissive glow layer — renders *_glowing.png regions at full brightness
        addRenderLayer(new AutoGlowingGeoLayer<>(this));

        // Aura render layer — translucent school-colored shell during combat
        addRenderLayer(new GodBossAuraLayer(this));

        // Shield bubble layer — visible when boss activates shield ability
        addRenderLayer(new GodBossShieldLayer(this));

        // Held item rendering layer
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable
            @Override
            protected ItemStack getStackForBone(GeoBone bone, GodBossEntity entity) {
                if (bone.getName().equals(RIGHT_HAND_BONE)) {
                    return entity.getItemBySlot(EquipmentSlot.MAINHAND);
                }
                if (bone.getName().equals(LEFT_HAND_BONE)) {
                    return entity.getItemBySlot(EquipmentSlot.OFFHAND);
                }
                return null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, GodBossEntity entity) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                              GodBossEntity entity, MultiBufferSource bufferSource,
                                              float partialTick, int packedLight, int packedOverlay) {
                poseStack.pushPose();

                if (bone.getName().equals(RIGHT_HAND_BONE)) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
                    poseStack.translate(0.0, 0.05, -0.15);
                }

                super.renderStackForBone(poseStack, bone, stack, entity, bufferSource,
                        partialTick, packedLight, packedOverlay);

                poseStack.popPose();
            }
        });
    }

    @Override
    public int getPackedOverlay(GodBossEntity entity, float u) {
        // Amplify hurt overlay flash when damaged
        if (entity.hurtTime > 0) {
            return OverlayTexture.pack(
                    OverlayTexture.u(u), OverlayTexture.v(true));
        }
        return super.getPackedOverlay(entity, u);
    }

    @Override
    public void preRender(PoseStack poseStack, GodBossEntity entity, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {
        // Hide accessory bones that don't belong to this god
        hideAccessoryBones(entity, model);

        super.preRender(poseStack, entity, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, GodBossEntity entity, BakedGeoModel model,
                                net.minecraft.client.renderer.RenderType renderType,
                                MultiBufferSource bufferSource, VertexConsumer buffer,
                                boolean isReRender, float partialTick, int packedLight,
                                int packedOverlay, float red, float green, float blue, float alpha) {
        // Apply enrage red tint with pulsing effect
        if (entity.isEnraged()) {
            float pulse = (float) (0.7 + 0.3 * Math.sin((entity.tickCount + partialTick) * 0.15));
            red = 1.0f;
            green = 0.5f * pulse;
            blue = 0.5f * pulse;
        }

        super.actuallyRender(poseStack, entity, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static final String[] ACCESSORY_BONES = {"crown", "horns", "wings", "cloak", "tail"};

    /**
     * Hides all accessory bones by default, then shows ones specified in the god definition.
     */
    private void hideAccessoryBones(GodBossEntity entity, BakedGeoModel model) {

        for (String boneName : ACCESSORY_BONES) {
            model.getBone(boneName).ifPresent(bone -> bone.setHidden(true));
        }

        // Show bones listed in the god's accessories
        var god = entity.getGodDefinition();
        if (god != null) {
            for (String accessory : god.boss().accessories()) {
                model.getBone(accessory).ifPresent(bone -> bone.setHidden(false));
            }
        }
    }
}
