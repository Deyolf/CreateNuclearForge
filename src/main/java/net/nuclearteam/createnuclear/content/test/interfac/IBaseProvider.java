package net.nuclearteam.createnuclear.content.test.interfac;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.nuclearteam.createnuclear.foundation.utility.CreateNuclearLang;

public interface IBaseProvider {
    ResourceLocation getRegistryName();


    default String getName() {
        return getRegistryName().getPath();
    }

    default Component getTextComponent() {
        return CreateNuclearLang.translate(getRegistryName().toLanguageKey()).component();
    }
}
