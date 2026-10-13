package com.almuradev.sgcraft.neoforge;

import com.almuradev.sgcraft.Platform;
import net.neoforged.fml.loading.FMLLoader;

public final class NeoForgePlatform implements Platform {

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }
}
