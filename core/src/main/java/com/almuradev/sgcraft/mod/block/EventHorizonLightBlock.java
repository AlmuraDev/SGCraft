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

package com.almuradev.sgcraft.mod.block;

import com.almuradev.sgcraft.mod.gate.GateBackLight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light of an open wormhole seen from behind a gate whose iris is shut, as in 2.2.0. The iris covers the horizon,
 * which so gives no light, and these unseen blocks just behind the gate light its back instead.
 *
 * <p>They behave as air: nothing collides with them, they can't be clicked, any block can be placed into them, and
 * pistons and fluids clear them. The gate places and takes them away (see {@link GateBackLight}), and each checks
 * every few seconds that a gate still wants it, taking itself away if not, so none is left behind when a gate's chunk
 * unloads, the gate breaks or the server stops at the wrong moment.</p>
 */
public final class EventHorizonLightBlock extends Block {

    /** How bright the block is, as a light block's level. */
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    // How often each block checks that a gate still wants it, in ticks
    private static final int CHECK_TICKS = 40;

    public EventHorizonLightBlock(final Properties properties) {
        super(properties.lightLevel(state -> state.getValue(EventHorizonLightBlock.LEVEL)));
        this.registerDefaultState(this.defaultBlockState().setValue(EventHorizonLightBlock.LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EventHorizonLightBlock.LEVEL);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState,
            final boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, EventHorizonLightBlock.CHECK_TICKS);
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos,
            final RandomSource random) {
        if (GateBackLight.wanted(level, pos)) {
            level.scheduleTick(pos, this, EventHorizonLightBlock.CHECK_TICKS);
        } else {
            level.removeBlock(pos, false);
        }
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
}
