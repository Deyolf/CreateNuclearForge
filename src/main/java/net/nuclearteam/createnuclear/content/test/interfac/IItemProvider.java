package net.nuclearteam.createnuclear.content.test.interfac;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

public interface IItemProvider extends IBaseProvider, ItemLike {
    default ItemStack getItemStack() {
        return getItemStack(1);
    }

    default ItemStack getItemStack(int size) {
        return new ItemStack(asItem(), size);
    }

    @Override
    default ResourceLocation getRegistryName() {
        return ForgeRegistries.ITEMS.getKey(asItem());
    }

    default String getTranslationKey() {
        return asItem().getDescriptionId();
    }
}
