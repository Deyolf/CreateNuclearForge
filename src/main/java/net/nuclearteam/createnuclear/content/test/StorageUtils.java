package net.nuclearteam.createnuclear.content.test;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.nuclearteam.createnuclear.content.test.fluid.BasicFluidTank;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

public class StorageUtils {

    private StorageUtils() {
    }


    public static void addStoredFluid(@NotNull ItemStack stack, @NotNull List<Component> tooltip, boolean showMissingCap) {
        addStoredFluid(stack, tooltip, showMissingCap, CreateNuclearLang.translate("tooltip", "no_fluid"));
    }

    public static void addStoredFluid(@NotNull ItemStack stack, @NotNull List<Component> tooltip, boolean showMissingCap, LangBuilder emptyLangEntry) {
        addStoredFluid(stack, tooltip, showMissingCap, emptyLangEntry, stored -> {
            if (stored.isEmpty()) {
                return emptyLangEntry.translateColored(EnumColor.GRAY);
            }
            return CreateNuclearLang.translate("tooltip", "stored_fluid", stored.getAmount());
        });
    }

    public static void addStoredFluid(@NotNull ItemStack stack, @NotNull List<Component> tooltip, boolean showMissingCap, LangBuilder emptyLangEntry,
                                      Function<FluidStack, Component> storedFunction) {
        Optional<IFluidHandlerItem> cap = FluidUtil.getFluidHandler(stack).resolve();
        if (cap.isPresent()) {
            IFluidHandlerItem handler = cap.get();
            for (int tank = 0, tanks = handler.getTanks(); tank < tanks; tank++) {
                tooltip.add(storedFunction.apply(handler.getFluidInTank(tank)));
            }
        } else if (showMissingCap) {
            tooltip.add(emptyLangEntry.translate());
        }
    }

    /**
     * @implNote Assumes there is only one "tank"
     */
    public static void addStoredSubstance(@NotNull ItemStack stack, @NotNull List<Component> tooltip, boolean isCreative) {
        FluidStack fluidStack = StorageUtils.getStoredFluidFromNBT(stack);

        if (fluidStack.isEmpty()) {
            tooltip.add(CreateNuclearLang.translate("gui", "empty").json());
            return;
        }
        LangBuilder type;
        Object contents;
        long amount;
        if (!fluidStack.isEmpty()) {
            contents = fluidStack;
            amount = fluidStack.getAmount();
            type = CreateNuclearLang.translate("gui", "liquid");
        }
        if (isCreative) {
            tooltip.add(CreateNuclearLang.translate("gui", "infinite").json());
        } else {
            tooltip.add(CreateNuclearLang.translate("gui", "infinite", amount).json());
        }
    }

    /**
     * Gets the fluid if one is stored from an item's tank going off the basis there is a single tank. This is for cases when we may not actually have a fluid handler
     * attached to our item, but it may have stored data in its tank from when it was a block
     */
    @NotNull
    public static FluidStack getStoredFluidFromNBT(ItemStack stack) {
        BasicFluidTank tank = BasicFluidTank.create(Integer.MAX_VALUE, null);
        ItemDataUtils.readContainers(stack, "FluidTanks", Collections.singletonList(tank));
        return tank.getFluid();
    }



    public static Component getStoragePercent(double ratio, boolean colorText) {
        Component text = TextUtils.getPercent(ratio);
        if (!colorText) {
            return text;
        }
        EnumColor color;
        if (ratio < 0.01F) {
            color = EnumColor.DARK_RED;
        } else if (ratio < 0.1F) {
            color = EnumColor.RED;
        } else if (ratio < 0.25F) {
            color = EnumColor.ORANGE;
        } else if (ratio < 0.5F) {
            color = EnumColor.YELLOW;
        } else {
            color = EnumColor.BRIGHT_GREEN;
        }
        return TextComponentUtil.build(color, text);
    }

    public static int getBarWidth(ItemStack stack) {
        return MathUtils.clampToInt(Math.round(13.0F - 13.0F * getDurabilityForDisplay(stack)));
    }

    private static double getDurabilityForDisplay(ItemStack stack) {
        double bestRatio = 0;
        bestRatio = calculateRatio(stack, bestRatio, Capabilities.GAS_HANDLER);
        bestRatio = calculateRatio(stack, bestRatio, Capabilities.INFUSION_HANDLER);
        bestRatio = calculateRatio(stack, bestRatio, Capabilities.PIGMENT_HANDLER);
        bestRatio = calculateRatio(stack, bestRatio, Capabilities.SLURRY_HANDLER);
        Optional<IFluidHandlerItem> fluidCapability = FluidUtil.getFluidHandler(stack).resolve();
        if (fluidCapability.isPresent()) {
            IFluidHandlerItem fluidHandlerItem = fluidCapability.get();
            int tanks = fluidHandlerItem.getTanks();
            for (int tank = 0; tank < tanks; tank++) {
                bestRatio = Math.max(bestRatio, getRatio(fluidHandlerItem.getFluidInTank(tank).getAmount(), fluidHandlerItem.getTankCapacity(tank)));
            }
        }
        return 1 - bestRatio;
    }

    public static int getEnergyBarWidth(ItemStack stack) {
        return MathUtils.clampToInt(Math.round(13.0F - 13.0F * getEnergyDurabilityForDisplay(stack)));
    }





    public static double getRatio(long amount, long capacity) {
        return capacity == 0 ? 1 : amount / (double) capacity;
    }

    public static void mergeFluidTanks(List<IExtendedFluidTank> tanks, List<IExtendedFluidTank> toAdd, List<FluidStack> rejects) {
        validateSizeMatches(tanks, toAdd, "tank");
        for (int i = 0; i < toAdd.size(); i++) {
            IExtendedFluidTank mergeTank = toAdd.get(i);
            if (!mergeTank.isEmpty()) {
                IExtendedFluidTank tank = tanks.get(i);
                FluidStack mergeStack = mergeTank.getFluid();
                if (tank.isEmpty()) {
                    int capacity = tank.getCapacity();
                    if (mergeStack.getAmount() <= capacity) {
                        tank.setStack(mergeStack);
                    } else {
                        tank.setStack(new FluidStack(mergeStack, capacity));
                        int remaining = mergeStack.getAmount() - capacity;
                        if (remaining > 0) {
                            rejects.add(new FluidStack(mergeStack, remaining));
                        }
                    }
                } else if (tank.isFluidEqual(mergeStack)) {
                    int amount = tank.growStack(mergeStack.getAmount(), Action.EXECUTE);
                    int remaining = mergeStack.getAmount() - amount;
                    if (remaining > 0) {
                        rejects.add(new FluidStack(mergeStack, remaining));
                    }
                } else {
                    rejects.add(mergeStack);
                }
            }
        }
    }

    public static <T> void validateSizeMatches(List<T> base, List<T> toAdd, String type) {
        if (base.size() != toAdd.size()) {
            throw new IllegalArgumentException("Mismatched " + type + " count, orig: " + base.size() + ", toAdd: " + toAdd.size());
        }
    }
}
