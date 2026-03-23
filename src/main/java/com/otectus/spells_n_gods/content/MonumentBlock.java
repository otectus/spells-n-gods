package com.otectus.spells_n_gods.content;

import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.prayer.PrayerManager;
import com.otectus.spells_n_gods.registry.ModBlockEntities;
import com.otectus.spells_n_gods.registry.ModParticles;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MonumentBlock extends Block implements EntityBlock {
    public MonumentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.MONUMENT.get().create(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof MonumentBlockEntity monument)) {
            return InteractionResult.FAIL;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        // Check if player is the owner or is an admin
        if (!isPlayerOwnerOrAdmin(serverPlayer, monument)) {
            serverPlayer.sendSystemMessage(Component.translatable("spells_n_gods.monument.not_owner"));
            return InteractionResult.FAIL;
        }

        // Check if player is bound to this god
        Optional<PlayerDivinityData> dataOpt = PlayerDivinityCapability.get(player);
        if (dataOpt.isEmpty()) {
            return InteractionResult.FAIL;
        }

        PlayerDivinityData data = dataOpt.get();
        String monumentGodId = monument.getGodId();

        if (data.getChosenGodId() == null || !data.getChosenGodId().equals(monumentGodId)) {
            serverPlayer.sendSystemMessage(Component.translatable("spells_n_gods.monument.wrong_god"));
            return InteractionResult.FAIL;
        }

        // Start prayer if not already praying
        if (PrayerManager.isPlayerPraying(player.getUUID())) {
            serverPlayer.sendSystemMessage(Component.translatable("spells_n_gods.prayer.already_praying"));
            return InteractionResult.CONSUME;
        }

        PrayerManager.startPrayer(serverPlayer, monument);
        return InteractionResult.CONSUME;
    }

    private boolean isPlayerOwnerOrAdmin(ServerPlayer player, MonumentBlockEntity monument) {
        if (monument.getOwner() == null) {
            return true;
        }
        if (player.getUUID().equals(monument.getOwner())) {
            return true;
        }
        return player.hasPermissions(2);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("spells_n_gods.monument.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("spells_n_gods.monument.tooltip2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!SpellsNGodsConfig.COMMON.monumentAmbientParticles.get()) return;
        if (!(level.getBlockEntity(pos) instanceof MonumentBlockEntity monument)) return;

        String godId = monument.getGodId();
        if (godId == null || godId.isEmpty()) return;

        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(godId));
        if (god == null) return;

        String school = god.magicSchool();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // Gentle ambient aura particle (30% chance per tick)
        if (random.nextFloat() < 0.3f) {
            double ox = (random.nextDouble() - 0.5) * 0.8;
            double oz = (random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ModParticles.DIVINE_AURA.get(),
                    cx + ox, cy + random.nextDouble() * 0.5, cz + oz,
                    0, 0.005, 0);
        }

        // School-themed particle (15% chance per tick)
        if (random.nextFloat() < 0.15f) {
            ParticleOptions schoolParticle = SchoolColors.getSchoolParticle(school);
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.3 + random.nextDouble() * 0.3;
            level.addParticle(schoolParticle,
                    cx + Math.cos(angle) * radius,
                    cy + 0.3 + random.nextDouble() * 0.8,
                    cz + Math.sin(angle) * radius,
                    0, 0.01, 0);
        }

        // School-colored dust pillar - slow rising column makes each monument visually distinct
        if (random.nextFloat() < 0.25f) {
            org.joml.Vector3f color = SchoolColors.getSchoolColor(school);
            DustParticleOptions schoolDust = new DustParticleOptions(color, 0.7f);
            level.addParticle(schoolDust,
                    cx + (random.nextDouble() - 0.5) * 0.2,
                    cy + 0.8 + random.nextDouble() * 0.5,
                    cz + (random.nextDouble() - 0.5) * 0.2,
                    0, 0.015, 0);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Block was destroyed - could add special handling here
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
