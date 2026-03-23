package com.otectus.spells_n_gods.content;

import com.otectus.spells_n_gods.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public class MonumentBlockEntity extends BlockEntity {
    private UUID owner;
    private String godId;
    private long createdAtEpochMs;
    private boolean desecrated;

    public MonumentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MONUMENT.get(), pos, state);
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public String getGodId() {
        return godId;
    }

    public void setGodId(String godId) {
        this.godId = godId;
        setChanged();
    }

    public long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }

    public void setCreatedAtEpochMs(long createdAtEpochMs) {
        this.createdAtEpochMs = createdAtEpochMs;
        setChanged();
    }

    public boolean isDesecrated() {
        return desecrated;
    }

    public void setDesecrated(boolean desecrated) {
        this.desecrated = desecrated;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
        }
        if (tag.contains("GodId")) {
            godId = tag.getString("GodId");
        }
        if (tag.contains("CreatedAt")) {
            createdAtEpochMs = tag.getLong("CreatedAt");
        }
        desecrated = tag.getBoolean("Desecrated");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (godId != null) {
            tag.putString("GodId", godId);
        }
        if (createdAtEpochMs > 0L) {
            tag.putLong("CreatedAt", createdAtEpochMs);
        }
        tag.putBoolean("Desecrated", desecrated);
    }
}
