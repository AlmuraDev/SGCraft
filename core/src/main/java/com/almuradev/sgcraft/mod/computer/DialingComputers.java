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
import com.almuradev.sgcraft.api.gate.DialResult;
import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Iris;
import com.almuradev.sgcraft.api.gate.IrisState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.block.ComputerDeskBlock;
import com.almuradev.sgcraft.mod.block.DialingComputerBlock;
import com.almuradev.sgcraft.mod.block.MalpConsoleBlock;
import com.almuradev.sgcraft.mod.block.OfficeChairBlock;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.dhd.DhdActions;
import com.almuradev.sgcraft.mod.entity.SeatEntity;
import com.almuradev.sgcraft.mod.malp.MalpLinks;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.ConsoleUse;
import com.almuradev.sgcraft.mod.malp.MalpPayloads.Use;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the server does as players work dialing computers. A player works a computer from an office chair standing in
 * front of it, facing it, within the server's {@link ComputerSettings#chairReach() chair reach}: sitting in the chair
 * opens the computer's screen, and getting up closes it. Using a computer with such a chair seats the player in it.
 * Using one without a chair steps the player up to its front, standing, and other players see them there with their
 * hands on its keyboard; walking away closes the screen.
 *
 * <p>The chairs, and where a player stands at a computer, are the same for every {@link ComputerDeskBlock computer
 * at a desk}: a chair facing a MALP console opens that console's screen; see {@link MalpLinks}.</p>
 */
public final class DialingComputers {

    // How far in front of a computer's middle a player stands to work it, in blocks
    private static final double FRONT = 0.95;
    // How far from that spot a standing player may wander before the screen closes, in blocks
    private static final double WANDER = 2.5;
    // How near a computer its user must be for a button to count, standing or seated, in blocks
    private static final double USE_RANGE = 8;

    /**
     * The computer a player works, and whether they stand at it rather than sit.
     *
     * @param computer the computer's middle lower part
     * @param standing whether the player stands at the computer, with no chair
     */
    public record Session(GlobalPos computer, boolean standing) {
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private DialingComputers() {
    }

    /**
     * A player used a computer: seats them in its chair if it has one, or else stands them at it. A player already
     * working it has its screen opened again.
     */
    public static void use(final ServerPlayer player, final BlockPos computer) {
        final ServerLevel level = player.level();
        if (!DialingComputers.startUp(player, computer)) {
            return;
        }
        if (player.getVehicle() instanceof final SeatEntity seat
                && computer.equals(DialingComputers.computerFacing(level, seat.chair()))) {
            DialingComputers.open(player, computer, false);
            return;
        }
        final BlockState state = level.getBlockState(computer);
        if (!(state.getBlock() instanceof DialingComputerBlock)) {
            return;
        }
        final BlockPos chair = DialingComputers.freeChair(level, computer);
        if (chair != null) {
            DialingComputers.sit(player, chair);
            return;
        }
        DialingComputers.stand(player, computer);
    }

    /**
     * @param computer a computer's middle lower part
     * @return an office chair in front of the computer, facing it, within the chair reach, that no one sits in; or
     *     null if there is none
     */
    public static @Nullable BlockPos freeChair(final ServerLevel level, final BlockPos computer) {
        final BlockState state = level.getBlockState(computer);
        if (!(state.getBlock() instanceof ComputerDeskBlock)) {
            return null;
        }
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        for (int distance = 1; distance <= ComputerSettings.current().chairReach(); distance++) {
            final BlockPos chair = computer.relative(facing, distance);
            if (level.getBlockState(chair).getBlock() instanceof OfficeChairBlock
                    && level.getBlockState(chair).getValue(HorizontalDirectionalBlock.FACING) == facing.getOpposite()
                    && level.getEntitiesOfClass(SeatEntity.class, new AABB(chair)).isEmpty()) {
                return chair;
            }
        }
        return null;
    }

    // Stands a player at a computer with no free chair: steps them to its front, if that is in reach and safe to stand
    // on, turns them to its monitor, and opens its screen
    private static void stand(final ServerPlayer player, final BlockPos computer) {
        if (player.isPassenger()) {
            return;
        }
        DialingComputers.stepUp(player, computer);
        final boolean before = DialingComputers.SESSIONS.containsKey(player.getUUID());
        DialingComputers.open(player, computer, true);
        if (!before) {
            DialingComputers.broadcastUse(player, Use.COMPUTER);
        }
    }

    /**
     * Steps a player on their own feet up to the front of a computer, if that spot is within their reach and safe to
     * stand on, and turns them to its monitor.
     *
     * @param computer the computer's middle lower part
     */
    public static void stepUp(final ServerPlayer player, final BlockPos computer) {
        final ServerLevel level = player.level();
        final BlockState state = level.getBlockState(computer);
        if (player.isPassenger() || !(state.getBlock() instanceof ComputerDeskBlock)) {
            return;
        }
        final Vec3 spot = DialingComputers.spot(computer, state.getValue(HorizontalDirectionalBlock.FACING));
        final AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(spot);
        final BlockPos ground = BlockPos.containing(spot).below();
        if (player.position().distanceTo(spot) <= player.blockInteractionRange() && level.noCollision(player, box)
                && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                && !level.containsAnyLiquid(box)) {
            player.teleportTo(spot.x, spot.y, spot.z);
            player.resetFallDistance();
        }
        DialingComputers.lookAtScreen(player, computer);
    }

    /** @return where a player stands to work a computer: in front of its middle, on the floor it stands on */
    public static Vec3 spot(final BlockPos computer, final Direction facing) {
        return Vec3.atBottomCenterOf(computer).add(facing.getStepX() * DialingComputers.FRONT, 0,
                facing.getStepZ() * DialingComputers.FRONT);
    }

    /** Turns a player to the middle of a computer's monitor. */
    public static void lookAtScreen(final ServerPlayer player, final BlockPos computer) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(computer).add(0, 1.25, 0));
    }

    /**
     * A player used a chair: seats them, and opens the computer the chair faces, if there is one: a dialing
     * computer's screen, or a MALP console's.
     */
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
            // A player standing at a computer leaves it to sit
            DialingComputers.end(player);
            final SeatEntity seat = SeatEntity.create(level, chair, facing);
            level.addFreshEntity(seat);
            if (!player.startRiding(seat, false, true)) {
                seat.discard();
                return;
            }
        }
        final BlockPos computer = DialingComputers.computerFacing(level, chair);
        if (computer == null) {
            return;
        }
        DialingComputers.lookAtScreen(player, computer);
        if (level.getBlockState(computer).getBlock() instanceof MalpConsoleBlock) {
            MalpLinks.open(player, GlobalPos.of(level.dimension(), computer));
        } else {
            DialingComputers.open(player, computer, false);
        }
    }

    /**
     * @return the middle lower part of the computer that a chair faces, a dialing computer or a MALP console,
     *     within the chair reach, with the computer's screen facing the chair; or null if there is none
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
            if (ComputerDeskBlock.isMain(state)
                    && state.getValue(HorizontalDirectionalBlock.FACING) == facing.getOpposite()) {
                return pos;
            }
        }
        return null;
    }

    private static void open(final ServerPlayer player, final BlockPos computer, final boolean standing) {
        if (!DialingComputers.startUp(player, computer)) {
            return;
        }
        DialingComputers.SESSIONS.put(player.getUUID(), new Session(GlobalPos.of(player.level().dimension(),
                computer), standing));
        SGNetwork.send(player, new ComputerPayloads.Screen(computer, true));
    }

    /**
     * Readies a computer, a dialing computer or a MALP console, for a player to use: switches it on if it was off,
     * looks again at once for power if it had none, and tells them if it still has none.
     *
     * @param computer the computer's middle lower part
     * @return whether it runs, and may be used
     */
    public static boolean startUp(final ServerPlayer player, final BlockPos computer) {
        if (!(player.level().getBlockEntity(computer) instanceof final PoweredComputer powered)) {
            return true;
        }
        if (!powered.running()) {
            powered.setOn(true);
        }
        if (!powered.running()) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.computer.no_power",
                    "The computer has no power").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    /**
     * Switches a computer, a dialing computer or a MALP console, off, or on again, with a click, and tells the player.
     *
     * @param computer the computer's middle lower part
     */
    public static void togglePower(final ServerPlayer player, final BlockPos computer) {
        final ServerLevel level = player.level();
        if (!(level.getBlockEntity(computer) instanceof final PoweredComputer powered)) {
            return;
        }
        final boolean on = !powered.power().on();
        powered.setOn(on);
        level.playSound(null, computer, on ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
                SoundSource.BLOCKS, 0.5F, on ? 0.6F : 0.5F);
        player.sendOverlayMessage(!on
                ? Component.translatableWithFallback("message.sgcraft.computer.off", "Computer switched off")
                : powered.running()
                        ? Component.translatableWithFallback("message.sgcraft.computer.on", "Computer switched on")
                        : Component.translatableWithFallback("message.sgcraft.computer.on_no_power",
                                "Computer switched on, but it has no power").withStyle(ChatFormatting.RED));
    }

    /** Ends the turn of everyone at a dialing computer, closing its screen, as it is switched off or loses power. */
    public static void shutDown(final ServerLevel level, final BlockPos computer) {
        final GlobalPos at = GlobalPos.of(level.dimension(), computer);
        for (final Map.Entry<UUID, Session> entry : List.copyOf(DialingComputers.SESSIONS.entrySet())) {
            if (!entry.getValue().computer().equals(at)) {
                continue;
            }
            final ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                DialingComputers.SESSIONS.remove(entry.getKey());
            } else {
                DialingComputers.end(player);
            }
        }
    }

    /** Ends a player's turn at a computer as they leave the server, so the others draw them as usual. */
    public static void left(final ServerPlayer player) {
        DialingComputers.end(player);
    }

    /**
     * Ends the turns at computers that are gone or whose chunk is no longer loaded, which don't tick to end them
     * themselves, and forgets players who have left. The server calls this once a second.
     */
    public static void sweep(final MinecraftServer server) {
        for (final Map.Entry<UUID, Session> entry : List.copyOf(DialingComputers.SESSIONS.entrySet())) {
            final ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            final GlobalPos at = entry.getValue().computer();
            final ServerLevel level = server.getLevel(at.dimension());
            if (player == null) {
                DialingComputers.SESSIONS.remove(entry.getKey());
            } else if (level == null || !level.isLoaded(at.pos())
                    || !(level.getBlockState(at.pos()).getBlock() instanceof DialingComputerBlock)) {
                DialingComputers.end(player);
            }
        }
    }

    /** Forgets every turn, as the server stops. */
    public static void clear() {
        DialingComputers.SESSIONS.clear();
    }

    /** A player got up from a chair: closes their computer's screen. */
    public static void stoodUp(final ServerPlayer player) {
        final Session session = DialingComputers.SESSIONS.get(player.getUUID());
        if (session != null && !session.standing()) {
            DialingComputers.end(player);
        }
    }

    // Ends a player's turn at a computer, closing its screen, and drawing them as usual again if they stood at it
    private static void end(final ServerPlayer player) {
        final Session session = DialingComputers.SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        SGNetwork.send(player, new ComputerPayloads.Screen(session.computer().pos(), false));
        if (session.standing()) {
            DialingComputers.broadcastUse(player, Use.NONE);
        }
    }

    /** @return the computer a player works, if they work one */
    public static Optional<GlobalPos> session(final ServerPlayer player) {
        return Optional.ofNullable(DialingComputers.SESSIONS.get(player.getUUID())).map(Session::computer);
    }

    /** @return whether a player works a computer standing at it, rather than from a chair */
    public static boolean standing(final ServerPlayer player) {
        final Session session = DialingComputers.SESSIONS.get(player.getUUID());
        return session != null && session.standing();
    }

    /**
     * Ends the turn of each player standing at a computer who has walked away from it, left its world or left the
     * server. The computer calls this each tick.
     */
    public static void tick(final ServerLevel level, final BlockPos computer) {
        if (DialingComputers.SESSIONS.isEmpty()) {
            return;
        }
        final BlockState state = level.getBlockState(computer);
        if (!(state.getBlock() instanceof DialingComputerBlock)) {
            return;
        }
        final Vec3 spot = DialingComputers.spot(computer, state.getValue(HorizontalDirectionalBlock.FACING));
        for (final Map.Entry<UUID, Session> entry : List.copyOf(DialingComputers.SESSIONS.entrySet())) {
            final Session session = entry.getValue();
            if (!session.standing() || !session.computer().pos().equals(computer)
                    || !session.computer().dimension().equals(level.dimension())) {
                continue;
            }
            final ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                DialingComputers.SESSIONS.remove(entry.getKey());
            } else if (player.isRemoved() || player.level() != level || player.isPassenger()
                    || player.position().distanceTo(spot) > DialingComputers.WANDER) {
                DialingComputers.end(player);
            }
        }
    }

    /** Tells a player who starts tracking another how to draw the other, if the other stands at a computer. */
    public static void startTracking(final ServerPlayer seen, final ServerPlayer viewer) {
        if (DialingComputers.standing(seen)) {
            SGNetwork.send(viewer, new ConsoleUse(seen.getId(), Use.COMPUTER));
        }
    }

    // Tells the players near a player how to draw them, at a computer or as usual; those who come near later are told
    // as they start tracking them
    private static void broadcastUse(final ServerPlayer player, final Use use) {
        SGNetwork.sendNear(player, new ConsoleUse(player.getId(), use));
    }

    /** A button pressed on a computer's screen, by a player working that computer. */
    public static void action(final ServerPlayer player, final ComputerPayloads.Action action) {
        final Session session = DialingComputers.SESSIONS.get(player.getUUID());
        if (session == null || !session.computer().dimension().equals(player.level().dimension())
                || !session.computer().pos().equals(action.computer())
                || !session.standing() && !(player.getVehicle() instanceof SeatEntity)) {
            return;
        }
        if (action.kind() == ComputerPayloads.Action.Kind.CLOSE) {
            if (session.standing()) {
                DialingComputers.end(player);
            } else {
                player.stopRiding();
            }
            return;
        }
        // A session the computer hasn't ended yet, such as after a respawn or teleport, works only near the computer;
        // this also keeps a far computer's chunk from being loaded by the lookup
        if (!action.computer().closerToCenterThan(player.position(), DialingComputers.USE_RANGE)) {
            DialingComputers.end(player);
            return;
        }
        if (!(player.level().getBlockEntity(action.computer()) instanceof final DialingComputerBlockEntity computer)) {
            return;
        }
        if (action.kind() == ComputerPayloads.Action.Kind.MODE) {
            Arrays.stream(ComputerDialMode.values()).filter(mode -> mode.getSerializedName().equals(action.address()))
                    .findFirst().ifPresent(computer::setMode);
            return;
        }
        if (action.kind() == ComputerPayloads.Action.Kind.ALARM) {
            computer.setAlarmOff(!computer.alarmOff());
            return;
        }
        final BlockPos at = computer.getBlockPos();
        final Optional<Stargate> gate = computer.gate();
        if (gate.isEmpty()) {
            DialingComputers.tell(player, at, "message.sgcraft.dialing_computer.unlinked",
                    "No stargate is in reach of this computer");
            return;
        }
        final Cause cause = Cause.of(computer, player);
        switch (action.kind()) {
            case DIAL -> Address.parse(action.address()).ifPresentOrElse(address -> {
                final CompletionStage<DialResult> result = gate.get().dial(address,
                        computer.mode().dialMode(gate.get()), cause);
                // Each dial goes at the top of the computer's history, once it is known whether it connected
                result.thenAccept(outcome -> {
                    if (!computer.isRemoved()) {
                        computer.remember(address.toString(), outcome == DialResult.CONNECTED);
                    }
                });
                DhdActions.report(result, (key, fallback) -> DialingComputers.tell(player, at, key, fallback));
            },
                    () -> DialingComputers.tell(player, at, "message.sgcraft.dialing_computer.bad_address",
                            "That isn't an address"));
            case ABORT -> {
                if (gate.get().state() != GateState.IDLE) {
                    gate.get().disconnect(cause);
                }
            }
            case IRIS -> DialingComputers.toggleIris(player, at, gate.get(), cause);
            default -> {
            }
        }
    }
    // Shuts an open iris, or opens a shut one
    private static void toggleIris(final ServerPlayer player, final BlockPos computer, final Stargate gate,
            final Cause cause) {
        final Optional<Iris> iris = gate.iris();
        if (iris.isEmpty()) {
            DialingComputers.tell(player, computer, "message.sgcraft.dialing_computer.no_iris",
                    "This gate has no iris");
            return;
        }
        final IrisState state = iris.get().state();
        final ActionResult result = state == IrisState.OPEN || state == IrisState.OPENING ? iris.get().close(cause)
                : iris.get().open(cause);
        if (result == ActionResult.DENIED) {
            DialingComputers.tell(player, computer, "message.sgcraft.dialing_computer.iris_denied",
                    "You may not work this gate's iris");
        }
    }

    // Tells the player at a computer something on its screen, where the hotbar's messages would be hidden behind it
    private static void tell(final ServerPlayer player, final BlockPos computer, final String key,
            final String fallback) {
        SGNetwork.send(player, new ComputerPayloads.Notice(computer, key, fallback));
    }
}
