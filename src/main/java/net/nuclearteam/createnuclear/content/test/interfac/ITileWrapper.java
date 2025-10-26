package net.nuclearteam.createnuclear.content.test.interfac;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

public interface ITileWrapper {
    BlockPos getBlockPos();

    Level getLevel();

    default GlobalPos getTileGlobalPos() {
        return GlobalPos.of(getLevel().dimension(), getBlockPos());
    }
}
