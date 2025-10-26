package net.nuclearteam.createnuclear.content.test;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class ReactorCache extends MultiblockCache<ReactorMultiblockData> {
    private double progress;
    private int inputProcessed;
    private boolean couldOperate;
    private double lastProcessed;

    @Override
    public void merge(MultiblockCache<ReactorMultiblockData> mergeCache, RejectContents rejectContents) {
        super.merge(mergeCache, rejectContents);
        progress += ((ReactorCache) mergeCache).progress;
        inputProcessed += ((ReactorCache) mergeCache).inputProcessed;
        couldOperate |= ((ReactorCache) mergeCache).couldOperate;
        lastProcessed = Math.max(lastProcessed, ((ReactorCache) mergeCache).lastProcessed);
    }

    @Override
    public void apply(ReactorMultiblockData data) {
        super.apply(data);
        data.progress = progress;
//        data.inputProcessed = inputProcessed;
//        data.couldOperate = couldOperate;
//        data.lastProcessed = lastProcessed;
    }

    @Override
    public void sync(ReactorMultiblockData data) {
        super.sync(data);
        progress = data.progress;
//        inputProcessed = data.inputProcessed;
//        couldOperate = data.couldOperate;
//        lastProcessed = data.lastProcessed;
    }

    @Override
    public void load(CompoundTag nbtTags) {
        super.load(nbtTags);
        NBTUtils.setDoubleIfPresent(nbtTags, "progress", val -> progress = val);
        NBTUtils.setIntIfPresent(nbtTags, "lastProcessed", val -> inputProcessed = val);
        NBTUtils.setBooleanIfPresent(nbtTags, "couldOperate", val -> couldOperate = val);
        NBTUtils.setDoubleIfPresent(nbtTags, "lastProcessed", val -> lastProcessed = val);
    }

    @Override
    public void save(CompoundTag nbtTags) {
        super.save(nbtTags);
        nbtTags.putDouble("progress", progress);
        nbtTags.putInt("lastProcessed", inputProcessed);
        nbtTags.putBoolean("couldOperate", couldOperate);
        nbtTags.putDouble("lastProcessed", lastProcessed);
    }
}
