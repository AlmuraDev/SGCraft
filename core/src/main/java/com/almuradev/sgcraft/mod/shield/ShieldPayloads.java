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

package com.almuradev.sgcraft.mod.shield;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import com.almuradev.sgcraft.mod.tool.ToolPayloads.GateInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

/**
 * What shield consoles and city shield generators send between the server and their screens.
 */
public final class ShieldPayloads {

    private ShieldPayloads() {
    }

    /**
     * A shield console's state, for its screen.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param console where the console is
     * @param gate the shielded gate it works, if one is near enough
     * @param range how far the console reaches, in blocks
     * @param control whether the player may raise and lower the gate's shield
     * @param message something to tell the player, or empty
     */
    public record ConsoleState(boolean open, BlockPos console, Optional<GateInfo> gate, int range, boolean control,
            String message) implements SGPacket {

        public static final Type<ConsoleState> TYPE = new Type<>(SGCraft.id("shield_console_state"));

        public ConsoleState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readBlockPos(), buf.readOptional(GateInfo::new), buf.readVarInt(),
                    buf.readBoolean(), buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeBlockPos(this.console);
            buf.writeOptional(this.gate, (out, gate) -> gate.write(out));
            buf.writeVarInt(this.range);
            buf.writeBoolean(this.control);
            buf.writeUtf(this.message);
        }

        @Override
        public Type<ConsoleState> type() {
            return ConsoleState.TYPE;
        }
    }

    /**
     * A city shield generator's state, for its screen.
     *
     * @param open whether to open the screen, rather than only update it if it is still open
     * @param generator where the generator is
     * @param active whether its shield is raised
     * @param width how far the shield reaches all round, in blocks
     * @param above how far it reaches up
     * @param below how far it reaches down
     * @param maxRadius the most it may reach each way
     * @param energyPerMillion how much energy it takes each tick for each million blocks it holds
     * @param available how much energy the power units round it hold
     * @param control whether the player may work it
     * @param message something to tell the player, or empty
     */
    public record CityState(boolean open, BlockPos generator, boolean active, int width, int above, int below,
            int maxRadius, double energyPerMillion, long available, boolean control, String message)
            implements SGPacket {

        public static final Type<CityState> TYPE = new Type<>(SGCraft.id("city_shield_state"));

        public CityState(final FriendlyByteBuf buf) {
            this(buf.readBoolean(), buf.readBlockPos(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readVarLong(), buf.readBoolean(),
                    buf.readUtf());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBoolean(this.open);
            buf.writeBlockPos(this.generator);
            buf.writeBoolean(this.active);
            buf.writeVarInt(this.width);
            buf.writeVarInt(this.above);
            buf.writeVarInt(this.below);
            buf.writeVarInt(this.maxRadius);
            buf.writeDouble(this.energyPerMillion);
            buf.writeVarLong(this.available);
            buf.writeBoolean(this.control);
            buf.writeUtf(this.message);
        }

        @Override
        public Type<CityState> type() {
            return CityState.TYPE;
        }
    }

    /**
     * Something done on a city shield generator's screen: raising or lowering it, or sizing it, as the screen's boxes
     * say.
     */
    public record CityAction(BlockPos generator, Kind kind, int width, int above, int below) implements SGPacket {

        public enum Kind {
            REFRESH, RAISE, APPLY, LOWER
        }

        public static final Type<CityAction> TYPE = new Type<>(SGCraft.id("city_shield_action"));

        public CityAction(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readEnum(Kind.class), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.generator);
            buf.writeEnum(this.kind);
            buf.writeVarInt(this.width);
            buf.writeVarInt(this.above);
            buf.writeVarInt(this.below);
        }

        @Override
        public Type<CityAction> type() {
            return CityAction.TYPE;
        }
    }

    /** Something done on a shield console's screen. */
    public record ConsoleAction(BlockPos console, Kind kind) implements SGPacket {

        public enum Kind {
            REFRESH, RAISE, LOWER
        }

        public static final Type<ConsoleAction> TYPE = new Type<>(SGCraft.id("shield_console_action"));

        public ConsoleAction(final FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readEnum(Kind.class));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeBlockPos(this.console);
            buf.writeEnum(this.kind);
        }

        @Override
        public Type<ConsoleAction> type() {
            return ConsoleAction.TYPE;
        }
    }
}
