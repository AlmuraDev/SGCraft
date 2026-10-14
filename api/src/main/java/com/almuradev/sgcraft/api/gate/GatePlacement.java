/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.almuradev.sgcraft.api.gate;

import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;

import java.util.Objects;

/**
 * A request to build a gate in the world, for {@link Stargates#place(GatePlacement, com.almuradev.sgcraft.api.Cause)}.
 *
 * <p>{@link #builder()} is the easiest way to make one.</p>
 *
 * @param position where the gate's base block goes
 * @param orientation which way the gate's front and the top of its ring point; see {@link Stargate#orientation()}
 * @param type the gate's type
 * @param size the size of the gate's ring
 * @param chevrons how many chevrons the gate has; at least 1
 * @param mode what to do about blocks already in the gate's space
 * @param searchRadius how far from {@code position}, in blocks, to look for a place where the gate fits, or 0 to use
 *     {@code position} only
 * @param fixtures the blocks to build around the gate, such as a DHD; empty for none. The record keeps its own
 *     copy, and {@link #fixtures()} returns a new copy each call
 * @since 0.1.0
 */
public record GatePlacement(GlobalPos position, FrontAndTop orientation, GateType type, GateSize size, int chevrons,
        PlacementMode mode, int searchRadius, FixtureBuffer fixtures) {

    /**
     * @throws IllegalArgumentException if {@code chevrons} is below 1 or {@code searchRadius} is negative
     */
    public GatePlacement {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(orientation, "orientation");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(mode, "mode");
        fixtures = Objects.requireNonNull(fixtures, "fixtures").copy();
        if (chevrons < 1) {
            throw new IllegalArgumentException("A gate has at least 1 chevron, not " + chevrons);
        }
        if (searchRadius < 0) {
            throw new IllegalArgumentException("The search radius can't be negative: " + searchRadius);
        }
    }

    /**
     * The same as the canonical constructor, for a type still in its registry holder.
     *
     * @throws IllegalArgumentException if {@code chevrons} is below 1 or {@code searchRadius} is negative
     */
    public GatePlacement(final GlobalPos position, final FrontAndTop orientation, final Holder<GateType> type,
            final GateSize size, final int chevrons, final PlacementMode mode, final int searchRadius,
            final FixtureBuffer fixtures) {
        this(position, orientation, Objects.requireNonNull(type, "type").value(), size, chevrons, mode, searchRadius,
                fixtures);
    }

    /**
     * @return a builder, with a standard size, {@link PlacementMode#ATTEMPT}, no search, and the type's own
     *     {@linkplain GateType#fixtures(GateSize) fixtures}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param position another position
     * @return this placement at that position, such as a spot a search found
     */
    public GatePlacement withPosition(final GlobalPos position) {
        return new GatePlacement(position, this.orientation, this.type, this.size, this.chevrons, this.mode,
                this.searchRadius, this.fixtures);
    }

    /** @return a copy of the blocks to build around the gate */
    @Override
    public FixtureBuffer fixtures() {
        return this.fixtures.copy();
    }

    /**
     * Builds a {@link GatePlacement}. The position, orientation, type and chevrons have no default and must be set.
     */
    public static final class Builder {

        private GlobalPos position;
        private FrontAndTop orientation;
        private GateType type;
        private GateSize size = GateSize.STANDARD;
        private int chevrons;
        private PlacementMode mode = PlacementMode.ATTEMPT;
        private int searchRadius;
        private FixtureBuffer fixtures;

        private Builder() {
        }

        public Builder position(final GlobalPos position) {
            this.position = Objects.requireNonNull(position, "position");
            return this;
        }

        public Builder orientation(final FrontAndTop orientation) {
            this.orientation = Objects.requireNonNull(orientation, "orientation");
            return this;
        }

        public Builder type(final GateType type) {
            this.type = Objects.requireNonNull(type, "type");
            return this;
        }

        public Builder type(final Holder<GateType> type) {
            return this.type(Objects.requireNonNull(type, "type").value());
        }

        public Builder size(final GateSize size) {
            this.size = Objects.requireNonNull(size, "size");
            return this;
        }

        public Builder chevrons(final int chevrons) {
            this.chevrons = chevrons;
            return this;
        }

        public Builder mode(final PlacementMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        public Builder searchRadius(final int searchRadius) {
            this.searchRadius = searchRadius;
            return this;
        }

        /**
         * @param fixtures the blocks to build around the gate, in place of the type's own
         * @return this builder
         */
        public Builder fixtures(final FixtureBuffer fixtures) {
            this.fixtures = Objects.requireNonNull(fixtures, "fixtures").copy();
            return this;
        }

        /** @return this builder, set to build no fixtures */
        public Builder noFixtures() {
            this.fixtures = new FixtureBuffer();
            return this;
        }

        /**
         * @return the placement
         * @throws IllegalStateException if the position, orientation or type isn't set
         * @throws IllegalArgumentException if the chevrons or search radius is out of range
         */
        public GatePlacement build() {
            if (this.position == null || this.orientation == null || this.type == null) {
                throw new IllegalStateException("A placement needs a position, an orientation and a type");
            }
            final FixtureBuffer fixtures = this.fixtures == null ? this.type.fixtures(this.size) : this.fixtures;
            return new GatePlacement(this.position, this.orientation, this.type, this.size, this.chevrons, this.mode,
                    this.searchRadius, fixtures);
        }
    }
}
