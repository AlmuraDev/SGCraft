package com.almuradev.sgcraft.fabric;

import com.almuradev.sgcraft.SGCraft;
import net.fabricmc.api.ModInitializer;

public final class SGCraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SGCraft.init();
    }
}
