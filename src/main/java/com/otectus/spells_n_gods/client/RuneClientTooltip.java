package com.otectus.spells_n_gods.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Client-only helper for the Rune's scar tooltip. Kept in the {@code client} package and invoked via
 * {@code DistExecutor} so the common {@code RuneItem} never references the client-only
 * {@code Minecraft}/{@code LocalPlayer} types — referencing them there would make {@code RuneItem}
 * fail to load on a dedicated server (RuntimeDistCleaner: "invalid dist DEDICATED_SERVER").
 */
public final class RuneClientTooltip {

    private RuneClientTooltip() {}

    /** Append the local player's apostasy-scar summary to a rune tooltip (client only). */
    public static void appendScarInfo(List<Component> tooltip) {
        Player holder = Minecraft.getInstance().player;
        if (holder == null) {
            return;
        }
        int scarCount = holder.getPersistentData().getInt("spells_n_gods:scar_count");
        if (scarCount <= 0) {
            return;
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.spells_n_gods.rune.scarred", scarCount)
                .withStyle(ChatFormatting.DARK_RED));
        float healthReduction = holder.getPersistentData().getFloat("spells_n_gods:scar_health_reduction");
        if (healthReduction > 0) {
            tooltip.add(Component.literal("  -" + (int) (healthReduction * 100) + "% max health")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
