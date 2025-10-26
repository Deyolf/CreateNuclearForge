package net.nuclearteam.createnuclear.content.test.interfac.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ISidedItemHandler extends IItemHandlerModifiable {

    @Nullable
    default Direction getInventorySideFor() {
        return null;
    }


    void setStackInSlot(int slot, ItemStack stack, @Nullable Direction side);

    @Override
    default void setStackInSlot(int slot, ItemStack stack) {
        setStackInSlot(slot, stack, getInventorySideFor());
    }


    int getSlots(@Nullable Direction side);

    @Override
    default int getSlots() {
        return getSlots(getInventorySideFor());
    }


    ItemStack getStackInSlot(int slot, @Nullable Direction side);

    @Override
    default ItemStack getStackInSlot(int slot) {
        return getStackInSlot(slot, getInventorySideFor());
    }


    ItemStack insertItem(int slot, ItemStack stack, @Nullable Direction side, Action action);

    @Override
    default ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insertItem(slot, stack, getInventorySideFor(), Action.get(!simulate));
    }


    ItemStack extractItem(int slot, int amount, @Nullable Direction side, Action action);

    @Override
    default ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItem(slot, amount, getInventorySideFor(), Action.get(!simulate));
    }


    int getSlotLimit(int slot, @Nullable Direction side);

    @Override
    default int getSlotLimit(int slot) {
        return getSlotLimit(slot, getInventorySideFor());
    }


    boolean isItemValid(int slot, ItemStack stack, @Nullable Direction side);

    @Override
    default boolean isItemValid(int slot, ItemStack stack) {
        return isItemValid(slot, stack, getInventorySideFor());
    }
}
