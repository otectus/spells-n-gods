package com.otectus.spells_n_gods.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders a prayer progress bar on the HUD when the player is actively praying.
 * Shows a centered bar with the remaining time.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID, value = Dist.CLIENT)
public class PrayerHudOverlay {

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_Y_OFFSET = 32;

    // Color constants (ARGB)
    private static final int BG_COLOR = 0xAA000000;
    private static final int BORDER_COLOR = 0xFF333333;
    // Default gold fallback
    private static final int DEFAULT_FILL_START = 0xFFCCA832;
    private static final int DEFAULT_FILL_END = 0xFFFFD700;
    private static final int DEFAULT_TEXT_COLOR = 0xFFD4AF37;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.EXPERIENCE_BAR.type()) return;
        if (!ClientPrayerHandler.isPraying()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float progress = ClientPrayerHandler.getProgress();
        int remainingSeconds = ClientPrayerHandler.getRemainingSeconds();

        int barX = (screenWidth - BAR_WIDTH) / 2;
        int barY = screenHeight - BAR_Y_OFFSET;

        // Border
        gui.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, BORDER_COLOR);

        // Background
        gui.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BG_COLOR);

        // Resolve school color for the player's bound god
        int fillStart = DEFAULT_FILL_START;
        int fillEnd = DEFAULT_FILL_END;
        int textColor = DEFAULT_TEXT_COLOR;
        if (mc.player != null) {
            mc.player.getCapability(PlayerDivinityCapability.DIVINITY).ifPresent(data -> {});
            String godId = mc.player.getPersistentData().contains("spells_n_gods:scar_count")
                    ? null : null; // Use capability instead
        }
        // Try to get school color from client-side capability
        var schoolColor = getPlayerSchoolColor(mc);
        if (schoolColor != null) {
            int r = (int)(schoolColor.x() * 255);
            int g = (int)(schoolColor.y() * 255);
            int b = (int)(schoolColor.z() * 255);
            // Darker variant for start, brighter for end
            fillStart = 0xFF000000 | (Math.max(0, r - 40) << 16) | (Math.max(0, g - 40) << 8) | Math.max(0, b - 40);
            fillEnd = 0xFF000000 | (Math.min(255, r + 20) << 16) | (Math.min(255, g + 20) << 8) | Math.min(255, b + 20);
            textColor = fillEnd;
        }

        // Fill bar with smooth interpolation
        float smoothProgress = Mth.clamp(progress, 0.0f, 1.0f);
        int fillWidth = (int) (BAR_WIDTH * smoothProgress);
        final int finalFillStart = fillStart;
        final int finalFillEnd = fillEnd;
        if (fillWidth > 0) {
            // Lerp color based on progress
            int color = lerpColor(finalFillStart, finalFillEnd, smoothProgress);
            gui.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, color);

            // Shimmer effect on the leading edge
            if (fillWidth > 2 && fillWidth < BAR_WIDTH) {
                long time = System.currentTimeMillis();
                float shimmer = (float) (Math.sin(time * 0.005) * 0.5 + 0.5);
                int shimmerAlpha = (int) (shimmer * 80);
                int shimmerColor = (shimmerAlpha << 24) | 0xFFFFFF;
                gui.fill(barX + fillWidth - 2, barY, barX + fillWidth, barY + BAR_HEIGHT, shimmerColor);
            }
        }

        // Text: remaining time
        String timeText;
        if (remainingSeconds > 0) {
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            if (minutes > 0) {
                timeText = String.format("Praying... %d:%02d", minutes, seconds);
            } else {
                timeText = String.format("Praying... %ds", seconds);
            }
        } else {
            timeText = "Prayer complete";
        }

        int textWidth = mc.font.width(timeText);
        int textX = (screenWidth - textWidth) / 2;
        int textY = barY - 10;

        gui.drawString(mc.font, timeText, textX, textY, textColor, true);
    }

    private static Vector3f getPlayerSchoolColor(Minecraft mc) {
        if (mc.player == null) return null;
        return mc.player.getCapability(PlayerDivinityCapability.DIVINITY).map(data -> {
            String godId = data.getChosenGodId();
            if (godId == null || godId.isEmpty()) return null;
            GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(godId));
            if (god == null) return null;
            return SchoolColors.getSchoolColor(god.magicSchool());
        }).orElse(null);
    }

    private static int lerpColor(int from, int to, float t) {
        int fa = (from >> 24) & 0xFF;
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;

        int ta = (to >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;

        int a = (int) Mth.lerp(t, fa, ta);
        int r = (int) Mth.lerp(t, fr, tr);
        int g = (int) Mth.lerp(t, fg, tg);
        int b = (int) Mth.lerp(t, fb, tb);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
