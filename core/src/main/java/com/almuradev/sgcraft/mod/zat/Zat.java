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

package com.almuradev.sgcraft.mod.zat;

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.sound.SGSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Goa'uld zat'nik'tel, as in 2.2.0: it charges as its sound starts, then fires a thin bolt where its holder is
 * looking, stopping at the first block or creature in the way. As on the show, one shot stuns, and a second while the
 * target is still stunned kills. Its damage goes through armor.
 */
public final class Zat {

    /** How long after the trigger is pulled the shot comes, in ticks, as the sound charges. */
    public static final int CHARGE_TICKS = 15;
    /** The Zat's damage. */
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, SGCraft.id("zat"));
    // How far the bolt's sparks are apart, in blocks
    private static final double SPARK_STEP = 0.4;

    // The shots charging, and when each entity was last stunned, in game time
    private static final List<Shot> CHARGING = new ArrayList<>();
    private static final Map<UUID, Long> STUNNED = new ConcurrentHashMap<>();
    // How often the stunned are looked through for those no longer stunned, in ticks
    private static final int FORGET_TICKS = 200;

    private record Shot(ServerPlayer player, ServerLevel level, int firesAt) {
    }

    private Zat() {
    }

    /** The trigger is pulled: it charges, then fires where its holder is looking by then. */
    public static void pullTrigger(final ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SGSounds.ZAT_FIRE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        Zat.CHARGING.add(new Shot(player, player.level(), player.level().getServer().getTickCount()
                + Zat.CHARGE_TICKS));
    }

    /** Fires the shots whose charge is done; one whose holder died or left the world goes. Called each server tick. */
    public static void tick(final MinecraftServer server) {
        // Those no longer stunned are forgotten now and then, so the stunned don't build up forever
        if (server.getTickCount() % Zat.FORGET_TICKS == 0 && !Zat.STUNNED.isEmpty()) {
            final long now = server.overworld().getGameTime();
            final int stun = ZatSettings.current().stunTicks();
            Zat.STUNNED.values().removeIf(since -> now < since || now - since > stun);
        }
        if (Zat.CHARGING.isEmpty()) {
            return;
        }
        final Iterator<Shot> each = Zat.CHARGING.iterator();
        while (each.hasNext()) {
            final Shot shot = each.next();
            if (shot.player().isRemoved() || shot.player().level() != shot.level() || !shot.player().isAlive()) {
                each.remove();
            } else if (server.getTickCount() >= shot.firesAt()) {
                each.remove();
                Zat.fire(shot.player());
            }
        }
    }

    /** Fires a shot where the player looks now. */
    public static void fire(final ServerPlayer player) {
        final ServerLevel level = player.level();
        final Vec3 eye = player.getEyePosition();
        final Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(ZatSettings.current().range()));
        final BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        final AABB reach = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1);
        final EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, reach,
                entity -> entity != player.getVehicle() && EntitySelector.NO_SPECTATORS.test(entity)
                        && entity.isPickable(), 0);
        if (hit != null) {
            end = hit.getLocation();
        }
        Zat.bolt(level, eye.add(look.scale(0.7)).add(0, -0.25, 0), end);
        if (hit != null && hit.getEntity() instanceof final LivingEntity target) {
            Zat.hit(player, target);
        }
    }

    // The bolt's thin blue-white line of sparks, and a burst where it lands
    private static void bolt(final ServerLevel level, final Vec3 from, final Vec3 to) {
        final Vec3 along = to.subtract(from);
        final int sparks = Math.max(1, (int) (along.length() / Zat.SPARK_STEP));
        for (int i = 0; i <= sparks; i++) {
            final Vec3 at = from.add(along.scale((double) i / sparks));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 12, 0.15, 0.15, 0.15, 0.1);
    }

    /** A first shot stuns, and a second while the target is still stunned kills. */
    public static void hit(final ServerPlayer shooter, final LivingEntity target) {
        final ServerLevel level = shooter.level();
        final ZatSettings settings = ZatSettings.current();
        final long now = level.getGameTime();
        final Long since = Zat.STUNNED.get(target.getUUID());
        final boolean stunned = since != null && now - since >= 0 && now - since <= settings.stunTicks();
        final DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(Zat.DAMAGE), shooter);
        if (stunned && settings.secondShotKills()) {
            if (target.hurtServer(level, source, Math.max(1000.0F, target.getMaxHealth() * 10))) {
                Zat.STUNNED.remove(target.getUUID());
            }
            return;
        }
        // Only what it may hurt here is stunned: not creative players, nor players where there's no PvP
        if (!target.hurtServer(level, source, Math.max(0.5F, settings.stunDamage()))) {
            return;
        }
        Zat.STUNNED.put(target.getUUID(), now);
        final int ticks = settings.stunTicks();
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 5, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 2, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, Math.min(ticks, 100), 0, false, false));
    }

    /** Forgets the shots charging and who is stunned, as the server stops. */
    public static void clear() {
        Zat.CHARGING.clear();
        Zat.STUNNED.clear();
    }

    /** @return whether an entity is stunned by a Zat now */
    public static boolean stunned(final Entity entity) {
        final Long since = Zat.STUNNED.get(entity.getUUID());
        final long now = entity.level().getGameTime();
        return since != null && now - since >= 0 && now - since <= ZatSettings.current().stunTicks();
    }
}
