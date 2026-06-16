package com.otectus.spells_n_gods.spawning;

import com.otectus.spells_n_gods.content.MonumentBlock;
import com.otectus.spells_n_gods.content.RuinedIdolBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Optional;

/**
 * Resolves a concrete, <em>safe</em> spawn position inside a structure's bounding box according
 * to a {@link SpawnPlacement} strategy. A position is considered safe only if it has a solid
 * floor, a breathable air column tall enough for the deity, and is not in lava or the void —
 * deities are never spawned inside blocks, in lava, in the void, or in inaccessible spaces.
 */
public final class PlacementResolver {
    private static final int MAX_RANDOM_ATTEMPTS = 24;

    private PlacementResolver() {
    }

    public static Optional<BlockPos> resolve(ServerLevel level, BoundingBox bounds,
                                             SpawnPlacement placement, int entityHeight, Vec3i offset) {
        int height = Math.max(2, entityHeight);
        RandomSource random = level.getRandom();
        BlockPos base = switch (placement) {
            case CENTER_OF_STRUCTURE -> centerColumn(level, bounds, height).orElse(null);
            case NEAREST_SAFE_FLOOR -> nearestSafeFloor(level, bounds, height).orElse(null);
            case NEAREST_AIR_ABOVE_FLOOR -> nearestSafeFloor(level, bounds, height).map(BlockPos::above).orElse(null);
            case RANDOM_VALID_POSITION -> randomValid(level, bounds, height, random).orElse(null);
            case SHRINE_ANCHOR -> shrineAnchor(level, bounds, height)
                    .or(() -> nearestSafeFloor(level, bounds, height)).orElse(null);
        };
        if (base == null) {
            // Last-ditch fallback so a configured spawn is never silently lost.
            base = nearestSafeFloor(level, bounds, height).orElse(null);
        }
        if (base == null) {
            return Optional.empty();
        }
        if (offset != null && (offset.getX() != 0 || offset.getY() != 0 || offset.getZ() != 0)) {
            BlockPos shifted = base.offset(offset);
            if (isSafeStanding(level, shifted, height)) {
                base = shifted;
            }
        }
        return Optional.of(base);
    }

    private static Optional<BlockPos> centerColumn(ServerLevel level, BoundingBox bounds, int height) {
        int cx = (bounds.minX() + bounds.maxX()) / 2;
        int cz = (bounds.minZ() + bounds.maxZ()) / 2;
        return scanColumnDown(level, cx, cz, bounds, height);
    }

    private static Optional<BlockPos> nearestSafeFloor(ServerLevel level, BoundingBox bounds, int height) {
        int cx = (bounds.minX() + bounds.maxX()) / 2;
        int cz = (bounds.minZ() + bounds.maxZ()) / 2;
        // Spiral outward from the centre so the chosen floor is close to the structure heart.
        int maxR = Math.max(bounds.maxX() - bounds.minX(), bounds.maxZ() - bounds.minZ()) / 2 + 1;
        for (int r = 0; r <= maxR; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue; // only the ring at radius r
                    }
                    int x = cx + dx;
                    int z = cz + dz;
                    if (x < bounds.minX() || x > bounds.maxX() || z < bounds.minZ() || z > bounds.maxZ()) {
                        continue;
                    }
                    Optional<BlockPos> hit = scanColumnDown(level, x, z, bounds, height);
                    if (hit.isPresent()) {
                        return hit;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> randomValid(ServerLevel level, BoundingBox bounds, int height,
                                                  RandomSource random) {
        int spanX = Math.max(1, bounds.maxX() - bounds.minX());
        int spanZ = Math.max(1, bounds.maxZ() - bounds.minZ());
        for (int i = 0; i < MAX_RANDOM_ATTEMPTS; i++) {
            int x = bounds.minX() + random.nextInt(spanX + 1);
            int z = bounds.minZ() + random.nextInt(spanZ + 1);
            Optional<BlockPos> hit = scanColumnDown(level, x, z, bounds, height);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return nearestSafeFloor(level, bounds, height);
    }

    private static Optional<BlockPos> shrineAnchor(ServerLevel level, BoundingBox bounds, int height) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof MonumentBlock || state.getBlock() instanceof RuinedIdolBlock) {
                        BlockPos above = cursor.above().immutable();
                        if (isSafeStanding(level, above, height)) {
                            return Optional.of(above);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Scan a single column from the top of the bounds downward for the first safe standing spot. */
    private static Optional<BlockPos> scanColumnDown(ServerLevel level, int x, int z,
                                                     BoundingBox bounds, int height) {
        int top = Math.min(bounds.maxY(), level.getMaxBuildHeight() - height - 1);
        int bottom = Math.max(bounds.minY(), level.getMinBuildHeight() + 1);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            cursor.set(x, y, z);
            if (isSafeStanding(level, cursor, height)) {
                return Optional.of(cursor.immutable());
            }
        }
        return Optional.empty();
    }

    /**
     * @return {@code true} if an entity {@code height} blocks tall can stand at {@code pos}:
     *         solid floor below, an open non-fluid column at and above {@code pos}, and not in
     *         the void.
     */
    public static boolean isSafeStanding(ServerLevel level, BlockPos pos, int height) {
        if (pos.getY() <= level.getMinBuildHeight() + 1 || pos.getY() + height >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState floor = level.getBlockState(pos.below());
        if (floor.isAir() || !floor.getFluidState().isEmpty()) {
            return false; // no solid footing (or standing on liquid)
        }
        if (floor.getCollisionShape(level, pos.below()).isEmpty()) {
            return false; // non-collidable floor (e.g. grass, torch)
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < height; dy++) {
            cursor.set(pos.getX(), pos.getY() + dy, pos.getZ());
            BlockState state = level.getBlockState(cursor);
            if (!state.getFluidState().isEmpty()) {
                return false; // never spawn in liquid (incl. lava)
            }
            if (!state.getCollisionShape(level, cursor).isEmpty()) {
                return false; // blocked / inside a block
            }
        }
        return true;
    }
}
