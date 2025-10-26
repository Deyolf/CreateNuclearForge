package net.nuclearteam.createnuclear.content.test;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.nuclearteam.createnuclear.CreateNuclear;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class WorldUtils {


    @Contract("null, _ -> false")
    public static boolean isChunkLoaded(@Nullable LevelReader world, @NotNull BlockPos pos) {
        return isChunkLoaded(world, SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }


    @Contract("null, _ -> false")
    public static boolean isChunkLoaded(@Nullable LevelReader world, ChunkPos chunkPos) {
        return isChunkLoaded(world, chunkPos.x, chunkPos.z);
    }


    @Contract("null, _, _ -> false")
    public static boolean isChunkLoaded(@Nullable LevelReader world, int chunkX, int chunkZ) {
        if (world == null) {
            return false;
        } else if (world instanceof LevelAccessor accessor) {
            if (!(accessor instanceof Level level) || !level.isClientSide) {
                return accessor.hasChunk(chunkX, chunkZ);
            }
            //Don't allow the client level to just return true for all cases, as we actually care if it is present
            // and instead use the fallback logic that we have
        }
        return world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }


    @Contract("null, _ -> false")
    public static boolean isBlockLoaded(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (world == null) {
            return false;
        } else if (world instanceof LevelReader reader) {
            if (reader instanceof Level level && !level.isInWorldBounds(pos)) {
                return false;
            }
            //TODO: If any cases come up where things are behaving oddly due to the change from reader.hasChunkAt(pos)
            // re-evaluate this and if the specific case is being handled properly
            return isChunkLoaded(reader, pos);
        }
        return true;
    }


    @Contract("null, _ -> false")
    public static boolean isBlockInBounds(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (world == null) {
            return false;
        } else if (world instanceof LevelReader reader) {
            return !(reader instanceof Level level) || level.isInWorldBounds(pos);
        }
        return true;
    }


    @Nullable
    @Contract("null, _, _ -> null")
    private static ChunkAccess getChunkForPos(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        if (!isBlockInBounds(world, pos)) {
            //Allow the world to be nullable to remove warnings when we are calling things from a place that world could be null
            // Also short circuit to check if the position is out of bounds before bothering to look up the chunk
            return null;
        }
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        long combinedChunk = ChunkPos.asLong(chunkX, chunkZ);
        //We get the chunk rather than the world, so we can cache the chunk improving the overall
        // performance for retrieving a bunch of chunks in the general vicinity
        ChunkAccess chunk = chunkMap.get(combinedChunk);
        if (chunk == null) {
            //Get the chunk but don't force load it
            chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk != null) {
                chunkMap.put(combinedChunk, chunk);
            }
        }
        return chunk;
    }


    @NotNull
    public static Optional<BlockState> getBlockState(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        //Get the blockstate using the chunk we found/had cached
        return getBlockState(getChunkForPos(world, chunkMap, pos), pos);
    }


    @NotNull
    public static Optional<BlockState> getBlockState(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return empty
            return Optional.empty();
        }
        return Optional.of(world.getBlockState(pos));
    }


    @NotNull
    public static Optional<FluidState> getFluidState(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        //Get the fluidstate using the chunk we found/had cached
        return getFluidState(getChunkForPos(world, chunkMap, pos), pos);
    }


    @NotNull
    public static Optional<FluidState> getFluidState(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return empty
            return Optional.empty();
        }
        return Optional.of(world.getFluidState(pos));
    }


    @Nullable
    @Contract("null, _, _ -> null")
    public static BlockEntity getTileEntity(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        //Get the tile entity using the chunk we found/had cached
        return getTileEntity(getChunkForPos(world, chunkMap, pos), pos);
    }


    @Nullable
    @Contract("_, null, _, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        return getTileEntity(clazz, world, chunkMap, pos, false);
    }


    @Nullable
    @Contract("_, null, _, _, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos,
                                                          boolean logWrongType) {
        //Get the tile entity using the chunk we found/had cached
        return getTileEntity(clazz, getChunkForPos(world, chunkMap, pos), pos, logWrongType);
    }


    @Nullable
    @Contract("null, _ -> null")
    public static BlockEntity getTileEntity(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return null
            return null;
        }
        return world.getBlockEntity(pos);
    }


    @Nullable
    @Contract("_, null, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable BlockGetter world, @NotNull BlockPos pos) {
        return getTileEntity(clazz, world, pos, false);
    }


    @Nullable
    @Contract("_, null, _, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable BlockGetter world, @NotNull BlockPos pos, boolean logWrongType) {
        BlockEntity tile = getTileEntity(world, pos);
        if (tile == null) {
            return null;
        }
        if (clazz.isInstance(tile)) {
            return clazz.cast(tile);
        } else if (logWrongType) {
            CreateNuclear.LOGGER.warn("Unexpected BlockEntity class at {}, expected {}, but found: {}", pos, clazz, tile.getClass());
        }
        return null;
    }


    public static void saveChunk(BlockEntity tile) {
        if (tile != null && !tile.isRemoved() && tile.getLevel() != null) {
            markChunkDirty(tile.getLevel(), tile.getBlockPos());
        }
    }


    public static void markChunkDirty(Level world, BlockPos pos) {
        if (isBlockLoaded(world, pos)) {
            world.getChunkAt(pos).setUnsaved(true);
        }
    }


    public static void dismantleBlock(BlockState state, Level world, BlockPos pos) {
        dismantleBlock(state, world, pos, getTileEntity(world, pos));
    }


    public static void dismantleBlock(BlockState state, Level world, BlockPos pos, @Nullable BlockEntity tile) {
        if (world instanceof ServerLevel level) {//Copy of Block#dropResources but skipping the xp dropping
            Block.getDrops(state, level, pos, tile).forEach(stack -> Block.popResource(world, pos, stack));
            state.spawnAfterBreak(level, pos, ItemStack.EMPTY, false);
        }
        world.removeBlock(pos, false);
    }


    public static double distanceBetween(BlockPos start, BlockPos end) {
        return Math.sqrt(start.distSqr(end));
    }


    public static Direction sideDifference(BlockPos pos, BlockPos other) {
        BlockPos diff = pos.subtract(other);
        for (Direction side : Direction.values()) {
            if (side.getStepX() == diff.getX() && side.getStepY() == diff.getY() && side.getStepZ() == diff.getZ()) {
                return side;
            }
        }
        return null;
    }


//    public static boolean isChunkVibrated(ChunkPos chunk, Level world) {
//        return Mekanism.activeVibrators.stream().anyMatch(coord -> coord.dimension == world.dimension() && SectionPos.blockToSectionCoord(coord.getX()) == chunk.x &&
//                SectionPos.blockToSectionCoord(coord.getZ()) == chunk.z);
//    }

    public static boolean tryPlaceContainedLiquid(@Nullable Player player, Level world, BlockPos pos, @NotNull FluidStack fluidStack, @Nullable Direction side) {
        Fluid fluid = fluidStack.getFluid();
        FluidType fluidType = fluid.getFluidType();
        if (!fluidType.canBePlacedInLevel(world, pos, fluidStack)) {
            //If there is no fluid, or it cannot be placed in the world just
            return false;
        }
        BlockState state = world.getBlockState(pos);
        boolean isReplaceable = state.canBeReplaced(fluid);
        boolean canContainFluid = state.getBlock() instanceof LiquidBlockContainer liquidBlockContainer && liquidBlockContainer.canPlaceLiquid(world, pos, state, fluid);
        if (state.isAir() || isReplaceable || canContainFluid) {
            if (world.dimensionType().ultraWarm() && fluidType.isVaporizedOnPlacement(world, pos, fluidStack)) {
                fluidType.onVaporize(player, world, pos, fluidStack);
            } else if (canContainFluid) {
                if (!((LiquidBlockContainer) state.getBlock()).placeLiquid(world, pos, state, fluidType.getStateForPlacement(world, pos, fluidStack))) {
                    //If something went wrong return that we couldn't actually place it
                    return false;
                }
                playEmptySound(player, world, pos, fluidType, fluidStack);
            } else {
                if (!world.isClientSide() && isReplaceable && !state.liquid()) {
                    world.destroyBlock(pos, true);
                }
                playEmptySound(player, world, pos, fluidType, fluidStack);
                world.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL_IMMEDIATE);
            }
            return true;
        }
        return side != null && tryPlaceContainedLiquid(player, world, pos.relative(side), fluidStack, null);
    }

    private static void playEmptySound(@Nullable Player player, LevelAccessor world, BlockPos pos, FluidType fluidType, @NotNull FluidStack fluidStack) {
        SoundEvent soundevent = fluidType.getSound(player, world, pos, SoundActions.BUCKET_EMPTY);
        if (soundevent == null) {
            soundevent = SoundEvents.BUCKET_EMPTY_LAVA;
        }
        world.playSound(player, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static void playFillSound(@Nullable Player player, LevelAccessor world, BlockPos pos, @NotNull FluidStack fluidStack, @Nullable SoundEvent soundEvent) {
        if (soundEvent == null) {
            Fluid fluid = fluidStack.getFluid();
            soundEvent = fluid.getPickupSound().orElseGet(() -> fluid.getFluidType().getSound(player, world, pos, SoundActions.BUCKET_FILL));
        }
        if (soundEvent != null) {
            world.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }


    public static boolean isGettingPowered(Level world, BlockPos pos) {
        if (isBlockLoaded(world, pos)) {
            for (Direction side : Direction.values()) {
                BlockPos offset = pos.relative(side);
                if (isBlockLoaded(world, offset)) {
                    BlockState blockState = world.getBlockState(offset);
                    boolean weakPower = blockState.getBlock().shouldCheckWeakPower(blockState, world, pos, side);
                    if (weakPower && isDirectlyGettingPowered(world, offset) || !weakPower && blockState.getSignal(world, offset, side) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public static boolean isDirectlyGettingPowered(Level world, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos offset = pos.relative(side);
            if (isBlockLoaded(world, offset)) {
                if (world.getSignal(pos, side) > 0) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean areBlocksValidAndReplaceable(@NotNull BlockGetter world, @NotNull BlockPos... positions) {
        return areBlocksValidAndReplaceable(world, Arrays.stream(positions));
    }


    public static boolean areBlocksValidAndReplaceable(@NotNull BlockGetter world, @NotNull Collection<BlockPos> positions) {
        //TODO: Potentially move more block placement over to these methods
        return areBlocksValidAndReplaceable(world, positions.stream());
    }


    public static boolean areBlocksValidAndReplaceable(@NotNull BlockGetter world, @NotNull Stream<BlockPos> positions) {
        return positions.allMatch(pos -> isValidReplaceableBlock(world, pos));
    }


    public static boolean isValidReplaceableBlock(@NotNull BlockGetter world, @NotNull BlockPos pos) {
        return isBlockInBounds(world, pos) && world.getBlockState(pos).canBeReplaced();
    }


    public static void notifyLoadedNeighborsOfTileChange(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        for (Direction dir : Direction.values()) {
            BlockPos offset = pos.relative(dir);
            if (isBlockLoaded(world, offset)) {
                notifyNeighborOfChange(world, offset, pos);
                if (world.getBlockState(offset).isRedstoneConductor(world, offset)) {
                    offset = offset.relative(dir);
                    if (isBlockLoaded(world, offset)) {
                        Block block1 = world.getBlockState(offset).getBlock();
                        //TODO: Make sure this is passing the correct state
                        if (block1.getWeakChanges(state, world, offset)) {
                            block1.onNeighborChange(state, world, offset, pos);
                        }
                    }
                }
            }
        }
    }


    public static void notifyNeighborsOfChange(@Nullable Level world, BlockPos fromPos, Set<Direction> neighbors) {
        if (!neighbors.isEmpty()) {
            getBlockState(world, fromPos).ifPresent(sourceState -> {
                for (Direction neighbor : neighbors) {
                    BlockPos pos = fromPos.relative(neighbor);
                    getBlockState(world, pos).ifPresent(state -> {
                        state.onNeighborChange(world, pos, fromPos);
                        state.neighborChanged(world, pos, sourceState.getBlock(), fromPos, false);
                    });
                }
            });
        }
    }


    public static void notifyNeighborOfChange(@Nullable Level world, BlockPos pos, BlockPos fromPos) {
        getBlockState(world, pos).ifPresent(state -> {
            state.onNeighborChange(world, pos, fromPos);
            state.neighborChanged(world, pos, world.getBlockState(fromPos).getBlock(), fromPos, false);
        });
    }


    public static void notifyNeighborOfChange(@Nullable Level world, Direction neighborSide, BlockPos fromPos) {
        notifyNeighborOfChange(world, fromPos.relative(neighborSide), fromPos);
    }


    public static void updateBlock(@Nullable Level world, @NotNull BlockPos pos, BlockState state) {
        if (isBlockLoaded(world, pos)) {
            world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
    }


    public static void recheckLighting(@Nullable BlockAndTintGetter world, @NotNull BlockPos pos) {
        if (isBlockLoaded(world, pos)) {
            world.getLightEngine().checkBlock(pos);
        }
    }

    /**
     * Vanilla copy of {@link net.minecraft.client.multiplayer.ClientLevel#getSkyDarken(float)} used to be World#getSunBrightness
     */
    public static float getSunBrightness(Level world, float partialTicks) {
        float f = world.getTimeOfDay(partialTicks);
        float f1 = 1.0F - (Mth.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.2F);
        f1 = Mth.clamp(f1, 0.0F, 1.0F);
        f1 = 1.0F - f1;
        f1 = (float) (f1 * (1.0D - world.getRainLevel(partialTicks) * 5.0F / 16.0D));
        f1 = (float) (f1 * (1.0D - world.getThunderLevel(partialTicks) * 5.0F / 16.0D));
        return f1 * 0.8F + 0.2F;
    }


    @Contract("null, _ -> false")
    public static boolean canSeeSun(@Nullable Level world, BlockPos pos) {
        //Note: We manually handle the world#isDaytime check by just checking the subtracted skylight
        // as vanilla returns false if the world's time is set to a fixed value even if that time
        // would effectively be daytime
        return world != null && world.dimensionType().hasSkyLight() && world.getSkyDarken() < 4 && world.canSeeSky(pos);
    }


    public static BlockPos getBlockPosFromChunkPos(long chunkPos) {
        return new BlockPos((int) chunkPos, 0, (int) (chunkPos >> 32));
    }
}
