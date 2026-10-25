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

package com.almuradev.sgcraft.mod.entity;

import com.almuradev.sgcraft.api.gate.GateState;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.block.entity.DialingComputerBlockEntity;
import com.almuradev.sgcraft.mod.computer.ComputerDialMode;
import com.almuradev.sgcraft.mod.computer.ComputerPayloads;
import com.almuradev.sgcraft.mod.computer.DialerRef;
import com.almuradev.sgcraft.mod.computer.DialingComputers;
import com.almuradev.sgcraft.mod.computer.DialingTerminal;
import com.almuradev.sgcraft.mod.computer.GateLinks;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.network.SGNetwork;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.InterpolationTracker;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A puddle jumper, the Ancients' small ship from the show: it flies, carries six seated, and has a dialing computer
 * aboard, so it dials a gate and flies through it with everyone in it.
 *
 * <p>Its drive is switched on and off from the dashboard, by its button or its key; off, it doesn't fly, its headlights
 * are dark and its pods drawn in, and it sinks gently to the ground. On, it lights its headlights.</p>
 *
 * <p>Whoever sits at the helm flies it: forward and back thrust and brake it along the way it faces, picking up speed
 * over a few seconds and drifting to a stop, left and right turn it, jump climbs and {@code JumperKeys.DESCEND}
 * descends. Its drive pods swing out on their struts as it lifts off, as on the show; their button puts them out
 * or folds them in, standing or flying, as for flying into a gate, and they stay as the pilot chose; sprint, held while going ahead, puts them out regardless and runs their
 * engines hard for far more speed. They draw in again of themselves near an open gate, which they would not fit
 * through. It hangs in the air where it stops, as it has no weight while its drive runs. The pilot's own client moves
 * it, as a boat's rower's does, so it answers their keys at once, and everyone else sees it glide. The pilot switches
 * its headlights, which each client lights the world ahead with as it draws it; see {@code JumperLights}.</p>
 *
 * <p>Using the jumper boards it, in the free seat the player looks at, or else the first free seat, helm first.
 * Seated, using it again while looking at a free seat moves there. The players at the helm and in the dialer's seat
 * open the computer aboard by its key, and it dials the gate in reach as a dialing computer on a desk does.
 * The pilot may use it only while no one sits in the dialer's seat.</p>
 *
 * <p>While it stands still, riders may get up and walk about inside, among its {@link JumperHull solid parts}, and its
 * hatch lowers so they can walk out; it shuts once it moves. Riders can't get up while it moves, and anyone standing
 * inside as it sets off is sat down in a free seat, or put out of the hatch if there is none.</p>
 *
 * <p>Its hitbox is a square as wide as its hull, as every entity's is, but its nose and tail stop against blocks as
 * well, so the whole of its length meets the world: it turns only where there is room, and goes through a gate head
 * or tail first. Its riders keep their seats through a gate, to another world too.</p>
 *
 * <p>Its cloak generator runs either a shield or a cloak, one at a time, from a charge of its own that refills while
 * it runs neither and the drive is on; see {@link JumperSettings}. The shield shows only where it is struck, and each
 * blow it stops costs it energy; emptied, it falls and stays down a while. The cloak hides the jumper and everyone in
 * it from those outside, but not its sounds, and a faint shimmer gives it away as it moves. Launching a drone drops
 * it.</p>
 */
public final class PuddleJumperEntity extends VehicleEntity {

    /** The ticket that keeps the world round an open gate's far end loaded while a jumper flies near it. */
    public static final TicketType FAR_SIDE = new TicketType(40L, TicketType.FLAG_LOADING);
    /** How wide and tall the hull is, which is the hitbox; it fits a standard gate's opening, three blocks square. */
    public static final float WIDTH = 2.75F;
    public static final float HEIGHT = 2.5F;
    /** How long the hull is, nose to tail, about two and a half times its width, as the show's. */
    public static final double LENGTH = 7;
    // The nose and tail stop against blocks too: squares reaching to the hull's ends, so together with the hitbox they
    // span its length; the tail's as wide as the hull, its middle this far behind the jumper's, and the nose's a little
    // lower and narrower
    private static final double END_OFFSET = (PuddleJumperEntity.LENGTH - PuddleJumperEntity.WIDTH) / 2;
    private static final double NOSE_WIDTH = 2.4;
    private static final double NOSE_HEIGHT = 2.1;
    // The engines' thrust, as their glow shows it: idling, how much more a tick's gain in speed adds, in blocks a tick,
    // and how quickly it spools up and down each tick
    private static final float IDLE_THRUST = 0.12F;
    private static final double FLARE = 12;
    private static final float SPOOL_UP = 0.06F;
    private static final float SPOOL_DOWN = 0.025F;
    // Its top speeds, in blocks a tick: ahead, ahead with the turbo, 40 meters a second, and astern
    private static final double TOP_SPEED = 0.55;
    private static final double TURBO_SPEED = 2;
    private static final double REVERSE_SPEED = 0.2;
    // How much its speed changes each tick, in blocks a tick: picking up, picking up with the turbo, easing off with no
    // keys held, and braking against the way it goes; it takes a few seconds to reach its top speed, or to drift to a
    // stop
    private static final double ACCELERATION = 0.01;
    private static final double TURBO_ACCELERATION = 0.025;
    private static final double COAST = 0.007;
    private static final double BRAKE = 0.025;
    // How much of any sideways drift it keeps each tick, as it turns
    private static final double DRIFT = 0.85;
    // Its top climbing speed, and how much that changes each tick, climbing and easing off
    private static final double CLIMB_SPEED = 0.3;
    private static final double CLIMB_ACCELERATION = 0.015;
    private static final double CLIMB_COAST = 0.02;
    // How fast its turning speeds up, in degrees a tick each tick, and how much of it it keeps
    private static final float TURN = 0.6F;
    private static final float TURN_DRAG = 0.8F;
    // How often the computer aboard checks its link, in ticks
    private static final int LINK_TICKS = 20;
    // How near, in degrees, a seated player must look at another seat to move to it
    private static final double SEAT_AIM = 25;
    // How much less a jumper is hurt by a blow than a boat is
    private static final float TOUGHNESS = 5;
    // How far it banks for each degree a tick it turns, and tips for each block a tick it climbs, the most it does
    // either, in degrees, and how much of the way there it eases each tick
    private static final float BANK = 3;
    private static final float MAX_BANK = 12;
    private static final float TILT = 25;
    private static final float MAX_TILT = 8;
    // How fast it must go along, in blocks a tick, for its nose to tip all the way as it climbs
    private static final double TILT_SPEED = 0.2;
    private static final float EASE = 0.25F;
    // How long the drive pods and the hatch take to move all the way, in ticks
    private static final float PODS_TICKS = 20;
    private static final float HATCH_TICKS = 20;
    // How little it may move in a tick, in blocks, or turn, in degrees, and still stand still, and for how many ticks it
    // must, before riders may get up, and before its hatch lowers
    private static final double STILL = 0.002;
    private static final float STILL_TURN = 0.05F;
    private static final int STILL_TICKS = 5;
    // How near, in degrees, a pilot must look at a button on the dashboard to press it
    private static final double BUTTON_AIM = 9;
    // How near an open gate draws its pods in, in blocks
    private static final double GATE_NEAR = 12;
    // More than it can turn in a tick of flying, in degrees: a turn this sudden came from going through a gate
    private static final float JUMP_TURN = 20;
    // More than it can fly in a tick, in blocks: a move this far was a teleport, as through a gate
    private static final double MOST_MOVED = 4;
    // The world out of an open gate's far end is loaded ahead of a flown jumper, so it doesn't fly out into chunks
    // that aren't there yet, but only while it heads into the gate: within this far of it, in blocks, some seconds out
    // even on the turbo, and due there within this long, in ticks, or anyway this near it, as when hovering in front.
    // As much is loaded as it would cover in this long out of the gate, in ticks, at the speed it closes on it, out of
    // the far end's front, the way it comes out: from this many chunks round, while slow, to as far as its riders
    // will be sent, at most this many
    private static final double LOAD_NEAR = 160;
    private static final int LOAD_LEAD_TICKS = 200;
    private static final double LOAD_ANYWAY = 24;
    private static final int LOAD_AHEAD_TICKS = 100;
    private static final int LOAD_LEAST = 3;
    private static final int LOAD_RADIUS = 10;
    // How far off a gate's base the jumper's computer links to it, in blocks: far beyond a desk computer's, as the
    // jumper covers the ground quickly
    private static final double GATE_REACH = 512;
    // Its drones, carried in the curves of its drone bays, on the lower sides of its front cabin: how many a new jumper
    // carries, how long the bays take to slide out or in, in ticks, how far off the operator may launch at a target,
    // in blocks, how long between launches, in ticks, how fast a drone leaves its bay, in blocks a tick, and where the
    // left bay's drones lie with it out, in the jumper's frame from its middle underside; the right's is across
    public static final int DRONES = 6;
    private static final float BAYS_TICKS = 10;
    public static final double DRONE_RANGE = 128;
    private static final int LAUNCH_TICKS = 8;
    private static final double LAUNCH_SPEED = 0.6;
    private static final Vec3 BAY = new Vec3(1.7, 0.75, 1.25);

    private static final EntityDataAccessor<String> SEATS = SynchedEntityData.defineId(PuddleJumperEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<BlockPos>> DIALER_LINKED = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<String> DIALER_MODE = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DIALER_ALARM_OFF = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DIALER_HISTORY = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> PODS_OUT = SynchedEntityData.defineId(PuddleJumperEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HATCH_OPEN = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEADLIGHTS = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> POWERED = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DRONES_LEFT = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(PuddleJumperEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FRIENDLY_FIRE = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(PuddleJumperEntity.class,
            EntityDataSerializers.INT);
    // The cloak generator: how full it is, from 0 to 1, whether it runs the shield or the cloak, and the game time the
    // shield may be raised again after it was knocked down
    private static final EntityDataAccessor<Float> GENERATOR = SynchedEntityData.defineId(PuddleJumperEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SHIELD = SynchedEntityData.defineId(PuddleJumperEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CLOAKED = SynchedEntityData.defineId(PuddleJumperEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> SHIELD_DOWN_UNTIL = SynchedEntityData.defineId(
            PuddleJumperEntity.class, EntityDataSerializers.LONG);
    // How finely clients are told how full the generator is, so its slow drain isn't sent every tick
    private static final float GENERATOR_STEP = 1 / 200F;
    // How long the cloak takes to come on or go off all the way, in ticks
    private static final float CLOAK_TICKS = 20;
    // Where the shield's middle is, above the jumper's middle underside, in blocks
    public static final double SHIELD_MIDDLE = PuddleJumperEntity.HEIGHT / 2.0;
    // A crash: how much of its speed it keeps at most, in a tick, to have stopped; how much of the fastest it has
    // lately gone is still remembered each tick after; and how far on, in blocks, a block must stand for it to have
    // met one
    private static final double CRASH_STOP = 0.3;
    private static final double CRASH_FADE = 0.8;
    private static final double CRASH_REACH = 0.3;
    // With its drive off it drops like the weight it is: how much faster it falls each tick, and the fastest, in
    // blocks a tick
    private static final double FALL_GRAVITY = 0.08;
    private static final double FALL_SPEED = 3;

    private static final Codec<Map<UUID, JumperSeat>> SEATS_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC,
            JumperSeat.CODEC);

    // Who sits where, by UUID. A player who has got out keeps their seat here until someone else takes it, so riders
    // going through a gate to another world sit down again where they were
    private final Map<UUID, JumperSeat> seats = new HashMap<>();
    private final Dialer dialer = new Dialer();
    // How fast it turns, in degrees a tick, and how far it turned this tick, which turns its riders with it
    private float turnRate;
    private float turned;
    // On a rider's own client: the jumper's yaw as their view was last turned with it, and which way they looked
    // from its ahead then, so a sudden turn, as through a gate, leaves them looking the same way out of it
    private float riderYaw;
    private float riderLook;
    private boolean riderLooking;
    // How far it banks and tips, now and a tick ago, for clients to draw
    private float bank;
    private float bankO;
    private float tilt;
    private float tiltO;
    // How far out the drive pods are, and how far down the hatch is, from 0 to 1, now and a tick ago
    private float pods;
    private float podsO;
    private float hatch;
    private float hatchO;
    // How far out its drone bays are, from 0 to 1, now and a tick ago; how many drones it has launched, which bay the
    // next leaves from, and when it may launch again, on the server
    private float bays;
    private float baysO;
    // How much gate energy the cloak generator holds, on the server; below 0 until first set, when it fills
    private double generator = -1;
    // How far the cloak has come on, from 0 to 1, now and a tick ago
    private float cloak;
    private float cloakO;
    // Watching for a crash, on the server: the fastest it has lately gone, in blocks a tick, and which way; how many
    // ticks it has since gone far slower, and how fast it went as it began to
    private double crashPeak;
    private Vec3 crashWay = Vec3.ZERO;
    private int slowTicks;
    private double impactSpeed;
    // On clients, the game time the shield last rose or fell, and whether it rose, for the sweep across it
    private long shieldChanged = Long.MIN_VALUE;
    private boolean shieldRose;
    private int launched;
    private long nextLaunch;
    // How many ticks it has spent building its next drone, on the server
    private int droneBuild;
    // On clients, how hard the engines run, from 0 to 1, now and a tick ago, and how fast it went last tick, in blocks
    private float thrust;
    private float thrustO;
    private double lastSpeed;
    // Whether the pilot wants the pods out for the turbo; and whether they have put them out or folded them in with
    // their button, standing or flying, or null if they haven't, when the pods come out of themselves while it flies
    private boolean podsWanted;
    private @Nullable Boolean podsChosen;
    // Whether it was off the ground when last looked at, so landing can draw the weapon bays in
    private boolean wasAirborne;
    // How many ticks it has stood still, and where it was a tick ago, on the server
    private int stillTicks;
    private @Nullable Vec3 lastPosition;
    private float lastYaw;
    private final JumperHeadlights headlights = new JumperHeadlights();
    // On its pilot's client, whether it is held waiting for the world round it to arrive
    private boolean awaitingTerrain;
    // The hull's solid parts where it stood when last asked, and where and how that was
    private List<VoxelShape> hull = List.of();
    private @Nullable Vec3 hullAt;
    private float hullYaw;
    private boolean hullShut;

    public PuddleJumperEntity(final EntityType<? extends PuddleJumperEntity> type, final Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    // Its world hands it a callback as it joins and takes it away as it leaves, on both loaders, which keeps the index
    // of the world's jumpers
    @Override
    public void setLevelCallback(final EntityInLevelCallback callback) {
        super.setLevelCallback(callback);
        if (callback == EntityInLevelCallback.NULL) {
            JumperIndex.remove(this);
        } else {
            JumperIndex.add(this);
        }
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PuddleJumperEntity.SEATS, "");
        builder.define(PuddleJumperEntity.DIALER_LINKED, Optional.empty());
        builder.define(PuddleJumperEntity.DIALER_MODE, ComputerDialMode.GATE.getSerializedName());
        builder.define(PuddleJumperEntity.DIALER_ALARM_OFF, false);
        builder.define(PuddleJumperEntity.DIALER_HISTORY, "");
        builder.define(PuddleJumperEntity.PODS_OUT, false);
        builder.define(PuddleJumperEntity.HATCH_OPEN, false);
        builder.define(PuddleJumperEntity.HEADLIGHTS, false);
        builder.define(PuddleJumperEntity.POWERED, false);
        builder.define(PuddleJumperEntity.DRONES_LEFT, PuddleJumperEntity.DRONES);
        builder.define(PuddleJumperEntity.ARMED, false);
        builder.define(PuddleJumperEntity.FRIENDLY_FIRE, false);
        builder.define(PuddleJumperEntity.TARGET, -1);
        builder.define(PuddleJumperEntity.GENERATOR, 1F);
        builder.define(PuddleJumperEntity.SHIELD, false);
        builder.define(PuddleJumperEntity.CLOAKED, false);
        builder.define(PuddleJumperEntity.SHIELD_DOWN_UNTIL, 0L);
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (PuddleJumperEntity.SEATS.equals(accessor) && this.level().isClientSide()) {
            this.seats.clear();
            this.seats.putAll(PuddleJumperEntity.decodeSeats(this.entityData.get(PuddleJumperEntity.SEATS)));
        }
        // The shield rising or falling sweeps across its shell, but not as a jumper with its shield up first comes in
        // sight
        if (PuddleJumperEntity.SHIELD.equals(accessor) && this.level().isClientSide() && this.tickCount > 0) {
            this.shieldChanged = this.level().getGameTime();
            this.shieldRose = this.shieldUp();
        }
    }

    /** @return the game time the shield last rose or fell, as its clients saw; only on clients */
    public long shieldChanged() {
        return this.shieldChanged;
    }

    /** @return whether the shield rose, rather than fell, when it last changed; only on clients */
    public boolean shieldRose() {
        return this.shieldRose;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return new PilotSnap(this, LinearInterpolationHandler.create(this, 3));
    }

    // Its pilot's client moves it, so the server's word of its motion is only for everyone else
    @Override
    public void lerpMotion(final Vec3 movement) {
        if (!this.isLocalInstanceAuthoritative()) {
            super.lerpMotion(movement);
        }
    }

    /**
     * Everyone but its pilot sees the jumper glide to where the server says it is. Its pilot's client is told only of
     * its teleports, such as through a gate, and puts it there at once, riders and all, rather than gliding it
     * there through everything between, as it would to a gate near by; the teleport's motion, which is the motion it
     * went in with, it keeps.
     */
    private static final class PilotSnap implements InterpolationHandler {

        private final PuddleJumperEntity jumper;
        private final InterpolationHandler glide;

        PilotSnap(final PuddleJumperEntity jumper, final InterpolationHandler glide) {
            this.jumper = jumper;
            this.glide = glide;
        }

        @Override
        public InterpolationTracker interpolationTracker() {
            return this.glide.interpolationTracker();
        }

        @Override
        public @Nullable PositionAndRotation target() {
            return this.glide.target();
        }

        @Override
        public boolean interpolateTo(final @Nullable PositionPath position, final float yRot, final float xRot,
                final boolean hasRotation) {
            if (position == null || !this.jumper.isLocalInstanceAuthoritative()) {
                return this.glide.interpolateTo(position, yRot, xRot, hasRotation);
            }
            this.glide.cancel();
            this.jumper.snapTo(position.endPosition(), hasRotation ? yRot : this.jumper.getYRot(),
                    hasRotation ? xRot : this.jumper.getXRot());
            for (final Entity rider : this.jumper.getPassengers()) {
                this.jumper.positionRider(rider);
                rider.setOldPosAndRot();
            }
            return true;
        }

        @Override
        public void interpolate() {
            this.glide.interpolate();
        }

        @Override
        public void applyPredictedMovement(final Vec3 delta) {
            this.glide.applyPredictedMovement(delta);
        }

        @Override
        public boolean hasActiveInterpolation() {
            return this.glide.hasActiveInterpolation();
        }

        @Override
        public void cancel() {
            this.glide.cancel();
        }
    }

    /** @return the dialing computer aboard */
    public DialingTerminal dialer() {
        return this.dialer;
    }

    // Flying

    @Override
    public void tick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0) {
            this.setDamage(this.getDamage() - 1);
        }
        super.tick();
        this.podsO = this.pods;
        this.hatchO = this.hatch;
        this.pods = Mth.approach(this.pods, this.entityData.get(PuddleJumperEntity.PODS_OUT) ? 1 : 0,
                1 / PuddleJumperEntity.PODS_TICKS);
        this.hatch = Mth.approach(this.hatch, this.entityData.get(PuddleJumperEntity.HATCH_OPEN) ? 1 : 0,
                1 / PuddleJumperEntity.HATCH_TICKS);
        this.baysO = this.bays;
        this.bays = Mth.approach(this.bays, this.armed() ? 1 : 0, 1 / PuddleJumperEntity.BAYS_TICKS);
        this.cloakO = this.cloak;
        this.cloak = Mth.approach(this.cloak, this.cloaked() ? 1 : 0, 1 / PuddleJumperEntity.CLOAK_TICKS);
        if (this.isLocalInstanceAuthoritative()) {
            // The pilot's client flies it; the server moves it only while no one does, to drift it to a stop
            final LivingEntity pilot = this.getControllingPassenger();
            final JumperInput input = this.level().isClientSide() && pilot != null ? JumperHooks.input(this, pilot)
                    : JumperInput.NONE;
            // Its pilot's client holds it where it is, keeping its motion, until the world it is in and flying into
            // has reached it, as out of a gate; flown on into chunks the client hasn't got, it would meet nothing
            // there, and the server, which has them, would put it back
            this.awaitingTerrain = this.level().isClientSide() && !this.terrainArrived();
            if (!this.awaitingTerrain) {
                // It won't go with its cargo door down; with its drive off, it falls to the ground
                if (!this.powered()) {
                    this.fall();
                } else {
                    this.fly(this.hatchOpen() ? JumperInput.NONE : input);
                }
                this.move(MoverType.SELF, this.getDeltaMovement());
            }
        } else {
            // Its motion stays as last known, as the server measures it, so whoever moves it next, its pilot's client
            // or the server once no one flies it, carries on with it: out of a gate into another world too
            this.turnRate = 0;
        }
        // Once it has moved this tick, flown or gliding to where the server says, so its pilot sees its engines run
        // as everyone else does
        if (this.level().isClientSide()) {
            this.spoolEngines();
        }
        // Flown here, or coming in from the server between the last tick and this one
        this.turned = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        if (this.level().isClientSide()) {
            // It banks into its turns and tips its nose up as it climbs, easing into each; but only as far as it
            // goes along as well, so it rises and sinks level where it hovers
            this.bankO = this.bank;
            this.tiltO = this.tilt;
            final float climb = (float) (this.getY() - this.yo);
            final float along = (float) Math.min(1, Math.hypot(this.getX() - this.xo, this.getZ() - this.zo)
                    / PuddleJumperEntity.TILT_SPEED);
            this.bank += (Mth.clamp(this.turned * PuddleJumperEntity.BANK, -PuddleJumperEntity.MAX_BANK,
                    PuddleJumperEntity.MAX_BANK) - this.bank) * PuddleJumperEntity.EASE;
            this.tilt += (Mth.clamp(climb * PuddleJumperEntity.TILT, -PuddleJumperEntity.MAX_TILT,
                    PuddleJumperEntity.MAX_TILT) * along - this.tilt) * PuddleJumperEntity.EASE;
        }
        if (this.level() instanceof final ServerLevel level) {
            this.serverTick(level);
        }
    }

    private void serverTick(final ServerLevel level) {
        // Whether it stood still this tick, however it was moved: by its pilot's client, or drifting here
        final Vec3 now = this.position();
        final boolean still = this.lastPosition != null && now.distanceToSqr(this.lastPosition)
                < PuddleJumperEntity.STILL * PuddleJumperEntity.STILL
                && Math.abs(Mth.wrapDegrees(this.getYRot() - this.lastYaw)) < PuddleJumperEntity.STILL_TURN;
        // Flown by its pilot's client, its motion is however far that moved it since last tick; not a jump through a
        // gate, nor a tick no word came from the client, which keeps what it had, as out of a gate into another world
        if (!this.isLocalInstanceAuthoritative() && this.lastPosition != null && !still) {
            final Vec3 moved = now.subtract(this.lastPosition);
            if (moved.lengthSqr() < PuddleJumperEntity.MOST_MOVED * PuddleJumperEntity.MOST_MOVED) {
                this.setDeltaMovement(moved);
            }
        }
        if (this.lastPosition != null) {
            this.watchForCrash(level, now.subtract(this.lastPosition));
        }
        this.lastPosition = now;
        this.lastYaw = this.getYRot();
        this.stillTicks = still ? this.stillTicks + 1 : 0;
        if (!still) {
            this.seatStanders(level);
        }
        this.runGenerator(level);
        // No pilot, no pods; nor near an open gate. Otherwise as the pilot chose by their button, or, if they haven't,
        // out off the ground; and for the turbo whether or not
        if (this.tickCount % 5 == 0) {
            // Landing draws the drone bays in as it does the engine pods; the pilot may arm them again on the ground
            final boolean airborne = this.airborne();
            if (this.wasAirborne && !airborne && this.armed()) {
                this.entityData.set(PuddleJumperEntity.ARMED, false);
                this.entityData.set(PuddleJumperEntity.TARGET, -1);
            }
            this.wasAirborne = airborne;
            this.entityData.set(PuddleJumperEntity.PODS_OUT, this.getControllingPassenger() != null
                    && !this.hatchOpen() && !this.nearOpenGate(level)
                    && this.powered() && (this.podsWanted
                    || (this.podsChosen != null ? this.podsChosen : this.airborne())));
        }
        if (this.tickCount % 10 == 0 && this.getControllingPassenger() != null) {
            this.loadFarSide(level);
        }
        // Light blocks an earlier version's headlights set, still standing
        if (this.headlights.any()) {
            this.headlights.clear(level);
        }
        this.rebuildDrones();
        if (this.tickCount % PuddleJumperEntity.LINK_TICKS == 0) {
            this.dialer.gate();
            this.forgetLeftSeats(level);
        }
    }

    // Flown into a block, by its pilot's client or drifting here, it stops dead; the server sees it go from its speed to
    // nothing with a block the way it was going, and hurts it the harder the faster it met it, the shield taking the
    // blow if it is up. A stop must last two ticks, so a tick with no word from the pilot's client isn't taken for
    // one, and a jump through a gate is no crash at all
    private void watchForCrash(final ServerLevel level, final Vec3 moved) {
        final double speed = moved.length();
        if (speed > PuddleJumperEntity.MOST_MOVED) {
            this.crashPeak = 0;
            this.slowTicks = 0;
            return;
        }
        if (speed < this.crashPeak * PuddleJumperEntity.CRASH_STOP) {
            if (++this.slowTicks == 1) {
                this.impactSpeed = this.crashPeak;
            }
            if (this.slowTicks == 2) {
                final JumperSettings settings = JumperSettings.current();
                if (this.impactSpeed > settings.crashSpeed() && this.blockedToward(this.crashWay)) {
                    this.crash(level, settings, this.impactSpeed, this.crashWay);
                }
                this.crashPeak = 0;
            }
            return;
        }
        this.slowTicks = 0;
        if (speed >= this.crashPeak * PuddleJumperEntity.CRASH_FADE) {
            this.crashWay = moved.scale(1 / Math.max(speed, 1.0E-6));
        }
        this.crashPeak = Math.max(speed, this.crashPeak * PuddleJumperEntity.CRASH_FADE);
    }

    // Whether the jumper, nose to tail, would meet a block a little further the way it was going
    private boolean blockedToward(final Vec3 way) {
        final Vec3 step = way.scale(PuddleJumperEntity.CRASH_REACH);
        if (!this.level().noCollision(this, this.getBoundingBox().move(step))) {
            return true;
        }
        for (final AABB end : this.ends(this.getYRot(), this.position())) {
            if (!this.level().noCollision(this, end.move(step))) {
                return true;
            }
        }
        return false;
    }

    // Met a block this fast, in blocks a tick, going this way: the faster, the harder it is hurt
    private void crash(final ServerLevel level, final JumperSettings settings, final double speed, final Vec3 way) {
        final float damage = (float) ((speed - settings.crashSpeed()) * settings.crashDamage());
        if (damage <= 0) {
            return;
        }
        this.playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, (float) Math.min(1.5, 0.4 + speed), 0.7F);
        final Vec3 from = this.position().add(0, PuddleJumperEntity.SHIELD_MIDDLE, 0).add(way.scale(10));
        this.hurtFrom(level, level.damageSources().flyIntoWall(), damage, from);
    }

    // Short of drones, it builds them again, one at a time, slowly
    private void rebuildDrones() {
        final int ticks = JumperSettings.current().droneRefillTicks();
        if (ticks <= 0 || this.drones() >= PuddleJumperEntity.DRONES) {
            this.droneBuild = 0;
            return;
        }
        if (++this.droneBuild >= ticks) {
            this.droneBuild = 0;
            this.entityData.set(PuddleJumperEntity.DRONES_LEFT, this.drones() + 1);
        }
    }

    // Whether a gate with its wormhole open stands near
    private boolean nearOpenGate(final ServerLevel level) {
        return SGServer.current().map(server -> server.stargates().in(level.dimension()).stream()
                .anyMatch(gate -> gate.state() == GateState.CONNECTED && gate.position().pos()
                        .closerToCenterThan(this.position(), PuddleJumperEntity.GATE_NEAR))).orElse(false);
    }

    // Whether the chunks the jumper is in, and those just ahead of its nose the way it is going, are loaded here
    private boolean terrainArrived() {
        final Vec3 ahead = this.position().add(Vec3.directionFromRotation(0, this.getYRot())
                .scale(PuddleJumperEntity.LENGTH / 2 + 1)).add(this.getDeltaMovement().scale(5));
        for (final Vec3 at : new Vec3[] {this.position(), ahead}) {
            if (!this.level().hasChunk(SectionPos.blockToSectionCoord(at.x), SectionPos.blockToSectionCoord(at.z))) {
                return false;
            }
        }
        return true;
    }

    /** @return whether its pilot's client holds it still, waiting for the world round it to arrive; only on clients */
    public boolean awaitingTerrain() {
        return this.awaitingTerrain;
    }

    // Heading into an open gate, the world out of its far end is loaded ahead of it, so it is ready to send its riders
    // the moment the jumper comes out there, rather than loaded, or made, only then; and only as much as it will
    // reach before its riders' own view of the world there catches up, the more the faster it comes
    private void loadFarSide(final ServerLevel level) {
        final Vec3 motion = this.getDeltaMovement();
        SGServer.current().ifPresent(server -> server.stargates().in(level.dimension()).stream()
                .filter(gate -> gate.state() == GateState.CONNECTED && gate.position().pos().closerToCenterThan(
                        this.position(), PuddleJumperEntity.LOAD_NEAR))
                .forEach(gate -> {
                    final Vec3 toGate = Vec3.atCenterOf(gate.position().pos()).subtract(this.position());
                    final double distance = toGate.length();
                    final double closing = distance < 1.0E-3 ? 0 : Math.max(0, motion.dot(toGate) / distance);
                    if (distance > PuddleJumperEntity.LOAD_ANYWAY
                            && (closing <= 0 || distance / closing > PuddleJumperEntity.LOAD_LEAD_TICKS)) {
                        return;
                    }
                    gate.connection().flatMap(connection -> connection.other(gate)).ifPresent(other ->
                            PuddleJumperEntity.loadOutOf(level, other, closing));
                }));
    }

    // Loads the world out of a gate's front, for a jumper coming out of it at a speed, in blocks a tick: as far
    // ahead as it will fly in a few seconds, the ticket's middle set that far out so little behind the gate is loaded
    private static void loadOutOf(final ServerLevel level, final Stargate gate, final double speed) {
        final ServerLevel there = level.getServer().getLevel(gate.position().dimension());
        if (there == null) {
            return;
        }
        final int most = Math.max(PuddleJumperEntity.LOAD_LEAST, Math.min(PuddleJumperEntity.LOAD_RADIUS,
                level.getServer().getPlayerList().getViewDistance()));
        // The square reaches about twice its radius out ahead, its middle a radius out
        final int ahead = Mth.ceil(speed * PuddleJumperEntity.LOAD_AHEAD_TICKS / SectionPos.SECTION_SIZE);
        final int radius = Mth.clamp(ahead / 2 + 2, PuddleJumperEntity.LOAD_LEAST, most);
        final Direction front = gate.orientation().front();
        final BlockPos middle = gate.position().pos().offset(front.getStepX() * (radius - 1) * SectionPos.SECTION_SIZE,
                0, front.getStepZ() * (radius - 1) * SectionPos.SECTION_SIZE);
        there.getChunkSource().addTicketWithRadius(PuddleJumperEntity.FAR_SIDE, ChunkPos.containing(middle), radius);
    }

    // Anyone standing inside as it moves is sat down in a free seat, or put out of the hatch if there is none
    private void seatStanders(final ServerLevel level) {
        for (final Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox()
                .inflate(PuddleJumperEntity.END_OFFSET, 0, PuddleJumperEntity.END_OFFSET))) {
            // Only those truly inside, feet and eyes within its walls, floor and roof: never one on its roof as it
            // rises under them, nor one leaning on its side
            if (player.getVehicle() == this || player.isSpectator() || !this.interior(player.position().add(0, 0.3, 0))
                    || !this.interior(player.getEyePosition())) {
                continue;
            }
            if (!player.startRiding(this)) {
                final Vec3 out = this.outside(player);
                player.teleportTo(out.x, out.y, out.z);
            }
        }
    }

    /**
     * @return whether it stands still, so its riders may get up and its cargo door may lower: with no pilot, the
     *     server moves it, and knows how fast; flown, it must have stood still for a moment. Only on the server
     */
    public boolean still() {
        return this.getControllingPassenger() == null ? this.getDeltaMovement().lengthSqr() < 1.0E-6
                : this.stillTicks >= PuddleJumperEntity.STILL_TICKS;
    }

    /** The pilot wants the drive pods out for the turbo, or no longer; the turbo unfolds them. */
    public void wantPods(final boolean out) {
        this.podsWanted = out;
        if (out) {
            this.podsChosen = null;
        }
    }

    // Whether it flies clear of the ground, nothing under it within a block
    private boolean airborne() {
        return this.level().noCollision(this, this.getBoundingBox().move(0, -1, 0));
    }

    // On clients, the engines' thrust, which their glow shows: idling while the pods are out, climbing with speed and
    // flaring as it picks up speed, as the show's engines do as they spool up, then settling back as it eases off
    private void spoolEngines() {
        this.thrustO = this.thrust;
        final double speed = Math.sqrt(Mth.square(this.getX() - this.xo) + Mth.square(this.getY() - this.yo)
                + Mth.square(this.getZ() - this.zo));
        final double gain = Math.max(0, speed - this.lastSpeed);
        this.lastSpeed = speed;
        float target = 0;
        if (this.pods > 0) {
            target = (float) Math.min(1, PuddleJumperEntity.IDLE_THRUST + speed / PuddleJumperEntity.TURBO_SPEED
                    + gain * PuddleJumperEntity.FLARE);
        }
        this.thrust = target > this.thrust ? Math.min(target, this.thrust + PuddleJumperEntity.SPOOL_UP)
                : Math.max(target, this.thrust - PuddleJumperEntity.SPOOL_DOWN);
    }

    /** @return how hard the engines run, from 0, off, to 1, flat out, between the last tick and this one */
    public float thrust(final float partialTick) {
        return Mth.lerp(partialTick, this.thrustO, this.thrust);
    }

    /** @return whether the drive pods are going out, or are out, rather than in */
    public boolean podsOut() {
        return this.entityData.get(PuddleJumperEntity.PODS_OUT);
    }

    /** @return how far out the drive pods are, from 0 to 1, between the last tick and this one */
    public float pods(final float partialTick) {
        return Mth.lerp(partialTick, this.podsO, this.pods);
    }

    /** @return how far down the hatch is, from 0 shut to 1 lying on the ground, between the last tick and this one */
    public float hatch(final float partialTick) {
        return Mth.lerp(partialTick, this.hatchO, this.hatch);
    }

    /** @return whether the cargo door is down, or on its way down; the jumper won't go until it is shut again */
    public boolean hatchOpen() {
        return this.entityData.get(PuddleJumperEntity.HATCH_OPEN);
    }

    /**
     * Lowers the cargo door, if the jumper stands still, or raises it, telling the player who did if it won't lower.
     */
    public void toggleHatch(final ServerPlayer player) {
        final boolean open = !this.hatchOpen();
        // With no one aboard it lowers whatever the jumper seems to be doing, should it think it moves when it doesn't
        if (open && !this.still() && this.anyoneAboard()) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.puddle_jumper.door_moving",
                    "Stop the jumper to lower its cargo door").withStyle(ChatFormatting.RED));
            return;
        }
        this.entityData.set(PuddleJumperEntity.HATCH_OPEN, open);
        this.playSound(open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 1, 0.6F);
    }

    // Whether anyone rides in it, or stands inside its hull
    private boolean anyoneAboard() {
        return this.isVehicle() || !this.level().getEntitiesOfClass(Player.class, this.getBoundingBox()
                .inflate(PuddleJumperEntity.END_OFFSET, 0, PuddleJumperEntity.END_OFFSET),
                player -> !player.isSpectator() && this.inside(player.getEyePosition())).isEmpty();
    }

    /** @return whether the headlights are on */
    public boolean headlights() {
        return this.entityData.get(PuddleJumperEntity.HEADLIGHTS);
    }

    /** @return how many drones it carries */
    public int drones() {
        return this.entityData.get(PuddleJumperEntity.DRONES_LEFT);
    }

    /** @return whether its drones are armed, their bays out or on their way out, so it may launch them */
    public boolean armed() {
        return this.entityData.get(PuddleJumperEntity.ARMED);
    }

    /** @return whether its drones may be launched at creatures that aren't hostile, and their blasts reach them */
    public boolean friendlyFire() {
        return this.entityData.get(PuddleJumperEntity.FRIENDLY_FIRE);
    }

    /** @return the entity id of the target its weapons' operator has locked, which its riders see; -1 for none */
    public int target() {
        return this.entityData.get(PuddleJumperEntity.TARGET);
    }

    /** Its weapons' operator locked a target, or let it go, so the riders beside them see it too. */
    public void shareTarget(final ServerPlayer player, final int targetId) {
        if (this.weaponsOperator() == player && player.getVehicle() == this) {
            this.entityData.set(PuddleJumperEntity.TARGET, this.armed() ? targetId : -1);
        }
    }

    /** @return who works its drones: the dialer's operator, if a player sits there, or else the pilot */
    public @Nullable Player weaponsOperator() {
        return this.occupant(JumperSeat.DIALER) instanceof final Player operator ? operator
                : this.getControllingPassenger() instanceof final Player pilot ? pilot : null;
    }

    /** @return how far out its drone bays are, from 0 to 1, between the last tick and this one */
    public float bays(final float partialTick) {
        return Mth.lerp(partialTick, this.baysO, this.bays);
    }

    /**
     * Its weapons' operator launched a drone at a target, or straight ahead with none: from the next bay, if its bays
     * are out, it has a drone left, or the operator is in creative, and the target is a creature in range, hostile
     * unless friendly fire is on, and not aboard a jumper. A drone takes a moment to ready after another.
     *
     * @param targetId the target's entity id, or -1 to launch straight ahead
     */
    public void launchDrone(final ServerPlayer player, final int targetId) {
        if (this.weaponsOperator() != player || player.getVehicle() != this || !this.powered()
                || this.bays < 1 || !(this.level() instanceof final ServerLevel level)
                || level.getGameTime() < this.nextLaunch) {
            return;
        }
        final boolean endless = player.isCreative();
        if (!endless && this.drones() <= 0) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.puddle_jumper.no_drones",
                    "No drones left").withStyle(ChatFormatting.RED));
            return;
        }
        Entity target = targetId < 0 ? null : level.getEntity(targetId);
        if (target instanceof final EnderDragonPart part) {
            target = part.parentMob;
        }
        if (targetId >= 0 && (!(target instanceof final LivingEntity living) || !living.isAlive()
                || !(target instanceof Enemy) && !this.friendlyFire() || JumperShelter.sheltered(target)
                || living.distanceToSqr(this) > PuddleJumperEntity.DRONE_RANGE * PuddleJumperEntity.DRONE_RANGE)) {
            return;
        }
        final boolean left = this.launched++ % 2 == 0;
        final float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        final Vec3 bay = left ? PuddleJumperEntity.BAY : PuddleJumperEntity.BAY.multiply(-1, 1, 1);
        final Vec3 from = this.position().add(JumperHull.toWorld(bay, yaw));
        // Out of its bay to the side and up, before it turns after its target; with none, straight ahead
        final Vec3 out = target == null ? Vec3.directionFromRotation(0, this.getYRot()).scale(DroneEntity.SPEED)
                .add(this.getDeltaMovement())
                : JumperHull.toWorld(new Vec3(left ? 1 : -1, 1.5, 0.5), yaw).normalize()
                        .scale(PuddleJumperEntity.LAUNCH_SPEED).add(this.getDeltaMovement());
        final DroneEntity drone = SGEntityTypes.DRONE.create(level, EntitySpawnReason.TRIGGERED);
        if (drone == null) {
            return;
        }
        drone.launch(player, from, out, target, !this.friendlyFire());
        level.addFreshEntity(drone);
        // Launching opens the bays through the cloak, which drops
        this.setCloaked(false);
        if (!endless) {
            this.entityData.set(PuddleJumperEntity.DRONES_LEFT, this.drones() - 1);
        }
        this.nextLaunch = level.getGameTime() + PuddleJumperEntity.LAUNCH_TICKS;
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 0.6F, 1.7F);
    }

    /** @return whether its drive is on, without which it neither flies nor lights its headlights */
    public boolean powered() {
        return this.entityData.get(PuddleJumperEntity.POWERED);
    }

    /** Switches its drive on, and its headlights with it, or off, and everything with it. */
    public void setPowered(final boolean on) {
        if (on != this.powered()) {
            this.playSound(on ? SGSounds.JUMPER_POWER_ON : SGSounds.JUMPER_POWER_OFF, 0.4F, 1);
        }
        this.entityData.set(PuddleJumperEntity.POWERED, on);
        this.setHeadlights(on);
        if (!on) {
            this.podsWanted = false;
            this.setShield(false);
            this.setCloaked(false);
            this.entityData.set(PuddleJumperEntity.ARMED, false);
            this.entityData.set(PuddleJumperEntity.TARGET, -1);
            // The computer goes dark, closing its screen for whoever works it
            for (final Entity passenger : this.getPassengers()) {
                if (passenger instanceof final ServerPlayer player) {
                    SGNetwork.send(player, new ComputerPayloads.Screen(DialerRef.ABOARD, false));
                }
            }
        }
    }

    /** Switches the headlights on, or off; they come on only while its drive is on. */
    public void setHeadlights(final boolean on) {
        this.entityData.set(PuddleJumperEntity.HEADLIGHTS, on && this.powered());
    }

    // The cloak generator. As on the show, where the expedition retuned a jumper's cloak to act as a shield, the one
    // generator runs either the shield or the cloak, never both, from a charge of its own

    /** @return how full the cloak generator is, from 0 to 1 */
    public float generator() {
        return this.entityData.get(PuddleJumperEntity.GENERATOR);
    }

    /** @return whether the shield is up */
    public boolean shieldUp() {
        return this.entityData.get(PuddleJumperEntity.SHIELD);
    }

    /** @return whether the cloak is on, or coming on */
    public boolean cloaked() {
        return this.entityData.get(PuddleJumperEntity.CLOAKED);
    }

    /** @return how far the cloak has come on, from 0 seen to 1 hidden, between the last tick and this one */
    public float cloak(final float partialTick) {
        return Mth.lerp(partialTick, this.cloakO, this.cloak);
    }

    /** @return how many ticks are left before the shield may be raised again after it was knocked down; 0 if none */
    public long shieldDownTicks() {
        return Math.max(0, this.entityData.get(PuddleJumperEntity.SHIELD_DOWN_UNTIL) - this.level().getGameTime());
    }

    /** @return how much gate energy the cloak generator holds now; only on the server */
    public double generatorEnergy() {
        if (this.generator < 0) {
            this.generator = JumperSettings.current().generatorCapacity();
        }
        return this.generator;
    }

    /** Sets how much gate energy the cloak generator holds, up to its capacity; only on the server. */
    public void setGeneratorEnergy(final double energy) {
        final double capacity = JumperSettings.current().generatorCapacity();
        this.generator = Mth.clamp(energy, 0, capacity);
        final float share = (float) (this.generator / capacity);
        // Told in steps, but always told once it is empty or full
        final float told = share <= 0 || share >= 1 ? share
                : Math.round(share / PuddleJumperEntity.GENERATOR_STEP) * PuddleJumperEntity.GENERATOR_STEP;
        if (told != this.generator()) {
            this.entityData.set(PuddleJumperEntity.GENERATOR, told);
        }
    }

    // Each tick the shield or the cloak draws on the generator, and with neither running it refills while the drive is
    // on. Emptied, the cloak goes off, and the shield is knocked down
    private void runGenerator(final ServerLevel level) {
        final JumperSettings settings = JumperSettings.current();
        final double energy = this.generatorEnergy();
        if (this.shieldUp()) {
            this.setGeneratorEnergy(energy - settings.shieldDrain());
            if (this.generatorEnergy() <= 0) {
                this.knockDownShield(level);
            }
        } else if (this.cloaked()) {
            this.setGeneratorEnergy(energy - settings.cloakDrain());
            if (this.generatorEnergy() <= 0) {
                this.setCloaked(false);
            }
        } else if (this.powered()) {
            this.setGeneratorEnergy(energy + settings.rechargeRate());
        } else {
            // Keeps clients' share right after the capacity changes
            this.setGeneratorEnergy(energy);
        }
    }

    /**
     * Raises the shield, dropping the cloak, if the drive is on, the generator holds any energy and the shield isn't
     * knocked down; or lowers it.
     *
     * @return why the shield won't rise, or null if it did as asked
     */
    public @Nullable Component setShield(final boolean up) {
        if (up == this.shieldUp()) {
            return null;
        }
        if (up) {
            final Component refusal = this.generatorRefusal(true);
            if (refusal != null) {
                return refusal;
            }
            this.setCloaked(false);
        }
        this.entityData.set(PuddleJumperEntity.SHIELD, up);
        this.playSound(up ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 0.6F, 1.6F);
        return null;
    }

    /**
     * Puts the cloak on, lowering the shield, if the drive is on and the generator holds any energy; or takes it off.
     *
     * @return why the cloak won't come on, or null if it did as asked
     */
    public @Nullable Component setCloak(final boolean on) {
        if (on && !this.cloaked()) {
            final Component refusal = this.generatorRefusal(false);
            if (refusal != null) {
                return refusal;
            }
            this.setShield(false);
        }
        this.setCloaked(on);
        return null;
    }

    private void setCloaked(final boolean on) {
        if (on != this.cloaked()) {
            this.entityData.set(PuddleJumperEntity.CLOAKED, on);
            this.playSound(on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 0.5F, 0.7F);
        }
    }

    // Why the generator won't start the shield or the cloak, or null if it will
    private @Nullable Component generatorRefusal(final boolean shield) {
        if (!this.powered()) {
            return Component.translatableWithFallback("message.sgcraft.puddle_jumper.generator_off",
                    "The cloak generator needs the drive on").withStyle(ChatFormatting.RED);
        }
        if (shield && this.shieldDownTicks() > 0) {
            return Component.translatableWithFallback("message.sgcraft.puddle_jumper.shield_recovering",
                    "The shield recovers in %s seconds", (this.shieldDownTicks() + 19) / 20)
                    .withStyle(ChatFormatting.RED);
        }
        if (this.generatorEnergy() <= 0) {
            return Component.translatableWithFallback("message.sgcraft.puddle_jumper.generator_empty",
                    "The cloak generator is empty").withStyle(ChatFormatting.RED);
        }
        return null;
    }

    // The generator emptied with the shield up: it falls, and stays down a while
    private void knockDownShield(final ServerLevel level) {
        this.entityData.set(PuddleJumperEntity.SHIELD, false);
        this.entityData.set(PuddleJumperEntity.SHIELD_DOWN_UNTIL, level.getGameTime()
                + JumperSettings.current().knockdownTicks());
        this.playSound(SoundEvents.BEACON_DEACTIVATE, 1, 0.8F);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.4F, 1.8F);
    }

    /** Clears the shield's knockdown, so it may be raised again at once. */
    public void clearKnockdown() {
        this.entityData.set(PuddleJumperEntity.SHIELD_DOWN_UNTIL, 0L);
    }

    // The shield stops a blow, spending energy on each point of damage, and its clients are shown where it was struck;
    // what the generator can't pay for gets through to the hull. Returns the damage that gets through
    private float shieldHit(final ServerLevel level, final Vec3 from, final float damage) {
        final JumperSettings settings = JumperSettings.current();
        final double cost = damage * settings.hitCost();
        final double energy = this.generatorEnergy();
        final boolean collapsed = cost >= energy;
        this.setGeneratorEnergy(energy - cost);
        // Where it was struck, from the shield's middle, in the jumper's own frame
        final Vec3 middle = this.position().add(0, PuddleJumperEntity.SHIELD_MIDDLE, 0);
        Vec3 way = JumperHull.toLocal(from.subtract(middle), this.getYRot());
        if (way.lengthSqr() < 1.0E-6) {
            way = new Vec3(0, -1, 0);
        }
        final float strength = (float) Mth.clamp(cost / Math.max(1, settings.generatorCapacity()) * 8, 0.25, 1);
        SGNetwork.sendNear(this, new JumperPayloads.ShieldHit(this.getId(), way.normalize().toVector3f(), strength,
                collapsed));
        this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1, 1.4F + this.random.nextFloat() * 0.3F);
        if (!collapsed) {
            return 0;
        }
        this.knockDownShield(level);
        return settings.hitCost() <= 0 ? 0 : (float) ((cost - energy) / settings.hitCost());
    }

    @Override
    public void onRemoval(final RemovalReason reason) {
        super.onRemoval(reason);
        if (this.level() instanceof final ServerLevel level) {
            this.headlights.clear(level);
        }
    }

    /** @return whether a point in the world is inside the hull's cabin, between its walls, floor and roof */
    public boolean interior(final Vec3 point) {
        return JumperHull.inside(JumperHull.toLocal(point.subtract(this.position()), this.getYRot()));
    }

    /** @return whether a point in the world is within the hull, walls and all */
    public boolean inside(final Vec3 point) {
        return JumperHull.within(JumperHull.toLocal(point.subtract(this.position()), this.getYRot()));
    }

    /**
     * @return the hull's solid parts where the jumper stands now, which others collide with in place of its hitbox;
     *     the hatch is solid until it is nearly all the way down
     */
    public List<VoxelShape> hull() {
        final boolean shut = this.hatch < 0.9F;
        if (!this.position().equals(this.hullAt) || this.getYRot() != this.hullYaw || shut != this.hullShut) {
            final Vec3 at = this.position();
            this.hull = JumperHull.shapes(this.getYRot(), shut).stream().map(shape -> shape.move(at.x, at.y, at.z))
                    .toList();
            this.hullAt = at;
            this.hullYaw = this.getYRot();
            this.hullShut = shut;
        }
        return this.hull;
    }

    // Speed along the way it faces picks up, eases off and brakes at their own rates towards what the pilot's keys ask
    // for; any sideways drift from turning dies away, and climbing and descending ease in and out as well
    private void fly(final JumperInput input) {
        final float turn = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        // Turning left, from above, is turning anticlockwise, which takes from Minecraft's yaw
        this.turnRate = this.turnRate * PuddleJumperEntity.TURN_DRAG - turn * PuddleJumperEntity.TURN;
        if (Math.abs(this.turnRate) < 0.01F) {
            this.turnRate = 0;
        }
        if (this.turnRate != 0) {
            final float yaw = this.getYRot() + this.turnRate;
            if (this.fits(yaw)) {
                this.setYRot(yaw);
            } else {
                this.turnRate = 0;
            }
        }
        final Vec3 forward = Vec3.directionFromRotation(0, this.getYRot());
        final Vec3 motion = this.getDeltaMovement();
        double speed = motion.x * forward.x + motion.z * forward.z;
        final Vec3 drift = new Vec3(motion.x - forward.x * speed, 0, motion.z - forward.z * speed)
                .scale(PuddleJumperEntity.DRIFT);
        // Turbo needs the pods all the way out
        final boolean turbo = input.turbo() && this.pods(1) >= 1;
        final double wanted = input.forward() == input.backward() ? 0 : input.forward()
                ? turbo ? PuddleJumperEntity.TURBO_SPEED : PuddleJumperEntity.TOP_SPEED
                : -PuddleJumperEntity.REVERSE_SPEED;
        final double rate;
        if (wanted == 0 || Math.abs(wanted) < Math.abs(speed) && Math.signum(wanted) == Math.signum(speed)) {
            rate = PuddleJumperEntity.COAST;
        } else if (speed != 0 && Math.signum(wanted) != Math.signum(speed)) {
            rate = PuddleJumperEntity.BRAKE;
        } else {
            rate = turbo ? PuddleJumperEntity.TURBO_ACCELERATION : PuddleJumperEntity.ACCELERATION;
        }
        speed = PuddleJumperEntity.approach(speed, wanted, rate);
        final double climb = input.up() == input.down() ? 0 : input.up() ? PuddleJumperEntity.CLIMB_SPEED
                : -PuddleJumperEntity.CLIMB_SPEED;
        final double vertical = PuddleJumperEntity.approach(motion.y, climb, climb == 0
                ? PuddleJumperEntity.CLIMB_COAST : PuddleJumperEntity.CLIMB_ACCELERATION);
        this.setDeltaMovement(forward.x * speed + drift.x, vertical, forward.z * speed + drift.z);
        if (this.getDeltaMovement().lengthSqr() < 1.0E-7) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    // With its drive off it coasts to a stop along, and falls, faster and faster, till it meets the ground
    private void fall() {
        final double was = this.getDeltaMovement().y;
        this.fly(JumperInput.NONE);
        final Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, this.airborne() ? Math.max(was - PuddleJumperEntity.FALL_GRAVITY,
                -PuddleJumperEntity.FALL_SPEED) : 0, motion.z);
    }

    // A speed moved towards another by at most a step
    private static double approach(final double from, final double to, final double step) {
        return from < to ? Math.min(from + step, to) : Math.max(from - step, to);
    }

    /** @return how far the jumper turned this tick, in degrees, positive clockwise from above */
    public float turned() {
        return this.turned;
    }

    /** @return how far it banks into a turn, in degrees, between the last tick and this one; only on clients */
    public float bank(final float partialTick) {
        return Mth.lerp(partialTick, this.bankO, this.bank);
    }

    /** @return how far it tips its nose up as it climbs, in degrees, between the last tick and this one */
    public float tilt(final float partialTick) {
        return Mth.lerp(partialTick, this.tiltO, this.tilt);
    }

    // The nose and tail stop against blocks as the hitbox does, before the hitbox moves
    @Override
    public void move(final MoverType type, final Vec3 delta) {
        Vec3 movement = delta;
        if (!this.noPhysics && movement.lengthSqr() > 0) {
            for (final AABB end : this.ends(this.getYRot(), this.position())) {
                movement = Entity.collideBoundingBox(this, movement, end, this.level(), List.of());
            }
        }
        super.move(type, movement);
        // Flown into a block, nose, tail or side, it stops dead along the ground, rather than pressing on into it at
        // the speed it had until that slowly eases off
        final boolean blocked = Math.abs(movement.x - delta.x) > 1.0E-7 || Math.abs(movement.z - delta.z) > 1.0E-7
                || this.horizontalCollision;
        if (type == MoverType.SELF && blocked) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }
    }

    /**
     * @param yaw which way the jumper faces
     * @param at where its middle underside is
     * @return the spaces its nose and tail take up, beyond the hitbox
     */
    public List<AABB> ends(final float yaw, final Vec3 at) {
        final Vec3 ahead = Vec3.directionFromRotation(0, yaw);
        final double nose = PuddleJumperEntity.NOSE_WIDTH / 2;
        final double tail = PuddleJumperEntity.WIDTH / 2.0;
        // Each reaches out to the end of the hull
        final Vec3 front = at.add(ahead.scale(PuddleJumperEntity.LENGTH / 2 - nose));
        final Vec3 back = at.subtract(ahead.scale(PuddleJumperEntity.END_OFFSET));
        return List.of(new AABB(front.x - nose, at.y, front.z - nose, front.x + nose, at.y
                        + PuddleJumperEntity.NOSE_HEIGHT, front.z + nose),
                new AABB(back.x - tail, at.y, back.z - tail, back.x + tail, at.y + PuddleJumperEntity.HEIGHT,
                        back.z + tail));
    }

    /** @return whether the whole jumper, nose to tail, has room facing a way where it is */
    public boolean fits(final float yaw) {
        if (!this.level().noCollision(this, this.getBoundingBox().deflate(1.0E-7))) {
            return false;
        }
        for (final AABB end : this.ends(yaw, this.position())) {
            if (!this.level().noCollision(this, end.deflate(1.0E-7))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    @Override
    public float maxUpStep() {
        return 0;
    }

    // It flies, so it never falls, and nor do its riders
    @Override
    protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState,
            final BlockPos pos) {
        this.resetFallDistance();
    }

    // Others collide with its hull's solid parts rather than its hitbox, so they may walk about inside; see
    // EntityMixin
    @Override
    public boolean canBeCollidedWith(final @Nullable Entity other) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(final Entity entity) {
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    // Its riders' eyes are inside its hitbox; whatever they use, they use the jumper
    @Override
    public boolean canBePickedFromInside() {
        return true;
    }

    // Those inside, even in the cabin's back, beyond the hitbox, reach the whole of the hull, as their client sees it
    @Override
    public float getPickRadius() {
        return this.level().isClientSide() && JumperHooks.viewerInside(this) ? (float) PuddleJumperEntity.END_OFFSET
                : 0;
    }

    // Seats

    @Override
    protected boolean canAddPassenger(final Entity passenger) {
        return passenger instanceof Player && this.getPassengers().size() < JumperSeat.values().length;
    }

    @Override
    protected void addPassenger(final Entity passenger) {
        super.addPassenger(passenger);
        if (this.level().isClientSide()) {
            return;
        }
        // A rider coming back, such as out of a gate into another world, sits where they sat
        if (!this.seats.containsKey(passenger.getUUID())) {
            final JumperSeat free = this.freeSeat();
            if (free != null) {
                this.sit(passenger, free);
            }
        }
    }

    // A rider getting out, on their own client, starts afresh the next time they sit
    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (passenger.isLocalInstanceAuthoritative()) {
            this.riderLooking = false;
        }
    }

    // A player who got out keeps their seat until someone else takes it, or they are no longer anywhere near
    private void forgetLeftSeats(final ServerLevel level) {
        final boolean forgot = this.seats.keySet().removeIf(id -> {
            final Entity rider = level.getEntity(id);
            return rider == null || rider.getVehicle() != this && rider.distanceToSqr(this) > 64;
        });
        if (forgot) {
            this.syncSeats();
        }
    }

    // Seats a rider somewhere, letting go of anyone who got out and was kept there
    private void sit(final Entity passenger, final JumperSeat seat) {
        this.seats.values().removeIf(seat::equals);
        this.seats.put(passenger.getUUID(), seat);
        this.syncSeats();
        // Someone taking the dialer's seat takes the computer from the pilot
        if (seat == JumperSeat.DIALER && this.occupant(JumperSeat.HELM) instanceof final ServerPlayer pilot
                && pilot != passenger) {
            SGNetwork.send(pilot, new ComputerPayloads.Screen(DialerRef.ABOARD, false));
        }
    }

    private void syncSeats() {
        this.entityData.set(PuddleJumperEntity.SEATS, PuddleJumperEntity.encodeSeats(this.seats));
    }

    private static String encodeSeats(final Map<UUID, JumperSeat> seats) {
        final StringBuilder text = new StringBuilder();
        seats.forEach((id, seat) -> text.append(id).append('=').append(seat.getSerializedName()).append(';'));
        return text.toString();
    }

    private static Map<UUID, JumperSeat> decodeSeats(final String text) {
        final Map<UUID, JumperSeat> seats = new HashMap<>();
        for (final String entry : text.split(";")) {
            final int equals = entry.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            final JumperSeat seat = JumperSeat.byName(entry.substring(equals + 1));
            try {
                if (seat != null) {
                    seats.put(UUID.fromString(entry.substring(0, equals)), seat);
                }
            } catch (final IllegalArgumentException ignored) {
                // Not a UUID; the server never sends one
            }
        }
        return seats;
    }

    /** @return the rider in a seat, or null if no one sits there */
    public @Nullable Entity occupant(final JumperSeat seat) {
        for (final Entity passenger : this.getPassengers()) {
            if (this.seats.get(passenger.getUUID()) == seat) {
                return passenger;
            }
        }
        return null;
    }

    /**
     * @return the seat a rider sits in: the one kept for them, or, before the server has said, the seat as far down
     *     the list as they are down the jumper's riders
     */
    public JumperSeat seatOf(final Entity passenger) {
        final JumperSeat seat = this.seats.get(passenger.getUUID());
        if (seat != null) {
            return seat;
        }
        final int index = Math.max(0, this.getPassengers().indexOf(passenger));
        return JumperSeat.values()[Math.min(index, JumperSeat.values().length - 1)];
    }

    // The first seat, helm first, that no one sits in, nor is kept for someone who got out; or, if every free seat is
    // kept, the first of those
    private @Nullable JumperSeat freeSeat() {
        JumperSeat kept = null;
        for (final JumperSeat seat : JumperSeat.values()) {
            if (this.occupant(seat) != null) {
                continue;
            }
            if (!this.seats.containsValue(seat)) {
                return seat;
            }
            if (kept == null) {
                kept = seat;
            }
        }
        return kept;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.occupant(JumperSeat.HELM) instanceof final Player pilot ? pilot : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(final Entity passenger, final EntityDimensions dimensions,
            final float scale) {
        return this.seatOf(passenger).offset().yRot(-this.getYRot() * Mth.DEG_TO_RAD);
    }

    // Riders face the way their seat does, ahead or across the cabin, and turn with the jumper; each may look about,
    // but not behind them
    @Override
    protected void positionRider(final Entity passenger, final Entity.MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (passenger.isLocalInstanceAuthoritative()) {
            final float turn = this.riderLooking ? Mth.wrapDegrees(this.getYRot() - this.riderYaw) : 0;
            if (this.riderLooking && Math.abs(turn) > PuddleJumperEntity.JUMP_TURN) {
                // Turned all at once, as through a gate: the rider looks the same way out of it as before, whatever
                // the server said of how they look
                passenger.setYRot(this.getYRot() + this.riderLook);
                passenger.setYHeadRot(this.getYRot() + this.riderLook);
            } else {
                passenger.setYRot(passenger.getYRot() + turn);
                passenger.setYHeadRot(passenger.getYHeadRot() + turn);
            }
        }
        this.clampRotation(passenger);
        if (passenger.isLocalInstanceAuthoritative()) {
            this.riderYaw = this.getYRot();
            this.riderLook = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
            this.riderLooking = true;
        }
    }

    @Override
    public void onPassengerTurned(final Entity passenger) {
        passenger.yRotO += this.clampRotation(passenger);
    }

    private float clampRotation(final Entity passenger) {
        final float body = this.getYRot() + this.seatOf(passenger).facing();
        passenger.setYBodyRot(body);
        final float delta = Mth.wrapDegrees(passenger.getYRot() - body);
        final float clamped = Mth.clamp(delta, -105.0F, 105.0F);
        passenger.setYRot(passenger.getYRot() + clamped - delta);
        return clamped - delta;
    }

    // A rider getting up stands beside their seat, in the cockpit or the cabin's aisle; riders may only get up while
    // it stands still, as PlayerMixin sees to
    @Override
    public Vec3 getDismountLocationForPassenger(final LivingEntity passenger) {
        final JumperSeat seat = this.seatOf(passenger);
        final Vec3 aisle = seat.atComputer() ? new Vec3(0, JumperSeat.FLOOR, 0.55)
                : new Vec3(seat.offset().x * 0.35, JumperSeat.FLOOR, seat.offset().z);
        return this.position().add(JumperHull.toWorld(aisle, this.getYRot() * Mth.DEG_TO_RAD));
    }

    // Out of the hatch in the back, or beside the jumper, or on its roof
    private Vec3 outside(final Entity passenger) {
        final Vec3 forward = Vec3.directionFromRotation(0, this.getYRot());
        final Vec3 left = forward.yRot(Mth.HALF_PI);
        final double out = PuddleJumperEntity.LENGTH / 2 + 0.6;
        final double side = PuddleJumperEntity.WIDTH / 2 + 0.6;
        for (final Vec3 place : new Vec3[] {forward.scale(-out), left.scale(side), left.scale(-side)}) {
            final Vec3 spot = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(),
                    BlockPos.containing(this.position().add(place).add(0, 0.5, 0)), true);
            if (spot != null) {
                return spot;
            }
        }
        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    // Using it

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player.getVehicle() == this) {
            if (player instanceof final ServerPlayer serverPlayer) {
                this.useAboard(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        final ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) && held.has(DataComponents.CUSTOM_NAME)) {
            if (!this.level().isClientSide()) {
                this.setCustomName(held.get(DataComponents.CUSTOM_NAME));
                held.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        final boolean aboard = this.inside(player.getEyePosition());
        if (player.isSecondaryUseActive() && !aboard) {
            if (!held.isEmpty() || this.isVehicle()) {
                return InteractionResult.PASS;
            }
            if (player instanceof final ServerPlayer serverPlayer) {
                this.pickUp(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof final ServerPlayer serverPlayer) {
            if (aboard) {
                this.useStanding(serverPlayer);
            } else {
                this.toggleHatch(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // A player standing inside used the jumper: sits in the free seat they look at, or works the cargo door if they
    // look back at it
    private void useStanding(final ServerPlayer player) {
        final JumperSeat aimed = this.seatLookedAt(player, null);
        if (aimed != null) {
            // Only from within the hull, not through its windows or walls: anywhere in the cabin, though the
            // bulkhead's doorway frame stands across the way to a seat ahead, or else with nothing of the hull between
            if (!this.interior(player.getEyePosition()) && !this.seatInSight(player, aimed)) {
                return;
            }
            if (this.occupant(aimed) != null) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        "message.sgcraft.puddle_jumper.seat_taken", "Someone sits there"));
            } else if (player.startRiding(this)) {
                this.sit(player, aimed);
            }
            return;
        }
        final Vec3 back = Vec3.directionFromRotation(0, this.getYRot() + 180);
        if (player.getViewVector(1).dot(back) > 0.5) {
            this.toggleHatch(player);
        }
    }

    /** A button on the dashboard, which the pilot presses by using it while looking at it, or by its key. */
    public enum Button {
        /** Switches its drive on or off. */
        POWER(new Vec3(1.05, 0.875, 2.95)),
        /** Switches the headlights. */
        HEADLIGHTS(new Vec3(0.85, 0.875, 2.95)),
        /** Puts the drive pods out, or folds them in, as for a gate. */
        PODS(new Vec3(0.6, 0.875, 2.95)),
        /** Raises or lowers the cargo door. */
        DOOR(new Vec3(0.35, 0.875, 2.95)),
        /** Arms its drones, sliding their bays out, or stows them again. */
        WEAPONS(new Vec3(0.1, 0.875, 2.95)),
        /** Lets its drones be launched at, and their blasts reach, creatures that aren't hostile, or no longer. */
        FRIENDLY_FIRE(new Vec3(-0.15, 0.875, 2.95)),
        /** Raises or lowers the shield, which drops the cloak. */
        SHIELD(new Vec3(-0.4, 0.875, 2.95)),
        /** Puts the cloak on or takes it off, which lowers the shield. */
        CLOAK(new Vec3(-0.65, 0.875, 2.95));

        /** @return whether the button works its weapons, so is its weapons' operator's to press */
        public boolean weapons() {
            return this == Button.WEAPONS || this == Button.FRIENDLY_FIRE;
        }

        private final Vec3 at;

        Button(final Vec3 at) {
            this.at = at;
        }

        /** @return where the button is, in the jumper's frame from its middle underside, in blocks */
        public Vec3 at() {
            return this.at;
        }
    }

    /** The pilot pressed a button on the dashboard. */
    public void press(final ServerPlayer player, final Button button) {
        if (player.getVehicle() != this || (button.weapons() ? this.weaponsOperator() != player
                : this.seatOf(player) != JumperSeat.HELM)) {
            return;
        }
        switch (button) {
            case POWER -> this.setPowered(!this.powered());
            case HEADLIGHTS -> this.setHeadlights(!this.headlights());
            // A toggle: out if they are in, standing or flying, and in if they are out
            case PODS -> this.podsChosen = !this.podsOut();
            case DOOR -> this.toggleHatch(player);
            case WEAPONS -> {
                this.entityData.set(PuddleJumperEntity.ARMED, !this.armed() && this.powered());
                this.entityData.set(PuddleJumperEntity.TARGET, -1);
            }
            case FRIENDLY_FIRE -> this.entityData.set(PuddleJumperEntity.FRIENDLY_FIRE, !this.friendlyFire());
            case SHIELD -> this.refuse(player, this.setShield(!this.shieldUp()));
            case CLOAK -> this.refuse(player, this.setCloak(!this.cloaked()));
        }
        this.playSound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6F, 1.4F);
    }

    // Tells a player why what they asked for wasn't done, if it wasn't
    private void refuse(final ServerPlayer player, final @Nullable Component refusal) {
        if (refusal != null) {
            player.sendOverlayMessage(refusal);
        }
    }

    // The button the pilot looks at, if they look near enough at one
    private @Nullable Button buttonLookedAt(final Player player) {
        final Vec3 eye = player.getEyePosition();
        final Vec3 look = player.getViewVector(1);
        Button best = null;
        double nearest = Math.cos(PuddleJumperEntity.BUTTON_AIM * Mth.DEG_TO_RAD);
        for (final Button button : Button.values()) {
            final Vec3 at = this.position().add(JumperHull.toWorld(button.at(), this.getYRot() * Mth.DEG_TO_RAD));
            final double facing = at.subtract(eye).normalize().dot(look);
            if (facing > nearest) {
                nearest = facing;
                best = button;
            }
        }
        return best;
    }

    // A rider used the jumper: the pilot presses the dashboard button they look at; anyone moves to the free seat they
    // look at. The computer is worked by its key; see openComputer
    private void useAboard(final ServerPlayer player) {
        final JumperSeat own = this.seatOf(player);
        if (own == JumperSeat.HELM) {
            final Button button = this.buttonLookedAt(player);
            if (button != null) {
                this.press(player, button);
                return;
            }
        }
        final JumperSeat aimed = this.seatLookedAt(player, own);
        if (aimed != null) {
            if (this.occupant(aimed) != null) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        "message.sgcraft.puddle_jumper.seat_taken", "Someone sits there"));
            } else {
                this.sit(player, aimed);
            }
        }
    }

    /** A rider pressed the computer's key: it opens for the pilot or the dialer's operator, as their seat may. */
    public void openComputer(final ServerPlayer player) {
        if (player.getVehicle() != this) {
            return;
        }
        final JumperSeat own = this.seatOf(player);
        if (!this.powered()) {
            player.sendOverlayMessage(Component.translatableWithFallback("message.sgcraft.puddle_jumper.computer_off",
                    "The computer is dark with the drive off").withStyle(ChatFormatting.RED));
            return;
        }
        if (!this.mayUseComputer(player)) {
            player.sendOverlayMessage(own == JumperSeat.HELM
                    ? Component.translatableWithFallback("message.sgcraft.puddle_jumper.dialer_taken",
                            "The computer is worked from the seat beside you")
                    : Component.translatableWithFallback("message.sgcraft.puddle_jumper.no_computer",
                            "The computer is worked from the front seats"));
            return;
        }
        this.dialer.gate();
        SGNetwork.send(player, new ComputerPayloads.Screen(DialerRef.ABOARD, true));
    }

    // The seat, other than their own, nearest where a rider looks, if they look near enough at it
    private @Nullable JumperSeat seatLookedAt(final Player player, final @Nullable JumperSeat own) {
        final Vec3 eye = player.getEyePosition();
        final Vec3 look = player.getViewVector(1);
        JumperSeat best = null;
        double nearest = Math.cos(PuddleJumperEntity.SEAT_AIM * Mth.DEG_TO_RAD);
        for (final JumperSeat seat : JumperSeat.values()) {
            if (seat == own) {
                continue;
            }
            // A sitter's middle, half a block over the cushion
            final Vec3 at = this.position().add(seat.offset().add(0, 0.5, 0).yRot(-this.getYRot() * Mth.DEG_TO_RAD));
            final double facing = at.subtract(eye).normalize().dot(look);
            if (facing > nearest) {
                nearest = facing;
                best = seat;
            }
        }
        return best;
    }

    // Whether nothing of the hull stands between a player's eyes and a seat
    private boolean seatInSight(final Player player, final JumperSeat seat) {
        final Vec3 eye = player.getEyePosition();
        final Vec3 at = this.position().add(seat.offset().add(0, 0.5, 0).yRot(-this.getYRot() * Mth.DEG_TO_RAD));
        for (final VoxelShape shape : this.hull()) {
            if (shape.clip(eye, at, BlockPos.ZERO) != null) {
                return false;
            }
        }
        return true;
    }

    // The dialer's operator works the computer; the pilot does while no one else sits beside them
    private boolean mayUseComputer(final Player player) {
        if (player.getVehicle() != this) {
            return false;
        }
        final JumperSeat seat = this.seatOf(player);
        return seat == JumperSeat.DIALER || seat == JumperSeat.HELM && !(this.occupant(JumperSeat.DIALER)
                instanceof Player);
    }

    /** A button pressed on the screen of the computer aboard, by one of the jumper's riders. */
    public void dialerAction(final ServerPlayer player, final ComputerPayloads.Action action) {
        if (action.kind() == ComputerPayloads.Action.Kind.CLOSE || !this.powered() || !this.mayUseComputer(player)) {
            return;
        }
        DialingComputers.perform(player, DialerRef.ABOARD, this.dialer, this, action);
    }

    // Packed up as an item, which keeps its name
    private void pickUp(final ServerPlayer player) {
        final ItemStack item = new ItemStack(SGItems.PUDDLE_JUMPER);
        item.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        if (!player.getInventory().add(item)) {
            this.spawnAtLocation(player.level(), item);
        }
        this.discard();
    }

    // Those aboard, seated or standing, can't hurt it, and others hurt it less than they would a boat. With its shield
    // up the shield takes the blow, and only what it can't stop reaches the hull; a player in creative, breaking it as
    // they would a boat, and whatever kills outright, go through
    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        return this.hurtFrom(level, source, damage, null);
    }

    /**
     * Hurts the jumper as {@link #hurtServer} does, its shield taking the blow if it is up, struck from a point.
     *
     * @param from where the blow came from, which the shield shows it struck nearest; null for wherever the damage
     *     says, its source's position or its attacker's eyes, or else from below
     * @return whether the hull was hurt
     */
    public boolean hurtFrom(final ServerLevel level, final DamageSource source, final float damage,
            final @Nullable Vec3 from) {
        final Entity attacker = source.getEntity();
        if (attacker != null && (attacker.getVehicle() == this || this.inside(attacker.getEyePosition()))) {
            return false;
        }
        float through = damage;
        if (this.shieldUp() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !(attacker instanceof final Player player && player.getAbilities().instabuild)) {
            final Vec3 struck = from != null ? from : source.getSourcePosition() != null ? source.getSourcePosition()
                    : attacker != null ? attacker.getEyePosition() : this.position().add(0, -1, 0);
            through = this.shieldHit(level, struck, damage);
            // Stopped, as an arrow glances off it
            if (through <= 0) {
                return false;
            }
        }
        return super.hurtServer(level, source, through / PuddleJumperEntity.TOUGHNESS);
    }

    @Override
    protected Item getDropItem() {
        return SGItems.PUDDLE_JUMPER;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(SGItems.PUDDLE_JUMPER);
    }

    // Saving

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        this.seats.clear();
        this.seats.putAll(input.read("seats", PuddleJumperEntity.SEATS_CODEC).orElse(Map.of()));
        this.syncSeats();
        this.entityData.set(PuddleJumperEntity.DIALER_LINKED, input.read("dialer_linked", BlockPos.CODEC));
        this.entityData.set(PuddleJumperEntity.DIALER_MODE, input.read("dialer_mode", ComputerDialMode.CODEC)
                .orElse(ComputerDialMode.GATE).getSerializedName());
        this.entityData.set(PuddleJumperEntity.DIALER_ALARM_OFF, input.getBooleanOr("dialer_alarm_off", false));
        this.dialer.setHistory(input.read("dialer_history", DialingComputerBlockEntity.Dialed.CODEC.listOf())
                .orElse(List.of()));
        this.entityData.set(PuddleJumperEntity.POWERED, input.getBooleanOr("powered", false));
        this.entityData.set(PuddleJumperEntity.DRONES_LEFT, input.getIntOr("drones", PuddleJumperEntity.DRONES));
        this.entityData.set(PuddleJumperEntity.ARMED, input.getBooleanOr("armed", false) && this.powered());
        this.entityData.set(PuddleJumperEntity.FRIENDLY_FIRE, input.getBooleanOr("friendly_fire", false));
        this.entityData.set(PuddleJumperEntity.HEADLIGHTS, input.getBooleanOr("headlights", false));
        this.setGeneratorEnergy(input.getDoubleOr("generator", JumperSettings.current().generatorCapacity()));
        this.entityData.set(PuddleJumperEntity.SHIELD, input.getBooleanOr("shield", false) && this.powered());
        this.entityData.set(PuddleJumperEntity.CLOAKED, input.getBooleanOr("cloaked", false) && this.powered()
                && !this.shieldUp());
        this.cloak = this.cloaked() ? 1 : 0;
        this.cloakO = this.cloak;
        // Lights left standing when last saved, which its next move takes away
        this.headlights.remember(input.read("lights", BlockPos.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        output.store("seats", PuddleJumperEntity.SEATS_CODEC, Map.copyOf(this.seats));
        this.dialer.linked().ifPresent(linked -> output.store("dialer_linked", BlockPos.CODEC, linked));
        output.store("dialer_mode", ComputerDialMode.CODEC, this.dialer.mode());
        output.putBoolean("dialer_alarm_off", this.dialer.alarmOff());
        output.store("dialer_history", DialingComputerBlockEntity.Dialed.CODEC.listOf(), this.dialer.history());
        output.putBoolean("powered", this.powered());
        output.putInt("drones", this.drones());
        output.putBoolean("armed", this.armed());
        output.putBoolean("friendly_fire", this.friendlyFire());
        output.putBoolean("headlights", this.headlights());
        output.putDouble("generator", this.generatorEnergy());
        output.putBoolean("shield", this.shieldUp());
        output.putBoolean("cloaked", this.cloaked());
        output.store("lights", BlockPos.CODEC.listOf(), this.headlights.placed());
    }

    /**
     * The dialing computer aboard: a dialing computer as one on a desk is, linked to the nearest gate in reach of the
     * jumper as it flies, and needing no power. What it keeps is the jumper's, and its clients are told it with the
     * jumper.
     */
    private final class Dialer implements DialingTerminal {

        // The history as clients were last told it, read again only when that changes
        private String historyText = "";
        private List<DialingComputerBlockEntity.Dialed> history = List.of();

        @Override
        public Optional<BlockPos> linked() {
            return PuddleJumperEntity.this.entityData.get(PuddleJumperEntity.DIALER_LINKED);
        }

        @Override
        public ComputerDialMode mode() {
            final String name = PuddleJumperEntity.this.entityData.get(PuddleJumperEntity.DIALER_MODE);
            for (final ComputerDialMode mode : ComputerDialMode.values()) {
                if (mode.getSerializedName().equals(name)) {
                    return mode;
                }
            }
            return ComputerDialMode.GATE;
        }

        @Override
        public boolean alarmOff() {
            return PuddleJumperEntity.this.entityData.get(PuddleJumperEntity.DIALER_ALARM_OFF);
        }

        @Override
        public List<DialingComputerBlockEntity.Dialed> history() {
            final String text = PuddleJumperEntity.this.entityData.get(PuddleJumperEntity.DIALER_HISTORY);
            if (!text.equals(this.historyText)) {
                final List<DialingComputerBlockEntity.Dialed> history = new ArrayList<>();
                for (final String line : text.split("\n")) {
                    if (line.length() > 1) {
                        history.add(new DialingComputerBlockEntity.Dialed(line.substring(1), line.charAt(0) == '1'));
                    }
                }
                this.history = List.copyOf(history);
                this.historyText = text;
            }
            return this.history;
        }

        void setHistory(final List<DialingComputerBlockEntity.Dialed> history) {
            final StringBuilder text = new StringBuilder();
            for (final DialingComputerBlockEntity.Dialed dialed : history) {
                text.append(dialed.connected() ? '1' : '0').append(dialed.address()).append('\n');
            }
            PuddleJumperEntity.this.entityData.set(PuddleJumperEntity.DIALER_HISTORY, text.toString());
        }

        @Override
        public String pictureName() {
            return "jumper_" + PuddleJumperEntity.this.getUUID();
        }

        @Override
        public boolean gone() {
            return PuddleJumperEntity.this.isRemoved();
        }

        @Override
        public Optional<Stargate> gate() {
            if (!(PuddleJumperEntity.this.level() instanceof final ServerLevel level)) {
                return Optional.empty();
            }
            // It keeps the gate it links to while that gate dials, opens or stands open; otherwise, as it flies, it
            // links to whichever gate is now nearest
            final BlockPos at = PuddleJumperEntity.this.blockPosition();
            final Optional<Stargate> linked = GateLinks.inReach(level, this.linked().orElse(null), at,
                    PuddleJumperEntity.GATE_REACH);
            if (linked.isPresent() && linked.get().state() != GateState.IDLE) {
                return linked;
            }
            final BlockPos nearest = GateLinks.nearest(level, at, PuddleJumperEntity.GATE_REACH);
            PuddleJumperEntity.this.entityData.set(PuddleJumperEntity.DIALER_LINKED, Optional.ofNullable(nearest));
            return GateLinks.inReach(level, nearest, at, PuddleJumperEntity.GATE_REACH);
        }

        @Override
        public void setMode(final ComputerDialMode mode) {
            PuddleJumperEntity.this.entityData.set(PuddleJumperEntity.DIALER_MODE, mode.getSerializedName());
        }

        @Override
        public void setAlarmOff(final boolean off) {
            PuddleJumperEntity.this.entityData.set(PuddleJumperEntity.DIALER_ALARM_OFF, off);
        }

        @Override
        public void remember(final String address, final boolean connected) {
            this.setHistory(DialingTerminal.remembering(this.history(), address, connected));
        }
    }
}
