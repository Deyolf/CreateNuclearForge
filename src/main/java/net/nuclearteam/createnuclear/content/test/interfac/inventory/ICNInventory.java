package net.nuclearteam.createnuclear.content.test.interfac.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import net.nuclearteam.createnuclear.content.test.enu.AutomationType;
import net.nuclearteam.createnuclear.content.test.interfac.IContentsListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ICNInventory extends ISidedItemHandler, IContentsListener {

    default boolean hasInventory() {
        return true;
    }


    List<IInventorySlot> getInventorySlots(@Nullable Direction side);


    @Nullable
    default IInventorySlot getInventorySlot(int slot, @Nullable Direction side) {
        List<IInventorySlot> slots = getInventorySlots(side);
        return slot >= 0 && slot < slots.size() ? slots.get(slot) : null;
    }

    @Override
    default void setStackInSlot(int slot, ItemStack stack, @Nullable Direction side) {
        IInventorySlot inventorySlot = getInventorySlot(slot, side);
        if (inventorySlot != null) {
            inventorySlot.setStack(stack);
        }
    }

    @Override
    default int getSlots(@Nullable Direction side) {
        return getInventorySlots(side).size();
    }

    @Override
    default ItemStack getStackInSlot(int slot, @Nullable Direction side) {
        IInventorySlot inventorySlot = getInventorySlot(slot, side);
        return inventorySlot == null ? ItemStack.EMPTY : inventorySlot.getStack();
    }

    @Override
    default ItemStack insertItem(int slot, ItemStack stack, @Nullable Direction side, Action action) {
        IInventorySlot inventorySlot = getInventorySlot(slot, side);
        if (inventorySlot == null) {
            return stack;
        }
        return inventorySlot.insertItem(stack, action, side == null ? AutomationType.INTERNAL : AutomationType.EXTERNAL);
    }

    @Override
    default ItemStack extractItem(int slot, int amount, @Nullable Direction side, Action action) {
        IInventorySlot inventorySlot = getInventorySlot(slot, side);
        if (inventorySlot == null) {
            return ItemStack.EMPTY;
        }
        return inventorySlot.extractItem(amount, action, side == null ? AutomationType.INTERNAL : AutomationType.EXTERNAL);
    }

    @Override
    default int getSlotLimit(int slot, @Nullable Direction side) {
        IInventorySlot inventorySlot = getInventorySlot(slot, side);
        return inventorySlot == null ? 0 : inventorySlot.getLimit(ItemStack.EMPTY);
    }

    @Override
    default boolean isItemValid(int slot, ItemStack stack, @Nullable Direction side) {
        IInventorySlot inventorySlot = getInventorySlot(slot, side);
        return inventorySlot != null && inventorySlot.isItemValid(stack);
    }

    /**
     * Are all the Slots empty?
     * @implNote named isInventoryEmpty to avoid clashing with any other isEmpty() method
     * @since 10.4.0
     *
     * @param side the side to query
     * @return true if completely empty on this side
     */
    default boolean isInventoryEmpty(@Nullable Direction side) {
        for (IInventorySlot slot : getInventorySlots(side)) {
            if (!slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sided inventory helper for isEmpty
     * @since 10.4.0
     *
     * @return true if completely empty on the default side
     */
    default boolean isInventoryEmpty() {
        return isInventoryEmpty(getInventorySideFor());
    }
}
