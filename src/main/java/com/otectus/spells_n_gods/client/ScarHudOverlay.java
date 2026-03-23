package com.otectus.spells_n_gods.client;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders a small scar indicator on the HUD when the player has apostasy scars.
 * Shows a dark icon with scar count in the top-left corner of the screen.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID, value = Dist.CLIENT)
public class ScarHudOverlay {

    private static final int ICON_X = 4;
    private static final int ICON_Y = 4;
    private static final int ICON_SIZE = 12;
    private static final int BG_COLOR = 0x88200820;
    private static final int BORDER_COLOR = 0xAA6B2D6B;
    private static final int TEXT_COLOR = 0xFFCC88CC;
    private static final int TEXT_COLOR_SEVERE = 0xFFFF4444;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        int scarCount = player.getPersistentData().getInt("spells_n_gods:scar_count");
        if (scarCount <= 0) return;

        float healthReduction = player.getPersistentData().getFloat("spells_n_gods:scar_health_reduction");

        GuiGraphics gui = event.getGuiGraphics();
        int x = ICON_X;
        int y = ICON_Y;

        // Background
        gui.fill(x, y, x + ICON_SIZE + 24, y + ICON_SIZE + 4, BG_COLOR);
        // Border
        gui.renderOutline(x, y, ICON_SIZE + 24, ICON_SIZE + 4, BORDER_COLOR);

        // Scar symbol (simple X mark)
        int cx = x + ICON_SIZE / 2 + 1;
        int cy = y + ICON_SIZE / 2 + 2;
        int symbolColor = scarCount >= 3 ? TEXT_COLOR_SEVERE : TEXT_COLOR;
        gui.fill(cx - 3, cy - 1, cx + 3, cy + 1, symbolColor);
        gui.fill(cx - 1, cy - 3, cx + 1, cy + 3, symbolColor);

        // Scar count text
        String countText = "x" + scarCount;
        gui.drawString(mc.font, Component.literal(countText),
                x + ICON_SIZE + 2, y + 3, symbolColor, true);
    }
}
