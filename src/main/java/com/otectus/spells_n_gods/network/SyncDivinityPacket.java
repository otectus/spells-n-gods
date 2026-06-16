package com.otectus.spells_n_gods.network;

import com.otectus.spells_n_gods.capability.BlessingState;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncDivinityPacket {
    private final String godId;
    private final float favor;
    private final BlessingState blessingState;
    private final DivineTier currentTier;
    private final long lastOfferingEpochMs;
    private final long lastPrayerEpochMs;
    private final int scarCount;
    private final float totalHealthReduction;

    public SyncDivinityPacket(String godId, float favor, BlessingState blessingState,
                               DivineTier currentTier, long lastOfferingEpochMs, long lastPrayerEpochMs,
                               int scarCount, float totalHealthReduction) {
        this.godId = godId;
        this.favor = favor;
        this.blessingState = blessingState;
        this.currentTier = currentTier;
        this.lastOfferingEpochMs = lastOfferingEpochMs;
        this.lastPrayerEpochMs = lastPrayerEpochMs;
        this.scarCount = scarCount;
        this.totalHealthReduction = totalHealthReduction;
    }

    public static void encode(SyncDivinityPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.godId != null);
        if (packet.godId != null) {
            buf.writeUtf(packet.godId, 256);
        }
        buf.writeFloat(packet.favor);
        buf.writeEnum(packet.blessingState);
        buf.writeEnum(packet.currentTier);
        buf.writeLong(packet.lastOfferingEpochMs);
        buf.writeLong(packet.lastPrayerEpochMs);
        buf.writeVarInt(packet.scarCount);
        buf.writeFloat(packet.totalHealthReduction);
    }

    public static SyncDivinityPacket decode(FriendlyByteBuf buf) {
        String godId = buf.readBoolean() ? buf.readUtf(256) : null;
        float favor = buf.readFloat();
        BlessingState blessingState = buf.readEnum(BlessingState.class);
        DivineTier currentTier = buf.readEnum(DivineTier.class);
        long lastOfferingEpochMs = buf.readLong();
        long lastPrayerEpochMs = buf.readLong();
        int scarCount = buf.readVarInt();
        float totalHealthReduction = buf.readFloat();
        return new SyncDivinityPacket(godId, favor, blessingState, currentTier,
                lastOfferingEpochMs, lastPrayerEpochMs, scarCount, totalHealthReduction);
    }

    public static void handle(SyncDivinityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.getCapability(PlayerDivinityCapability.DIVINITY).ifPresent(data -> {
                        data.setChosenGodId(packet.godId);
                        data.setFavor(packet.favor);
                        data.setBlessingState(packet.blessingState);
                        data.setCurrentTier(packet.currentTier);
                        data.setLastOfferingEpochMs(packet.lastOfferingEpochMs);
                        data.setLastPrayerEpochMs(packet.lastPrayerEpochMs);
                    });
                    // Store scar info in persistent data for client-side rendering
                    player.getPersistentData().putInt("spells_n_gods:scar_count", packet.scarCount);
                    player.getPersistentData().putFloat("spells_n_gods:scar_health_reduction", packet.totalHealthReduction);
                }
            })
        );
        ctx.get().setPacketHandled(true);
    }
}
