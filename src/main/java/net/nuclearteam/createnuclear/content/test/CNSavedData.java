package net.nuclearteam.createnuclear.content.test;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.nuclearteam.createnuclear.CreateNuclear;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Supplier;

public abstract class CNSavedData extends SavedData {
    public abstract void load(@NotNull CompoundTag nbt);

    @Override
    public void save(@NotNull File file) {
        if (isDirty()) {
            //This is loosely based on Refined Storage's RSSavedData's system of saving first to a temp file
            // to reduce the odds of corruption if the user's computer crashes while the file is being written
            File tempFile = file.toPath().getParent().resolve(file.getName() + ".tmp").toFile();
            super.save(tempFile);
            if (file.exists() && !file.delete()) {
                CreateNuclear.LOGGER.error("Failed to delete " + file.getName());
            }
            if (!tempFile.renameTo(file)) {
                CreateNuclear.LOGGER.error("Failed to rename " + tempFile.getName());
            }
        }
    }

    /**
     * Note: This should only be called from the server side
     */
    public static <DATA extends CNSavedData> DATA createSavedData(Supplier<DATA> createFunction, String name) {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        if (currentServer == null) {
            throw new IllegalStateException("Current server is null");
        }
        DimensionDataStorage dataStorage = currentServer.overworld().getDataStorage();
        return createSavedData(dataStorage, createFunction, name);
    }

    /**
     * Note: This should only be called from the server side
     */
    public static <DATA extends CNSavedData> DATA createSavedData(DimensionDataStorage dataStorage, Supplier<DATA> createFunction, String name) {
        return dataStorage.computeIfAbsent(tag -> {
            DATA handler = createFunction.get();
            handler.load(tag);
            return handler;
        }, createFunction, CreateNuclear.MOD_ID + "_" + name);
    }

    public abstract void load(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider);

    @NotNull
    public abstract CompoundTag save(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider);
}
