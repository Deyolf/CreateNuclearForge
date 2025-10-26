package net.nuclearteam.createnuclear.content.test.interfac.multiblock;


import net.nuclearteam.createnuclear.content.test.VoxelCuboid;
import net.nuclearteam.createnuclear.content.test.VoxelPlane;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.Structure.Axis;
import  net.nuclearteam.createnuclear.content.test.VoxelCuboid.CuboidSide;
import  net.nuclearteam.createnuclear.content.test.VoxelCuboid.CuboidSide.Face;
import  net.nuclearteam.createnuclear.content.test.VoxelCuboid.CuboidBuilder;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

public class StructureHelper {

    private StructureHelper() {
    }

    public static VoxelCuboid fetchCuboid(Structure structure, VoxelCuboid minBounds, VoxelCuboid maxBounds) {
        VoxelCuboid prev = null;
        for (Axis axis : Axis.AXES) {
            NavigableMap<Integer, VoxelPlane> majorAxisMap = structure.getMajorAxisMap(axis);
            Map.Entry<Integer, VoxelPlane> firstMajor = majorAxisMap.firstEntry(), lastMajor = majorAxisMap.lastEntry();
            if (firstMajor == null || !firstMajor.getValue().equals(lastMajor.getValue()) || !firstMajor.getValue().isFull()) {
                //If there is no major plane, or the two parallel planes are mismatched in size,
                // or if the plane is missing pieces then fail
                return null;
            }
            VoxelCuboid cuboid = VoxelCuboid.from(firstMajor.getValue(), lastMajor.getValue(), firstMajor.getKey(), lastMajor.getKey());
            // if this is the first axial cuboid check, make sure we have the correct bounds
            if (prev == null && (!cuboid.greaterOrEqual(minBounds) || !maxBounds.greaterOrEqual(cuboid))) {
                return null;
            }
            // if this isn't the first axial cuboid check, make sure the cuboids match
            if (prev != null && !prev.equals(cuboid)) {
                return null;
            }
            // check to make sure that we don't have any framed minor planes sticking out of our cuboid
            NavigableMap<Integer, VoxelPlane> minorAxisMap = structure.getMinorAxisMap(axis);
            if (!minorAxisMap.isEmpty()) {
                if (hasOutOfBoundsNegativeMinor(minorAxisMap, firstMajor.getKey()) || hasOutOfBoundsPositiveMinor(minorAxisMap, lastMajor.getKey())) {
                    return null;
                }
            }
            prev = cuboid;
        }
        return prev;
    }


    public static VoxelCuboid fetchCuboid(Structure structure, VoxelCuboid minBounds, VoxelCuboid maxBounds, Set<CuboidSide> sides, int tolerance) {
        // make sure we have enough sides to create cuboidal dimensions
        if (sides.size() < 2) {
            return null;
        }
        int missing = 0;
        CuboidBuilder builder = new CuboidBuilder();
        for (CuboidSide side : sides) {
            Axis axis = side.getAxis(), horizontal = axis.horizontal(), vertical = axis.vertical();
            NavigableMap<Integer, VoxelPlane> majorAxisMap = structure.getMajorAxisMap(axis);
            Map.Entry<Integer, VoxelPlane> majorEntry = side.getFace().isPositive() ? majorAxisMap.lastEntry() : majorAxisMap.firstEntry();
            // fail fast if the plane doesn't exist
            if (majorEntry == null) {
                return null;
            }
            VoxelPlane plane = majorEntry.getValue();
            // handle missing blocks based on tolerance value
            missing += plane.getMissing();
            if (missing > tolerance) {
                return null;
            }
            int majorKey = majorEntry.getKey();
            // set bounds from dimension of plane's axis
            builder.set(side, majorKey);
            // update cuboidal dimensions from each corner of the plane
            if (!builder.trySet(CuboidSide.get(Face.NEGATIVE, horizontal), plane.getMinCol()) ||
                    !builder.trySet(CuboidSide.get(Face.POSITIVE, horizontal), plane.getMaxCol()) ||
                    !builder.trySet(CuboidSide.get(Face.NEGATIVE, vertical), plane.getMinRow()) ||
                    !builder.trySet(CuboidSide.get(Face.POSITIVE, vertical), plane.getMaxRow())) {
                return null;
            }
            // check to make sure that we don't have any framed minor planes sticking out of our plane
            NavigableMap<Integer, VoxelPlane> minorAxisMap = structure.getMinorAxisMap(axis);
            if (!minorAxisMap.isEmpty()) {
                if (side.getFace().isPositive()) {
                    if (hasOutOfBoundsPositiveMinor(minorAxisMap, majorKey)) {
                        return null;
                    }
                } else if (hasOutOfBoundsNegativeMinor(minorAxisMap, majorKey)) {
                    return null;
                }
            }
        }
        VoxelCuboid ret = builder.build();
        // make sure the cuboid has the correct bounds
        if (!ret.greaterOrEqual(minBounds) || !maxBounds.greaterOrEqual(ret)) {
            return null;
        }
        return ret;
    }


    private static boolean hasOutOfBoundsPositiveMinor(NavigableMap<Integer, VoxelPlane> minorAxisMap, int majorKey) {
        Map.Entry<Integer, VoxelPlane> minorEntry = minorAxisMap.lastEntry();
        while (minorEntry != null) {
            int minorKey = minorEntry.getKey();
            if (minorKey <= majorKey) {
                //If our outer minor plane is in the bounds of our major plane
                // then just exit as the other minor planes will be as well
                break;
            } else if (minorEntry.getValue().hasFrame()) {
                //Otherwise, if it isn't in the bounds, and it has a frame, fail out
                return true;
            }
            //If we don't have a frame and are out of bounds, see if we have any minor entries that
            // are "smaller" that may be invalid
            minorEntry = minorAxisMap.lowerEntry(minorKey);
        }
        return false;
    }


    private static boolean hasOutOfBoundsNegativeMinor(NavigableMap<Integer, VoxelPlane> minorAxisMap, int majorKey) {
        Map.Entry<Integer, VoxelPlane> minorEntry = minorAxisMap.firstEntry();
        while (minorEntry != null) {
            int minorKey = minorEntry.getKey();
            if (minorKey >= majorKey) {
                //If our outer minor plane is in the bounds of our major plane
                // then just exit as the other minor planes will be as well
                break;
            } else if (minorEntry.getValue().hasFrame()) {
                //Otherwise, if it isn't in the bounds, and it has a frame, fail out
                return true;
            }
            //If we don't have a frame and are out of bounds, see if we have any minor entries that
            // are "smaller" that may be invalid
            minorEntry = minorAxisMap.higherEntry(minorKey);
        }
        return false;
    }
}
