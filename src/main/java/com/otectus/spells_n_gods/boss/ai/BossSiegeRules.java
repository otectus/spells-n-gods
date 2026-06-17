package com.otectus.spells_n_gods.boss.ai;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Decides whether a god boss is permitted to break a given block while sieging toward the player.
 *
 * <p>A block is breakable only if it is solid, finitely hard, not flagged unbreakable by the
 * {@link #BOSS_UNBREAKABLE} datapack tag, holds no block entity (so containers don't spill), and
 * does not lie inside a protected temple region. Temple protection reuses the same
 * {@link GodWorldState#isPositionProtected(BlockPos)} check that guards against player griefing,
 * so a boss can never demolish its own arena.
 */
public final class BossSiegeRules {
    private BossSiegeRules() {
    }

    /** Datapack-overridable tag of blocks a boss must never break (bedrock, obsidian, temple cores…). */
    public static final TagKey<Block> BOSS_UNBREAKABLE = TagKey.create(
            Registries.BLOCK, new ResourceLocation(SpellsNGodsMod.MODID, "boss_unbreakable"));

    /**
     * @return {@code true} if the boss may break the block at {@code pos} in {@code level}.
     */
    public static boolean canBreak(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        // Unbreakable hardness (bedrock = -1) — never destructible.
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (state.is(BOSS_UNBREAKABLE)) {
            return false;
        }
        // Skip block entities (chests, monuments, spawners…) to avoid item spills and grief.
        if (state.hasBlockEntity()) {
            return false;
        }
        return !isProtected(pos);
    }

    private static boolean isProtected(BlockPos pos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        return GodWorldState.get(server).isPositionProtected(pos);
    }
}
