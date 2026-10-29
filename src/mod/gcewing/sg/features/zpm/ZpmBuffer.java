package gcewing.sg.features.zpm;

import java.util.function.DoubleConsumer;

/**
 * A {@link ZpmSource} without IC2: just the energy, for stargates and Forge Energy machines to draw on, as the ZPM
 * console does.
 */
public final class ZpmBuffer implements ZpmSource {

    private double capacity;
    private double energy;
    private int tier;
    // Told of each draw, after it is taken: the block shares it among its ZPMs, or saves and checks for a tainted one
    private final DoubleConsumer onDraw;

    public ZpmBuffer(double capacity, int tier, DoubleConsumer onDraw) {
        this.capacity = capacity;
        this.tier = tier;
        this.onDraw = onDraw;
    }

    @Override
    public void update() {
    }

    @Override
    public void onChunkUnload() {
    }

    @Override
    public void invalidate() {
    }

    @Override
    public double getEnergyStored() {
        return this.energy;
    }

    @Override
    public void setEnergyStored(double energy) {
        this.energy = Math.max(0, Math.min(energy, this.capacity));
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    @Override
    public void setCapacity(double capacity) {
        this.capacity = capacity;
        this.energy = Math.min(this.energy, capacity);
    }

    @Override
    public void drawEnergy(double amount) {
        if (amount <= 0) {
            return;
        }
        this.energy = Math.max(0, this.energy - amount);
        this.onDraw.accept(amount);
    }

    @Override
    public double getOfferedEnergy() {
        // No energy network to limit it by tier
        return this.energy;
    }

    @Override
    public int getSourceTier() {
        return this.tier;
    }

    @Override
    public void setSourceTier(int tier) {
        this.tier = tier;
    }
}
