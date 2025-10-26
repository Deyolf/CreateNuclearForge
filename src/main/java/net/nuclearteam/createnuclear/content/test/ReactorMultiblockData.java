package net.nuclearteam.createnuclear.content.test;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.IValveHandler;

public class ReactorMultiblockData extends MultiblockData implements IValveHandler {
    public double progress;
    private AABB deathZone;

    public ReactorMultiblockData(BlockEntity tile) {
        super(tile);
    }

    @Override
    public void onCreated(Level world) {
        super.onCreated(world);
        deathZone = new AABB(getMinPos().offset(1,1,1), getMaxPos());
    }

    private long getMaxInputItem() {
        return 1L;
    }
}
