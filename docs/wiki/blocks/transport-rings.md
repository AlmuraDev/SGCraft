# Transport Rings

Goa'uld transport rings swap whatever stands inside one set of rings with whatever stands inside another, in the same world, as in SGCraft 2.2.0. A ring platform sits in a floor, or in a ceiling, at the middle of its five rings. A Ring Control Panel on a wall nearby, or a Ring Remote in your hand, starts them. The platform that starts a transport pays for it in gate energy (GE).

```infobox
Ring Platform
ID: sgcraft:ring_platform, sgcraft:ring_platform_ceiling
Category: Transport
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 5
Blast resistance: 20
Waterloggable: No
Drops: Itself, and the naquadah in it
Block entity: Yes
```

## Obtaining

### Crafting

```recipe
sgcraft:ring_platform
```

A floor platform crafts into a ceiling platform on its own, and back again.

```recipe
sgcraft:ring_platform_ceiling
```

```recipe
sgcraft:ring_platform_from_ceiling
```

```recipe
sgcraft:ring_panel
```

```recipe
sgcraft:ring_remote
```

## Placing

Set a Transport Ring Platform (Floor) in a floor. Its rings rise around the three by three blocks around it, so leave that space and about three blocks above it clear. At rest the rings lie sunk in the floor, a lip showing.

Set a Transport Ring Platform (Ceiling) in a ceiling. Its rings hang flat under it and drop to the floor below, up to 12 blocks down, stacking on the first block they can stand on. A floor platform with air under it works as a ceiling platform too.

Whoever places a platform owns it. Breaking a platform drops its naquadah and unlinks it from its partner.

## Linking

1. Sneak and use the first platform with an empty hand.
2. Sneak and use a second platform in the same world, up to 2,000 blocks away. The two are now linked, each to the other.

Sneak-use the first one again instead to cancel. Linking a platform that was already linked unlinks its old partner. You need to be allowed to configure both platforms to link them: their owner, an operator, or someone they list as an admin. Neither may be running.

## Transporting

Press a Ring Control Panel within 16 blocks of a platform, or use a Ring Remote while standing in a platform's rings or within 8 blocks of one. The nearest platform starts, then its partner. A transport runs in two halves of about 7 seconds each:

1. The five rings of the platform you started rise one after another, the first highest, and lock in a stack around those inside, with the ring sound.
2. A quarter of a second after the last ring locks, a column of solid white lines snaps on inside them, from the floor to the top ring, and the rings' inner rims glow a steady white. The screens of the players inside fade to white, then to black.
3. The light fades and the rings go back one at a time, the one nearest its resting place first.
4. The partner's rings rise, and the ring sound plays again there as the first one stops. As they set off, everything inside each set of rings swaps with everything inside the other, each keeping its place in the rings and the way it faces. Players in the partner's rings see a white flash as they go.
5. The partner's light snaps on around whoever arrived, with white sparkles, and their screens go from black to white, then clear. Its rings go back too. The whole run takes about 14 seconds.

Players in the first platform's rings are held where they stand from the moment its rings set off, and players in the partner's rings from just before the swap, until the partner's rings are home. A held player can look around but can't move, and takes no damage except from `/kill`. Dying, leaving the world or leaving the server ends the hold.

Players, creatures and dropped items all go. The sparkles show only where something arrives. The remote rests for a second between uses.

## Power

A transport costs the platform that starts it 1,000 GE, and 1 GE more for each block between the two platforms. Its partner pays nothing.

A platform holds up to 50,000 GE from other mods' cables, which put FE in at the server's `fe_in_per_ge` rate (80 FE to a GE unless set). Use the platform with an empty hand to open its screen: the GE it holds, whether it is linked, how many transports it has left, and four slots for naquadah. Hoppers fill the slots too, and take nothing out. When a transport needs more than the platform holds, it burns naquadah from the slots a piece at a time, each worth the server's `energy.per_naquadah` (96,000 GE unless set), as in a DHD.

2.2.0 charged in FE: 20,000 FE a transport and 20 FE a block, with a piece of naquadah worth five transports. 3.0 keeps all its energy in GE, so the costs are set in GE in the `rings` settings. See [Settings](../settings.md#transport-rings).

## Who may use it

As with the [Asgard Obelisk](asgard-obelisk.md#who-may-use-it): by default anyone may use a platform, save the players its list marks as not allowed, and its owner can switch it so only they and those listed as allowed may. The owner and operators may always use and configure it.

## Configuring

Use a platform with a [Stargate Configurator](../items/configurator.md) in your hand. It opens the obelisks' screen, titled Transport Rings: the platform it is linked to, with **Unlink**, its owner, its energy, naquadah and transports left, and the same list of players with **Use** and **Admin** switches. See [Configuring](asgard-obelisk.md#configuring).

## Commands

Operators can run rings without any screen: `/sgcraft rings <pos>` shows a platform, and `link`, `unlink`, `start` and `energy` change it. See [Commands](../commands.md#transport-rings).
