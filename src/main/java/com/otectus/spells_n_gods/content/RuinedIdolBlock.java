package com.otectus.spells_n_gods.content;

import com.otectus.spells_n_gods.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

/**
 * A ruined idol block - the result of a desecrated monument after apostasy.
 * Cannot be used for prayer, only serves as a grim reminder.
 */
public class RuinedIdolBlock extends Block implements EntityBlock {

    public RuinedIdolBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.RUINED_IDOL.get().create(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        // Cannot pray at a ruined idol
        if (level.getBlockEntity(pos) instanceof RuinedIdolBlockEntity ruinedIdol) {
            String godName = ruinedIdol.getOriginalGodId();
            if (godName != null) {
                serverPlayer.sendSystemMessage(Component.translatable("spells_n_gods.ruined_idol.abandoned", godName));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("spells_n_gods.ruined_idol.message"));
            }
        } else {
            serverPlayer.sendSystemMessage(Component.translatable("spells_n_gods.ruined_idol.message"));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("spells_n_gods.ruined_idol.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("spells_n_gods.ruined_idol.tooltip2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Spawn smoke particles occasionally
        if (random.nextInt(5) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        }

        // Occasional soul particles for a haunted effect
        if (random.nextInt(20) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + 0.5 + random.nextDouble() * 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.01, 0);
        }
    }
}
