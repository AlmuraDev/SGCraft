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

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An Ancient drone, as a puddle jumper launches them on the show: a glowing projectile that seeks the target its
 * pilot locked, turning after it as it flies, and bursts in a small explosion on whatever it meets first, the target
 * or a block in its way. Its blast spares the jumpers and anyone in them. At the Ender Dragon it seeks the head, which
 * explosions hurt most. One that loses its target flies on straight, and one that meets nothing in time burns out.
 * Drones are never saved.
 */
public final class DroneEntity extends Projectile {

    /** How fast it flies, in blocks a tick. */
    public static final double SPEED = 1.6;
    // How much of the way to its target's bearing it turns each tick
    private static final double TURN = 0.22;
    // How long it flies before it burns out, in ticks
    private static final int LIFE = 200;
    // How big its blast is, as a creeper's is 3
    private static final float BLAST = 2.5F;
    // Spares the jumpers, and anyone riding in or standing inside one, from a drone's blast; and, with friendly fire
    // off, any creature that isn't hostile
    private final ExplosionDamageCalculator sparing = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(final Explosion explosion, final Entity entity) {
            return !DroneEntity.this.spared(entity) && super.shouldDamageEntity(explosion, entity);
        }
    };

    private @Nullable Entity target;
    private boolean spareFriends = true;
    private int age;

    public DroneEntity(final EntityType<? extends DroneEntity> type, final Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /**
     * @param from where it launches
     * @param heading which way, and how fast, it leaves its bay
     * @param target what it seeks, or null to fly straight on
     * @param spareFriends whether it spares creatures that aren't hostile, passing them by and its blast sparing them
     */
    public void launch(final Entity owner, final Vec3 from, final Vec3 heading, final @Nullable Entity target,
            final boolean spareFriends) {
        this.setOwner(owner);
        this.setPos(from);
        this.setDeltaMovement(heading);
        this.target = target;
        this.spareFriends = spareFriends;
        this.face(heading);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        final Vec3 at = this.position();
        Vec3 motion = this.getDeltaMovement();
        if (this.level() instanceof final ServerLevel level) {
            if (++this.age > DroneEntity.LIFE) {
                level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.02);
                this.discard();
                return;
            }
            // Turning after its target, at its full speed
            final Vec3 aim = this.aim();
            if (aim != null) {
                final Vec3 wanted = aim.subtract(at).normalize().scale(DroneEntity.SPEED);
                motion = motion.add(wanted.subtract(motion).scale(DroneEntity.TURN));
                motion = motion.normalize().scale(Math.min(DroneEntity.SPEED, motion.length() + 0.1));
                this.setDeltaMovement(motion);
            }
            final Vec3 next = at.add(motion);
            // Whatever it meets on its way this tick: the first creature, or else a block
            final BlockHitResult block = level.clip(new ClipContext(at, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            final Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            final EntityHitResult creature = ProjectileUtil.getEntityHitResult(level, this, at, end,
                    this.getBoundingBox().expandTowards(motion).inflate(1), this::canHitEntity);
            if (creature != null) {
                this.burst(level, creature.getLocation());
                return;
            }
            if (block.getType() != HitResult.Type.MISS) {
                this.burst(level, block.getLocation());
                return;
            }
            if (aim != null && aim.distanceToSqr(at) < 1.5 * 1.5) {
                this.burst(level, at);
                return;
            }
        }
        this.setPos(at.add(motion));
        this.face(motion);
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0, 0, 0);
        }
    }

    // Where it seeks: the middle of its target, or the Ender Dragon's head; nowhere once the target is gone
    private @Nullable Vec3 aim() {
        if (this.target == null || !this.target.isAlive() || this.target.level() != this.level()) {
            this.target = null;
            return null;
        }
        if (this.target instanceof final EnderDragon dragon) {
            return dragon.head.getBoundingBox().getCenter();
        }
        return this.target.getBoundingBox().getCenter();
    }

    // Nothing of the jumpers, nor their riders, nor whoever launched it, nor what it spares, stops it
    @Override
    protected boolean canHitEntity(final Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof DroneEntity) && entity != this.getOwner()
                && !this.spared(entity);
    }

    // Whether it spares something: the jumpers and anyone in one, and, sparing friends, whatever isn't hostile
    private boolean spared(final Entity entity) {
        final Entity whole = entity instanceof final EnderDragonPart part ? part.parentMob : entity;
        return whole instanceof PuddleJumperEntity || JumperShelter.sheltered(whole)
                || this.spareFriends && whole instanceof LivingEntity && !(whole instanceof Enemy);
    }

    private void burst(final ServerLevel level, final Vec3 at) {
        final DamageSource source = this.damageSources().explosion(this, this.getOwner());
        level.explode(this, source, this.sparing, at.x, at.y, at.z, DroneEntity.BLAST, false,
                Level.ExplosionInteraction.MOB);
        this.discard();
    }

    private void face(final Vec3 heading) {
        if (heading.lengthSqr() < 1.0E-6) {
            return;
        }
        this.setYRot((float) (Mth.atan2(heading.x, heading.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(heading.y, heading.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
    }
}
