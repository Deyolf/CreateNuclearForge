package net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid;

import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import net.nuclearteam.createnuclear.content.test.enu.AutomationType;
import net.nuclearteam.createnuclear.content.test.interfac.IContentsListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ICNFluidHandler extends ISidedFluidHandler, IContentsListener {
    default boolean canHandleFluid() {
        return true;
    }


    List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side);


    @Nullable
    default IExtendedFluidTank getFluidTank(int tank, @Nullable Direction side) {
        List<IExtendedFluidTank> tanks = getFluidTanks(side);
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank) : null;
    }

    @Override
    default int getTanks(@Nullable Direction side) {
        return getFluidTanks(side).size();
    }

    @Override
    default FluidStack getFluidInTank(int tank, @Nullable Direction side) {
        IExtendedFluidTank fluidTank = getFluidTank(tank, side);
        return fluidTank == null ? FluidStack.EMPTY : fluidTank.getFluid();
    }

    @Override
    default void setFluidInTank(int tank, FluidStack stack, @Nullable Direction side) {
        IExtendedFluidTank fluidTank = getFluidTank(tank, side);
        if (fluidTank != null) {
            fluidTank.setStack(stack);
        }
    }

    @Override
    default int getTankCapacity(int tank, @Nullable Direction side) {
        IExtendedFluidTank fluidTank = getFluidTank(tank, side);
        return fluidTank == null ? 0 : fluidTank.getCapacity();
    }

    @Override
    default boolean isFluidValid(int tank, FluidStack stack, @Nullable Direction side) {
        IExtendedFluidTank fluidTank = getFluidTank(tank, side);
        return fluidTank != null && fluidTank.isFluidValid(stack);
    }

    @Override
    default FluidStack insertFluid(int tank, FluidStack stack, @Nullable Direction side, Action action) {
        IExtendedFluidTank fluidTank = getFluidTank(tank, side);
        return fluidTank == null ? stack : fluidTank.insert(stack, action, side == null ? AutomationType.INTERNAL : AutomationType.EXTERNAL);
    }

    @Override
    default FluidStack extractFluid(int tank, int amount, @Nullable Direction side, Action action) {
        IExtendedFluidTank fluidTank = getFluidTank(tank, side);
        return fluidTank == null ? FluidStack.EMPTY : fluidTank.extract(amount, action, side == null ? AutomationType.INTERNAL : AutomationType.EXTERNAL);
    }
}
