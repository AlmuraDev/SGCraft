package gcewing.sg.features.ic2.zpm.modulehub;

import gcewing.sg.features.zpm.ZpmSource;
import ic2.api.energy.prefab.BasicSource;
import ic2.api.energy.tile.IEnergyAcceptor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

// The hub's source with IC2 installed: on the IC2 energy network as well (made by Ic2ZpmSources, so the hub never names it)
public final class ZpmHubBasicSource extends BasicSource implements ZpmSource {

    private final TileEntity parent;

    public ZpmHubBasicSource(final TileEntity parent, final double capacity, final int tier) {
        super(parent, capacity, tier);
        this.parent = parent;
    }

    // ZPMs in a hub together go further: with n of them, each unit drawn costs them 1/n (the "optimization factor" its
    // screen shows), shared among them. Their own energies, which they take back as they leave, used to be lowered by
    // whole units only, so small draws (from a slow cable) took nothing from them at all.
    @Override
    public void drawEnergy(double amount) {
        if (this.parent instanceof ZpmHubTE) {
            ((ZpmHubTE) this.parent).drawShared(amount); // sets this source's energy from theirs
        }
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing direction) {
        // Todo: math needed to only output energy on one side of the zpm interface cart
        return true;
    }
}
