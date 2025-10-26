package net.nuclearteam.createnuclear.content.test.interfac;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public interface IConfigurable {
    InteractionResult onSneakRightClick(Player player);

    InteractionResult onRightClick(Player player);
}
