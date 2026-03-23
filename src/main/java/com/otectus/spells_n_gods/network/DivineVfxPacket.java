package com.otectus.spells_n_gods.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * General-purpose visual effect packet for divine gameplay events.
 * Sent from server to client to trigger particle bursts and screen effects.
 */
public class DivineVfxPacket {

    public enum VfxType {
        OFFERING_ACCEPTED,
        BINDING_COMPLETE,
        TIER_UP,
        APOSTASY_COMPLETE,
        APOSTASY_TRIAL_START,
        PRAYER_COMPLETE,
        CURSE_LIFTED
    }

    private final VfxType type;
    private final double x, y, z;
    private final String school;
    private final int intensity;

    public DivineVfxPacket(VfxType type, double x, double y, double z, String school, int intensity) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.school = school;
        this.intensity = intensity;
    }

    public static void encode(DivineVfxPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeUtf(msg.school, 64);
        buf.writeVarInt(msg.intensity);
    }

    public static DivineVfxPacket decode(FriendlyByteBuf buf) {
        VfxType type = buf.readEnum(VfxType.class);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String school = buf.readUtf(64);
        int intensity = buf.readVarInt();
        return new DivineVfxPacket(type, x, y, z, school, intensity);
    }

    public static void handle(DivineVfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.otectus.spells_n_gods.client.DivineVfxHandler.handleVfx(
                    msg.type, msg.x, msg.y, msg.z, msg.school, msg.intensity);
        });
        ctx.get().setPacketHandled(true);
    }
}
