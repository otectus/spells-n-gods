package com.otectus.spells_n_gods.binding;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.BlessingState;
import com.otectus.spells_n_gods.capability.CapabilityHandler;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.compat.SpellsNGodsEvents;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.content.MonumentBlockEntity;
import com.otectus.spells_n_gods.content.RuneItem;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.animation.PlayerAnimationType;
import com.otectus.spells_n_gods.network.DivineVfxPacket;
import com.otectus.spells_n_gods.network.ModNetwork;
import com.otectus.spells_n_gods.network.PlayerAnimationPacket;
import com.otectus.spells_n_gods.registry.ModBlocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class BindingHandler {

    public static InteractionResult tryBind(Player player, ArmorStand stand, ItemStack runeStack) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Optional<ResourceLocation> godIdOpt = RuneItem.getGodId(runeStack);
        if (godIdOpt.isEmpty()) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.error.rune_not_bound"));
            return InteractionResult.FAIL;
        }

        ResourceLocation godId = godIdOpt.get();
        GodDefinition god = SpellsNGodsDataManager.getGods().get(godId);
        if (god == null) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.error.unknown_god", godId.toString()));
            return InteractionResult.FAIL;
        }

        Optional<PlayerDivinityData> dataOpt = PlayerDivinityCapability.get(player);
        if (dataOpt.isEmpty()) {
            SpellsNGodsMod.LOGGER.error("Player {} has no divinity capability", player.getName().getString());
            return InteractionResult.FAIL;
        }

        PlayerDivinityData data = dataOpt.get();

        if (!canPlayerBind(data, player)) {
            return InteractionResult.FAIL;
        }

        BlockPos monumentPos = findMonumentPosition(stand);
        if (monumentPos == null) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.error.no_valid_position"));
            return InteractionResult.FAIL;
        }

        Level level = player.level();

        BlockState monumentState = ModBlocks.MONUMENT.get().defaultBlockState();
        level.setBlock(monumentPos, monumentState, 3);

        if (!(level.getBlockEntity(monumentPos) instanceof MonumentBlockEntity be)) {
            // Monument block entity creation failed - restore the original block and abort
            level.removeBlock(monumentPos, false);
            player.sendSystemMessage(Component.translatable("spells_n_gods.error.monument_creation_failed"));
            return InteractionResult.FAIL;
        }

        // Monument created successfully - now consume resources
        stand.discard();
        runeStack.shrink(1);

        be.setOwner(player.getUUID());
        be.setGodId(godId.toString());
        be.setCreatedAtEpochMs(System.currentTimeMillis());

        bindPlayerToGod(data, godId, god, (ServerPlayer) player);

        player.sendSystemMessage(Component.translatable("spells_n_gods.binding.success", god.displayName())
                .withStyle(style -> style.withColor(0xFFD700)));

        // Send binding VFX to the player
        ModNetwork.sendToPlayer((ServerPlayer) player, new DivineVfxPacket(
                DivineVfxPacket.VfxType.BINDING_COMPLETE,
                monumentPos.getX() + 0.5, monumentPos.getY() + 0.5, monumentPos.getZ() + 0.5,
                god.magicSchool(), 0));

        // Play binding ceremony animation
        ModNetwork.sendToTrackingAndSelf((ServerPlayer) player, new PlayerAnimationPacket(
                player.getUUID(), PlayerAnimationType.BINDING_CEREMONY, PlayerAnimationPacket.Action.PLAY));

        SpellsNGodsMod.LOGGER.info("Player {} bound to god {}", player.getName().getString(), godId);

        return InteractionResult.CONSUME;
    }

    private static boolean canPlayerBind(PlayerDivinityData data, Player player) {
        if (data.getBlessingState() == BlessingState.UNBOUND) {
            return true;
        }

        if (data.getBlessingState() == BlessingState.UNBOUND_COOLDOWN) {
            long now = System.currentTimeMillis();
            if (now > data.getApostasyCooldownEndMs()) {
                return true;
            } else {
                long remainingMs = Math.max(0, data.getApostasyCooldownEndMs() - now);
                long remainingHours = remainingMs / 3600_000L;
                player.sendSystemMessage(Component.translatable("spells_n_gods.error.apostasy_cooldown", remainingHours));
                return false;
            }
        }

        // Already bound - prevent duplicate binding to same or different god
        if (data.isBound()) {
            player.sendSystemMessage(Component.translatable("spells_n_gods.error.already_bound"));
            return false;
        }

        return true;
    }

    private static BlockPos findMonumentPosition(ArmorStand stand) {
        BlockPos standPos = stand.blockPosition();
        Level level = stand.level();

        if (canPlaceMonument(level, standPos)) {
            return standPos;
        }

        if (canPlaceMonument(level, standPos.above())) {
            return standPos.above();
        }

        if (canPlaceMonument(level, standPos.below())) {
            return standPos.below();
        }

        return null;
    }

    private static boolean canPlaceMonument(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static void bindPlayerToGod(PlayerDivinityData data, ResourceLocation godId, GodDefinition god, ServerPlayer player) {
        long now = System.currentTimeMillis();

        data.setChosenGodId(godId.toString());
        data.setFavor(0.0f);
        data.setLastOfferingEpochMs(now);
        data.setLastPrayerEpochMs(0L);
        data.setLastFavorUpdateMs(now);
        data.setBlessingState(BlessingState.ACTIVE);
        data.setCurrentTier(DivineTier.NONE);
        data.setApostasyCooldownEndMs(0L);
        data.setLatentCurseEndMs(0L);

        CapabilityHandler.syncToClient(player);

        // Fire event for mod compatibility (KubeJS, FTB Quests, etc.)
        MinecraftForge.EVENT_BUS.post(new SpellsNGodsEvents.PlayerBoundEvent(player, god));
    }
}
