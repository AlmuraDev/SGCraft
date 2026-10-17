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

package com.almuradev.sgcraft.mod.computer;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.gate.DialMode;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.DialingComputerBlock;
import com.almuradev.sgcraft.mod.block.OfficeChairBlock;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.dhd.DhdActions;
import com.almuradev.sgcraft.mod.entity.SeatEntity;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the server does as players sit at dialing computers and work them. A player works a computer from an office
 * chair standing in front of it, facing it, within the server's {@link ComputerSettings#chairReach() chair reach}:
 * sitting in the chair opens the computer's screen, and getting up closes it. Using the computer seats the player
 * in its chair.
 */
public final class DialingComputers {

    // The computer each seated player works
    private static final Map<UUID, GlobalPos> SESSIONS = new ConcurrentHashMap<>();

    private DialingComputers() {
    }

    /** A player used a computer: seats them in its chair, or tells them it needs one. */
    public static void use(final ServerPlayer player, final BlockPos computer) {
        final ServerLevel level = player.level();
        if (player.getVehicle() instanceof final SeatEntity seat
                && computer.equals(DialingComputers.computerFacing(level, seat.chair()))) {
            DialingComputers.open(player, computer);
            return;
        }
        final BlockState state = level.getBlockState(computer);
        if (!(state.getBlock() instanceof DialingComputerBlock)) {
            return;
        }
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        for (int distance = 1; distance <= ComputerSettings.current().chairReach(); distance++) {
            final BlockPos chair = computer.relative(facing, distance);
            if (level.getBlockState(chair).getBlock() instanceof OfficeChairBlock
                    && level.getBlockState(chair).getValue(HorizontalDirectionalBlock.FACING) == facing.getOpposite()) {
                DialingComputers.sit(player, chair);
                return;
            }
        }
        player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.dialing_computer.no_chair",
                "Put an office chair in front of the computer, facing it, and sit down to use it")
                .withStyle(ChatFormatting.YELLOW));
    }

    /** A player used a chair: seats them, and opens the computer the chair faces, if there is one. */
    public static void sit(final ServerPlayer player, final BlockPos chair) {
        final ServerLevel level = player.level();
        final BlockState state = level.getBlockState(chair);
        if (!(state.getBlock() instanceof OfficeChairBlock)) {
            return;
        }
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (!(player.getVehicle() instanceof final SeatEntity seat && seat.chair().equals(chair))) {
            if (player.isPassenger() || !level.getEntitiesOfClass(SeatEntity.class, new AABB(chair)).isEmpty()) {
                return;
            }
            final SeatEntity seat = SeatEntity.create(level, chair, facing);
            level.addFreshEntity(seat);
            if (!player.startRiding(seat, false, true)) {
                seat.discard();
                return;
            }
        }
        final BlockPos computer = DialingComputers.computerFacing(level, chair);
        if (computer != null) {
            // Looks at the middle of the computer's screen
            final Vec3 screen = Vec3.atBottomCenterOf(computer).add(0, 1.25, 0);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, screen);
            DialingComputers.open(player, computer);
        }
    }

    /**
     * @return the middle lower part of the computer that a chair faces, within the chair reach, with the computer's
     *     screen facing the chair; or null if there is none
     */
    public static @Nullable BlockPos computerFacing(final ServerLevel level, final BlockPos chair) {
        final BlockState chairState = level.getBlockState(chair);
        if (!(chairState.getBlock() instanceof OfficeChairBlock)) {
            return null;
        }
        final Direction facing = chairState.getValue(HorizontalDirectionalBlock.FACING);
        for (int distance = 1; distance <= ComputerSettings.current().chairReach(); distance++) {
            final BlockPos pos = chair.relative(facing, distance);
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DialingComputerBlock
                    && state.getValue(HorizontalDirectionalBlock.FACING) == facing.getOpposite()
                    && state.getValue(DialingComputerBlock.PART) == DialingComputerBlock.Part.MIDDLE_LOWER) {
                return pos;
            }
        }
        return null;
    }

    private static void open(final ServerPlayer player, final BlockPos computer) {
        DialingComputers.SESSIONS.put(player.getUUID(), GlobalPos.of(player.level().dimension(), computer));
        SGNetwork.send(player, new ComputerPayloads.Screen(computer, true));
    }

    /** A player got up from a chair: closes their computer's screen. */
    public static void stoodUp(final ServerPlayer player) {
        final GlobalPos computer = DialingComputers.SESSIONS.remove(player.getUUID());
        if (computer != null) {
            SGNetwork.send(player, new ComputerPayloads.Screen(computer.pos(), false));
        }
    }

    /** @return the computer a player is seated at, if they are */
    public static Optional<GlobalPos> session(final ServerPlayer player) {
        return Optional.ofNullable(DialingComputers.SESSIONS.get(player.getUUID()));
    }

    /** A button pressed on a computer's screen, by a player seated at that computer. */
    public static void action(final ServerPlayer player, final ComputerPayloads.Action action) {
        final GlobalPos session = DialingComputers.SESSIONS.get(player.getUUID());
        if (session == null || !session.dimension().equals(player.level().dimension())
                || !session.pos().equals(action.computer()) || !(player.getVehicle() instanceof SeatEntity)) {
            return;
        }
        if (action.kind() == ComputerPayloads.Action.Kind.CLOSE) {
            player.stopRiding();
            return;
        }
        if (!(player.level().getBlockEntity(action.computer()) instanceof final DialingComputerBlockEntity computer)) {
            return;
        }
        final Optional<Stargate> gate = computer.gate();
        if (gate.isEmpty()) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.dialing_computer.unlinked",
                    "No stargate is in reach of this computer").withStyle(ChatFormatting.RED));
            return;
        }
        final Cause cause = Cause.of(computer, player);
        switch (action.kind()) {
            case DIAL_SLOW, DIAL_FAST -> Address.parse(action.address()).ifPresentOrElse(address ->
                    DhdActions.report(player, gate.get().dial(address,
                            action.kind() == ComputerPayloads.Action.Kind.DIAL_FAST ? DialMode.QUICK
                                    : DialMode.STANDARD, cause)),
                    () -> player.sendOverlayMessage(Component.translatableWithFallback(
                            "message.sgcraft.dialing_computer.bad_address", "That isn't an address")
                            .withStyle(ChatFormatting.RED)));
            case ABORT -> {
                if (gate.get().state() != GateState.IDLE) {
                    gate.get().disconnect(cause);
                }
            }
            case IRIS -> DialingComputers.toggleIris(player, gate.get(), cause);
            default -> {
            }
        }
    }

    // Shuts an open iris, or opens a shut one
    private static void toggleIris(final ServerPlayer player, final Stargate gate, final Cause cause) {
        final Optional<Iris> iris = gate.iris();
        if (iris.isEmpty()) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.dialing_computer.no_iris",
                    "This gate has no iris").withStyle(ChatFormatting.RED));
            return;
        }
        final IrisState state = iris.get().state();
        final ActionResult result = state == IrisState.OPEN || state == IrisState.OPENING ? iris.get().close(cause)
                : iris.get().open(cause);
        if (result == ActionResult.DENIED) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.dialing_computer.iris_denied",
                    "You may not work this gate's iris").withStyle(ChatFormatting.RED));
        }
    }
}
