package com.almuradev.sgcraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the mod. Each loader calls {@link #init()} once, from its own entry point.
 */
public final class SGCraft {

    public static final String MOD_ID = "sgcraft";
    public static final String MOD_NAME = "SGCraft";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private SGCraft() {
    }

    public static void init() {
        LOG.info("Starting {} on {}", MOD_NAME, Platform.get().loaderName());
    }
}
