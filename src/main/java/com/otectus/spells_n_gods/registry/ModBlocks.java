package com.otectus.spells_n_gods.registry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.content.MonumentBlock;
import com.otectus.spells_n_gods.content.RuinedIdolBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SpellsNGodsMod.MODID);

    public static final RegistryObject<Block> MONUMENT = BLOCKS.register("monument",
            () -> new MonumentBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 6.0f)));

    public static final RegistryObject<Block> RUINED_IDOL = BLOCKS.register("ruined_idol",
            () -> new RuinedIdolBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(2.0f, 4.0f)
                    .noOcclusion()));

    private ModBlocks() {
    }
}
