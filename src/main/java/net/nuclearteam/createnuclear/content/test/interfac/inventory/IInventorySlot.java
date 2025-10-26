package net.nuclearteam.createnuclear.content.test.interfac.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import net.nuclearteam.createnuclear.content.test.enu.AutomationType;
import net.nuclearteam.createnuclear.content.test.interfac.IContentsListener;
import org.jetbrains.annotations.Nullable;

public interface IInventorySlot extends INBTSerializable<CompoundTag>, IContentsListener {
    ItemStack getStack();

    void setStack(ItemStack stack);


    default ItemStack insertItem(ItemStack stack, Action action, AutomationType automationType) {
        if (stack.isEmpty() || !isItemValid(stack)) {
            //"Fail quick" if the given stack is empty, or we can never insert the item or currently are unable to insert it
            return stack;
        }
        int needed = getLimit(stack) - getCount();
        if (needed <= 0) {
            //Fail if we are a full slot
            return stack;
        }
        boolean sameType = false;
        if (isEmpty() || (sameType = ItemHandlerHelper.canItemStacksStack(getStack(), stack))) {
            int toAdd = Math.min(stack.getCount(), needed);
            if (action.execute()) {
                //If we want to actually insert the item, then update the current item
                if (sameType) {
                    // Note: this also will mark that the contents changed
                    //We can just grow our stack by the amount we want to increase it
                    growStack(toAdd, action);
                } else {
                    //If we are not the same type then we have to copy the stack and set it
                    // Note: this also will mark that the contents changed
                    setStack(stack.copyWithCount(toAdd));
                }
            }
            return stack.copyWithCount(stack.getCount() - toAdd);
        }
        //If we didn't accept this item, then just return the given stack
        return stack;
    }


    default ItemStack extractItem(int amount, Action action, AutomationType automationType) {
        if (isEmpty() || amount < 1) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getStack();

        int currentAmount = Math.min(getCount(), current.getMaxStackSize());
        if (currentAmount < amount) {
            amount = currentAmount;
        }

        ItemStack toReturn = current.copyWithCount(amount);
        if (action.execute()) {

            shrinkStack(amount, action);
        }
        return toReturn;
    }


    int getLimit(ItemStack stack);


    boolean isItemValid(ItemStack stack);


    @Nullable
    Slot createContainerSlot();


    default int setStackSize(int amount, Action action) {
        if (isEmpty()) {
            return 0;
        } else if (amount <= 0) {
            if (action.execute()) {
                setEmpty();
            }
            return 0;
        }
        ItemStack stack = getStack();
        int maxStackSize = getLimit(stack);
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }
        if (stack.getCount() == amount || action.simulate()) {
            //If our size is not changing, or we are only simulating the change, don't do anything
            return amount;
        }
        setStack(stack.copyWithCount(amount));
        return amount;
    }


    default int growStack(int amount, Action action) {
        int current = getCount();
        if (amount > 0) {
            //Cap adding amount at how much we need, so that we don't risk integer overflow
            amount = Math.min(amount, getLimit(getStack()));
        }
        int newSize = setStackSize(current + amount, action);
        return newSize - current;
    }


    default int shrinkStack(int amount, Action action) {
        return -growStack(-amount, action);
    }


    default boolean isEmpty() {
        return getStack().isEmpty();
    }


    default void setEmpty() {
        setStack(ItemStack.EMPTY);
    }


    default int getCount() {
        return getStack().getCount();
    }
}
