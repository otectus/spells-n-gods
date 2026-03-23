package com.otectus.spells_n_gods.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.network.BossVisualEffectPacket;
import com.otectus.spells_n_gods.util.SchoolColors;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side screen effects for boss encounters:
 * - Camera shake on heavy hits / leap landings
 * - Red vignette during enrage flash
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID, value = Dist.CLIENT)
public class BossFightScreenEffects {

    private static int shakeTicks = 0;
    private static int shakeStartTicks = 0;
    private static float shakeIntensity = 0.0f;
    private static int vignetteFlashTicks = 0;
    private static float vignetteRed = 1.0f;
    private static float vignetteGreen = 0.0f;
    private static float vignetteBlue = 0.0f;

    public static void handleEffect(BossVisualEffectPacket.EffectType type,
                                     double x, double y, double z) {
        handleEffect(type, x, y, z, "");
    }

    public static void handleEffect(BossVisualEffectPacket.EffectType type,
                                     double x, double y, double z, String school) {
        if (!SpellsNGodsConfig.COMMON.bossScreenEffectsEnabled.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Attenuate by distance
        double distSq = mc.player.distanceToSqr(x, y, z);
        if (distSq > 2500) return; // 50 blocks max

        float distanceFactor = 1.0f - (float) Math.sqrt(distSq) / 50.0f;

        switch (type) {
            case CAMERA_SHAKE_LIGHT -> {
                shakeTicks = 8;
                shakeStartTicks = 8;
                shakeIntensity = 1.5f * distanceFactor;
            }
            case CAMERA_SHAKE_HEAVY -> {
                shakeTicks = 15;
                shakeStartTicks = 15;
                shakeIntensity = 4.0f * distanceFactor;
            }
            case ENRAGE_FLASH -> {
                vignetteFlashTicks = 20;
                // Use school color for god-themed enrage flash
                if (!school.isEmpty()) {
                    Vector3f color = SchoolColors.getSchoolColor(school);
                    // Brighten the school color for dramatic flash
                    vignetteRed = Math.min(1.0f, color.x() * 1.3f);
                    vignetteGreen = Math.min(1.0f, color.y() * 1.3f);
                    vignetteBlue = Math.min(1.0f, color.z() * 1.3f);
                } else {
                    vignetteRed = 0.8f;
                    vignetteGreen = 0.1f;
                    vignetteBlue = 0.1f;
                }
            }
            case DEATH_FLASH -> {
                vignetteFlashTicks = 30;
                // Use desaturated school color for death flash
                if (!school.isEmpty()) {
                    Vector3f color = SchoolColors.getSchoolColor(school);
                    // Desaturate toward gold-white
                    vignetteRed = color.x() * 0.5f + 0.5f;
                    vignetteGreen = color.y() * 0.5f + 0.42f;
                    vignetteBlue = color.z() * 0.3f + 0.15f;
                } else {
                    vignetteRed = 1.0f;
                    vignetteGreen = 0.85f;
                    vignetteBlue = 0.3f;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0 || !SpellsNGodsConfig.COMMON.bossScreenEffectsEnabled.get()) return;

        shakeTicks--;
        float progress = shakeStartTicks > 0 ? (float) shakeTicks / shakeStartTicks : 0f;
        float intensity = shakeIntensity * progress;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float shakeX = (float) (Math.sin(shakeTicks * 1.7) * intensity);
        float shakeY = (float) (Math.cos(shakeTicks * 2.3) * intensity * 0.5);

        event.setPitch(event.getPitch() + shakeX);
        event.setYaw(event.getYaw() + shakeY);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (vignetteFlashTicks <= 0 || !SpellsNGodsConfig.COMMON.bossScreenEffectsEnabled.get()) return;

        vignetteFlashTicks--;
        float alpha = Mth.clamp((float) vignetteFlashTicks / 20.0f * 0.3f, 0.0f, 0.3f);

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Draw a colored vignette overlay
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(0, h, 0).color(vignetteRed, vignetteGreen, vignetteBlue, alpha).endVertex();
        builder.vertex(w, h, 0).color(vignetteRed, vignetteGreen, vignetteBlue, alpha).endVertex();
        builder.vertex(w, 0, 0).color(vignetteRed, vignetteGreen, vignetteBlue, 0.0f).endVertex();
        builder.vertex(0, 0, 0).color(vignetteRed, vignetteGreen, vignetteBlue, 0.0f).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.disableBlend();
    }
}
