package net.nuclearteam.createnuclear.content.test;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.nuclearteam.createnuclear.content.test.fluid.BasicFluidTank;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.ICNInventory;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.IInventorySlot;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid.ICNFluidHandler;
import net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid.IExtendedFluidTank;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MultiblockCache<T extends MultiblockData> implements ICNInventory, ICNFluidHandler {

    private final List<IInventorySlot> inventorySlots = new ArrayList<>();
    private final List<IExtendedFluidTank> fluidTanks = new ArrayList<>();

    public void apply(T data) {
        for (CacheSubstance<?, INBTSerializable<CompoundTag>> type : CacheSubstance.VALUES) {
            List<? extends INBTSerializable<CompoundTag>> containers = type.getContainerList(data);
            if (containers != null) {
                List<? extends INBTSerializable<CompoundTag>> cacheContainers = type.getContainerList(this);
                for (int i = 0; i < cacheContainers.size(); i++) {
                    if (i < containers.size()) {
                        //Copy it via NBT to ensure that we set it using the "unsafe" method in case there is a problem with the types somehow
                        containers.get(i).deserializeNBT(cacheContainers.get(i).serializeNBT());
                    }
                }
            }
        }
    }

    public void sync(T data) {
        for (CacheSubstance<?, INBTSerializable<CompoundTag>> type : CacheSubstance.VALUES) {
            List<? extends INBTSerializable<CompoundTag>> containersToCopy = type.getContainerList(data);
            if (containersToCopy != null) {
                List<? extends INBTSerializable<CompoundTag>> cacheContainers = type.getContainerList(this);
                if (cacheContainers.isEmpty()) {
                    type.prefab(this, containersToCopy.size());
                }
                for (int i = 0; i < containersToCopy.size(); i++) {
                    type.sync(cacheContainers.get(i), containersToCopy.get(i));
                }
            }
        }
    }

    public void load(CompoundTag nbtTags) {
        for (CacheSubstance<?, INBTSerializable<CompoundTag>> type : CacheSubstance.VALUES) {
            int stored = nbtTags.getInt(type.getTagKey() + "_stored");
            if (stored > 0) {
                type.prefab(this, stored);
                DataHandlerUtils.readContainers(type.getContainerList(this), nbtTags.getList(type.getTagKey(), Tag.TAG_COMPOUND));
            }
        }
    }

    public void save(CompoundTag nbtTags) {
        for (CacheSubstance<?, INBTSerializable<CompoundTag>> type : CacheSubstance.VALUES) {
            List<INBTSerializable<CompoundTag>> containers = type.getContainerList(this);
            if (!containers.isEmpty()) {
                //Note: We can skip putting stored at zero if containers is empty (in addition to skipping actually writing the containers)
                // because getInt will default to 0 for keys that aren't present
                nbtTags.putInt(type.getTagKey() + "_stored", containers.size());
                nbtTags.put(type.getTagKey(), DataHandlerUtils.writeContainers(containers));
            }
        }
    }

    public void merge(MultiblockCache<T> mergeCache, RejectContents rejectContents) {
        // prefab enough containers for each substance type to support the merge cache
        for (CacheSubstance<?, INBTSerializable<CompoundTag>> type : CacheSubstance.VALUES) {
            type.preHandleMerge(this, mergeCache);
        }

        // Items
        StackUtils.merge(getInventorySlots(null), mergeCache.getInventorySlots(null), rejectContents.rejectedItems);
        // Fluid
        StorageUtils.mergeFluidTanks(getFluidTanks(null), mergeCache.getFluidTanks(null), rejectContents.rejectedFluids);
    }

    @Override
    public void onContentsChanged() {
    }

    @NotNull
    @Override
    public List<IInventorySlot> getInventorySlots(@Nullable Direction side) {
        return inventorySlots;
    }

    @NotNull
    @Override
    public List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side) {
        return fluidTanks;
    }



    public static class RejectContents {

        public final List<ItemStack> rejectedItems = new ArrayList<>();
        public final List<FluidStack> rejectedFluids = new ArrayList<>();
    }

    public abstract static class CacheSubstance<HANDLER, ELEMENT> {

        public static final CacheSubstance<ICNInventory, IInventorySlot> ITEMS = new CacheSubstance<>("Items") {
            @Override
            protected void defaultPrefab(MultiblockCache<?> cache) {
                cache.inventorySlots.add(BasicInventorySlot.at(cache, 0, 0));
            }

            @Override
            protected List<IInventorySlot> containerList(ICNInventory inventory) {
                return inventory.getInventorySlots(null);
            }

            @Override
            public void sync(IInventorySlot cache, IInventorySlot data) {
                cache.setStack(data.getStack());
            }
        };

        public static final CacheSubstance<ICNFluidHandler, IExtendedFluidTank> FLUID = new CacheSubstance<>("FluidTanks") {
            @Override
            protected void defaultPrefab(MultiblockCache<?> cache) {
                cache.fluidTanks.add(BasicFluidTank.create(Integer.MAX_VALUE, cache));
            }

            @Override
            protected List<IExtendedFluidTank> containerList(ICNFluidHandler fluidHandler) {
                return fluidHandler.getFluidTanks(null);
            }

            @Override
            public void sync(IExtendedFluidTank cache, IExtendedFluidTank data) {
                cache.setStack(data.getFluid());
            }
        };



        @SuppressWarnings({"unchecked"})
        public static final CacheSubstance<?, INBTSerializable<CompoundTag>>[] VALUES = new CacheSubstance[]{
                ITEMS,
                FLUID,
        };

        private final String tagKey;

        public CacheSubstance(String tagKey) {
            this.tagKey = tagKey;
        }

        protected abstract void defaultPrefab(MultiblockCache<?> cache);

        protected abstract List<ELEMENT> containerList(HANDLER handler);

        private void prefab(MultiblockCache<?> cache, int count) {
            for (int i = 0; i < count; i++) {
                defaultPrefab(cache);
            }
        }

        public List<ELEMENT> getContainerList(Object holder) {
            return containerList((HANDLER) holder);
        }

        public abstract void sync(ELEMENT cache, ELEMENT data);

        public void preHandleMerge(MultiblockCache<?> cache, MultiblockCache<?> merge) {
            int diff = getContainerList(merge).size() - getContainerList(cache).size();
            if (diff > 0) {
                prefab(cache, diff);
            }
        }

        public String getTagKey() {
            return tagKey;
        }
    }
}
