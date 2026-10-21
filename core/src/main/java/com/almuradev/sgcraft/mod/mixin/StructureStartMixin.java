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

package com.almuradev.sgcraft.mod.mixin;

import com.almuradev.sgcraft.mod.worldgen.StargatePiece;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// Keeps a gate added beside a structure as it was when the structure is loaded from its save: an ocean monument
// rebuilds its pieces then and would leave the gate out, and a gate settling with its structure finds the piece it
// settles with again
@Mixin(StructureStart.class)
abstract class StructureStartMixin {

    @Redirect(method = "loadStaticStart", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/structures/OceanMonumentStructure;"
                    + "regeneratePiecesAfterLoad(Lnet/minecraft/world/level/ChunkPos;J"
                    + "Lnet/minecraft/world/level/levelgen/structure/pieces/PiecesContainer;)"
                    + "Lnet/minecraft/world/level/levelgen/structure/pieces/PiecesContainer;"))
    private static PiecesContainer sgcraft$keepGate(final ChunkPos chunk, final long seed,
            final PiecesContainer saved) {
        final PiecesContainer rebuilt = OceanMonumentStructure.regeneratePiecesAfterLoad(chunk, seed, saved);
        final List<StructurePiece> pieces = new ArrayList<>(rebuilt.pieces());
        saved.pieces().stream().filter(StargatePiece.class::isInstance).forEach(pieces::add);
        return new PiecesContainer(pieces);
    }

    @Inject(method = "loadStaticStart", at = @At("RETURN"))
    private static void sgcraft$reanchor(final StructurePieceSerializationContext context, final CompoundTag tag,
            final long seed, final CallbackInfoReturnable<StructureStart> cir) {
        final StructureStart start = cir.getReturnValue();
        if (start != null) {
            StargatePiece.reanchor(start.getPieces());
        }
    }
}
