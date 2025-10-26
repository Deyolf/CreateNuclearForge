package net.nuclearteam.createnuclear.content.test;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import net.nuclearteam.createnuclear.content.test.enu.AutomationType;
import net.nuclearteam.createnuclear.content.test.interfac.IContentsListener;
import net.nuclearteam.createnuclear.content.test.interfac.IShape;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.ICNInventory;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.IInventorySlot;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid.ICNFluidHandler;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid.IExtendedFluidTank;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.*;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.IValveHandler.ValveData;
import net.nuclearteam.createnuclear.content.test.MultiblockCache.CacheSubstance;
import net.nuclearteam.createnuclear.content.test.VoxelCuboid.CuboidRelative;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class MultiblockData implements ICNInventory, ICNFluidHandler {
    protected static final Map<Direction, Set<Direction>> SIDE_REFERENCES = new EnumMap<>(Direction.class);
    public Set<BlockPos> locations = new ObjectOpenHashSet<>();

    public Set<BlockPos> internalLocations = new ObjectOpenHashSet<>();
    public Set<ValveData> valves = new ObjectOpenHashSet<>();

    private int volume;

    public UUID inventoryID;

    public boolean hasMaster;

    @Nullable//may be null if structure has not been fully sent
    public BlockPos renderLocation;

    private VoxelCuboid bounds = new VoxelCuboid(0, 0, 0);

    private boolean formed;
    public boolean recheckStructure;

    private int currentRedstoneLevel;

    private final BooleanSupplier remoteSupplier;
    private final Supplier<Level> worldSupplier;

    protected final List<IInventorySlot> inventorySlots = new ArrayList<>();
    protected final List<IExtendedFluidTank> fluidTanks = new ArrayList<>();

    private final BiPredicate<Object, @NotNull AutomationType> formedBiPred = (t, automationType) -> isFormed();
    private final BiPredicate<Object, @NotNull AutomationType> notExternalFormedBiPred = (t, automationType) -> automationType != AutomationType.EXTERNAL && isFormed();

    private boolean dirty;

    public MultiblockData(BlockEntity tile) {
        remoteSupplier = () -> tile.getLevel().isClientSide();
        worldSupplier = tile::getLevel;
    }

    @SuppressWarnings("unchecked")
    public <T> BiPredicate<T, @NotNull AutomationType> formedBiPred() {
        return (BiPredicate<T, @NotNull AutomationType>) formedBiPred;
    }

    @SuppressWarnings("unchecked")
    public <T> BiPredicate<T, @NotNull AutomationType> notExternalFormedBiPred() {
        return (BiPredicate<T, @NotNull AutomationType>) notExternalFormedBiPred;
    }

    protected IContentsListener createSaveAndComparator() {
        return createSaveAndComparator(this);
    }

    protected IContentsListener createSaveAndComparator(IContentsListener contentsListener) {
        return () -> {
            contentsListener.onContentsChanged();
            if (!isRemote()) {
                markDirtyComparator(getWorld());
            }
        };
    }

    public boolean isDirty() {
        return dirty;
    }

    public void resetDirty() {
        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    /**
     * Tick the multiblock.
     *
     * @return if we need an update packet
     */
    public boolean tick(Level world) {
        boolean needsPacket = false;
        for (ValveData data : valves) {
            data.activeTicks = Math.max(0, data.activeTicks - 1);
            if (data.activeTicks > 0 != data.prevActive) {
                needsPacket = true;
            }
            data.prevActive = data.activeTicks > 0;
        }
        return needsPacket;
    }

//    protected double calculateAverageAmbientTemperature(Level world) {
//        //Take a rough average of the biome temperature by calculating the average of all the corners of the multiblock
//        BlockPos min = getMinPos();
//        BlockPos max = getMaxPos();
//        return HeatAPI.getAmbientTemp(getBiomeTemp(world,
//                min,
//                new BlockPos(max.getX(), min.getY(), min.getZ()),
//                new BlockPos(min.getX(), min.getY(), max.getZ()),
//                new BlockPos(max.getX(), min.getY(), max.getZ()),
//                new BlockPos(min.getX(), max.getY(), min.getZ()),
//                new BlockPos(max.getX(), max.getY(), min.getZ()),
//                new BlockPos(min.getX(), max.getY(), max.getZ()),
//                max
//        ));
//    }

//    private static double getBiomeTemp(Level world, BlockPos... positions) {
//        if (positions.length == 0) {
//            throw new IllegalArgumentException("No positions given.");
//        }
//        return Arrays.stream(positions).mapToDouble(pos -> world.getBiome(pos).value().getTemperature(pos)).sum() / positions.length;
//    }

    public boolean setShape(IShape shape) {
        if (shape instanceof VoxelCuboid cuboid) {
            bounds = cuboid;
            renderLocation = cuboid.getMinPos().relative(Direction.UP);
            setVolume(bounds.length() * bounds.width() * bounds.height());
            return true;
        }
        return false;
    }

    public void onCreated(Level world) {
        for (BlockPos pos : internalLocations) {
            BlockEntity tile = WorldUtils.getTileEntity(world, pos);
            if (tile instanceof IInternalMultiblock internalMultiblock) {
                internalMultiblock.setMultiblock(this);
            }
        }

        if (shouldCap(CacheSubstance.FLUID)) {
            for (IExtendedFluidTank tank : getFluidTanks(null)) {
                tank.setStackSize(Math.min(tank.getFluidAmount(), tank.getCapacity()), Action.EXECUTE);
            }
        }

        updateEjectors(world);
        forceUpdateComparatorLevel();
    }

    protected void updateEjectors(Level world) {
        for (ValveData valve : valves) {
            BlockEntity tile = WorldUtils.getTileEntity(world, valve.location);
            if (tile instanceof IMultiblockEjector ejector) {//Check if this valve is an ejector as not all of them are
                //Ensure we don't use create a bunch of identical collections potentially using up a bunch of memory
                Set<Direction> sides = SIDE_REFERENCES.computeIfAbsent(valve.side, Collections::singleton);
                ejector.setEjectSides(sides);
            }
        }
    }

    protected boolean isRemote() {
        return remoteSupplier.getAsBoolean();
    }

    protected Level getWorld() {
        return worldSupplier.get();
    }

    protected boolean shouldCap(CacheSubstance<?, ?> type) {
        return true;
    }

    public void remove(Level world) {
        for (BlockPos pos : internalLocations) {
            BlockEntity tile = WorldUtils.getTileEntity(world, pos);
            if (tile instanceof IInternalMultiblock internalMultiblock) {
                internalMultiblock.setMultiblock(null);
            }
        }
        inventoryID = null;
        formed = false;
        recheckStructure = false;
    }

    public void meltdownHappened(Level world) {
    }

    public void readUpdateTag(CompoundTag tag) {
        NBTUtils.setIntIfPresent(tag, "volume", this::setVolume);
        NBTUtils.setBlockPosIfPresent(tag, "renderLocation", value -> renderLocation = value);
        bounds = new VoxelCuboid(NbtUtils.readBlockPos(tag.getCompound("min")),
                NbtUtils.readBlockPos(tag.getCompound("max")));
        NBTUtils.setUUIDIfPresentElse(tag, "inventoryID", value -> inventoryID = value, () -> inventoryID = null);
    }

    public void writeUpdateTag(CompoundTag tag) {
        tag.putInt("volume", getVolume());
        if (renderLocation != null) {//In theory this shouldn't be null here but check it anyway
            tag.put("renderLocation", NbtUtils.writeBlockPos(renderLocation));
        }
        tag.put("min", NbtUtils.writeBlockPos(bounds.getMinPos()));
        tag.put("max", NbtUtils.writeBlockPos(bounds.getMaxPos()));
        if (inventoryID != null) {
            tag.putUUID("inventoryID", inventoryID);
        }
    }

    public int length() {
        return bounds.length();
    }

    public int getLength() {
        return this.length();
    }

    public int width() {
        return bounds.width();
    }

    public int getWidth() {
        return this.width();
    }

    public int height() {
        return bounds.height();
    }

    public int getHeight() {
        return this.height();
    }

    public BlockPos getMinPos() {
        return bounds.getMinPos();
    }

    public BlockPos getMaxPos() {
        return bounds.getMaxPos();
    }

    public VoxelCuboid getBounds() {
        return bounds;
    }

    /**
     * Checks if this multiblock is formed and the given position is insides the bounds of this multiblock
     */
    public <T extends MultiblockData> boolean isPositionInsideBounds(@NotNull Structure structure, @NotNull BlockPos pos) {
        if (isFormed()) {
            CuboidRelative relativeLocation = getBounds().getRelativeLocation(pos);
            if (relativeLocation == CuboidRelative.INSIDE) {
                return true;
            } else if (relativeLocation.isWall()) {
                //If we are in the wall check if we are really an inner position. For example evap towers
                MultiblockManager<T> manager = (MultiblockManager<T>) structure.getManager();
                if (manager != null) {
                    IStructureValidator<T> validator = manager.createValidator();
                    if (validator instanceof CuboidStructureValidator<T> cuboidValidator) {
                        validator.init(getWorld(), manager, structure);
                        cuboidValidator.loadCuboid(getBounds());
                        return cuboidValidator.getStructureRequirement(pos) == FormationProtocol.StructureRequirement.INNER;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if this multiblock is formed and the given position is insides the bounds of this multiblock
     */
    public boolean isPositionOutsideBounds(@NotNull BlockPos pos) {
        return isFormed() && getBounds().getRelativeLocation(pos) == CuboidRelative.OUTSIDE;
    }

    @Nullable
    public Direction getOutsideSide(@NotNull BlockPos pos) {
        if (isFormed()) {
            VoxelCuboid bounds = getBounds();
            for (Direction direction : Direction.values()) {
                if (bounds.getRelativeLocation(pos.relative(direction)) == CuboidRelative.OUTSIDE) {
                    return direction;
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public List<IInventorySlot> getInventorySlots(@Nullable Direction side) {
        return isFormed() ? inventorySlots : Collections.emptyList();
    }

    @NotNull
    @Override
    public List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side) {
        return isFormed() ? fluidTanks : Collections.emptyList();
    }

    public Set<Direction> getDirectionsToEmit(BlockPos pos) {
        Set<Direction> directionsToEmit = null;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (!isKnownLocation(neighborPos)) {
                if (directionsToEmit == null) {
                    directionsToEmit = EnumSet.noneOf(Direction.class);
                }
                directionsToEmit.add(direction);
            }
        }
        return directionsToEmit == null ? Collections.emptySet() : directionsToEmit;
    }

    public boolean isKnownLocation(BlockPos pos) {
        return locations.contains(pos) || internalLocations.contains(pos);
    }

    public Collection<ValveData> getValveData() {
        return valves;
    }

    @Override
    public void onContentsChanged() {
        markDirty();
    }

    @Override
    public int hashCode() {
        int code = 1;
        code = 31 * code + locations.hashCode();
        code = 31 * code + bounds.hashCode();
        code = 31 * code + getVolume();
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        MultiblockData data = (MultiblockData) obj;
        if (!data.locations.equals(locations)) {
            return false;
        }
        if (!data.bounds.equals(bounds)) {
            return false;
        }
        return data.getVolume() == getVolume();
    }

    public boolean isFormed() {
        return formed;
    }

    public void setFormedForce(boolean formed) {
        this.formed = formed;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    // Only call from the server
    public void markDirtyComparator(Level world) {
        if (!isFormed()) {
            return;
        }
        int newRedstoneLevel = getMultiblockRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            //Update the comparator value if it changed
            currentRedstoneLevel = newRedstoneLevel;
            //And inform all the valves that the level they should be supplying changed
            notifyAllUpdateComparator(world);
        }
    }

    public void notifyAllUpdateComparator(Level world) {
        for (ValveData valve : valves) {
            BlockEntityMultiblock<?> tile = WorldUtils.getTileEntity(BlockEntityMultiblock.class, world, valve.location);
//            if (tile != null) {
//                tile.markDirtyComparator();
//            }
        }
    }

    public void forceUpdateComparatorLevel() {
        currentRedstoneLevel = getMultiblockRedstoneLevel();
    }

    protected int getMultiblockRedstoneLevel() {
        return 0;
    }

    public int getCurrentRedstoneLevel() {
        return currentRedstoneLevel;
    }
}

