package com.otectus.spells_n_gods.worldstate;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = SpellsNGodsMod.MODID)
public class StructureProtectionHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!SpellsNGodsConfig.COMMON.structureProtection.get()) return;
        if (event.getPlayer().hasPermissions(4)) return; // Creative/OP bypass

        if (isProtected(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("spells_n_gods.structure.protected"));
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!SpellsNGodsConfig.COMMON.structureProtection.get()) return;
        if (event.getEntity() instanceof ServerPlayer sp && sp.hasPermissions(4)) return;

        if (isProtected(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!SpellsNGodsConfig.COMMON.protectFromExplosions.get()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        GodWorldState state = GodWorldState.get(serverLevel.getServer());
        event.getAffectedBlocks().removeIf(state::isPositionProtected);
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!SpellsNGodsConfig.COMMON.protectFromPistons.get()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos targetPos = event.getPos().relative(event.getDirection());
        GodWorldState state = GodWorldState.get(serverLevel.getServer());
        if (state.isPositionProtected(targetPos)) {
            event.setCanceled(true);
        }
    }

    private static boolean isProtected(LevelAccessor level, BlockPos pos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        GodWorldState state = GodWorldState.get(server);
        return state.isPositionProtected(pos);
    }
}
