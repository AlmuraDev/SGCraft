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

package com.almuradev.sgcraft.block;

import com.almuradev.sgcraft.gate.FluidSpill;
import com.almuradev.sgcraft.gate.GateFrame;
import com.almuradev.sgcraft.gate.GateLight;
import com.almuradev.sgcraft.gate.GateShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The open wormhole's event horizon, where nothing else is in a gate's opening: unseen, without a shape and not to be
 * broken, so travelers and light pass through it. Fluid flows into the horizon of a gate sending through the
 * wormhole, which takes it in, and the gate counts what runs into its front as fluid in its opening; the horizon of a
 * gate not sending holds fluid back, as a wall would. Fluid so goes into and comes out of the front of a gate only, and
 * never runs through a gate from front to back. Nothing replaces the horizon, not even fluid from a bucket: a bucket
 * emptied into the front of a sending horizon pours in front of it, to run in from there, and one emptied anywhere
 * else is refused.
 *
 * <p>The horizon lights the world round the gate, its light rising and falling as waves run across it; see
 * {@link GateLight#horizon}.</p>
 */
public final class EventHorizonBlock extends Block implements LiquidBlockContainer {

    /** Whether the gate whose opening this is sends through the wormhole, and so takes in fluid. */
    public static final BooleanProperty SENDING = BooleanProperty.create("sending");
    /** How bright the horizon is here, as a light block's level. */
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public EventHorizonBlock(final Properties properties) {
        super(properties.lightLevel(state -> state.getValue(EventHorizonBlock.LEVEL)));
        this.registerDefaultState(this.defaultBlockState().setValue(EventHorizonBlock.SENDING, false)
                .setValue(BlockStateProperties.FACING, Direction.NORTH).setValue(EventHorizonBlock.LEVEL, 0));
    }

    /**
     * Fills the empty blocks of a gate's opening with the event horizon, or makes the horizon already there send or
     * not.
     *
     * @param sending whether the gate sends through the wormhole
     * @param time the game time, which the horizon's light follows
     */
    public static void seal(final Level level, final GateFrame frame, final boolean sending, final long time) {
        final BlockState horizon = SGBlocks.EVENT_HORIZON.defaultBlockState().setValue(EventHorizonBlock.SENDING,
                sending).setValue(BlockStateProperties.FACING, frame.front());
        EventHorizonBlock.forEachInOpening(frame, (pos, x, y) -> {
            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                level.setBlock(pos, horizon.setValue(EventHorizonBlock.LEVEL,
                        GateLight.horizon(frame.shape(), x, y, time)), Block.UPDATE_ALL);
            } else if (state.getBlock() instanceof EventHorizonBlock) {
                // Keeping its light, which moves on by itself
                final BlockState sealed = horizon.setValue(EventHorizonBlock.LEVEL,
                        state.getValue(EventHorizonBlock.LEVEL));
                if (state != sealed) {
                    level.setBlock(pos, sealed, Block.UPDATE_ALL);
                }
            }
        });
    }

    /**
     * Moves the light of a gate's horizon on with its waves, changing only the blocks whose light changes.
     *
     * @param time the game time, which the horizon's light follows
     * @param dark whether the horizon gives no light, as behind a shut iris
     */
    public static void shimmer(final Level level, final GateFrame frame, final long time, final boolean dark) {
        EventHorizonBlock.forEachInOpening(frame, (pos, x, y) -> {
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof EventHorizonBlock) {
                final int light = dark ? 0 : GateLight.horizon(frame.shape(), x, y, time);
                if (state.getValue(EventHorizonBlock.LEVEL) != light) {
                    level.setBlock(pos, state.setValue(EventHorizonBlock.LEVEL, light), GateLight.LIGHT_UPDATE);
                }
            }
        });
    }

    /** @return whether a block is the horizon of a gate sending through the wormhole */
    public static boolean isSending(final BlockState state) {
        return state.getBlock() instanceof EventHorizonBlock && state.getValue(EventHorizonBlock.SENDING);
    }

    /** Takes the event horizon out of a gate's opening again, leaving it empty. */
    public static void unseal(final Level level, final GateFrame frame) {
        EventHorizonBlock.forEachInOpening(frame, (pos, x, y) -> {
            if (level.getBlockState(pos).getBlock() instanceof EventHorizonBlock) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
    }

    private static void forEachInOpening(final GateFrame frame, final OpeningAction action) {
        final GateShape shape = frame.shape();
        for (int y = 1; y < shape.height() - 1; y++) {
            for (int x = -shape.radius(); x <= shape.radius(); x++) {
                if (FluidSpill.inOpening(shape, x, y)) {
                    action.accept(frame.block(x, y, 0), x, y);
                }
            }
        }
    }

    // Something done to a block of a gate's opening, given where it is in the gate's frame
    @FunctionalInterface
    private interface OpeningAction {

        void accept(BlockPos pos, int x, int y);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EventHorizonBlock.SENDING, BlockStateProperties.FACING, EventHorizonBlock.LEVEL);
    }

    // Fluid flowing in may come from either side, as the engine doesn't say which; whoever empties a bucket into it
    // must stand in front, where there is room for the fluid
    @Override
    public boolean canPlaceLiquid(final @Nullable LivingEntity user, final BlockGetter level, final BlockPos pos,
            final BlockState state, final Fluid type) {
        if (!state.getValue(EventHorizonBlock.SENDING)) {
            return false;
        }
        if (user == null) {
            return true;
        }
        final Direction facing = state.getValue(BlockStateProperties.FACING);
        final Vec3 toUser = user.position().subtract(Vec3.atCenterOf(pos));
        return toUser.dot(Vec3.atLowerCornerOf(facing.getUnitVec3i())) > 0
                && level.getBlockState(pos.relative(facing)).isAir();
    }

    // Flowing fluid goes into the wormhole, and the gate measures it where it runs in; a source, from a bucket, is put
    // in front, to run in from there
    @Override
    public boolean placeLiquid(final LevelAccessor level, final BlockPos pos, final BlockState state,
            final FluidState fluidState) {
        if (!state.getValue(EventHorizonBlock.SENDING)) {
            return false;
        }
        final BlockPos front = pos.relative(state.getValue(BlockStateProperties.FACING));
        if (fluidState.isSource() && level.getBlockState(front).isAir()) {
            level.setBlock(front, fluidState.createLegacyBlock(), Block.UPDATE_ALL);
        }
        return true;
    }

    // Unlike air, fluid never takes its place
    @Override
    protected boolean canBeReplaced(final BlockState state, final Fluid fluid) {
        return false;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state) {
        return true;
    }
}
