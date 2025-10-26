package net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import net.nuclearteam.createnuclear.content.test.fluid.ExtendedFluidHandlerUtils;

public interface IExtendedFluidHandler extends IFluidHandler {
    void setFluidInTank(int tank, FluidStack stack);


    FluidStack insertFluid(int tank, FluidStack stack, Action action);


    FluidStack extractFluid(int tank, int amount, Action action);


    default FluidStack insertFluid(FluidStack stack, Action action) {
        return ExtendedFluidHandlerUtils.insert(stack, action, this::getTanks, this::getFluidInTank, this::insertFluid);
    }


    default FluidStack extractFluid(int amount, Action action) {
        return ExtendedFluidHandlerUtils.extract(amount, action, this::getTanks, this::getFluidInTank, this::extractFluid);
    }


    default FluidStack extractFluid(FluidStack stack, Action action) {
        return ExtendedFluidHandlerUtils.extract(stack, action, this::getTanks, this::getFluidInTank, this::extractFluid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    default int fill(FluidStack stack, FluidAction action) {
        return stack.getAmount() - insertFluid(stack, Action.fromFluidAction(action)).getAmount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    default FluidStack drain(FluidStack stack, FluidAction action) {
        return extractFluid(stack, Action.fromFluidAction(action));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    default FluidStack drain(int amount, FluidAction action) {
        return extractFluid(amount, Action.fromFluidAction(action));
    }
}
