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

import com.almuradev.sgcraft.mod.item.BatteryItem;
import com.almuradev.sgcraft.mod.item.MalpItem;
import com.almuradev.sgcraft.mod.item.MalpTabletItem;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.malp.MalpConfigurator;
import com.almuradev.sgcraft.mod.malp.MalpMenu;
import com.almuradev.sgcraft.mod.malp.MalpOptions;
import com.almuradev.sgcraft.mod.malp.MalpRegistry;
import com.almuradev.sgcraft.mod.malp.MalpSettings;
import com.almuradev.sgcraft.mod.view.RemoteViewSource;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * A MALP, the show's Mobile Analytic Laboratory Probe: a six-wheeled probe driven from a {@code MalpConsole} through
 * a stargate, to see what is on the other side. It has no mind of its own; it drives only while someone at its
 * console holds the controls, and only while its battery holds gate energy, unless its {@link MalpOptions} say it needs
 * no power. Its camera sits on a mast that pans and tilts apart from the body, and a lamp lights what the camera sees.
 *
 * <p>It belongs to the player who first put it down, if anyone did; its owner and operators change its settings
 * with a configurator, and pair it with MALP tablets by using a tablet on it while sneaking, besides its one console.
 * Only they pack it up, rename it or open its battery; anyone may handle a MALP no one owns.
 * It goes through gates as anything else does, keeping its battery, its console, its name, its
 * owner and its settings, and the server keeps where it was last seen in the {@link MalpRegistry}, so its console
 * finds it while its chunk is unloaded.</p>
 */
public final class MalpEntity extends VehicleEntity implements RemoteViewSource {

    /** The farthest the camera pans either way from straight ahead, in degrees. */
    public static final float MAX_PAN = 120;
    /** The farthest the camera tilts up or down, in degrees. */
    public static final float MAX_TILT = 45;
    // How fast it drives, in blocks a tick, and turns, in degrees a tick
    private static final double SPEED = 0.15;
    private static final float TURN_SPEED = 4;
    // How long a drive order lasts without another, in ticks, so it stops when the console stops sending
    private static final int DRIVE_TICKS = 4;
    // How often it tells the registry where it is, in ticks, besides each time it drives into another chunk
    private static final int RECORD_TICKS = 20;
    // Where the camera is, from the MALP's feet: how high, and how far ahead of the middle
    private static final double CAMERA_HEIGHT = 18.5 / 16;
    private static final double CAMERA_FORWARD = 2.5 / 16;

    private static final EntityDataAccessor<Float> MAST_YAW = SynchedEntityData.defineId(MalpEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAST_PITCH = SynchedEntityData.defineId(MalpEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAMP = SynchedEntityData.defineId(MalpEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> POWERED = SynchedEntityData.defineId(MalpEntity.class,
            EntityDataSerializers.BOOLEAN);

    private final SimpleContainer batterySlot = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return stack.is(SGItems.BATTERY);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private @Nullable GlobalPos console;
    private @Nullable UUID owner;
    /** The most characters a MALP's name may have, set from its configurator. */
    public static final int MAX_NAME = 32;
    private MalpOptions options = MalpOptions.DEFAULTS;
    private float driveForward;
    private float driveTurn;
    private long driveUntil = Long.MIN_VALUE;
    // The chunk it was in when it last told the registry where it is, or null if it hasn't yet
    private @Nullable ChunkPos recordedChunk;
    // How far each side's wheels have turned, for the client to draw, in radians, now and a tick ago
    private float leftWheels;
    private float rightWheels;
    private float leftWheelsO;
    private float rightWheelsO;
    private float mastYawO;
    private float mastPitchO;

    public MalpEntity(final EntityType<? extends MalpEntity> type, final Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MalpEntity.MAST_YAW, 0F);
        builder.define(MalpEntity.MAST_PITCH, 0F);
        builder.define(MalpEntity.LAMP, false);
        builder.define(MalpEntity.POWERED, false);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
    }

    /** @return the battery in the MALP, or an empty stack if it has none */
    public ItemStack battery() {
        return this.batterySlot.getItem(0);
    }

    /** Puts a battery in the MALP, in place of any it had. */
    public void setBattery(final ItemStack battery) {
        this.batterySlot.setItem(0, battery.copyWithCount(battery.isEmpty() ? 0 : 1));
    }

    /** @return the MALP's battery slot, for its menu */
    public SimpleContainer batterySlot() {
        return this.batterySlot;
    }

    /** @return the gate energy in the MALP's battery */
    public double energy() {
        return this.battery().isEmpty() ? 0 : BatteryItem.energy(this.battery());
    }

    /**
     * Uses gate energy from the MALP's battery for its work, as much as the battery holds. A MALP that needs no power
     * uses none.
     *
     * @return how much it used
     */
    public double drain(final double amount) {
        if (!this.options.powerRequired()) {
            return 0;
        }
        final double used = BatteryItem.extract(this.battery(), amount, false);
        if (used > 0) {
            this.batterySlot.setChanged();
        }
        return used;
    }

    /** @return whether the MALP can work: it needs no power, or its battery holds gate energy */
    public boolean hasPower() {
        return !this.options.powerRequired() || this.energy() > 0;
    }

    /** @return what {@link #hasPower()} said at the MALP's last tick; the client knows this too */
    public boolean powered() {
        return this.entityData.get(MalpEntity.POWERED);
    }

    /** @return the MALP's own settings */
    public MalpOptions options() {
        return this.options;
    }

    /** Changes the MALP's own settings. */
    public void setOptions(final MalpOptions options) {
        this.options = options;
        if (!this.level().isClientSide()) {
            this.entityData.set(MalpEntity.POWERED, this.hasPower());
        }
        this.record();
    }

    /** @return the UUID of the player the MALP belongs to, or null if it belongs to no one */
    public @Nullable UUID owner() {
        return this.owner;
    }

    /** Gives the MALP to a player, by UUID, or to no one. */
    public void setOwner(final @Nullable UUID owner) {
        this.owner = owner;
        this.record();
    }

    /** @return the console the MALP reports to, if it is paired with one */
    public @Nullable GlobalPos console() {
        return this.console;
    }

    /** Pairs the MALP with a console, or with none. */
    public void setConsole(final @Nullable GlobalPos console) {
        this.console = console;
        this.record();
    }

    /** @return the MALP's name: its custom name, or {@code MALP} and the start of its UUID */
    public String displayName() {
        final Component name = this.getCustomName();
        return name != null ? name.getString() : MalpEntity.defaultName(this.getUUID());
    }

    /**
     * Renames the MALP, as a name tag does; a blank name gives it back the name of a MALP with no custom name.
     *
     * @param name the new name, cut to {@value #MAX_NAME} characters
     */
    public void rename(final String name) {
        final String trimmed = name.strip();
        this.setCustomName(trimmed.isEmpty() ? null : Component.literal(trimmed.length() > MalpEntity.MAX_NAME
                ? trimmed.substring(0, MalpEntity.MAX_NAME) : trimmed));
        this.record();
    }

    /** @return the name of a MALP with no custom name */
    public static String defaultName(final UUID id) {
        return "MALP " + id.toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    /** @return how far the camera is panned from straight ahead, in degrees, positive to the right */
    public float mastYaw() {
        return this.entityData.get(MalpEntity.MAST_YAW);
    }

    /** @return how far the camera is tilted, in degrees, positive down as Minecraft measures pitch */
    public float mastPitch() {
        return this.entityData.get(MalpEntity.MAST_PITCH);
    }

    /** @return the camera's pan, between the last tick and this one */
    public float mastYaw(final float partialTick) {
        return Mth.lerp(partialTick, this.mastYawO, this.mastYaw());
    }

    /** @return the camera's tilt, between the last tick and this one */
    public float mastPitch(final float partialTick) {
        return Mth.lerp(partialTick, this.mastPitchO, this.mastPitch());
    }

    /** Pans and tilts the camera by an amount, within its limits. */
    public void aim(final float pan, final float tilt) {
        this.entityData.set(MalpEntity.MAST_YAW, Mth.clamp(this.mastYaw() + pan, -MalpEntity.MAX_PAN,
                MalpEntity.MAX_PAN));
        this.entityData.set(MalpEntity.MAST_PITCH, Mth.clamp(this.mastPitch() + tilt, -MalpEntity.MAX_TILT,
                MalpEntity.MAX_TILT));
    }

    /** Turns the camera back to face straight ahead, the way the MALP drives. */
    public void centerMast() {
        this.entityData.set(MalpEntity.MAST_YAW, 0F);
        this.entityData.set(MalpEntity.MAST_PITCH, 0F);
    }

    /** @return whether the lamp is on */
    public boolean lamp() {
        return this.entityData.get(MalpEntity.LAMP);
    }

    public void setLamp(final boolean lamp) {
        this.entityData.set(MalpEntity.LAMP, lamp);
    }

    /**
     * Drives the MALP for the next few ticks, if it has power.
     *
     * @param forward how fast forward, from -1 for full reverse to 1 for full ahead
     * @param turn how fast it turns, from -1 for full left to 1 for full right
     */
    public void drive(final float forward, final float turn) {
        this.driveForward = Mth.clamp(forward, -1, 1);
        this.driveTurn = Mth.clamp(turn, -1, 1);
        this.driveUntil = this.level().getGameTime() + MalpEntity.DRIVE_TICKS;
    }

    /** Stops driving at once. */
    public void stop() {
        this.driveUntil = Long.MIN_VALUE;
    }

    private boolean driving() {
        return this.level().getGameTime() <= this.driveUntil && this.hasPower();
    }

    /** @return how far each side's wheels have turned, in radians, between the last tick and this one */
    public float wheels(final boolean left, final float partialTick) {
        return left ? Mth.lerp(partialTick, this.leftWheelsO, this.leftWheels)
                : Mth.lerp(partialTick, this.rightWheelsO, this.rightWheels);
    }

    @Override
    public void tick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0) {
            this.setDamage(this.getDamage() - 1);
        }
        this.mastYawO = this.mastYaw();
        this.mastPitchO = this.mastPitch();
        super.tick();
        if (!this.level().isClientSide()) {
            this.serverTick();
        } else {
            this.turnWheels();
        }
    }

    private void serverTick() {
        this.entityData.set(MalpEntity.POWERED, this.hasPower());
        final boolean driving = this.driving();
        if (driving) {
            this.setYRot(this.getYRot() + this.driveTurn * MalpEntity.TURN_SPEED);
        }
        this.applyGravity();
        Vec3 motion = this.getDeltaMovement();
        if (driving && this.driveForward != 0) {
            final Vec3 ahead = Vec3.directionFromRotation(0, this.getYRot()).scale(this.driveForward
                    * MalpEntity.SPEED);
            motion = new Vec3(ahead.x, motion.y, ahead.z);
        } else {
            motion = motion.multiply(this.onGround() ? 0.5 : 0.9, 1, this.onGround() ? 0.5 : 0.9);
        }
        if (this.isInWater()) {
            motion = motion.multiply(0.5, 0.6, 0.5);
        }
        this.setDeltaMovement(motion);
        final Vec3 before = this.position();
        this.move(MoverType.SELF, this.getDeltaMovement());
        // The blocks it moved through act on it as on any creature: lava and fire burn it, cactus pricks it
        this.applyEffectsFromBlocks();
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
        }
        if (driving) {
            this.payForDriving(before);
        }
        if (this.tickCount % MalpEntity.RECORD_TICKS == 0 || !this.chunkPosition().equals(this.recordedChunk)) {
            this.recordedChunk = this.chunkPosition();
            this.record();
        }
    }

    // Driving costs gate energy for each block covered across the ground; rolling on, or being pushed, costs nothing,
    // and a MALP that needs no power pays nothing
    private void payForDriving(final Vec3 before) {
        if (!this.options.powerRequired()) {
            return;
        }
        final double moved = this.position().subtract(before).horizontalDistance();
        if (moved <= 0) {
            return;
        }
        this.drain(moved * MalpSettings.current().driveDrain());
    }

    // The client turns the wheels with how far it moved and turned: both sides together going ahead, and against
    // each other turning on the spot
    private void turnWheels() {
        this.leftWheelsO = this.leftWheels;
        this.rightWheelsO = this.rightWheels;
        final Vec3 moved = new Vec3(this.getX() - this.xo, 0, this.getZ() - this.zo);
        final double ahead = moved.dot(Vec3.directionFromRotation(0, this.getYRot()));
        final float turned = Mth.wrapDegrees(this.getYRot() - this.yRotO) * Mth.DEG_TO_RAD;
        // The wheels are half a block across, and the two sides about a block apart
        final float radius = 4F / 16;
        final float roll = (float) (ahead / radius);
        final float twist = turned * 0.5F / radius;
        this.leftWheels += roll + twist;
        this.rightWheels += roll - twist;
    }

    /** Tells the registry where the MALP is, how charged it is, and its console. */
    public void record() {
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            MalpRegistry.get(level.getServer()).update(this);
        }
    }

    @Override
    public void onRemoval(final RemovalReason reason) {
        super.onRemoval(reason);
        if (this.level() instanceof ServerLevel level) {
            if (reason.shouldDestroy()) {
                MalpRegistry.get(level.getServer()).remove(this.getUUID());
            } else if (reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
                MalpRegistry.get(level.getServer()).update(this);
            }
        }
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.08;
    }

    @Override
    public boolean canBeCollidedWith(final @Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    // Its name tag shows all the time, not only while a player looks at it, so a MALP can be told apart from afar
    @Override
    public boolean shouldShowName() {
        return true;
    }

    // With no custom name, it goes by MALP and the start of its UUID, on its name tag and wherever the game names it
    @Override
    protected Component getTypeName() {
        return Component.literal(MalpEntity.defaultName(this.getUUID()));
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
        final ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) && held.has(DataComponents.CUSTOM_NAME)) {
            if (player instanceof final ServerPlayer serverPlayer && !MalpConfigurator.mayHandle(player, this)) {
                MalpConfigurator.refuseHandling(serverPlayer);
            } else if (!this.level().isClientSide()) {
                this.setCustomName(held.get(DataComponents.CUSTOM_NAME));
                held.consume(1, player);
                this.record();
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(SGItems.CONFIGURATOR)) {
            if (player instanceof ServerPlayer serverPlayer) {
                MalpConfigurator.open(serverPlayer, this);
            }
            return InteractionResult.SUCCESS;
        }
        // A tablet used without sneaking opens its own screen, as when used in the air
        if (held.is(SGItems.MALP_TABLET) && player.isSecondaryUseActive()) {
            if (player instanceof ServerPlayer serverPlayer) {
                MalpTabletItem.pairOnUse(serverPlayer, held, this);
            }
            return InteractionResult.SUCCESS;
        }
        if (!held.isEmpty() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        // Only its owner, or an operator, packs up or opens a MALP someone owns
        if (player instanceof ServerPlayer serverPlayer) {
            if (!MalpConfigurator.mayHandle(player, this)) {
                MalpConfigurator.refuseHandling(serverPlayer);
            } else if (player.isSecondaryUseActive()) {
                this.pickUp(serverPlayer);
            } else {
                serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MalpMenu(id, inventory, this),
                        Component.literal(this.displayName())));
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Packed up as an item, which keeps its battery, its console, its name, its owner and its settings
    private void pickUp(final ServerPlayer player) {
        final ItemStack item = MalpItem.of(this);
        if (!player.getInventory().add(item)) {
            this.spawnAtLocation(player.level(), item);
        }
        this.discard();
    }

    @Override
    protected void destroy(final ServerLevel level, final DamageSource source) {
        this.kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            // Its battery drops on its own, beside the MALP, which stays paired with its console
            this.spawnAtLocation(level, MalpItem.withoutBattery(this));
            if (!this.battery().isEmpty()) {
                this.spawnAtLocation(level, this.battery().copy());
                this.setBattery(ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected Item getDropItem() {
        return SGItems.MALP;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(SGItems.MALP);
    }

    @Override
    public Vec3 viewPosition() {
        final Vec3 ahead = Vec3.directionFromRotation(0, this.getYRot()).scale(MalpEntity.CAMERA_FORWARD);
        return this.position().add(ahead.x, MalpEntity.CAMERA_HEIGHT, ahead.z);
    }

    @Override
    public float viewYaw() {
        return Mth.wrapDegrees(this.getYRot() + this.mastYaw());
    }

    @Override
    public float viewPitch() {
        return this.mastPitch();
    }

    @Override
    public boolean viewLamp() {
        return this.lamp() && this.powered();
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        this.setBattery(input.read("battery", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.console = input.read("console", GlobalPos.CODEC).orElse(null);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.options = input.read("options", MalpOptions.CODEC).orElse(MalpOptions.DEFAULTS);
        this.entityData.set(MalpEntity.LAMP, input.getBooleanOr("lamp", false));
        this.entityData.set(MalpEntity.MAST_YAW, input.getFloatOr("mast_yaw", 0));
        this.entityData.set(MalpEntity.MAST_PITCH, input.getFloatOr("mast_pitch", 0));
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        if (!this.battery().isEmpty()) {
            output.store("battery", ItemStack.CODEC, this.battery());
        }
        if (this.console != null) {
            output.store("console", GlobalPos.CODEC, this.console);
        }
        if (this.owner != null) {
            output.store("owner", UUIDUtil.CODEC, this.owner);
        }
        output.store("options", MalpOptions.CODEC, this.options);
        output.putBoolean("lamp", this.lamp());
        output.putFloat("mast_yaw", this.mastYaw());
        output.putFloat("mast_pitch", this.mastPitch());
    }
}
