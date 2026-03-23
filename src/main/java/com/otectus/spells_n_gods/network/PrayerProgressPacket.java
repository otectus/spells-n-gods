package com.otectus.spells_n_gods.network;

import com.otectus.spells_n_gods.client.ClientPrayerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PrayerProgressPacket {
    private final boolean isPraying;
    private final float progress;
    private final int remainingSeconds;

    public PrayerProgressPacket(boolean isPraying, float progress, int remainingSeconds) {
        this.isPraying = isPraying;
        this.progress = progress;
        this.remainingSeconds = remainingSeconds;
    }

    public static void encode(PrayerProgressPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.isPraying);
        buf.writeFloat(packet.progress);
        buf.writeInt(packet.remainingSeconds);
    }

    public static PrayerProgressPacket decode(FriendlyByteBuf buf) {
        boolean isPraying = buf.readBoolean();
        float progress = buf.readFloat();
        int remainingSeconds = buf.readInt();
        return new PrayerProgressPacket(isPraying, progress, remainingSeconds);
    }

    public static void handle(PrayerProgressPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPrayerHandler.handlePrayerProgress(packet.isPraying, packet.progress, packet.remainingSeconds)
            );
        });
        ctx.get().setPacketHandled(true);
    }

    public boolean isPraying() {
        return isPraying;
    }

    public float getProgress() {
        return progress;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}
