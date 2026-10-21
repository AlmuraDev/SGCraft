# Asgard Obelisk

An Asgard obelisk beams a player to the obelisk it is linked to, for free and across worlds, as in SGCraft 2.2.0. There are two kinds: the Asgard Transport Obelisk, a gray stone pillar with Thor's hammer carved on top, as on Cimmeria, and the Asgard Knotwork Obelisk, a tapering sandstone pillar carved with knotwork. Each links only with its own kind.

```infobox
Asgard Obelisk
ID: sgcraft:asgard_obelisk, sgcraft:asgard_obelisk_knotwork
Category: Transport
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 3
Blast resistance: 15
Waterloggable: No
Drops: Itself, from its bottom half
Block entity: Yes
Size: Two blocks tall
```

## Obtaining

### Crafting

```recipe
sgcraft:asgard_obelisk
```

```recipe
sgcraft:asgard_obelisk_knotwork
```

## Placing

Place it where there is room for both its halves. Its red gem faces you, on its top half. Whoever places it owns it. Breaking either half breaks the whole obelisk.

## Linking

1. Sneak and use the first obelisk with an empty hand.
2. Sneak and use a second obelisk of the same kind. The two are now linked, each to the other, even in different worlds.

Sneak-use the first one again instead to cancel. Linking an obelisk that was already linked unlinks its old partner. You need to be allowed to configure both obelisks to link them: their owner, an operator, or someone they list as an admin.

## Beaming

1. Stand within 4 blocks of an obelisk and use its gem with an empty hand.
2. Stand still. You hear the beam, the gem glows brighter and whiter, and your screen fades to white.
3. Two and a half seconds later you arrive 2 blocks out in front of the other obelisk, facing it.

Moving off the spot before the beam calls it off. After arriving, you can't beam again for a second and a half. If the other obelisk is gone, the obelisk unlinks and says so.

## Who may use it

By default anyone may use an obelisk, save the players its list marks as not allowed. Its owner can switch it so only they and the players listed as allowed may use it. The owner and operators may always use and configure it. An obelisk with no owner can be configured only by operators.

## Configuring

Use the obelisk with a [Stargate Configurator](../items/configurator.md) in your hand. Its screen shows:

- The obelisk it is linked to, as its world and place, with **Unlink**.
- Its owner.
- Whether anyone may use it, or only its owner and those listed.
- The players on its list, five at a time, each with **Use** and **Admin** switches and an **x** button to take them off. An admin may configure it and link it.
- **Player name** and **Add player**, which lists a player who is online now, allowed to use it.

**Save** keeps the changes. A list holds up to 64 players.
