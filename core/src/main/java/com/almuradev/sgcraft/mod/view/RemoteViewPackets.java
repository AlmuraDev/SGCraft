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

import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.network.SGPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the server streams to a player watching a remote view. Every packet names the view it belongs to, so a client
 * drops anything left over from a view that has ended. A view begins with {@link Begin}, which a client also takes as
 * the word to drop everything it has when the source moves to another world, and ends with {@link End}.
 */
public final class RemoteViewPackets {

    private RemoteViewPackets() {
    }

    /**
     * A view begins, or its source has moved to another world: the client drops what it had of the view.
     *
     * @param view the view's number, which every later packet of the view carries
     * @param own whether this is the viewer's own view, which their screens show; a viewer has at most one own view
     *     at a time, and any number of others, each shown on a display in their world
     * @param display where in the viewer's world the view is shown on a display, such as a console's monitor, if
     *     anywhere
     * @param world the world the source is in, by identifier
     * @param skybox the world's sky, as the ordinal of its dimension type's skybox
     * @param cardinalLighting how the world shades block faces, as the ordinal of its cardinal light type
     * @param ambientLight the world's dimension type's ambient light
     * @param horizontalRadius how far out from the camera the view reaches, in blocks
     * @param verticalRadius how far above and below the camera the view reaches, in blocks
     */
    public record Begin(int view, boolean own, Optional<BlockPos> display, Identifier world, boolean hasSkyLight,
            int skybox, int cardinalLighting, float ambientLight, int minY, int height, int horizontalRadius,
            int verticalRadius) implements SGPacket {

        public static final Type<Begin> TYPE = new Type<>(SGCraft.id("remote_view_begin"));

        public Begin(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readBoolean(), buf.readOptional(BlockPos.STREAM_CODEC), buf.readIdentifier(),
                    buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.view);
            buf.writeBoolean(this.own);
            buf.writeOptional(this.display, BlockPos.STREAM_CODEC);
            buf.writeIdentifier(this.world);
            buf.writeBoolean(this.hasSkyLight);
            buf.writeVarInt(this.skybox);
            buf.writeVarInt(this.cardinalLighting);
            buf.writeFloat(this.ambientLight);
            buf.writeVarInt(this.minY);
            buf.writeVarInt(this.height);
            buf.writeVarInt(this.horizontalRadius);
            buf.writeVarInt(this.verticalRadius);
        }

        @Override
        public Type<Begin> type() {
            return Begin.TYPE;
        }
    }

    /**
     * One 16 by 16 by 16 section of blocks, as {@link RemoteViewSections} encodes it.
     *
     * @param pos the section's position, as {@code SectionPos.asLong} packs it
     */
    public record Section(long pos, byte[] data) {

        public Section(final FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readByteArray());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeLong(this.pos);
            buf.writeByteArray(this.data);
        }
    }

    /**
     * Sections the client should take, each replacing what it had there, and sections it should forget.
     *
     * @param forgotten the positions of sections to forget, packed as {@link Section#pos()} is
     */
    public record Sections(int view, List<Section> sections, List<Long> forgotten) implements SGPacket {

        public static final Type<Sections> TYPE = new Type<>(SGCraft.id("remote_view_sections"));

        public Sections(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), SGPacket.readList(buf, Section::new), SGPacket.readList(buf,
                    FriendlyByteBuf::readLong));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.view);
            SGPacket.writeList(buf, this.sections, (out, section) -> section.write(out));
            SGPacket.writeList(buf, this.forgotten, FriendlyByteBuf::writeLong);
        }

        @Override
        public Type<Sections> type() {
            return Sections.TYPE;
        }
    }

    /**
     * A block entity as the client sees it.
     *
     * @param pos its position, as {@code BlockPos.asLong} packs it
     * @param type its type, by its id in the block entity type registry
     * @param tag the data it sends its clients
     */
    public record BlockEntityState(long pos, int type, CompoundTag tag) {

        public BlockEntityState(final FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), RemoteViewPackets.readTag(buf));
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeLong(this.pos);
            buf.writeVarInt(this.type);
            buf.writeNbt(this.tag);
        }
    }

    /**
     * Block entities that are new or have changed, and the positions of block entities that are gone.
     *
     * @param removed positions packed as {@link BlockEntityState#pos()} is
     */
    public record BlockEntities(int view, List<BlockEntityState> changed, List<Long> removed) implements SGPacket {

        public static final Type<BlockEntities> TYPE = new Type<>(SGCraft.id("remote_view_block_entities"));

        public BlockEntities(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), SGPacket.readList(buf, BlockEntityState::new), SGPacket.readList(buf,
                    FriendlyByteBuf::readLong));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.view);
            SGPacket.writeList(buf, this.changed, (out, entity) -> entity.write(out));
            SGPacket.writeList(buf, this.removed, FriendlyByteBuf::writeLong);
        }

        @Override
        public Type<BlockEntities> type() {
            return BlockEntities.TYPE;
        }
    }

    /**
     * Where the view's camera is and which way it looks.
     *
     * @param yaw in degrees, as Minecraft measures entity yaw
     * @param pitch in degrees, as Minecraft measures entity pitch
     * @param lamp whether the source lights up what it sees
     */
    public record Camera(double x, double y, double z, float yaw, float pitch, boolean lamp) {

        public Camera(final FriendlyByteBuf buf) {
            this(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(),
                    buf.readBoolean());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeFloat(this.yaw);
            buf.writeFloat(this.pitch);
            buf.writeBoolean(this.lamp);
        }
    }

    /**
     * The sky, light and weather where the camera is, as the server's world gives them. Colors are packed RGB, or
     * ARGB for {@link #sunriseColor()}; angles are in degrees.
     *
     * @param gameTime the world's game time
     * @param moonPhase the moon's phase, by its index
     * @param fogStart where the world's own fog starts, in blocks
     * @param fogEnd where the world's own fog is thickest, in blocks
     * @param skyFogEnd how far the sky's fog reaches, in blocks
     */
    public record Atmosphere(long gameTime, float rain, float thunder, int skyColor, int fogColor,
            int sunriseColor, float sunAngle, float moonAngle, float starAngle, float starBrightness, int moonPhase,
            float skyLightFactor, int skyLightColor, int ambientColor, int blockLightTint, float fogStart,
            float fogEnd, float skyFogEnd) {

        public Atmosphere(final FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(),
                    buf.readFloat(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeLong(this.gameTime);
            buf.writeFloat(this.rain);
            buf.writeFloat(this.thunder);
            buf.writeInt(this.skyColor);
            buf.writeInt(this.fogColor);
            buf.writeInt(this.sunriseColor);
            buf.writeFloat(this.sunAngle);
            buf.writeFloat(this.moonAngle);
            buf.writeFloat(this.starAngle);
            buf.writeFloat(this.starBrightness);
            buf.writeVarInt(this.moonPhase);
            buf.writeFloat(this.skyLightFactor);
            buf.writeInt(this.skyLightColor);
            buf.writeInt(this.ambientColor);
            buf.writeInt(this.blockLightTint);
            buf.writeFloat(this.fogStart);
            buf.writeFloat(this.fogEnd);
            buf.writeFloat(this.skyFogEnd);
        }
    }

    /**
     * Where an entity in view is, and how it is turned and posed. Angles are in degrees.
     *
     * @param id the entity's id in the source's world
     */
    public record EntityMove(int id, double x, double y, double z, float yaw, float pitch, float headYaw,
            float bodyYaw, boolean onGround) {

        public EntityMove(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.id);
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeFloat(this.yaw);
            buf.writeFloat(this.pitch);
            buf.writeFloat(this.headYaw);
            buf.writeFloat(this.bodyYaw);
            buf.writeBoolean(this.onGround);
        }
    }

    /**
     * The view as it stands this tick: the camera, the atmosphere and, on the ticks they are sent, where the
     * entities in view are.
     */
    public record Frame(int view, Camera camera, Atmosphere atmosphere, List<EntityMove> moves) implements SGPacket {

        public static final Type<Frame> TYPE = new Type<>(SGCraft.id("remote_view_frame"));

        public Frame(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), new Camera(buf), new Atmosphere(buf), SGPacket.readList(buf, EntityMove::new));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.view);
            this.camera.write(buf);
            this.atmosphere.write(buf);
            SGPacket.writeList(buf, this.moves, (out, move) -> move.write(out));
        }

        @Override
        public Type<Frame> type() {
            return Frame.TYPE;
        }
    }

    /**
     * An entity that has come into view, or whose looks have changed.
     *
     * @param id the entity's id in the source's world
     * @param type its type, by its id in the entity type registry
     * @param name a player's name, so their skin can be found; empty for anything else
     * @param data its synced data and what it wears and holds, as {@link RemoteViewEntityData} encodes them
     */
    public record EntityLooks(int id, int type, UUID uuid, Optional<String> name, byte[] data) {

        public EntityLooks(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readUUID(), buf.readOptional(FriendlyByteBuf::readUtf),
                    buf.readByteArray());
        }

        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.id);
            buf.writeVarInt(this.type);
            buf.writeUUID(this.uuid);
            buf.writeOptional(this.name, FriendlyByteBuf::writeUtf);
            buf.writeByteArray(this.data);
        }
    }

    /**
     * Entities that have come into view or changed their looks, and the ids of entities that have left it. Where
     * they are comes with each {@link Frame}.
     */
    public record Entities(int view, List<EntityLooks> changed, List<Integer> removed) implements SGPacket {

        public static final Type<Entities> TYPE = new Type<>(SGCraft.id("remote_view_entities"));

        public Entities(final FriendlyByteBuf buf) {
            this(buf.readVarInt(), SGPacket.readList(buf, EntityLooks::new), SGPacket.readList(buf,
                    FriendlyByteBuf::readVarInt));
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.view);
            SGPacket.writeList(buf, this.changed, (out, entity) -> entity.write(out));
            SGPacket.writeList(buf, this.removed, FriendlyByteBuf::writeVarInt);
        }

        @Override
        public Type<Entities> type() {
            return Entities.TYPE;
        }
    }

    /** A view has ended: the client drops all it has of it. */
    public record End(int view) implements SGPacket {

        public static final Type<End> TYPE = new Type<>(SGCraft.id("remote_view_end"));

        public End(final FriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        @Override
        public void write(final FriendlyByteBuf buf) {
            buf.writeVarInt(this.view);
        }

        @Override
        public Type<End> type() {
            return End.TYPE;
        }
    }

    private static CompoundTag readTag(final FriendlyByteBuf buf) {
        final CompoundTag tag = buf.readNbt();
        return tag == null ? new CompoundTag() : tag;
    }
}
