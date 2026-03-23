package com.otectus.spells_n_gods.registry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SpellsNGodsMod.MODID);

    public static final RegistryObject<SimpleParticleType> DIVINE_AURA =
            PARTICLE_TYPES.register("divine_aura", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> DIVINE_RUNE =
            PARTICLE_TYPES.register("divine_rune", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> DIVINE_BURST =
            PARTICLE_TYPES.register("divine_burst", () -> new SimpleParticleType(false));
}
