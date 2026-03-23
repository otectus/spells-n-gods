package com.otectus.spells_n_gods.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPrayerHandler {
    private static boolean isPraying = false;
    private static float progress = 0.0f;
    private static int remainingSeconds = 0;

    public static void handlePrayerProgress(boolean praying, float prog, int remaining) {
        isPraying = praying;
        progress = prog;
        remainingSeconds = remaining;
    }

    public static boolean isPraying() {
        return isPraying;
    }

    public static float getProgress() {
        return progress;
    }

    public static int getRemainingSeconds() {
        return remainingSeconds;
    }

    public static void reset() {
        isPraying = false;
        progress = 0.0f;
        remainingSeconds = 0;
    }
}
