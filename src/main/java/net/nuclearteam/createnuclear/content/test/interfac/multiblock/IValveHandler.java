package net.nuclearteam.createnuclear.content.test.interfac.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.nuclearteam.createnuclear.content.test.NBTUtils;

import java.util.Collection;

public interface IValveHandler {
    default void writeValves(CompoundTag updateTag) {
        ListTag valves = new ListTag();
        for (ValveData valveData : getValveData()) {
            if (valveData.activeTicks > 0) {
                CompoundTag valveNBT = new CompoundTag();
                valveNBT.put("position", NbtUtils.writeBlockPos(valveData.location));
                NBTUtils.writeEnum(valveNBT, "side", valveData.side);
                valves.add(valveNBT);
            }
        }
        updateTag.put("valve", valves);
    }

    default void readValves(CompoundTag updateTag) {
        getValveData().clear();
        if (updateTag.contains("valve", Tag.TAG_LIST)) {
            ListTag valves = updateTag.getList("valve", Tag.TAG_COMPOUND);
            for (int i = 0; i < valves.size(); i++) {
                CompoundTag valveNBT = valves.getCompound(i);
                NBTUtils.setBlockPosIfPresent(valveNBT, "position", pos -> {
                    Direction side = Direction.from3DDataValue(valveNBT.getInt("side"));
                    getValveData().add(new ValveData(pos, side));
                });
            }
        }
    }

    default void triggerValveTransfer(IMultiblock<?> multiblock) {
        if (multiblock.getMultiblock().isFormed()) {
            for (ValveData data : getValveData()) {
                if (multiblock.getBlockPos().equals(data.location)) {
                    data.onTransfer();
                    break;
                }
            }
        }
    }

    Collection<ValveData> getValveData();

    class ValveData {

        public final BlockPos location;
        public final Direction side;

        public boolean prevActive;
        public int activeTicks;

        public ValveData(BlockPos location, Direction side) {
            this.location = location;
            this.side = side;
        }

        public void onTransfer() {
            activeTicks = 30;
        }

        @Override
        public int hashCode() {
            int code = 1;
            code = 31 * code + side.ordinal();
            code = 31 * code + location.hashCode();
            return code;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ValveData other && other.side == side && other.location.equals(location);
        }
    }
}
