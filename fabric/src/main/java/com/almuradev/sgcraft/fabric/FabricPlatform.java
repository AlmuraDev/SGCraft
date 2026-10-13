package com.almuradev.sgcraft.fabric;

import com.almuradev.sgcraft.Platform;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatform implements Platform {

    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
