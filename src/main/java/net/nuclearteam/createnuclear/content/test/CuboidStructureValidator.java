package net.nuclearteam.createnuclear.content.test;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.nuclearteam.createnuclear.content.test.interfac.IShape;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.*;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.FormationProtocol.StructureRequirement;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.FormationProtocol.FormationResult;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.IValveHandler.ValveData;
import net.nuclearteam.createnuclear.content.test.VoxelCuboid.WallRelative;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;

import java.util.Optional;
import java.util.UUID;

public abstract class CuboidStructureValidator<T extends MultiblockData> implements IStructureValidator<T> {

    private final VoxelCuboid minBounds;
    private final VoxelCuboid maxBounds;

    protected VoxelCuboid cuboid;
    protected Structure structure;

    protected Level world;
    protected MultiblockManager<T> manager;

    public CuboidStructureValidator() {
        this(new VoxelCuboid(3, 3, 3), new VoxelCuboid(18, 18, 18));
    }

    public CuboidStructureValidator(VoxelCuboid minBounds, VoxelCuboid maxBounds) {
        this.minBounds = minBounds;
        this.maxBounds = maxBounds;
    }

    @Override
    public void init(Level world, MultiblockManager<T> manager, Structure structure) {
        this.world = world;
        this.manager = manager;
        this.structure = structure;
    }

    @Override
    public FormationResult validate(FormationProtocol<T> ctx, Long2ObjectMap<ChunkAccess> chunkMap) {
        BlockPos min = cuboid.getMinPos(), max = cuboid.getMaxPos();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    mutablePos.set(x, y, z);
                    FormationResult ret = validateNode(ctx, chunkMap, mutablePos);
                    if (!ret.formed()) {
                        return ret;
                    }
                }
            }
        }
        return FormationResult.SUCCESS;
    }

    /**
     * @param pos Mutable BlockPos
     */
    protected FormationResult validateNode(FormationProtocol<T> ctx, Long2ObjectMap<ChunkAccess> chunkMap, BlockPos pos) {
        Optional<BlockState> optionalState = WorldUtils.getBlockState(world, chunkMap, pos);
        if (optionalState.isEmpty()) {
            //If the position is not in a loaded chunk or out of bounds of the world, fail
            return FormationResult.FAIL;
        }
        BlockState state = optionalState.get();
        StructureRequirement requirement = getStructureRequirement(pos);
        if (requirement.isCasing()) {
            FormationProtocol.CasingType type = getCasingType(state);
            FormationResult ret = validateFrame(ctx, pos, state, type, requirement.needsFrame());
            if ((requirement != StructureRequirement.IGNORED || ret.noIgnore()) && !ret.formed()) {
                return ret;
            }
        } else if (!validateInner(state, chunkMap, pos)) {
            return FormationResult.fail(CreateNuclearLang.translateDirect("MULTIBLOCK_INVALID_INNER", pos), true);
        } else if (!state.isAir()) {
            //Make sure the position is immutable before we store it
            ctx.internalLocations.add(pos.immutable());
        }
        return FormationResult.SUCCESS;
    }

    /**
     * @param pos Mutable BlockPos
     */
    protected boolean validateInner(BlockState state, Long2ObjectMap<ChunkAccess> chunkMap, BlockPos pos) {
        return state.isAir();
    }

    protected abstract FormationProtocol.CasingType getCasingType(BlockState state);

    protected boolean isFrameCompatible(BlockEntity tile) {
        if (tile instanceof IStructuralMultiblock multiblock && multiblock.canInterface(manager)) {
            return true;
        }
        return manager.isCompatible(tile);
    }

    /**
     * @param pos Mutable BlockPos
     */
    protected FormationResult validateFrame(FormationProtocol<T> ctx, BlockPos pos, BlockState state, FormationProtocol.CasingType type, boolean needsFrame) {
        IMultiblockBase tile = structure.getTile(pos);
        // terminate if we encounter a node that already failed this tick
        if (!isFrameCompatible((BlockEntity) tile) || (needsFrame && !type.isFrame())) {
            //If it is not a valid node or if it is supposed to be a frame but is invalid
            // then we are not valid over all
            return FormationResult.fail(CreateNuclearLang.translateDirect("MULTIBLOCK_INVALID_INNER", pos), true);
        }
        if (tile instanceof IMultiblock<?> multiblockTile) {
            UUID uuid = multiblockTile.getCacheID();
            if (uuid != null && multiblockTile.getManager() == manager) {
                MultiblockCache<T> cache = manager.getCache(uuid);
                if (cache == null) {
                    //If there is no cache matching the id the multiblock has reset the id it has cached as it is stale
                    multiblockTile.resetCache();
                } else {
                    ctx.idsFound.put(uuid, cache);
                }
            }
        }
        //Make sure the position is immutable before we store it
        pos = pos.immutable();
        ctx.locations.add(pos);
        if (type.isValve()) {
            ValveData data = new ValveData(pos, getSide(pos));
            ctx.valves.add(data);
        }
        return FormationResult.SUCCESS;
    }

    @Override
    public FormationResult postcheck(T structure, Long2ObjectMap<ChunkAccess> chunkMap) {
        return FormationResult.SUCCESS;
    }

    protected StructureRequirement getStructureRequirement(BlockPos pos) {
        WallRelative relative = cuboid.getWallRelative(pos);
        if (relative.isOnEdge()) {
            return StructureRequirement.FRAME;
        }
        return relative.isWall() ? StructureRequirement.OTHER : StructureRequirement.INNER;
    }

    protected Direction getSide(BlockPos pos) {
        return cuboid.getSide(pos);
    }

    @Override
    public IShape getShape() {
        return cuboid;
    }

    @Override
    public boolean precheck() {
        cuboid = StructureHelper.fetchCuboid(structure, minBounds, maxBounds);
        return cuboid != null;
    }

    public void loadCuboid(VoxelCuboid cuboid) {
        this.cuboid = cuboid;
    }
}
