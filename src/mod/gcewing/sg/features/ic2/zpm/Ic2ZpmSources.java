package gcewing.sg.features.ic2.zpm;

import gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCartBasicSource;
import gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCartTE;
import gcewing.sg.features.ic2.zpm.modulehub.ZpmHubBasicSource;
import gcewing.sg.features.ic2.zpm.modulehub.ZpmHubTE;
import gcewing.sg.features.zpm.ZpmSource;
import ic2.api.energy.prefab.BasicSource;
import ic2.api.energy.tile.IEnergyAcceptor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * The IC2 side of the ZPM hub and interface cart, in one class that is only loaded with IC2 installed: the blocks call
 * it only after checking for IC2, and it holds every IC2 class they would otherwise name.
 */
public final class Ic2ZpmSources {

    private Ic2ZpmSources() {
    }

    public static ZpmSource hub(ZpmHubTE te, double capacity, int tier) {
        return new ZpmHubBasicSource(te, capacity, tier);
    }

    public static ZpmSource cart(ZpmInterfaceCartTE te, double capacity, int tier) {
        return new ZpmInterfaceCartBasicSource(te, capacity, tier);
    }

    /** Reads the energy as IC2's source saved it, before the blocks saved it themselves. */
    public static void readLegacy(ZpmSource source, NBTTagCompound compound) {
        ((BasicSource) source).readFromNBT(compound);
    }

    public static boolean emitsEnergyTo(ZpmSource source, IEnergyAcceptor acceptor, EnumFacing side) {
        return ((BasicSource) source).emitsEnergyTo(acceptor, side);
    }
}
