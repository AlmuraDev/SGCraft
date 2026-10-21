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

package com.almuradev.sgcraft.neoforge.test;

import com.almuradev.sgcraft.api.ActionResult;
import com.almuradev.sgcraft.api.Cause;
import com.almuradev.sgcraft.api.SGCraftApi;
import com.almuradev.sgcraft.api.access.GateRight;
import com.almuradev.sgcraft.api.address.Address;
import com.almuradev.sgcraft.api.addressbook.AddressBook;
import com.almuradev.sgcraft.api.event.EventBus;
import com.almuradev.sgcraft.api.event.gate.ChangeGateOwnerEvent;
import com.almuradev.sgcraft.api.gate.GateProperties;
import com.almuradev.sgcraft.api.gate.GateTypes;
import com.almuradev.sgcraft.api.gate.Stargate;
import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGCraft;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.addressbook.ItemAddressBook;
import com.almuradev.sgcraft.mod.addressbook.ListAddressBook;
import com.almuradev.sgcraft.mod.block.GateSpawnerBlock;
import com.almuradev.sgcraft.mod.block.SGBlocks;
import com.almuradev.sgcraft.mod.gate.GateFormation;
import com.almuradev.sgcraft.mod.gate.GateFrame;
import com.almuradev.sgcraft.mod.gate.GateProtection;
import com.almuradev.sgcraft.mod.gate.GateShape;
import com.almuradev.sgcraft.mod.gate.SGStargates;
import com.almuradev.sgcraft.mod.item.SGItems;
import com.almuradev.sgcraft.mod.network.SGPacket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Game tests of gate owners and rights, who may break a gate, and the server's values for gates. They run with
 * {@link SGGameTestsNeoForge}, which registers them.
 */
final class SGAccessGameTestsNeoForge {

    // Where a test's second and third gates go, clear of its first
    private static final BlockPos B = new BlockPos(128, 0, 0);
    private static final BlockPos C = new BlockPos(0, 0, 48);

    private SGAccessGameTestsNeoForge() {
    }

    // The server's value fills in for a gate with none of its own, and not for one with its own
    static void gateDefaults(final GameTestHelper helper, final BlockPos origin) {
        final SGStargates stargates = SGServer.require().stargates();
        final Map<Identifier, JsonElement> before = SGServer.require().config().gateDefaults();
        final Stargate plain = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate own = SGGameTestsNeoForge.place(helper, origin.offset(SGAccessGameTestsNeoForge.B), 9);
        own.set(GateProperties.OPEN_TICKS, 100, SGGameTestsNeoForge.server(helper));
        final JsonArray useOnly = new JsonArray();
        useOnly.add("sgcraft:use");
        stargates.setGateDefaults(Map.of(SGCraft.id("open_ticks"), new JsonPrimitive(1234),
                SGCraft.id("default_rights"), useOnly));
        try {
            helper.assertTrue(plain.get(GateProperties.OPEN_TICKS).orElse(0) == 1234,
                    "A gate without its own value has " + plain.get(GateProperties.OPEN_TICKS));
            helper.assertTrue(own.get(GateProperties.OPEN_TICKS).orElse(0) == 100,
                    "A gate's own value gave way to the server's: " + own.get(GateProperties.OPEN_TICKS));
            own.remove(GateProperties.OPEN_TICKS, SGGameTestsNeoForge.server(helper));
            helper.assertTrue(own.get(GateProperties.OPEN_TICKS).orElse(0) == 1234,
                    "Clearing a gate's own value didn't give it the server's");
            final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            helper.assertTrue(plain.access().allows(Cause.of(player), GateRight.USE)
                    && !plain.access().allows(Cause.of(player), GateRight.IRIS),
                    "The server's default rights don't apply");
        } finally {
            stargates.setGateDefaults(before);
        }
        helper.assertTrue(plain.get(GateProperties.OPEN_TICKS).orElse(0) == 6000,
                "Without the server's value, the gate doesn't have the property's default");
        helper.succeed();
    }

    // A gate a player builds gets the server's iris and nine chevrons as it forms, and breaking it gives back no
    // upgrades for them; a gate placed whole gets the iris and keeps the chevrons it was placed with
    static void formationDefaults(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final SGStargates stargates = SGServer.require().stargates();
        final Map<Identifier, JsonElement> before = SGServer.require().config().gateDefaults();
        stargates.setGateDefaults(Map.of(SGConfig.CHEVRONS, new JsonPrimitive(9), SGCraft.id("iris"),
                new JsonPrimitive("sgcraft:metal")));
        try {
            level.getChunk(origin);
            GateFormation.buildRing(level, new GateFrame(origin, Direction.NORTH, GateShape.STANDARD));
            level.setBlock(origin, SGBlocks.STARGATE_BASE.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            final Stargate built = SGCraftApi.get().stargates().at(GlobalPos.of(level.dimension(), origin))
                    .orElseThrow(() -> helper.assertionException(Component.literal("The gate didn't form")));
            helper.assertTrue(built.chevrons() == 9, "The built gate has " + built.chevrons() + " chevrons");
            helper.assertTrue(built.iris().isPresent(), "The built gate has no iris");
            final Stargate placed = SGGameTestsNeoForge.place(helper, origin.offset(SGAccessGameTestsNeoForge.B), 7);
            helper.assertTrue(placed.chevrons() == 7, "The placed gate has " + placed.chevrons() + " chevrons");
            helper.assertTrue(placed.iris().isPresent(), "The placed gate has no iris");
            level.destroyBlock(origin, false);
            final List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
            helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(SGItems.CHEVRON_UPGRADE)
                    || drop.getItem().is(SGItems.IRIS_UPGRADE)), "Breaking the gate gave back upgrades it came with");
        } finally {
            stargates.setGateDefaults(before);
        }
        helper.succeed();
    }

    // The player who finishes a gate owns it, as does one whose spawner builds it; a gate a structure's spawner
    // builds has no owner
    static void builderOwns(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        level.getChunk(origin);
        level.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        GateFormation.buildRing(level, new GateFrame(origin, Direction.NORTH, GateShape.STANDARD));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // In front of the ring, facing it, so the base faces the way the ring does
        player.snapTo(Vec3.atCenterOf(origin.north(3)), 0, 30);
        final ItemStack stack = new ItemStack(SGBlocks.STARGATE_BASE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, new BlockHitResult(
                Vec3.atCenterOf(origin.below()).add(0, 0.5, 0), Direction.UP, origin.below(), false));
        final GateSpawnerBlock spawner = GateSpawnerBlock.forGate(GateTypes.MILKY_WAY, false).orElseThrow();
        final BlockPos generated = origin.offset(SGAccessGameTestsNeoForge.B);
        final BlockPos theirs = origin.offset(SGAccessGameTestsNeoForge.C);
        for (final BlockPos pos : List.of(generated, theirs)) {
            level.getChunk(pos);
            level.setBlock(pos, spawner.defaultBlockState(), Block.UPDATE_ALL);
        }
        spawner.build(level, generated, null, 7, List.of());
        spawner.build(level, theirs, player, 9, List.of());
        helper.succeedWhen(() -> {
            final Stargate built = SGAccessGameTestsNeoForge.at(helper, origin);
            helper.assertTrue(built.owner().equals(Optional.of(player.getUUID())),
                    "The player who finished the gate doesn't own it: " + built.owner());
            helper.assertTrue(SGAccessGameTestsNeoForge.at(helper, generated).owner().isEmpty(),
                    "A structure's gate has an owner");
            helper.assertTrue(SGAccessGameTestsNeoForge.at(helper, theirs).owner()
                    .equals(Optional.of(player.getUUID())),
                    "The player whose spawner built the gate doesn't own it");
        });
    }

    // Another player changes a gate's settings only with the configure right, which the owner gives; only the owner
    // and operators change who may do what, and hand the gate over
    static void configureRights(final GameTestHelper helper, final BlockPos origin) {
        final Stargate gate = SGGameTestsNeoForge.place(helper, origin, 9);
        final Cause server = SGGameTestsNeoForge.server(helper);
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final ServerPlayer other = helper.makeMockServerPlayerInLevel();
        final ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(gate.owner().isEmpty(), "A gate placed by the server has an owner");
        SGAccessGameTestsNeoForge.expect(helper, gate.setOwner(Optional.of(owner.getUUID()), Cause.of(other)),
                ActionResult.DENIED, "A player claimed a gate with no owner");
        SGAccessGameTestsNeoForge.expect(helper, gate.setOwner(Optional.of(owner.getUUID()), server), ActionResult.DONE,
                "The server couldn't give the gate an owner");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.OPEN_TICKS, 100, Cause.of(other)),
                ActionResult.DENIED, "A player without the configure right changed a setting");
        helper.assertTrue(gate.access().allows(Cause.of(other), GateRight.USE)
                && gate.access().allows(Cause.of(other), GateRight.IRIS), "Players can't use the gate and its iris");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.OPEN_TICKS, 100, Cause.of(owner)),
                ActionResult.DONE, "The owner couldn't change a setting");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.PLAYER_RIGHTS, Map.of(other.getUUID(),
                Set.of(GateRight.USE, GateRight.CONFIGURE)), Cause.of(owner)), ActionResult.DONE,
                "The owner couldn't give a player rights");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.OPEN_TICKS, 200, Cause.of(other)),
                ActionResult.DONE, "A player with the configure right couldn't change a setting");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.DEFAULT_RIGHTS, Set.of(), Cause.of(other)),
                ActionResult.DENIED, "The configure right let a player change the default rights");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.PLAYER_RIGHTS, Map.of(), Cause.of(other)),
                ActionResult.DENIED, "The configure right let a player change the players' rights");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.BREAKABLE, true, Cause.of(other)),
                ActionResult.DENIED, "The configure right let a player make the gate breakable");
        SGAccessGameTestsNeoForge.expect(helper, gate.setOwner(Optional.of(other.getUUID()), Cause.of(other)),
                ActionResult.DENIED, "The configure right let a player take the gate");
        SGAccessGameTestsNeoForge.op(operator);
        final EventBus.Registration listener = SGCraftApi.get().eventBus().register(ChangeGateOwnerEvent.Pre.class,
                event -> event.setCanceled(event.gate().equals(gate)));
        try {
            SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.DEFAULT_RIGHTS, Set.of(GateRight.USE),
                    Cause.of(operator)), ActionResult.DONE, "An operator couldn't change the default rights");
            SGAccessGameTestsNeoForge.expect(helper, gate.setOwner(Optional.of(other.getUUID()), Cause.of(operator)),
                    ActionResult.CANCELED, "A listener didn't stop the owner changing");
        } finally {
            listener.unregister();
            operator.level().getServer().getPlayerList().deop(operator.nameAndId());
        }
        SGAccessGameTestsNeoForge.expect(helper, gate.setOwner(Optional.of(other.getUUID()), Cause.of(owner)),
                ActionResult.DONE, "The owner couldn't hand the gate over");
        helper.assertTrue(gate.owner().equals(Optional.of(other.getUUID())), "The gate has the wrong owner");
        SGAccessGameTestsNeoForge.expect(helper, gate.set(GateProperties.OPEN_TICKS, 300, Cause.of(owner)),
                ActionResult.DENIED, "The old owner kept their rights");
        helper.succeed();
    }

    // Operators break any gate; owners break theirs while the server allows it; anyone breaks a breakable gate,
    // which a generated gate is when the server says so and it has no value of its own
    static void breaking(final GameTestHelper helper, final BlockPos origin) {
        final ServerLevel level = helper.getLevel();
        final SGStargates stargates = SGServer.require().stargates();
        final SGConfig config = SGServer.require().config();
        final boolean owners = config.ownersBreakGates();
        final boolean generated = config.generatedGatesBreakable();
        final Cause server = SGGameTestsNeoForge.server(helper);
        final Stargate built = SGGameTestsNeoForge.place(helper, origin, 9);
        final Stargate world = SGGameTestsNeoForge.place(helper, origin.offset(SGAccessGameTestsNeoForge.B), 9);
        world.set(GateProperties.GENERATED, true, server);
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final ServerPlayer other = helper.makeMockServerPlayerInLevel();
        final ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        built.setOwner(Optional.of(owner.getUUID()), server);
        final BlockPos mine = built.position().pos();
        final BlockPos wild = world.position().pos();
        SGAccessGameTestsNeoForge.op(operator);
        try {
            stargates.setBreaking(true, false);
            helper.assertTrue(GateProtection.mayBreak(owner, level, mine), "The owner can't break their gate");
            helper.assertTrue(!GateProtection.mayBreak(other, level, mine), "Another player can break the gate");
            helper.assertTrue(GateProtection.mayBreak(operator, level, mine), "An operator can't break the gate");
            stargates.setBreaking(false, false);
            helper.assertTrue(!GateProtection.mayBreak(owner, level, mine),
                    "The owner can break their gate with owners' breaking off");
            built.set(GateProperties.BREAKABLE, true, Cause.of(owner));
            helper.assertTrue(GateProtection.mayBreak(other, level, mine) && GateProtection.mayBreak(owner, level,
                    mine), "A breakable gate can't be broken");
            helper.assertTrue(!GateProtection.mayBreak(other, level, wild), "A generated gate can be broken");
            helper.assertTrue(GateProtection.mayBreak(operator, level, wild),
                    "An operator can't break a generated gate");
            stargates.setBreaking(false, true);
            helper.assertTrue(GateProtection.mayBreak(other, level, wild),
                    "A generated gate can't be broken with the setting on");
            world.set(GateProperties.BREAKABLE, false, server);
            helper.assertTrue(!GateProtection.mayBreak(other, level, wild),
                    "A generated gate's own value gave way to the setting");
        } finally {
            stargates.setBreaking(owners, generated);
            level.getServer().getPlayerList().deop(operator.nameAndId());
        }
        helper.succeed();
    }

    // Makes a player an operator; the test server's own operators have no permissions, so this one is a game master
    private static void op(final ServerPlayer player) {
        player.level().getServer().getPlayerList().op(player.nameAndId(),
                Optional.of(LevelBasedPermissionSet.GAMEMASTER), Optional.empty());
    }

    private static Stargate at(final GameTestHelper helper, final BlockPos pos) {
        return SGCraftApi.get().stargates().at(GlobalPos.of(helper.getLevel().dimension(), pos))
                .orElseThrow(() -> helper.assertionException(Component.literal("No gate formed at " + pos)));
    }

    // Lists a client can grow are capped: a gate's player and address lists, an address book, a list in a packet;
    // and an energy setting can't be infinite
    static void entryLimits(final GameTestHelper helper, final BlockPos origin) {
        final Map<UUID, Set<GateRight>> players = new HashMap<>();
        for (int i = 0; i <= GateProperties.MAX_ENTRIES; i++) {
            players.put(new UUID(0, i), Set.of(GateRight.USE));
        }
        helper.assertTrue(GateProperties.PLAYER_RIGHTS.codec().encodeStart(JsonOps.INSTANCE, players).isError(),
                "A gate took " + players.size() + " players");
        players.remove(new UUID(0, 0));
        helper.assertTrue(GateProperties.PLAYER_RIGHTS.codec().encodeStart(JsonOps.INSTANCE, players).isSuccess(),
                "A gate refused " + players.size() + " players");
        helper.assertTrue(GateProperties.ENERGY_PER_NAQUADAH.codec()
                .encodeStart(JsonOps.INSTANCE, Double.POSITIVE_INFINITY).isError(), "Energy took infinity");
        final AddressBook book = ItemAddressBook.of(new ItemStack(SGItems.PDD));
        final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < ListAddressBook.MAX_ENTRIES; i++) {
            final Address address = Address.parse("A" + letters.charAt(i / 26) + letters.charAt(i % 26)).orElseThrow();
            SGAccessGameTestsNeoForge.expect(helper, book.add(address, Component.literal("Gate"), Cause.of(helper.getLevel())),
                    ActionResult.DONE, "Adding address " + i);
        }
        SGAccessGameTestsNeoForge.expect(helper, book.add(Address.parse("BAA").orElseThrow(),
                Component.literal("Gate"), Cause.of(helper.getLevel())), ActionResult.NOT_POSSIBLE, "A full book took another");
        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(1_000_000_000);
        buf.writeByte(0);
        try {
            SGPacket.readList(buf, FriendlyByteBuf::readByte);
            helper.fail("A packet's list claimed a billion elements and was read");
        } catch (final DecoderException expected) {
            // Refused before anything was allocated for it
        }
        helper.succeed();
    }

    private static void expect(final GameTestHelper helper, final ActionResult result, final ActionResult expected,
            final String message) {
        helper.assertTrue(result == expected, message + ": " + result);
    }
}
