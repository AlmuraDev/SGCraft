# Pegasus Stargate Base

The Pegasus Stargate Base is the block at the bottom center of a standard Pegasus gate's ring. It makes the gate a Pegasus gate, opens the gate's address screen and holds its chevrons, camouflage and fitted upgrades.

```infobox
Pegasus Stargate Base
ID: sgcraft:stargate_base_pegasus
Category: Gate blocks
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Waterloggable: Yes
Drops: Itself, its camouflage and its fitted upgrades
Block entity: Yes
Gate type: Pegasus (sgcraft:pegasus)
Dialed by: Pegasus DHD
```

## The finished gate

```gallery
scenes/pegasus-day.jpg | Idle, by day
scenes/pegasus-night.jpg | Idle, by night
scenes/pegasus-open-day.jpg | Open, by day
scenes/pegasus-open-night.jpg | Open, by night
```

## Obtaining

### Crafting

You make it from a {{item:sgcraft:stargate_base}}.

```recipe
sgcraft:stargate_base_pegasus
```

### Generated gates

The world builds a Pegasus gate with this base, and a Pegasus DHD, on the platform near an igloo. Players can't break a generated gate unless its **Breakable** setting (`sgcraft:breakable`) is on, or the server setting `gates.generated_gates_breakable` is on for gates without a setting of their own. See [World generation](../worldgen.md#structure-gates).

## Usage

Seen from the front, a standard gate is laid out like this:

```
C R C R C
R . . . R
C . . . C
R . . . R
C R B R C
```

`B` is the base, `R` a {{item:sgcraft:stargate_ring}}, `C` a {{item:sgcraft:stargate_chevron}} and `.` the opening, which must be air, fluid or event horizon.

1. Place the base where the bottom center of the ring goes. It faces you, and the gate faces the same way.
2. Build the ring of 8 ring blocks and 7 chevron blocks across its front. The gate forms when you place the last block of the ring, or the base next to a complete ring. If there is no standing ring, the base looks for one lying flat at its own height, running out from the base along its back.
3. Place a {{item:sgcraft:dhd_pegasus}} less than 6 blocks from the base (`dhd_range`, default 6) and no more than 2 blocks above or below it. The DHD dials the gate and holds its fuel.

The player whose block finished the gate owns it. See [Building gates](../building-gates.md#forming-a-gate) for the full build.

## Gate type

This base makes Pegasus gates (`sgcraft:pegasus`). A Pegasus gate's ring stays still as it dials: a light runs around its screens to each chevron. Its ring speed is about 7.14 degrees a tick by default, where a Milky Way gate's is 2 (`ring_speed`). It shows addresses in the Pegasus glyphs: {{glyphs-pegasus:ABCDEF}}. The Pegasus gate has no `@` glyph of its own, so it borrows the Milky Way one.

A {{item:sgcraft:dhd_pegasus}} links to it and dials it. A Milky Way DHD doesn't link to a Pegasus gate. A {{item:sgcraft:pdd}}, a {{item:sgcraft:gate_control_tablet}} and `/sgcraft dial` dial it too, and it can dial gates of any type.

Each time a standard gate forms, it takes the type of its base block. To change the type of a formed, idle gate, use a {{item:sgcraft:universe_upgrade}} on it. You need the right to configure the gate, and the upgrade is used up. Operators and creative players can set any type on the {{item:sgcraft:configurator}}'s **Gate** page, and operators can run `/sgcraft type <type>`.

A gate keeps the type an upgrade gave it while its base stays in place, even if the ring breaks apart and forms again. The base block keeps its own kind: a Pegasus base in a gate turned Universe drops a Pegasus base, and the upgrade drops beside it. See [Building gates](../building-gates.md#gate-type-upgrades).

## The address screen

Right-click the base, or any ring block of the formed gate, to open the **Stargate Address** screen. The base opens nothing until the gate has formed. Gate tools and upgrades in your hand work on the gate instead of opening the screen.

- The screen shows the gate's address in the glyphs of the gate's type, with the address in letters below.
- Click the panel to copy the address. The client setting `copy_address_on_open` (default `false`) copies it each time the screen opens.
- When the gate's `address_visible` property is off, the screen shows "... Gate Address Hidden ...". See [Addresses](../addresses.md#hidden-addresses).
- A player with the right to configure a standing gate also sees 5 slots labeled **Base Camouflage**. Each slot takes one block and covers one block of the gate's bottom row, left to right as seen from the front. Any block works except ring blocks, bases and DHDs. A flat gate takes no camouflage. See [Building gates](../building-gates.md#camouflage).

## Address

A gate's address is the address of the cell holding its base. A cell is 1 chunk across. Two gates in one chunk share an address and a dial reaches only one of them, so build one gate per chunk. A base beyond the address grid, about 373,248 blocks out from x 0, z 0, gives its gate no address. See [Addresses](../addresses.md#cells-and-the-grid).

The gate forms with 7 chevrons, which dial gates in the same world. Each {{item:sgcraft:chevron_upgrade}} adds one, up to 9: 8 reach other worlds of the same group, 9 reach other groups. The server's `gates.defaults` setting `sgcraft:chevrons` can give new gates more. See [Building gates](../building-gates.md#chevrons-and-upgrades).

## Redstone

The base of a formed gate works with redstone:

- While the gate does anything other than stand idle, the base gives out a full redstone signal of 15, which also powers the blocks it touches. The gate property `redstone_output` (default `true`) turns this off.
- A redstone signal arriving at the base shuts the gate's iris, and the signal going opens it. The gate property `redstone_input` (default `true`) turns this off. Redstone has every right, so it works whatever the gate's access settings say.

See [Dialing](../dialing.md#redstone) and [The iris](../iris.md#redstone).

## Breaking

Breaking the base, or any block of the gate, breaks the gate apart. A player may break it if they are an operator, if they own the gate and the server setting `gates.owners_break_gates` is on (the default), or if the gate's **Breakable** setting is on. Anyone else sees "This gate can't be broken". Explosions and commands still break gates.

When the base breaks, it drops:

- Itself.
- Its camouflage blocks.
- One {{item:sgcraft:chevron_upgrade}} for each chevron added with a Chevron Upgrade.
- An {{item:sgcraft:iris_upgrade}} if the gate's iris was fitted with one.
- The {{item:sgcraft:pegasus_upgrade}} or {{item:sgcraft:universe_upgrade}} that set the gate's type, if one did.

Chevrons, irises and types the gate came with, or that an operator, a creative player or a command set, drop nothing.

Breaking a ring block instead leaves the base in place with its type, chevrons and camouflage, and the gate forms again when the ring is whole. Breaking a gate while its wormhole is open sets off a breach explosion, strength 10 by default (`breach_explosion`). See [Building gates](../building-gates.md#breach-explosion).

## Commands

Most `/sgcraft` gate commands act on your selected gate, or else the gate you look at, through its ring or its opening, up to 64 blocks away. Operators can name a gate by any of its blocks with `/sgcraft gate <pos> <command>`.

| Command | Level | What it does |
| --- | --- | --- |
| `/sgcraft address` | All | The gate's type, position and address |
| `/sgcraft info` | All | The gate's type, position, state, chevrons, owner and connection |
| `/sgcraft dial <address>` | All | Dial an address from the gate |
| `/sgcraft disconnect` | All | Stop dialing, or close the wormhole |
| `/sgcraft type <type>` | Op | Change the gate's type |
| `/sgcraft chevrons <1-9>` | Op | Set how many chevrons the gate has |
| `/sgcraft camouflage set <slot> <block>` | Op | Camouflage one block of the bottom row, slot 0 to 4 |
| `/sgcraft iris open`, `/sgcraft iris close` | All | Open or close the iris |
| `/sgcraft owner <player>` | All | Hand the gate to another player (owner or op only) |
| `/sgcraft place sgcraft:pegasus` | Op | Build a whole Pegasus gate with 9 chevrons 3 blocks in front of you |

`type` and `chevrons` changes need the gate to be idle. See [Commands](../commands.md#gates) for every command.

## See also

- [Building gates](../building-gates.md#gate-types)
- [Milky Way Stargate Base](stargate-base.md)
- [Universe Stargate Base](stargate-base-universe.md)
- [The DHD](../dhd.md)
- [Addresses](../addresses.md#symbols)
- [Commands](../commands.md#gates)
