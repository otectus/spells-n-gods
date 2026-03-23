package com.otectus.spells_n_gods.worldstate;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.boss.BossSpawnAnimationHandler;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class RespawnTickHandler {
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 200; // Every 10 seconds

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        if (!SpellsNGodsConfig.COMMON.bossesEnabled.get()) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        GodWorldState state = GodWorldState.get(server);
        long gameTime = server.overworld().getGameTime();

        for (StructureRecord record : state.getAllStructures()) {
            if (record.bossAlive()) continue;

            // Phase 1: Respawn timer elapsed — mark as awaiting player proximity
            if (!record.awaitingPlayerSpawn()
                    && record.respawnDueTime() > 0
                    && gameTime >= record.respawnDueTime()) {
                state.setStructure(record.godId(), record.withAwaitingPlayerSpawn(true));
                SpellsNGodsMod.LOGGER.debug("Boss for {} is now awaiting player proximity to spawn",
                        record.godId());
                continue;
            }

            // Phase 2: Awaiting player — check if a player is near the altar
            if (record.awaitingPlayerSpawn()) {
                tryTriggerSpawn(server, state, record);
            }
        }
    }

    private static void tryTriggerSpawn(MinecraftServer server, GodWorldState state,
                                         StructureRecord record) {
        ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(
                Registries.DIMENSION, new ResourceLocation(record.dimension()));
        ServerLevel level = server.getLevel(dimKey);
        if (level == null) return;

        BlockPos center = record.center();
        if (!level.isLoaded(center)) return;

        // Check for any player near the altar center
        double triggerRange = SpellsNGodsConfig.COMMON.bossSpawnTriggerRange.get();
        double triggerRangeSq = triggerRange * triggerRange;
        boolean playerNearby = false;
        for (ServerPlayer player : level.players()) {
            double distSq = player.blockPosition().distSqr(center);
            if (distSq <= triggerRangeSq) {
                playerNearby = true;
                break;
            }
        }

        if (!playerNearby) return;

        // Verify god definition still exists
        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(record.godId()));
        if (god == null) {
            SpellsNGodsMod.LOGGER.warn("Cannot spawn boss for {}: god definition not found", record.godId());
            return;
        }

        // Begin the dramatic spawn sequence
        BossSpawnAnimationHandler.beginSpawnSequence(level, state, record);
    }
}
