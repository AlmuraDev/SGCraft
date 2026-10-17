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

package com.almuradev.sgcraft.mod.address;

import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.address.AddressPartition;
import com.almuradev.sgcraft.api.address.AddressReach;
import com.almuradev.sgcraft.api.address.AddressTarget;
import com.almuradev.sgcraft.api.address.Addressing;
import com.almuradev.sgcraft.api.address.Symbol;
import com.almuradev.sgcraft.api.address.WorldGroup;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A reach that names a cell of a world by its position on a grid, then names the world, the group, or neither.
 *
 * <p>With {@code p} position symbols there are {@code 36^p} codes, which is {@code (6^p)^2}, so the grid is
 * {@code 6^p} cells square, centered on the world's middle. The cell's code is scrambled so that gates near each other
 * don't get addresses that look alike. The {@code @} symbol is never a position symbol.</p>
 */
public final class GridReach implements AddressReach {

    /** What the symbols after the position name. */
    public enum Kind {
        /** Nothing: the dialing gate's own world. */
        SAME_WORLD(0),
        /** The world, within the dialing gate's group, which may be the dialing gate's own world. */
        SAME_GROUP(1),
        /** The world, then its group, which may be the dialing gate's own group. */
        OTHER_GROUP(2);

        private final int extraSymbols;

        Kind(final int extraSymbols) {
            this.extraSymbols = extraSymbols;
        }
    }

    // Symbols a position can use: every symbol but the last, @
    private static final int BASE = Symbol.COUNT - 1;
    private static final BigInteger SCRAMBLE = BigInteger.valueOf(2_654_435_761L);
    private static final BigInteger OFFSET = BigInteger.valueOf(1_013_904_223L);

    private final int symbolCount;
    private final Kind kind;

    public GridReach(final int symbolCount, final Kind kind) {
        this.symbolCount = symbolCount;
        this.kind = kind;
    }

    @Override
    public int symbolCount() {
        return this.symbolCount;
    }

    /** @return what the symbols after the position name */
    public Kind kind() {
        return this.kind;
    }

    @Override
    public Optional<Address> addressOf(final GlobalPos target, final ResourceKey<Level> from) {
        return this.addressOf(target, from, false);
    }

    /**
     * @return the address of a cell that names its world, and so reaches it from the cell's own world too, or empty
     *     if this reach names no world
     */
    public Optional<Address> namingAddressOf(final GlobalPos target) {
        return this.kind == Kind.SAME_WORLD ? Optional.empty() : this.addressOf(target, target.dimension(), true);
    }

    // The address of a cell from a world; where it may be written without naming the world or group, it isn't,
    // unless the naming is wanted
    private Optional<Address> addressOf(final GlobalPos target, final ResourceKey<Level> from, final boolean naming) {
        final Addressing addressing = SGCraftApi.get().addressing();
        final ResourceKey<Level> world = target.dimension();
        final List<Symbol> after = new ArrayList<>(2);
        switch (this.kind) {
            case SAME_WORLD -> {
                if (!world.equals(from)) {
                    return Optional.empty();
                }
            }
            case SAME_GROUP -> {
                final WorldGroup group = addressing.groupOf(from);
                if (world.equals(from) && !naming || !addressing.groupOf(world).equals(group)) {
                    return Optional.empty();
                }
                final Optional<Symbol> worldSymbol = group.symbolOf(world);
                if (worldSymbol.isEmpty()) {
                    return Optional.empty();
                }
                after.add(worldSymbol.get());
            }
            case OTHER_GROUP -> {
                final WorldGroup group = addressing.groupOf(world);
                if (group.equals(addressing.groupOf(from)) && !naming) {
                    return Optional.empty();
                }
                final Optional<Symbol> worldSymbol = group.symbolOf(world);
                if (worldSymbol.isEmpty()) {
                    return Optional.empty();
                }
                after.add(worldSymbol.get());
                after.add(group.symbol());
            }
        }
        final Optional<Symbol> origin = addressing.partition(from).pointOfOrigin();
        final int positions = this.positionSymbols(origin.isPresent());
        if (positions < 1) {
            return Optional.empty();
        }
        final Optional<List<Symbol>> position = GridReach.encode(ChunkPos.containing(target.pos()),
                addressing.partition(world), positions);
        if (position.isEmpty()) {
            return Optional.empty();
        }
        final List<Symbol> symbols = new ArrayList<>(position.get());
        symbols.addAll(after);
        origin.ifPresent(symbols::add);
        return Optional.of(new Address(symbols));
    }

    @Override
    public Optional<AddressTarget> resolve(final Address address, final ResourceKey<Level> from) {
        if (address.size() != this.symbolCount) {
            return Optional.empty();
        }
        final Addressing addressing = SGCraftApi.get().addressing();
        final List<Symbol> symbols = new ArrayList<>(address.symbols());
        final Optional<Symbol> origin = addressing.partition(from).pointOfOrigin();
        if (origin.isPresent() && !symbols.removeLast().equals(origin.get())) {
            return Optional.empty();
        }
        final Optional<ResourceKey<Level>> world = switch (this.kind) {
            case SAME_WORLD -> Optional.of(from);
            case SAME_GROUP -> {
                final Symbol worldSymbol = symbols.removeLast();
                final WorldGroup group = addressing.groupOf(from);
                yield GridReach.worldWithSymbol(group, worldSymbol);
            }
            case OTHER_GROUP -> {
                final Symbol groupSymbol = symbols.removeLast();
                final Symbol worldSymbol = symbols.removeLast();
                yield addressing.groups().stream()
                        .filter(group -> group.symbol().equals(groupSymbol))
                        .findFirst()
                        .flatMap(group -> GridReach.worldWithSymbol(group, worldSymbol));
            }
        };
        if (world.isEmpty() || symbols.isEmpty()) {
            return Optional.empty();
        }
        return GridReach.decode(symbols, addressing.partition(world.get()))
                .map(chunk -> new AddressTarget(world.get(), chunk));
    }

    private int positionSymbols(final boolean origin) {
        return this.symbolCount - this.kind.extraSymbols - (origin ? 1 : 0);
    }

    private static Optional<ResourceKey<Level>> worldWithSymbol(final WorldGroup group, final Symbol symbol) {
        return group.worlds().stream()
                .filter(world -> group.symbolOf(world).filter(symbol::equals).isPresent())
                .findFirst();
    }

    /**
     * @return the position symbols of the cell holding a chunk: the cell's column and row on the grid, scrambled,
     *     written in base 36 with the most significant symbol first; empty if the cell is off the grid
     */
    public static Optional<List<Symbol>> encode(final ChunkPos chunk, final AddressPartition partition,
            final int positions) {
        final long side = GridReach.side(positions);
        final long column = Math.floorDiv(chunk.x(), partition.cellChunks()) + side / 2;
        final long row = Math.floorDiv(chunk.z(), partition.cellChunks()) + side / 2;
        if (column < 0 || column >= side || row < 0 || row >= side) {
            return Optional.empty();
        }
        final BigInteger codes = BigInteger.valueOf(GridReach.BASE).pow(positions);
        BigInteger code = BigInteger.valueOf(column).multiply(BigInteger.valueOf(side)).add(BigInteger.valueOf(row))
                .multiply(GridReach.SCRAMBLE).add(GridReach.OFFSET).mod(codes);
        final Symbol[] symbols = new Symbol[positions];
        final BigInteger base = BigInteger.valueOf(GridReach.BASE);
        for (int i = positions - 1; i >= 0; i--) {
            final BigInteger[] division = code.divideAndRemainder(base);
            symbols[i] = new Symbol(division[1].intValueExact());
            code = division[0];
        }
        return Optional.of(List.of(symbols));
    }

    /** @return the chunk with the lowest x and z of the cell some position symbols name, if they name one */
    public static Optional<ChunkPos> decode(final List<Symbol> symbols, final AddressPartition partition) {
        final int positions = symbols.size();
        final BigInteger base = BigInteger.valueOf(GridReach.BASE);
        BigInteger code = BigInteger.ZERO;
        for (final Symbol symbol : symbols) {
            if (symbol.index() >= GridReach.BASE) {
                return Optional.empty();
            }
            code = code.multiply(base).add(BigInteger.valueOf(symbol.index()));
        }
        final BigInteger codes = base.pow(positions);
        final long cell = code.subtract(GridReach.OFFSET).multiply(GridReach.SCRAMBLE.modInverse(codes)).mod(codes)
                .longValueExact();
        final long side = GridReach.side(positions);
        final long column = cell / side - side / 2;
        final long row = cell % side - side / 2;
        final long x = column * partition.cellChunks();
        final long z = row * partition.cellChunks();
        if (x != (int) x || z != (int) z) {
            return Optional.empty();
        }
        return Optional.of(new ChunkPos((int) x, (int) z));
    }

    // How many cells wide and long the grid is: 6^positions, the square root of 36^positions
    private static long side(final int positions) {
        return BigInteger.valueOf(6).pow(positions).longValueExact();
    }
}
