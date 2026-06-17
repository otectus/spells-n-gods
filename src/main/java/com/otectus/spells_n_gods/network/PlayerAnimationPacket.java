package com.otectus.spells_n_gods.network;

import com.otectus.spells_n_gods.animation.PlayerAnimationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerAnimationPacket {

    public enum Action {
        PLAY,
        STOP
    }

    private final UUID targetPlayerId;
    private final PlayerAnimationType animationType;
    private final Action action;

    public PlayerAnimationPacket(UUID targetPlayerId, PlayerAnimationType animationType, Action action) {
        this.targetPlayerId = targetPlayerId;
        this.animationType = animationType;
        this.action = action;
    }

    public static void encode(PlayerAnimationPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetPlayerId);
        buf.writeEnum(msg.animationType);
        buf.writeEnum(msg.action);
    }

    public static PlayerAnimationPacket decode(FriendlyByteBuf buf) {
        UUID targetPlayerId = buf.readUUID();
        PlayerAnimationType animationType = buf.readEnum(PlayerAnimationType.class);
        Action action = buf.readEnum(Action.class);
        return new PlayerAnimationPacket(targetPlayerId, animationType, action);
    }

    public static void handle(PlayerAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // Guard the client-only handler so this common-side class never links client/playerAnimator
        // types on a dedicated server (mirrors the other PLAY_TO_CLIENT packets in this mod).
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.otectus.spells_n_gods.client.PlayerAnimationHandler.handleAnimation(
                        msg.targetPlayerId, msg.animationType, msg.action)));
        ctx.get().setPacketHandled(true);
    }
}
