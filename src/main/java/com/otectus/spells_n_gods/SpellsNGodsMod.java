package com.otectus.spells_n_gods;

import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.boss.GodBossRenderer;
import com.otectus.spells_n_gods.command.SpellsNGodsCommands;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.config.ShrineConfig;
import com.otectus.spells_n_gods.config.StructureSpawnConfig;
import com.otectus.spells_n_gods.spawning.StructureSpawnValidator;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.item.ability.DivineAbilityRegistry;
import com.otectus.spells_n_gods.network.ModNetwork;
import com.otectus.spells_n_gods.structure.SchematicLoader;
import com.otectus.spells_n_gods.registry.ModBlockEntities;
import com.otectus.spells_n_gods.registry.ModBlocks;
import com.otectus.spells_n_gods.registry.ModEntities;
import com.otectus.spells_n_gods.registry.ModItems;
import com.otectus.spells_n_gods.registry.ModMenus;
import com.otectus.spells_n_gods.registry.ModStructurePieces;
import com.otectus.spells_n_gods.registry.ModCreativeTabs;
import com.otectus.spells_n_gods.registry.ModParticles;
import com.otectus.spells_n_gods.registry.ModStructures;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(SpellsNGodsMod.MODID)
public class SpellsNGodsMod {
    public static final String MODID = "spells_n_gods";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpellsNGodsMod() {
        // Initialize GeckoLib (required for entity animation/rendering in GeckoLib 4.x)
        software.bernie.geckolib.GeckoLib.initialize();

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModStructures.STRUCTURE_TYPES.register(modBus);
        ModStructurePieces.STRUCTURE_PIECES.register(modBus);
        ModCreativeTabs.CREATIVE_TABS.register(modBus);
        ModParticles.PARTICLE_TYPES.register(modBus);

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onEntityAttributeCreation);
        modBus.addListener(ModCreativeTabs::onBuildContents);

        // Client-only mod-bus listeners. Registering these on a dedicated server would force FML to
        // load client-only event classes (RegisterParticleProvidersEvent -> ParticleEngine,
        // FMLClientSetupEvent's client renderer refs) and crash with "invalid dist DEDICATED_SERVER".
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::onClientSetup);
            modBus.addListener(this::onRegisterParticleProviders);
        }

        LOGGER.info("[SpellsNGods] Mod constructor complete — all registries queued");

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SpellsNGodsConfig.COMMON_SPEC, "spells_n_gods/spells_n_gods-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SpellsNGodsConfig.SERVER_SPEC, "spells_n_gods/spells_n_gods-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShrineConfig.SPEC, "spells_n_gods/shrines.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StructureSpawnConfig.SPEC, "spells_n_gods/structure_spawns.toml");

        // Ensure the schematics directory exists
        try {
            Path schematicsDir = FMLPaths.CONFIGDIR.get().resolve("spells_n_gods/schematics");
            Files.createDirectories(schematicsDir);
        } catch (IOException e) {
            LOGGER.warn("[SpellsNGods] Failed to create schematics directory: {}", e.getMessage());
        }

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            DivineAbilityRegistry.init();
            LOGGER.info("Spells n'' Gods network and ability registry initialized");
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.GOD_BOSS.get(), GodBossRenderer::new);
            com.otectus.spells_n_gods.client.PlayerAnimationHandler.init();
        });
    }

    private void onRegisterParticleProviders(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DIVINE_AURA.get(),
                com.otectus.spells_n_gods.client.particle.DivineAuraParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DIVINE_RUNE.get(),
                com.otectus.spells_n_gods.client.particle.DivineRuneParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DIVINE_BURST.get(),
                com.otectus.spells_n_gods.client.particle.DivineBurstParticle.Provider::new);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GOD_BOSS.get(), GodBossEntity.createAttributes().build());
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        LOGGER.info("[SpellsNGods] AddReloadListenerEvent fired — registering data reload listeners");
        SpellsNGodsDataManager.registerReloadListeners(event);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("[SpellsNGods] RegisterCommandsEvent fired — registering /spellsngods commands");
        SpellsNGodsCommands.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        SchematicLoader.init(event.getServer());
        // Validate the data-driven structure-spawn config once deity data is loaded.
        StructureSpawnValidator.validateAll();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        SchematicLoader.clear();
    }
}
