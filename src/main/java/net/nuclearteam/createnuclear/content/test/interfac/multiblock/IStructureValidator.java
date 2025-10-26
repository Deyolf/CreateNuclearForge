package net.nuclearteam.createnuclear.content.test.interfac.multiblock;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.nuclearteam.createnuclear.content.test.MultiblockData;
import net.nuclearteam.createnuclear.content.test.MultiblockManager;
import net.nuclearteam.createnuclear.content.test.interfac.IShape;
import net.nuclearteam.createnuclear.content.test.interfac.multiblock.FormationProtocol.FormationResult;

public interface IStructureValidator<T extends MultiblockData> {

    void init(Level world, MultiblockManager<T> manager, Structure structure);

    boolean precheck();

    FormationResult validate(FormationProtocol<T> ctx, Long2ObjectMap<ChunkAccess> chunkMap);

    FormationResult postcheck(T structure, Long2ObjectMap<ChunkAccess> chunkMap);

    IShape getShape();
}
