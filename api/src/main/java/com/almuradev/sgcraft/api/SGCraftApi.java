package com.almuradev.sgcraft.api;

/**
 * The entry point add-ons use to reach SGCraft.
 *
 * <p>Add-ons build against the api jar alone. Anything outside this package is SGCraft's own and may change in
 * any release.</p>
 */
public final class SGCraftApi {

    /**
     * The version of the API contracts, which follows semantic versioning on its own, apart from the mod's version.
     * A major version may break add-ons; a minor version only adds to the API.
     */
    public static final String VERSION = "0.1.0";

    private SGCraftApi() {
    }
}
