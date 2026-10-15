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

package com.almuradev.sgcraft.mod.mixin.client;

import com.almuradev.sgcraft.mod.client.view.RemoteViewDisplays;
import com.almuradev.sgcraft.mod.client.view.RemoteViewRenderer;
import com.almuradev.sgcraft.mod.client.view.RemoteViewSnapshot;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands out a remote view's own lightmap while the view is drawn, so the game's entity and block entity renderers
 * light what they draw into the view by the view's light, rather than by the player's own world's or the screen's;
 * and draws the views on displays in the world, and any snapshot of the player's own view asked for, once the world
 * is drawn, before the screen.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "lightmap", at = @At("HEAD"), cancellable = true)
    private void sgcraft$remoteViewLightmap(final CallbackInfoReturnable<GpuTextureView> callback) {
        final GpuTextureView lightmap = RemoteViewRenderer.lightmapOverride();
        if (lightmap != null) {
            callback.setReturnValue(lightmap);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void sgcraft$drawDisplays(final CallbackInfo callback) {
        RemoteViewDisplays.render();
        RemoteViewSnapshot.render();
    }
}
