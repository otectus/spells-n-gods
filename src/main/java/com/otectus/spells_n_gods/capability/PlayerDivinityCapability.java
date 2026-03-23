package com.otectus.spells_n_gods.capability;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerDivinityCapability implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerDivinityData> DIVINITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(SpellsNGodsMod.MODID, "divinity");

    private final PlayerDivinityData data;
    private final LazyOptional<PlayerDivinityData> optional;

    public PlayerDivinityCapability() {
        this.data = new PlayerDivinityData();
        this.optional = LazyOptional.of(() -> data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == DIVINITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.load(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }

    public static Optional<PlayerDivinityData> get(Player player) {
        return player.getCapability(DIVINITY).resolve();
    }

    public static PlayerDivinityData getOrCreate(Player player) {
        return player.getCapability(DIVINITY).orElseGet(PlayerDivinityData::new);
    }
}
