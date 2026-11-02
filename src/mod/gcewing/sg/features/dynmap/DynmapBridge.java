package gcewing.sg.features.dynmap;

import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.SGAddressing;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

// Everything that uses Dynmap's classes, so that only a server with Dynmap ever loads them (see DynmapMarkers)
final class DynmapBridge extends DynmapCommonAPIListener {

    private static final String MARKER_SET = "sgcraft.stargates";

    // Set while Dynmap's API is up; Dynmap keeps the set and its markers itself, across restarts
    private static MarkerSet markers;
    private static MarkerIcon icon;
    // Dynmap's Forge mod names worlds one of three ways, depending on the server and its settings, and its API doesn't
    // say which; so its own ForgeWorld.getWorldName is asked. Without that, the world's own name, as Sponge has it.
    private static Method dynmapWorldName;

    static void register() {
        try {
            dynmapWorldName = Class.forName("org.dynmap.forge_1_12_2.ForgeWorld").getMethod("getWorldName", World.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            dynmapWorldName = null;
        }
        DynmapCommonAPIListener.register(new DynmapBridge());
    }

    @Override
    public void apiEnabled(DynmapCommonAPI api) {
        MarkerAPI markerApi = api.getMarkerAPI();
        if (markerApi == null) {
            return; // Dynmap's markers are switched off
        }
        MarkerSet set = markerApi.getMarkerSet(MARKER_SET);
        if (set == null) {
            set = markerApi.createMarkerSet(MARKER_SET, "Stargates", null, true);
        }
        MarkerIcon portal = markerApi.getMarkerIcon("portal");
        icon = portal != null ? portal : markerApi.getMarkerIcon(MarkerIcon.DEFAULT);
        markers = set;
        LogManager.getLogger().info("SGCraft - showing gates on Dynmap (world names from {})", dynmapWorldName != null ? "Dynmap" : "the worlds");

        // Gates loaded before Dynmap was up, brought into line on the server's thread
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            server.addScheduledTask(() -> {
                for (WorldServer world : server.worlds) {
                    List<TileEntity> loaded = new ArrayList<>(world.loadedTileEntityList);
                    for (TileEntity te : loaded) {
                        if (te instanceof SGBaseTE) {
                            update((SGBaseTE) te);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        markers = null;
        icon = null;
    }

    static void update(SGBaseTE te) {
        MarkerSet set = markers;
        if (set == null) {
            return;
        }
        String id = markerId(te);
        Marker marker = set.findMarker(id);
        if (!te.isMerged || !te.showOnDynmap || te.isInvalid()) {
            if (marker != null) {
                marker.deleteMarker();
            }
            return;
        }
        String label = label(te);
        String world = worldName(te.getWorld());
        BlockPos pos = te.getPos();
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (marker == null) {
            set.createMarker(id, label, false, world, x, y, z, icon, true);
        } else {
            marker.setLabel(label);
            marker.setLocation(world, x, y, z);
        }
    }

    // One per gate base, so a gate keeps its marker whatever its address
    private static String markerId(SGBaseTE te) {
        BlockPos pos = te.getPos();
        return "sg_" + te.getWorld().provider.getDimension() + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    private static String label(SGBaseTE te) {
        if (te.showAddressOnDynmap && te.homeAddress != null) {
            return "Stargate " + SGAddressing.formatAddress(te.homeAddress, "-", "-");
        }
        return "Stargate";
    }

    private static String worldName(World world) {
        if (dynmapWorldName != null) {
            try {
                return (String) dynmapWorldName.invoke(null, world);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LogManager.getLogger().warn("SGCraft - couldn't get Dynmap's name for {}", world.getWorldInfo().getWorldName(), e);
                dynmapWorldName = null;
            }
        }
        return world.getWorldInfo().getWorldName();
    }
}
