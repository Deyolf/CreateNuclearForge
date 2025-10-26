package net.nuclearteam.createnuclear.content.test.interfac.multiblock;

import net.nuclearteam.createnuclear.content.test.MultiblockManager;

import java.util.Map;

public interface IStructuralMultiblock extends IMultiblockBase {

    boolean canInterface(MultiblockManager<?> manager);

    Map<MultiblockManager<?>, Structure> getStructureMap();

    boolean hasFormedMultiblock();

    boolean structuralGuiAccessAllowed();
}