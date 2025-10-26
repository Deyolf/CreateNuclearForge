package net.nuclearteam.createnuclear.content.test.interfac.inventory.fluid;

import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.nuclearteam.createnuclear.content.test.enu.Action;
import net.nuclearteam.createnuclear.content.test.fluid.ExtendedFluidHandlerUtils;
import org.jetbrains.annotations.Nullable;

public interface ISidedFluidHandler extends IExtendedFluidHandler {
    @Nullable
    default Direction getFluidSideFor() {
        return null;
    }


    int getTanks(@Nullable Direction side);

    @Override
    default int getTanks() {
        return getTanks(getFluidSideFor());
    }

    FluidStack getFluidInTank(int tank, @Nullable Direction side);

    @Override
    default FluidStack getFluidInTank(int tank) {
        return getFluidInTank(tank, getFluidSideFor());
    }


    void setFluidInTank(int tank, FluidStack stack, @Nullable Direction side);

    @Override
    default void setFluidInTank(int tank, FluidStack stack) {
        setFluidInTank(tank, stack, getFluidSideFor());
    }


    int getTankCapacity(int tank, @Nullable Direction side);

    @Override
    default int getTankCapacity(int tank) {
        return getTankCapacity(tank, getFluidSideFor());
    }


    boolean isFluidValid(int tank, FluidStack stack, @Nullable Direction side);

    @Override
    default boolean isFluidValid(int tank, FluidStack stack) {
        return isFluidValid(tank, stack, getFluidSideFor());
    }


    FluidStack insertFluid(int tank, FluidStack stack, @Nullable Direction side, Action action);

    @Override
    default FluidStack insertFluid(int tank, FluidStack stack, Action action) {
        return insertFluid(tank, stack, getFluidSideFor(), action);
    }


    FluidStack extractFluid(int tank, int amount, @Nullable Direction side, Action action);

    @Override
    default FluidStack extractFluid(int tank, int amount, Action action) {
        return extractFluid(tank, amount, getFluidSideFor(), action);
    }


    default FluidStack insertFluid(FluidStack stack, @Nullable Direction side, Action action) {
        return ExtendedFluidHandlerUtils.insert(stack, action, () -> getTanks(side), tank -> getFluidInTank(tank, side), (tank, s, a) -> insertFluid(tank, s, side, a));
    }


    default FluidStack extractFluid(int amount, @Nullable Direction side, Action action) {
        return ExtendedFluidHandlerUtils.extract(amount, action, () -> getTanks(side), tank -> getFluidInTank(tank, side), (tank, a, act) -> extractFluid(tank, a, side, act));
    }


    default FluidStack extractFluid(FluidStack stack, @Nullable Direction side, Action action) {
        return ExtendedFluidHandlerUtils.extract(stack, action, () -> getTanks(side), tank -> getFluidInTank(tank, side), (tank, a, act) -> extractFluid(tank, a, side, act));
    }
}
