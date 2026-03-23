package com.otectus.spells_n_gods.registry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.content.MonumentBlockEntity;
import com.otectus.spells_n_gods.content.RuinedIdolBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SpellsNGodsMod.MODID);

    public static final RegistryObject<BlockEntityType<MonumentBlockEntity>> MONUMENT = BLOCK_ENTITIES.register(
            "monument",
            () -> BlockEntityType.Builder.of(MonumentBlockEntity::new, ModBlocks.MONUMENT.get()).build(null)
    );

    public static final RegistryObject<BlockEntityType<RuinedIdolBlockEntity>> RUINED_IDOL = BLOCK_ENTITIES.register(
            "ruined_idol",
            () -> BlockEntityType.Builder.of(RuinedIdolBlockEntity::new, ModBlocks.RUINED_IDOL.get()).build(null)
    );

    private ModBlockEntities() {
    }
}
