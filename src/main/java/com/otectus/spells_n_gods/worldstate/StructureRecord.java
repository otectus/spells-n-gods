package com.otectus.spells_n_gods.worldstate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record StructureRecord(
        String godId,
        BlockPos center,
        BoundingBox bounds,
        String dimension,
        long lastKilledTime,
        long respawnDueTime,
        boolean bossAlive,
        @Nullable UUID bossEntityUUID,
        boolean awaitingPlayerSpawn
) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("GodId", godId);
        tag.putInt("CenterX", center.getX());
        tag.putInt("CenterY", center.getY());
        tag.putInt("CenterZ", center.getZ());
        tag.putInt("BoundsMinX", bounds.minX());
        tag.putInt("BoundsMinY", bounds.minY());
        tag.putInt("BoundsMinZ", bounds.minZ());
        tag.putInt("BoundsMaxX", bounds.maxX());
        tag.putInt("BoundsMaxY", bounds.maxY());
        tag.putInt("BoundsMaxZ", bounds.maxZ());
        tag.putString("Dimension", dimension);
        tag.putLong("LastKilledTime", lastKilledTime);
        tag.putLong("RespawnDueTime", respawnDueTime);
        tag.putBoolean("BossAlive", bossAlive);
        tag.putBoolean("AwaitingPlayerSpawn", awaitingPlayerSpawn);
        if (bossEntityUUID != null) {
            tag.putUUID("BossEntityUUID", bossEntityUUID);
        }
        return tag;
    }

    public static StructureRecord fromTag(String godId, CompoundTag tag) {
        BlockPos center = new BlockPos(
                tag.getInt("CenterX"),
                tag.getInt("CenterY"),
                tag.getInt("CenterZ")
        );
        BoundingBox bounds = new BoundingBox(
                tag.getInt("BoundsMinX"), tag.getInt("BoundsMinY"), tag.getInt("BoundsMinZ"),
                tag.getInt("BoundsMaxX"), tag.getInt("BoundsMaxY"), tag.getInt("BoundsMaxZ")
        );
        String dimension = tag.getString("Dimension");
        long lastKilledTime = tag.getLong("LastKilledTime");
        long respawnDueTime = tag.getLong("RespawnDueTime");
        boolean bossAlive = tag.getBoolean("BossAlive");
        UUID bossUUID = tag.hasUUID("BossEntityUUID") ? tag.getUUID("BossEntityUUID") : null;
        boolean awaitingPlayerSpawn = tag.getBoolean("AwaitingPlayerSpawn");

        return new StructureRecord(godId, center, bounds, dimension,
                lastKilledTime, respawnDueTime, bossAlive, bossUUID, awaitingPlayerSpawn);
    }

    public StructureRecord withBossKilled(long gameTime, int respawnDelayTicks) {
        return new StructureRecord(godId, center, bounds, dimension,
                gameTime, gameTime + respawnDelayTicks, false, null, false);
    }

    public StructureRecord withBossSpawned(UUID entityUUID) {
        return new StructureRecord(godId, center, bounds, dimension,
                lastKilledTime, 0L, true, entityUUID, false);
    }

    public StructureRecord withBossAlive(boolean alive) {
        return new StructureRecord(godId, center, bounds, dimension,
                lastKilledTime, respawnDueTime, alive, alive ? bossEntityUUID : null,
                awaitingPlayerSpawn);
    }

    public StructureRecord withAwaitingPlayerSpawn(boolean awaiting) {
        return new StructureRecord(godId, center, bounds, dimension,
                lastKilledTime, respawnDueTime, bossAlive, bossEntityUUID, awaiting);
    }
}
