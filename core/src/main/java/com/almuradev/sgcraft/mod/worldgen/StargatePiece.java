/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 * It is based on the original SGCraft by gcewing.
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) 2016 gcewing <https://github.com/gcewing>
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

package com.almuradev.sgcraft.mod.worldgen;

import com.almuradev.sgcraft.mod.block.GateSpawnerBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.block.entity.GateSpawnerBlockEntity;
import com.almuradev.sgcraft.mod.mixin.ScatteredFeaturePieceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A gate added beside a structure: a template holding a gate spawner, which builds the gate once the world runs, and
 * whatever stands around it. The template turns about its own center, and a foundation can hold it up.
 */
public final class StargatePiece extends TemplateStructurePiece {

    /** The piece's type, under which it is saved with its structure. */
    public static final StructurePieceType TYPE = StargatePiece::new;

    // The side of each corner pillar of a foundation
    private static final int PILLAR = 2;

    private final Rotation rotation;
    private final StructureGate.Foundation foundation;
    private final BlockState foundationBlock;
    private final int chevrons;
    private final boolean tokra;
    // Built into a structure that settles onto the ground once it is built, the piece goes where the structure's
    // first piece settles, its top or its bottom moved by the offset; the first piece is only known until the
    // structure is saved, and the piece builds nothing if it is lost before the structure settles
    private boolean following;
    private boolean top;
    private int yOffset;
    private @Nullable StructurePiece anchor;

    StargatePiece(final StructureTemplateManager templates, final Identifier template, final BlockPos position,
            final Rotation rotation, final StructureGate.Foundation foundation, final BlockState foundationBlock,
            final int chevrons, final boolean tokra) {
        super(StargatePiece.TYPE, 0, templates, template, template.toString(),
                StargatePiece.settings(templates.getOrCreate(template), rotation), position);
        this.rotation = rotation;
        this.foundation = foundation;
        this.foundationBlock = foundationBlock;
        this.chevrons = chevrons;
        this.tokra = tokra;
    }

    private StargatePiece(final StructurePieceSerializationContext context, final CompoundTag tag) {
        super(StargatePiece.TYPE, tag, context.structureTemplateManager(), template -> StargatePiece.settings(
                context.structureTemplateManager().getOrCreate(template),
                tag.read("rotation", Rotation.CODEC).orElse(Rotation.NONE)));
        this.rotation = tag.read("rotation", Rotation.CODEC).orElse(Rotation.NONE);
        this.foundation = tag.read("foundation", StructureGate.Foundation.CODEC)
                .orElse(StructureGate.Foundation.NONE);
        this.foundationBlock = tag.read("foundation_block", BlockState.CODEC)
                .orElse(Blocks.STONE_BRICKS.defaultBlockState());
        this.chevrons = tag.getIntOr("chevrons", 0);
        this.tokra = tag.getBooleanOr("tokra", false);
        this.following = tag.getBooleanOr("following", false);
        this.top = tag.getBooleanOr("top", false);
        this.yOffset = tag.getIntOr("y_offset", 0);
    }

    /**
     * Has the piece follow a piece of its structure that settles onto the ground once it is built.
     *
     * @return this piece
     */
    StargatePiece follow(final StructurePiece anchor, final StructureGate.Ground ground, final int yOffset) {
        if (anchor instanceof ScatteredFeaturePiece && ground.heightmap().isEmpty()) {
            this.following = true;
            this.top = ground.top();
            this.yOffset = yOffset;
            this.anchor = anchor;
        }
        return this;
    }

    // Turned about the template's center, so turning it keeps it where it was put
    private static StructurePlaceSettings settings(final StructureTemplate template, final Rotation rotation) {
        final Vec3i size = template.getSize();
        return new StructurePlaceSettings().setRotation(rotation)
                .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2));
    }

    @Override
    protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.store("rotation", Rotation.CODEC, this.rotation);
        tag.store("foundation", StructureGate.Foundation.CODEC, this.foundation);
        tag.store("foundation_block", BlockState.CODEC, this.foundationBlock);
        tag.putInt("chevrons", this.chevrons);
        tag.putBoolean("tokra", this.tokra);
        tag.putBoolean("following", this.following);
        tag.putBoolean("top", this.top);
        tag.putInt("y_offset", this.yOffset);
    }

    @Override
    public void postProcess(final WorldGenLevel level, final StructureManager structureManager,
            final ChunkGenerator generator, final RandomSource random, final BoundingBox chunkBB,
            final ChunkPos chunkPos, final BlockPos referencePos) {
        if (this.following) {
            // The structure's piece settles in its own first chunk, which comes before this piece's
            if (!(this.anchor instanceof ScatteredFeaturePieceAccessor settling)
                    || settling.sgcraft$heightPosition() < 0) {
                return;
            }
            final BoundingBox settled = this.anchor.getBoundingBox();
            this.templatePosition = new BlockPos(this.templatePosition.getX(),
                    (this.top ? settled.maxY() : settled.minY()) + this.yOffset, this.templatePosition.getZ());
            this.following = false;
            this.anchor = null;
        }
        super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
        this.buildFoundation(level, chunkBB);
        // The spawner builds the gate with the chevrons chosen for this structure, and a Tok'ra if it has one
        for (final GateSpawnerBlock spawner : SGBlocks.SPAWNERS) {
            for (final StructureTemplate.StructureBlockInfo info : this.template.filterBlocks(this.templatePosition,
                    this.placeSettings, spawner)) {
                if (chunkBB.isInside(info.pos())
                        && level.getBlockEntity(info.pos()) instanceof GateSpawnerBlockEntity entity) {
                    entity.chevrons(this.chevrons);
                    entity.tokra(this.tokra);
                }
            }
        }
    }

    // Fills down from under the template's bottom layer until the ground is solid
    private void buildFoundation(final WorldGenLevel level, final BoundingBox chunkBB) {
        if (this.foundation == StructureGate.Foundation.NONE) {
            return;
        }
        final BoundingBox box = this.getBoundingBox();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (this.foundation == StructureGate.Foundation.CORNERS && !StargatePiece.corner(box, x, z)) {
                    continue;
                }
                final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, box.minY() - 1, z);
                if (!chunkBB.isInside(pos) || level.getBlockState(pos.above()).isAir()) {
                    continue;
                }
                while (pos.getY() > level.getMinY() && StargatePiece.open(level.getBlockState(pos))) {
                    level.setBlock(pos, this.foundationBlock, Block.UPDATE_CLIENTS);
                    pos.move(0, -1, 0);
                }
            }
        }
    }

    private static boolean corner(final BoundingBox box, final int x, final int z) {
        return (x < box.minX() + StargatePiece.PILLAR || x > box.maxX() - StargatePiece.PILLAR)
                && (z < box.minZ() + StargatePiece.PILLAR || z > box.maxZ() - StargatePiece.PILLAR);
    }

    private static boolean open(final BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    @Override
    protected void handleDataMarker(final String markerId, final BlockPos position, final ServerLevelAccessor level,
            final RandomSource random, final BoundingBox chunkBB) {
    }

    /** @return whether any of the pieces is in the way of this one */
    boolean collides(final List<StructurePiece> pieces) {
        return pieces.stream().anyMatch(piece -> piece.getBoundingBox().intersects(this.getBoundingBox()));
    }
}
