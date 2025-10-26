package net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import net.nuclearteam.createnuclear.content.test.enu.AutomationType;
import net.nuclearteam.createnuclear.content.test.interfac.IContentsListener;

public interface IExtendedFluidTank extends IFluidTank, INBTSerializable<CompoundTag>, IContentsListener {
    void setStack(FluidStack stack);


    void setStackUnchecked(FluidStack stack);


    default FluidStack insert(FluidStack stack, Action action, AutomationType automationType) {
        if (stack.isEmpty() || !isFluidValid(stack)) {
            //"Fail quick" if the given stack is empty, or we can never insert the item or currently are unable to insert it
            return stack;
        }
        int needed = getNeeded();
        if (needed <= 0) {
            //Fail if we are a full tank
            return stack;
        }
        boolean sameType = false;
        if (isEmpty() || (sameType = stack.isFluidEqual(getFluid()))) {
            int toAdd = Math.min(stack.getAmount(), needed);
            if (action.execute()) {
                //If we want to actually insert the fluid, then update the current fluid
                if (sameType) {
                    //We can just grow our stack by the amount we want to increase it
                    // Note: this also will mark that the contents changed
                    growStack(toAdd, action);
                } else {
                    //If we are not the same type then we have to copy the stack and set it
                    // Note: this also will mark that the contents changed
                    setStack(new FluidStack(stack, toAdd));
                }
            }
            return new FluidStack(stack, stack.getAmount() - toAdd);
        }
        //If we didn't accept this fluid, then just return the given stack
        return stack;
    }


    default FluidStack extract(int amount, Action action, AutomationType automationType) {
        if (isEmpty() || amount < 1) {
            return FluidStack.EMPTY;
        }
        FluidStack ret = new FluidStack(getFluid(), Math.min(getFluidAmount(), amount));
        if (!ret.isEmpty() && action.execute()) {
            // Note: this also will mark that the contents changed
            shrinkStack(ret.getAmount(), action);
        }
        return ret;
    }


    default int setStackSize(int amount, Action action) {
        if (isEmpty()) {
            return 0;
        } else if (amount <= 0) {
            if (action.execute()) {
                setEmpty();
            }
            return 0;
        }
        int maxStackSize = getCapacity();
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }
        if (getFluidAmount() == amount || action.simulate()) {
            //If our size is not changing, or we are only simulating the change, don't do anything
            return amount;
        }
        setStack(new FluidStack(getFluid(), amount));
        return amount;
    }


    default int growStack(int amount, Action action) {
        int current = getFluidAmount();
        if (amount > 0) {
            //Cap adding amount at how much we need, so that we don't risk integer overflow
            amount = Math.min(amount, getNeeded());
        }
        int newSize = setStackSize(current + amount, action);
        return newSize - current;
    }


    default int shrinkStack(int amount, Action action) {
        return -growStack(-amount, action);
    }


    default boolean isEmpty() {
        return getFluid().isEmpty();
    }


    default void setEmpty() {
        setStack(FluidStack.EMPTY);
    }


    default boolean isFluidEqual(FluidStack other) {
        return getFluid().isFluidEqual(other);
    }


    default int getNeeded() {
        return Math.max(0, getCapacity() - getFluidAmount());
    }

    @Override
    default CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (!isEmpty()) {
            nbt.put("stored", getFluid().writeToNBT(new CompoundTag()));
        }
        return nbt;
    }

    @Override
    @Deprecated
    default int fill(FluidStack stack, FluidAction action) {
        return stack.getAmount() - insert(stack, Action.fromFluidAction(action), AutomationType.EXTERNAL).getAmount();
    }


    @Override
    @Deprecated
    default FluidStack drain(FluidStack stack, FluidAction action) {
        if (!isEmpty() && getFluid().isFluidEqual(stack)) {
            return extract(stack.getAmount(), Action.fromFluidAction(action), AutomationType.EXTERNAL);
        }
        return FluidStack.EMPTY;
    }


    @Override
    @Deprecated
    default FluidStack drain(int amount, FluidAction action) {
        return extract(amount, Action.fromFluidAction(action), AutomationType.EXTERNAL);
    }
}
