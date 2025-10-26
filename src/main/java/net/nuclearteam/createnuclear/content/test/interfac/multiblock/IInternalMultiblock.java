package net.nuclearteam.createnuclear.content.test.interfac.multiblock;

import net.nuclearteam.createnuclear.content.test.MultiblockData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IInternalMultiblock {
    @Nullable
    UUID getMultiblockUUID();

    /**
     * Only valid on the server
     */
    @Nullable
    MultiblockData getMultiblock();

    void setMultiblock(@Nullable MultiblockData multiblock);

    default boolean hasFormedMultiblock() {
        return getMultiblockUUID() != null;
    }
}
