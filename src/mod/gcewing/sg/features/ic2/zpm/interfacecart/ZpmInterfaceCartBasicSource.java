package gcewing.sg.features.ic2.zpm.interfacecart;

import gcewing.sg.features.zpm.ZpmSource;
import ic2.api.energy.prefab.BasicSource;
import ic2.api.energy.tile.IEnergyAcceptor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

// The cart's source with IC2 installed: on the IC2 energy network as well (made by Ic2ZpmSources, so the cart never names it)
public final class ZpmInterfaceCartBasicSource extends BasicSource implements ZpmSource {

    private final TileEntity parent;

    public ZpmInterfaceCartBasicSource(final TileEntity parent, final double capacity, final int tier) {
        super(parent, capacity, tier);
        this.parent = parent;
    }

    @Override
    public void drawEnergy(double amount) {
        super.drawEnergy(amount);
        if (this.parent instanceof ZpmInterfaceCartTE) {
            ((ZpmInterfaceCartTE) this.parent).afterDraw(amount);
        }
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing direction) {
        // Todo: math needed to only output energy on one side of the zpm interface cart
        return true;
    }
}
