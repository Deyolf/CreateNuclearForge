package net.nuclearteam.createnuclear.content.test;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.nuclearteam.createnuclear.CNBlocks;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.FormationProtocol.FormationResult;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.FormationProtocol.StructureRequirement;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.FormationProtocol.CasingType;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.IValveHandler.ValveData;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.Structure.Axis;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.StructureHelper;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;

import java.util.EnumSet;
import java.util.Set;

public class ReactorValidator extends CuboidStructureValidator<ReactorMultiblockData> {
    private static final VoxelCuboid BOUNDS = new VoxelCuboid(5, 7, 5);
    private static final byte[][] ALLOWED_GRID = {
            {1, 1, 1, 1, 1},
            {0, 1, 2, 2, 2},
            {1, 2, 2, 2, 2},
            {1, 2, 2, 2, 2},
            {1, 2, 2, 2, 2},
            {0, 1, 2, 2, 2},
            {0, 0, 1, 1, 1}
    };

    @Override
    protected StructureRequirement getStructureRequirement(BlockPos pos) {
        VoxelCuboid.WallRelative relative = cuboid.getWallRelative(pos);
        if (relative.isWall()) {
            Axis axis = Axis.get(cuboid.getSide(pos));
            Axis h = axis.horizontal(), v = axis.vertical();
            //Note: This ends up becoming immutable by doing this but that is fine and doesn't really matter
            pos = pos.subtract(cuboid.getMinPos());
            return StructureRequirement.REQUIREMENTS[ALLOWED_GRID[h.getCoord(pos)][v.getCoord(pos)]];
        }
        return super.getStructureRequirement(pos);
    }

    @Override
    protected CasingType getCasingType(BlockState state) {
        Block block = state.getBlock();
        if (CNBlocks.REACTOR_CASING.is(block)) {
            return CasingType.FRAME;
        } else if (CNBlocks.REACTOR_INPUT.is(block)) {
            return CasingType.VALVE;
        }
        return CasingType.INVALID;
    }

    @Override
    protected boolean validateInner(BlockState state, Long2ObjectMap<ChunkAccess> chunkMap, BlockPos pos) {
        if (super.validateInner(state, chunkMap, pos)) {
            return true;
        }
        return CNBlocks.REACTOR_COOLER.is(state.getBlock());
    }

    @Override
    public boolean precheck() {
        // 72 = (12 missing blocks possible on each face) * (6 sides)
        cuboid = StructureHelper.fetchCuboid(structure, BOUNDS, BOUNDS, EnumSet.allOf(VoxelCuboid.CuboidSide.class), 72);
        return cuboid != null;
    }

    @Override
    public FormationResult postcheck(ReactorMultiblockData structure, Long2ObjectMap<ChunkAccess> chunkMap) {
        Set<BlockPos> validCoils = new ObjectOpenHashSet<>();
        for (ValveData valve : structure.valves) {
            BlockPos pos = valve.location.relative(valve.side.getOpposite());
            if (structure.internalLocations.contains(pos)) {
//                structure.addCoil(valve.location, valve.side.getOpposite());
                validCoils.add(pos);
            }
        }
        // fail if there's a coil not connected to a port
        // Note: As we only support coils as internal multiblocks for the SPS we can just compare the size of the sets
        if (structure.internalLocations.size() != validCoils.size()) {
            return FormationResult.fail(CreateNuclearLang.text("SPS_INVALID_DISCONNECTED_COIL").component());
        }
        return FormationResult.SUCCESS;
    }
}
