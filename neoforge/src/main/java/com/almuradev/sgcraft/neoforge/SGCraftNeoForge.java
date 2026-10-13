package com.almuradev.sgcraft.neoforge;

import com.almuradev.sgcraft.SGCraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(SGCraft.MOD_ID)
public final class SGCraftNeoForge {

    public SGCraftNeoForge(IEventBus modBus) {
        SGCraft.init();
    }
}
