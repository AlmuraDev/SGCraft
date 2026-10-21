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

package com.almuradev.sgcraft.mod.obelisk;

import com.almuradev.sgcraft.mod.access.SGGateAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Who owns an Asgard obelisk or a ring platform, and who may use and configure it, as 2.2.0's: its owner, whoever
 * placed it, may always; anyone else may use it by default, unless listed as not allowed, or only if listed, and
 * configures it only if listed as its admin. Operators always may. Whatever holds it saves it and marks itself
 * changed.
 */
public final class TransportAccess {

    private @Nullable UUID owner;
    private String ownerName = "";
    private boolean everyone = true;
    private final Map<UUID, ObeliskAccess> access = new LinkedHashMap<>();

    /** @return its owner, if it has one */
    public Optional<UUID> owner() {
        return Optional.ofNullable(this.owner);
    }

    /** @return its owner's name when they placed it */
    public String ownerName() {
        return this.ownerName;
    }

    public void setOwner(final UUID owner, final String name) {
        this.owner = owner;
        this.ownerName = name;
    }

    /** @return whether anyone may use it, save those listed as not allowed */
    public boolean everyone() {
        return this.everyone;
    }

    /** @return the players on its list */
    public List<ObeliskAccess> entries() {
        return List.copyOf(this.access.values());
    }

    public void set(final boolean everyone, final List<ObeliskAccess> entries) {
        this.everyone = everyone;
        this.access.clear();
        entries.forEach(entry -> this.access.put(entry.player(), entry));
    }

    /** @return whether a player may use it: its owner, an operator, or anyone the list or the default lets */
    public boolean mayUse(final Player player) {
        if (this.isOwner(player) || SGGateAccess.isOperator(player)) {
            return true;
        }
        final ObeliskAccess entry = this.access.get(player.getUUID());
        return this.everyone ? entry == null || entry.use() : entry != null && entry.use();
    }

    /**
     * @return whether a player may link it and change who may use it: its owner, an operator, or one listed as its
     *     admin. One with no owner only operators may, or anyone could take it over
     */
    public boolean mayConfigure(final Player player) {
        if (this.isOwner(player) || SGGateAccess.isOperator(player)) {
            return true;
        }
        final ObeliskAccess entry = this.access.get(player.getUUID());
        return entry != null && entry.admin();
    }

    private boolean isOwner(final Player player) {
        return this.owner != null && this.owner.equals(player.getUUID());
    }

    public void load(final ValueInput input) {
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getStringOr("owner_name", "");
        this.everyone = input.getBooleanOr("everyone", true);
        this.access.clear();
        input.read("access", ObeliskAccess.CODEC.listOf()).orElse(List.of())
                .forEach(entry -> this.access.put(entry.player(), entry));
    }

    public void save(final ValueOutput output) {
        if (this.owner != null) {
            output.store("owner", UUIDUtil.CODEC, this.owner);
        }
        output.putString("owner_name", this.ownerName);
        output.putBoolean("everyone", this.everyone);
        output.store("access", ObeliskAccess.CODEC.listOf(), List.copyOf(this.access.values()));
    }
}
