package com.otectus.spells_n_gods.content;

import com.otectus.spells_n_gods.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Block entity for ruined idol blocks.
 * Stores information about the original monument before desecration.
 */
public class RuinedIdolBlockEntity extends BlockEntity {

    private UUID originalOwner;
    private String originalGodId;
    private long desecrationTimeMs;

    public RuinedIdolBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUINED_IDOL.get(), pos, state);
    }

    public UUID getOriginalOwner() {
        return originalOwner;
    }

    public void setOriginalOwner(UUID originalOwner) {
        this.originalOwner = originalOwner;
        setChanged();
    }

    public String getOriginalGodId() {
        return originalGodId;
    }

    public void setOriginalGodId(String originalGodId) {
        this.originalGodId = originalGodId;
        setChanged();
    }

    public long getDesecrationTimeMs() {
        return desecrationTimeMs;
    }

    public void setDesecrationTimeMs(long desecrationTimeMs) {
        this.desecrationTimeMs = desecrationTimeMs;
        setChanged();
    }

    /**
     * Initialize from a monument block entity during desecration.
     */
    public void initFromMonument(MonumentBlockEntity monument) {
        this.originalOwner = monument.getOwner();
        this.originalGodId = monument.getGodId();
        this.desecrationTimeMs = System.currentTimeMillis();
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OriginalOwner")) {
            originalOwner = tag.getUUID("OriginalOwner");
        }
        if (tag.contains("OriginalGodId")) {
            originalGodId = tag.getString("OriginalGodId");
        }
        if (tag.contains("DesecrationTime")) {
            desecrationTimeMs = tag.getLong("DesecrationTime");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (originalOwner != null) {
            tag.putUUID("OriginalOwner", originalOwner);
        }
        if (originalGodId != null) {
            tag.putString("OriginalGodId", originalGodId);
        }
        if (desecrationTimeMs > 0L) {
            tag.putLong("DesecrationTime", desecrationTimeMs);
        }
    }
}
