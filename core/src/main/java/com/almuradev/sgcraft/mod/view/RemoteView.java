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

package com.almuradev.sgcraft.mod.view;

import com.almuradev.sgcraft.mod.Platform;
import com.almuradev.sgcraft.mod.SGCraft;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * One player's view out of one source: what it has sent them of the world around the source, and the work of
 * keeping that up to date each tick. It is the player's own view, or one shown on a display in their world. Only
 * {@link RemoteViews} makes these, on the server thread.
 */
final class RemoteView {

    // How often a sent section is looked at again, past the changes the world reports, rain heights are, and block
    // entities and entities are gone over
    private static final int SECTION_CHECK_TICKS = 400;
    private static final int HEIGHT_CHECK_TICKS = 10;
    private static final int BLOCK_ENTITY_CHECK_TICKS = 4;
    private static final int ENTITY_TICKS = 2;
    // How long a source may be missing, such as between leaving one world and turning up in another
    private static final int MISSING_GRACE_TICKS = 20;
    // Sections stay sent until this far outside the radius, so a source moving back and forth doesn't resend them
    private static final int FORGET_MARGIN = 16;
    // Each packet is kept well under the payload limit; one section is at most about 12 KiB
    private static final int PACKET_BYTES = 32 * 1024;
    private static final int MAX_ENTITIES = 64;
    private static final int MAX_TAG_BYTES = 16 * 1024;

    final int id;
    final UUID viewer;
    final UUID source;
    // Whether this is the viewer's own view, and the display in their world it is shown on, if any
    boolean own;
    final @Nullable BlockPos display;
    private final RemoteViewConfig reach;
    private final @Nullable Consumer<CustomPacketPayload> sink;
    private Entity current;
    private ServerLevel level;
    private int ticks;
    private int missing;
    // Sent sections by position, with the hash of what was sent
    private final Long2LongMap sections = new Long2LongOpenHashMap();
    // The sent sections whose blocks or light have changed since, to send again; and the sections whose block
    // entities are to be looked at again: changed, newly sent, or forgotten
    private final LongSet changed = new LongOpenHashSet();
    private final LongSet blockEntitySections = new LongOpenHashSet();
    // Chunks whose rain heights were sent, with the hash of what was sent
    private final Long2LongMap heights = new Long2LongOpenHashMap();
    // Sent block entities by position, with a hash of what was sent
    private final Long2LongMap blockEntities = new Long2LongOpenHashMap();
    // Entities in view by id, with a hash of how they looked when sent
    private final Int2LongMap entities = new Int2LongOpenHashMap();
    private int resends;

    RemoteView(final int id, final ServerPlayer viewer, final Entity source, final boolean own,
            final @Nullable BlockPos display, final RemoteViewConfig reach,
            final @Nullable Consumer<CustomPacketPayload> sink) {
        this.id = id;
        this.viewer = viewer.getUUID();
        this.source = source.getUUID();
        this.own = own;
        this.display = display;
        this.reach = reach;
        this.sink = sink;
        this.current = source;
        this.level = (ServerLevel) source.level();
        this.begin(viewer);
    }

    /** @return the world the source is in */
    ServerLevel level() {
        return this.level;
    }

    /** @return the positions of the sections sent and not since forgotten */
    LongSet sentSections() {
        return new LongOpenHashSet(this.sections.keySet());
    }

    /** @return how many times a section has been sent again since the view began */
    int resends() {
        return this.resends;
    }

    /** Makes the view the viewer's own, or no longer theirs, telling them, without its beginning again. */
    void own(final ServerPlayer player, final boolean own) {
        this.own = own;
        this.send(player, new RemoteViewPackets.Own(this.id, own));
    }

    /** Ends the view, telling the viewer if they are still here. */
    void end(final MinecraftServer server) {
        final ServerPlayer player = server.getPlayerList().getPlayer(this.viewer);
        if (player != null) {
            this.send(player, new RemoteViewPackets.End(this.id));
        }
    }

    /**
     * Brings the viewer up to date by a tick.
     *
     * @return whether the view goes on; a view whose viewer or source is gone does not
     */
    boolean tick(final MinecraftServer server) {
        final ServerPlayer player = server.getPlayerList().getPlayer(this.viewer);
        if (player == null || player.hasDisconnected()) {
            return false;
        }
        final Entity found = this.findSource(server);
        if (found == null) {
            if (++this.missing > RemoteView.MISSING_GRACE_TICKS || this.current.getRemovalReason() != null
                    && this.current.getRemovalReason().shouldDestroy()) {
                this.send(player, new RemoteViewPackets.End(this.id));
                return false;
            }
            return true;
        }
        this.missing = 0;
        this.current = found;
        if (found.level() != this.level) {
            this.level = (ServerLevel) found.level();
            this.sections.clear();
            this.changed.clear();
            this.blockEntitySections.clear();
            this.heights.clear();
            this.blockEntities.clear();
            this.entities.clear();
            this.begin(player);
        }
        final RemoteViewPackets.Camera camera = RemoteView.camera(found);
        final Vec3 eye = new Vec3(camera.x(), camera.y(), camera.z());
        this.sendSections(player, eye);
        this.sendHeights(player);
        if (this.ticks % RemoteView.BLOCK_ENTITY_CHECK_TICKS == 0) {
            this.sendBlockEntities(player);
        }
        final List<RemoteViewPackets.EntityMove> moves = this.ticks % RemoteView.ENTITY_TICKS == 0
                ? this.sendEntities(player, eye) : List.of();
        this.send(player, new RemoteViewPackets.Frame(this.id, camera, this.atmosphere(eye), moves));
        this.ticks++;
        return true;
    }

    // The source as it is now: the same entity while it is in the world, or one with its UUID in any world, as a
    // source that goes through a gate to another world turns up as a new entity
    private @Nullable Entity findSource(final MinecraftServer server) {
        if (!this.current.isRemoved()) {
            return this.current;
        }
        if (this.current.getRemovalReason() != null && this.current.getRemovalReason().shouldDestroy()) {
            return null;
        }
        final Entity here = this.level.getEntity(this.source);
        if (here != null && !here.isRemoved()) {
            return here;
        }
        for (final ServerLevel other : server.getAllLevels()) {
            final Entity there = other.getEntity(this.source);
            if (there != null && !there.isRemoved()) {
                return there;
            }
        }
        return null;
    }

    private void begin(final ServerPlayer player) {
        final DimensionType type = this.level.dimensionType();
        this.send(player, new RemoteViewPackets.Begin(this.id, this.own, Optional.ofNullable(this.display),
                this.level.dimension().identifier(), type.hasSkyLight(), type.skybox().ordinal(),
                type.cardinalLightType().ordinal(), type.ambientLight(), this.level.getMinY(), this.level.getHeight(),
                this.level.getSeaLevel(), this.reach.horizontalRadius(), this.reach.verticalRadius()));
    }

    private static RemoteViewPackets.Camera camera(final Entity source) {
        if (source instanceof RemoteViewSource view) {
            final Vec3 pos = view.viewPosition();
            return new RemoteViewPackets.Camera(pos.x, pos.y, pos.z, view.viewYaw(), view.viewPitch(),
                    view.viewLamp());
        }
        // A source that has come out of a gate as some other kind of entity is looked out of from its eyes
        final Vec3 eye = source.getEyePosition();
        return new RemoteViewPackets.Camera(eye.x, eye.y, eye.z, source.getYRot(), source.getXRot(), false);
    }

    // Sections: forget those well out of range, send again those that have changed, then send those in range not yet
    // sent, nearest first, as far as this tick's share allows
    private void sendSections(final ServerPlayer player, final Vec3 eye) {
        final int radius = this.reach.horizontalRadius();
        final int vertical = this.reach.verticalRadius();
        final List<Long> forgotten = new ArrayList<>();
        final LongIterator iterator = this.sections.keySet().iterator();
        while (iterator.hasNext()) {
            final long pos = iterator.nextLong();
            if (!RemoteView.inRange(pos, eye, radius + RemoteView.FORGET_MARGIN,
                    vertical + RemoteView.FORGET_MARGIN)) {
                iterator.remove();
                forgotten.add(pos);
                this.blockEntitySections.add(pos);
            }
        }
        final Batch batch = new Batch(player, forgotten);
        int budget = this.reach.bytesPerTick();
        // Those the world has said changed, and now and then each of the rest, in case one changed unannounced
        final LongIterator changes = this.changed.iterator();
        while (budget > 0 && changes.hasNext()) {
            final long pos = changes.nextLong();
            changes.remove();
            budget -= this.resend(batch, pos);
        }
        for (final Long2LongMap.Entry entry : this.sections.long2LongEntrySet()) {
            if (budget <= 0) {
                break;
            }
            final long pos = entry.getLongKey();
            if (Math.floorMod(Long.hashCode(pos) + this.ticks, RemoteView.SECTION_CHECK_TICKS) == 0) {
                budget -= this.resend(batch, pos);
            }
        }
        if (budget > 0) {
            for (final long pos : this.missingSections(eye, radius, vertical)) {
                final byte[] data = this.encode(pos);
                if (data == null) {
                    continue;
                }
                this.sections.put(pos, RemoteViewSections.hash(data));
                this.blockEntitySections.add(pos);
                batch.add(pos, data);
                budget -= data.length;
                if (budget <= 0) {
                    break;
                }
            }
        }
        batch.flush();
    }

    /** A block or the light in a section of a world has changed; if the view has sent it, it is sent again. */
    void changed(final ServerLevel level, final long section) {
        if (level == this.level && this.sections.containsKey(section)) {
            this.changed.add(section);
            this.blockEntitySections.add(section);
        }
    }

    // Sends a sent section again if it has changed since, returning how many bytes that took
    private int resend(final Batch batch, final long pos) {
        if (!this.sections.containsKey(pos)) {
            return 0;
        }
        final byte[] data = this.encode(pos);
        if (data == null) {
            return 0;
        }
        final long hash = RemoteViewSections.hash(data);
        if (hash == this.sections.get(pos)) {
            return 0;
        }
        this.sections.put(pos, hash);
        batch.add(pos, data);
        this.resends++;
        return data.length;
    }

    // Rain heights, for the client to draw the source's weather: those of each chunk a section of which has been
    // sent, once, and again when they change, looked at every few ticks as sent sections are; those of a chunk none of
    // whose sections are sent any more are forgotten
    private void sendHeights(final ServerPlayer player) {
        final LongSet columns = new LongOpenHashSet();
        for (final long pos : this.sections.keySet()) {
            columns.add(ChunkPos.pack(SectionPos.x(pos), SectionPos.z(pos)));
        }
        final List<Long> forgotten = new ArrayList<>();
        final LongIterator iterator = this.heights.keySet().iterator();
        while (iterator.hasNext()) {
            final long column = iterator.nextLong();
            if (!columns.contains(column)) {
                iterator.remove();
                forgotten.add(column);
            }
        }
        final List<RemoteViewPackets.RainHeights> changed = new ArrayList<>();
        for (final long column : columns) {
            final boolean sent = this.heights.containsKey(column);
            if (sent && Math.floorMod(Long.hashCode(column) + this.ticks, RemoteView.HEIGHT_CHECK_TICKS) != 0) {
                continue;
            }
            final LevelChunk chunk = this.level.getChunkSource().getChunkNow(ChunkPos.getX(column),
                    ChunkPos.getZ(column));
            if (chunk == null) {
                continue;
            }
            final short[] heights = RemoteView.rainHeights(chunk);
            final long hash = Arrays.hashCode(heights);
            if (!sent || this.heights.get(column) != hash) {
                this.heights.put(column, hash);
                changed.add(new RemoteViewPackets.RainHeights(column, heights));
            }
        }
        // As many chunks to a packet as keep it under the size each packet is kept to
        final int perPacket = RemoteView.PACKET_BYTES / (Long.BYTES + RemoteViewPackets.RainHeights.COLUMNS
                * Short.BYTES);
        for (int from = 0; from < changed.size() || from == 0 && !forgotten.isEmpty(); from += perPacket) {
            final List<RemoteViewPackets.RainHeights> some = changed.subList(from,
                    Math.min(changed.size(), from + perPacket));
            this.send(player, new RemoteViewPackets.Heights(this.id, List.copyOf(some),
                    from == 0 ? forgotten : List.of()));
        }
    }

    // Where rain stops in each block column of a chunk, as the game's own weather reads it: one above the highest
    // block that blocks movement or holds a fluid
    private static short[] rainHeights(final LevelChunk chunk) {
        final short[] heights = new short[RemoteViewPackets.RainHeights.COLUMNS];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                heights[x + z * 16] = (short) (chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1);
            }
        }
        return heights;
    }

    // Sections in range not yet sent whose chunks are loaded, nearest first
    private List<Long> missingSections(final Vec3 eye, final int radius, final int vertical) {
        final int minX = SectionPos.blockToSectionCoord(eye.x - radius);
        final int maxX = SectionPos.blockToSectionCoord(eye.x + radius);
        final int minZ = SectionPos.blockToSectionCoord(eye.z - radius);
        final int maxZ = SectionPos.blockToSectionCoord(eye.z + radius);
        final int minY = Math.max(this.level.getMinSectionY(), SectionPos.blockToSectionCoord(eye.y - vertical));
        final int maxY = Math.min(this.level.getMaxSectionY(), SectionPos.blockToSectionCoord(eye.y + vertical));
        final List<Long> missing = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Never loads a chunk: only those already loaded are read
                if (this.level.getChunkSource().getChunkNow(x, z) == null) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    final long pos = SectionPos.asLong(x, y, z);
                    if (!this.sections.containsKey(pos) && RemoteView.inRange(pos, eye, radius, vertical)) {
                        missing.add(pos);
                    }
                }
            }
        }
        missing.sort(Comparator.comparingDouble(pos -> RemoteView.distanceSq(pos, eye)));
        return missing;
    }

    private byte @Nullable [] encode(final long pos) {
        final LevelChunk chunk = this.level.getChunkSource().getChunkNow(SectionPos.x(pos), SectionPos.z(pos));
        if (chunk == null) {
            return null;
        }
        return RemoteViewSections.encode(chunk, SectionPos.y(pos), this.level.getLightEngine(),
                this.level.dimensionType().hasSkyLight());
    }

    // Whether a section comes within reach of the camera: across, by the nearest point of its column, and up and down
    private static boolean inRange(final long pos, final Vec3 eye, final int radius, final int vertical) {
        final double minY = SectionPos.sectionToBlockCoord(SectionPos.y(pos));
        if (eye.y + vertical < minY || eye.y - vertical > minY + 16) {
            return false;
        }
        final double minX = SectionPos.sectionToBlockCoord(SectionPos.x(pos));
        final double minZ = SectionPos.sectionToBlockCoord(SectionPos.z(pos));
        final double dx = Math.max(0, Math.max(minX - eye.x, eye.x - (minX + 16)));
        final double dz = Math.max(0, Math.max(minZ - eye.z, eye.z - (minZ + 16)));
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    private static double distanceSq(final long pos, final Vec3 eye) {
        final double dx = SectionPos.sectionToBlockCoord(SectionPos.x(pos)) + 8 - eye.x;
        final double dy = SectionPos.sectionToBlockCoord(SectionPos.y(pos)) + 8 - eye.y;
        final double dz = SectionPos.sectionToBlockCoord(SectionPos.z(pos)) + 8 - eye.z;
        return dx * dx + dy * dy + dz * dz;
    }

    // Block entities in the sent sections: new or changed ones are sent, and gone ones are named. Only those in the
    // sections the world has said changed, or that were sent or forgotten since, are looked at, as a block entity
    // whose look changes says so as its block does; and now and then all of them, in case one changed unannounced
    private void sendBlockEntities(final ServerPlayer player) {
        final boolean all = Math.floorMod(this.ticks, RemoteView.SECTION_CHECK_TICKS) == 0;
        if (!all && this.blockEntitySections.isEmpty()) {
            return;
        }
        final LongSet looked = new LongOpenHashSet(this.blockEntitySections);
        this.blockEntitySections.clear();
        final LongSet seen = new LongOpenHashSet();
        final List<RemoteViewPackets.BlockEntityState> changed = new ArrayList<>();
        final LongSet columns = new LongOpenHashSet();
        for (final long pos : all ? this.sections.keySet() : looked) {
            columns.add(ChunkPos.pack(SectionPos.x(pos), SectionPos.z(pos)));
        }
        int bytes = 0;
        for (final long column : columns) {
            final LevelChunk chunk = this.level.getChunkSource().getChunkNow(ChunkPos.getX(column),
                    ChunkPos.getZ(column));
            if (chunk == null) {
                continue;
            }
            for (final BlockEntity entity : chunk.getBlockEntities().values()) {
                final BlockPos at = entity.getBlockPos();
                final long section = SectionPos.asLong(at);
                if (!this.sections.containsKey(section) || !all && !looked.contains(section)) {
                    continue;
                }
                final long key = at.asLong();
                seen.add(key);
                final CompoundTag tag = RemoteView.updateTag(entity, this.level);
                final int type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(entity.getType());
                final long hash = (long) tag.hashCode() << 32 ^ type;
                if (!this.blockEntities.containsKey(key) || this.blockEntities.get(key) != hash) {
                    this.blockEntities.put(key, hash);
                    changed.add(new RemoteViewPackets.BlockEntityState(key, type, tag));
                    bytes += tag.sizeInBytes();
                    if (bytes > RemoteView.PACKET_BYTES) {
                        this.send(player, new RemoteViewPackets.BlockEntities(this.id, List.copyOf(changed),
                                List.of()));
                        changed.clear();
                        bytes = 0;
                    }
                }
            }
        }
        final List<Long> removed = new ArrayList<>();
        final LongIterator iterator = this.blockEntities.keySet().iterator();
        while (iterator.hasNext()) {
            final long key = iterator.nextLong();
            if (!seen.contains(key) && (all || looked.contains(SectionPos.blockToSection(key)))) {
                iterator.remove();
                removed.add(key);
            }
        }
        if (!changed.isEmpty() || !removed.isEmpty()) {
            this.send(player, new RemoteViewPackets.BlockEntities(this.id, changed, removed));
        }
    }

    // What a block entity tells its clients; one that fails to say, or says too much, is sent with nothing
    private static CompoundTag updateTag(final BlockEntity entity, final ServerLevel level) {
        try {
            final CompoundTag tag = entity.getUpdateTag(level.registryAccess());
            return tag.sizeInBytes() > RemoteView.MAX_TAG_BYTES ? new CompoundTag() : tag;
        } catch (final RuntimeException e) {
            SGCraft.LOG.debug("Couldn't get the update tag of {} for a remote view", entity, e);
            return new CompoundTag();
        }
    }

    // Entities near the camera, other than the source: new ones and those whose looks have changed are sent, gone
    // ones are named, and where each is goes with this tick's frame. Parts of an entity, such as the dragon's, are
    // left out: they have its type, so each would be made as a whole copy of it, and it is drawn with them anyway
    private List<RemoteViewPackets.EntityMove> sendEntities(final ServerPlayer player, final Vec3 eye) {
        final AABB box = new AABB(eye, eye).inflate(this.reach.horizontalRadius(), this.reach.verticalRadius(),
                this.reach.horizontalRadius());
        final Platform platform = Platform.get();
        final List<Entity> near = this.level.getEntities(this.current, box, entity -> entity.isAlive()
                && !entity.isSpectator() && !entity.getUUID().equals(this.source) && !platform.isPart(entity));
        near.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(eye)));
        final List<Entity> shown = near.size() > RemoteView.MAX_ENTITIES
                ? near.subList(0, RemoteView.MAX_ENTITIES) : near;
        final IntSet seen = new IntOpenHashSet();
        final List<RemoteViewPackets.EntityLooks> changed = new ArrayList<>();
        final List<RemoteViewPackets.EntityMove> moves = new ArrayList<>();
        int bytes = 0;
        for (final Entity entity : shown) {
            seen.add(entity.getId());
            final byte[] data = RemoteViewEntityData.encode(entity, this.level.registryAccess());
            final long hash = RemoteViewSections.hash(data);
            if (!this.entities.containsKey(entity.getId()) || this.entities.get(entity.getId()) != hash) {
                this.entities.put(entity.getId(), hash);
                final Optional<String> name = entity instanceof Player p
                        ? Optional.of(p.getGameProfile().name()) : Optional.empty();
                changed.add(new RemoteViewPackets.EntityLooks(entity.getId(),
                        BuiltInRegistries.ENTITY_TYPE.getId(entity.getType()), entity.getUUID(), name, data));
                bytes += data.length;
                if (bytes > RemoteView.PACKET_BYTES) {
                    this.send(player, new RemoteViewPackets.Entities(this.id, List.copyOf(changed), List.of()));
                    changed.clear();
                    bytes = 0;
                }
            }
            final float headYaw = entity instanceof LivingEntity living ? living.getYHeadRot() : entity.getYRot();
            final float bodyYaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
            moves.add(new RemoteViewPackets.EntityMove(entity.getId(), entity.getX(), entity.getY(), entity.getZ(),
                    entity.getYRot(), entity.getXRot(), headYaw, bodyYaw, entity.onGround()));
        }
        final List<Integer> removed = new ArrayList<>();
        final IntIterator iterator = this.entities.keySet().iterator();
        while (iterator.hasNext()) {
            final int key = iterator.nextInt();
            if (!seen.contains(key)) {
                iterator.remove();
                removed.add(key);
            }
        }
        if (!changed.isEmpty() || !removed.isEmpty()) {
            this.send(player, new RemoteViewPackets.Entities(this.id, changed, removed));
        }
        return moves;
    }

    private RemoteViewPackets.Atmosphere atmosphere(final Vec3 eye) {
        final EnvironmentAttributeSystem attributes = this.level.environmentAttributes();
        return new RemoteViewPackets.Atmosphere(this.level.getGameTime(), this.level.getRainLevel(1),
                this.level.getThunderLevel(1),
                RemoteView.rgb(attributes.getValue(EnvironmentAttributes.SKY_COLOR, eye, null)),
                RemoteView.rgb(attributes.getValue(EnvironmentAttributes.FOG_COLOR, eye, null)),
                RemoteView.argb(attributes.getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, eye, null)),
                attributes.getValue(EnvironmentAttributes.SUN_ANGLE, eye, null),
                attributes.getValue(EnvironmentAttributes.MOON_ANGLE, eye, null),
                attributes.getValue(EnvironmentAttributes.STAR_ANGLE, eye, null),
                attributes.getValue(EnvironmentAttributes.STAR_BRIGHTNESS, eye, null),
                attributes.getValue(EnvironmentAttributes.MOON_PHASE, eye, null).index(),
                attributes.getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, eye, null),
                RemoteView.rgb(attributes.getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, eye, null)),
                RemoteView.rgb(attributes.getValue(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, eye, null)),
                RemoteView.rgb(attributes.getValue(EnvironmentAttributes.BLOCK_LIGHT_TINT, eye, null)),
                attributes.getValue(EnvironmentAttributes.FOG_START_DISTANCE, eye, null),
                attributes.getValue(EnvironmentAttributes.FOG_END_DISTANCE, eye, null),
                attributes.getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, eye, null));
    }

    private static int rgb(final Vector3fc color) {
        return RemoteView.channel(color.x()) << 16 | RemoteView.channel(color.y()) << 8 | RemoteView.channel(color.z());
    }

    private static int argb(final Vector4fc color) {
        return RemoteView.channel(color.w()) << 24 | RemoteView.channel(color.x()) << 16
                | RemoteView.channel(color.y()) << 8 | RemoteView.channel(color.z());
    }

    private static int channel(final float value) {
        return Mth.clamp(Math.round(value * 255), 0, 255);
    }

    private void send(final ServerPlayer player, final CustomPacketPayload payload) {
        if (this.sink != null) {
            this.sink.accept(payload);
        } else {
            RemoteViews.send(player, payload);
        }
    }

    // Sections to send this tick, split into packets well under the payload limit
    private final class Batch {

        private final ServerPlayer player;
        private List<Long> forgotten;
        private final List<RemoteViewPackets.Section> pending = new ArrayList<>();
        private int bytes;

        Batch(final ServerPlayer player, final List<Long> forgotten) {
            this.player = player;
            this.forgotten = forgotten;
        }

        void add(final long pos, final byte[] data) {
            if (this.bytes + data.length > RemoteView.PACKET_BYTES && !this.pending.isEmpty()) {
                this.flush();
            }
            this.pending.add(new RemoteViewPackets.Section(pos, data));
            this.bytes += data.length;
        }

        void flush() {
            if (this.pending.isEmpty() && this.forgotten.isEmpty()) {
                return;
            }
            RemoteView.this.send(this.player, new RemoteViewPackets.Sections(RemoteView.this.id,
                    List.copyOf(this.pending), this.forgotten));
            this.pending.clear();
            this.forgotten = List.of();
            this.bytes = 0;
        }
    }
}
