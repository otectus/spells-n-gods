package com.otectus.spells_n_gods.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * One-shot visual effect packet sent from server to nearby clients during boss events.
 * Used for camera shake and screen vignette effects.
 */
public class BossVisualEffectPacket {

    public enum EffectType {
        CAMERA_SHAKE_LIGHT,
        CAMERA_SHAKE_HEAVY,
        ENRAGE_FLASH,
        DEATH_FLASH
    }

    private final EffectType effectType;
    private final double originX, originY, originZ;
    private final String school;

    public BossVisualEffectPacket(EffectType effectType, double x, double y, double z) {
        this(effectType, x, y, z, "");
    }

    public BossVisualEffectPacket(EffectType effectType, double x, double y, double z, String school) {
        this.effectType = effectType;
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.school = school != null ? school : "";
    }

    public static void encode(BossVisualEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.effectType);
        buf.writeDouble(msg.originX);
        buf.writeDouble(msg.originY);
        buf.writeDouble(msg.originZ);
        buf.writeUtf(msg.school, 64);
    }

    public static BossVisualEffectPacket decode(FriendlyByteBuf buf) {
        EffectType type = buf.readEnum(EffectType.class);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String school = buf.readUtf(64);
        return new BossVisualEffectPacket(type, x, y, z, school);
    }

    public static void handle(BossVisualEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.otectus.spells_n_gods.client.BossFightScreenEffects.handleEffect(
                        msg.effectType, msg.originX, msg.originY, msg.originZ, msg.school)
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
