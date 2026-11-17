package gcewing.sg.features.dynmap;

import gcewing.sg.tileentity.SGBaseTE;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;

// Markers for gates on the server's Dynmap, for those whose Configurator has "Show on Dynmap" ticked. Dynmap is
// optional: without it nothing here touches its classes (they're all in DynmapBridge), and every call does nothing.
public final class DynmapMarkers {

    private static boolean available;

    private DynmapMarkers() {
    }

    // At server start; Dynmap tells DynmapBridge once its API is up (straight away, if it already is)
    public static void init() {
        if (available || !Loader.isModLoaded("dynmap")) {
            return;
        }
        try {
            DynmapBridge.register();
            available = true;
        } catch (Throwable e) { // an old or unexpected Dynmap: no markers, but no crash either
            LogManager.getLogger().warn("SGCraft - couldn't hook into Dynmap, gates won't be shown on it", e);
        }
    }

    // Puts the gate's marker on the map, moves or relabels it, or takes it off, to match the gate as it is now
    public static void update(SGBaseTE te) {
        if (available && te.getWorld() != null && !te.getWorld().isRemote) {
            DynmapBridge.update(te);
        }
    }
}
