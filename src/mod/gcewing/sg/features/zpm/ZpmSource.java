package gcewing.sg.features.zpm;

/**
 * The energy a ZPM hub or interface cart holds for what draws on it: stargates (SG energy), Forge Energy machines and,
 * with IC2 installed, the IC2 energy network. With IC2 it is an IC2 BasicSource (see Ic2ZpmSources), without it a
 * {@link ZpmBuffer}; either way in ZPM energy units.
 */
public interface ZpmSource {

    /** Each tick: IC2's source joins the energy network on its first. */
    void update();

    void onChunkUnload();

    void invalidate();

    double getEnergyStored();

    void setEnergyStored(double energy);

    double getCapacity();

    void setCapacity(double capacity);

    /** Takes {@code amount} for whatever drew it. */
    void drawEnergy(double amount);

    /** What it offers per tick. */
    double getOfferedEnergy();

    int getSourceTier();

    void setSourceTier(int tier);
}
