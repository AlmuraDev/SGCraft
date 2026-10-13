package com.almuradev.sgcraft;

import java.util.ServiceLoader;

/**
 * What common code needs from the mod loader it runs on. Each loader project provides one implementation, named in
 * its META-INF/services file.
 */
public interface Platform {

    static Platform get() {
        return Holder.INSTANCE;
    }

    String loaderName();

    boolean isDevelopmentEnvironment();

    final class Holder {

        private static final Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + Platform.class.getName() + " for this mod loader"));

        private Holder() {
        }
    }
}
