# Supergate Base

The Supergate Base is the block at the bottom of a supergate's ring. It finds the ring around it, opens the gate's address screen and holds the gate's type, chevrons, camouflage and fitted upgrades.

```infobox
Supergate Base
ID: sgcraft:supergate_base
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
Gate type: Milky Way until changed
Size: Radius 4 to 30 (9 to 61 blocks across)
```

## Obtaining

### Crafting

You make it from a {{item:sgcraft:stargate_base}}.

```recipe
sgcraft:supergate_base
```

## Usage

A supergate's ring is every block a circle passes through, with the base at the bottom. The 9 blocks nearest the 9 chevron angles are {{item:sgcraft:supergate_chevron}} and the rest are {{item:sgcraft:supergate_ring}}. See [Supergate Ring Block](supergate-ring.md#usage) for how many blocks each size takes.

1. Place the base where the bottom of the ring goes. It faces you, and the gate faces the same way.
2. Build the ring across its front. The space inside doesn't need to be clear: only the ring blocks are checked.
3. The gate forms when you place the last block of the ring, or the base next to a complete ring. The base looks for the smallest complete ring of supergate blocks around it, from radius 4 to 30. If there is no standing ring, it looks for one lying flat at its own height, running out from the base along its back.

The player whose block finished the gate owns it. The ring is hard to lay out by hand: an operator can run `/sgcraft place sgcraft:milky_way 9 10` to build a supergate of radius 10, or place a supergate spawner. See [Building gates](../building-gates.md#supergates) for the full build.

## Gate type

A supergate forms as a Milky Way gate (`sgcraft:milky_way`). To change the type of a formed, idle supergate, use a {{item:sgcraft:pegasus_upgrade}} or a {{item:sgcraft:universe_upgrade}} on it. You need the right to configure the gate, and the upgrade is used up. Operators and creative players can set the type on the {{item:sgcraft:configurator}}'s **Gate** page instead, and operators can run `/sgcraft type <type>`.

A supergate keeps the type it was given while its base stays in place, even if the ring breaks apart and forms again. Breaking the base drops the upgrade that set the type, if one did, and the Supergate Base forms a Milky Way gate again when placed.

A DHD of the gate's type links to a supergate the same way it links to a standard gate: by the distance to the base, less than the gate's `dhd_range` (default 6). See [The DHD](../dhd.md#linking-to-a-gate).

## Supergates and standard gates

A supergate works like a standard gate, with these differences:

- When the wormhole opens, the kawoosh clears at most 4096 blocks in front of the gate.
- A wormhole to or from a supergate costs 8 times as much (`supergate_energy_multiplier`, default 8.0). See [Power](../power.md#other-worlds-and-supergates).
- Between gates of different sizes, travelers come out inside the far opening where they went in, scaled to its size. See [Dialing](../dialing.md#traveling-through-the-wormhole).
- The open event horizon gives light from only some of its blocks, spread across the opening. See [Event Horizon](event-horizon.md#light).

## The address screen

Right-click the base, or any ring block of the formed supergate, to open the **Stargate Address** screen. The base opens nothing until the gate has formed. Gate tools and upgrades in your hand work on the gate instead of opening the screen.

- The screen shows the gate's address in the glyphs of the gate's type, with the address in letters below.
- Click the panel to copy the address. The client setting `copy_address_on_open` (default `false`) copies it each time the screen opens.
- When the gate's `address_visible` property is off, the screen shows "... Gate Address Hidden ...". See [Addresses](../addresses.md#hidden-addresses).
- A player with the right to configure a standing gate also sees 5 slots labeled **Base Camouflage**. Each slot takes one block and covers one of the 5 blocks of the bottom row centered on the base, left to right as seen from the front. Any block works except ring blocks, bases and DHDs. A flat gate takes no camouflage. See [Building gates](../building-gates.md#camouflage).

## Address

A gate's address is the address of the cell holding its base. A cell is 1 chunk across. Two gates in one chunk share an address and a dial reaches only one of them, so build one gate per chunk. A base beyond the address grid, about 373,248 blocks out from x 0, z 0, gives its gate no address. See [Addresses](../addresses.md#cells-and-the-grid).

A supergate forms with 7 chevrons, which dial gates in the same world. Each {{item:sgcraft:chevron_upgrade}} adds one, up to 9: 8 reach other worlds of the same group, 9 reach other groups. The server's `gate_defaults` setting `sgcraft:chevrons` can give new gates more. See [Building gates](../building-gates.md#chevrons-and-upgrades).

## Redstone

The base of a formed gate works with redstone:

- While the gate does anything other than stand idle, the base gives out a full redstone signal of 15, which also powers the blocks it touches. The gate property `redstone_output` (default `true`) turns this off.
- A redstone signal arriving at the base shuts the gate's iris, and the signal going opens it. The gate property `redstone_input` (default `true`) turns this off. Redstone has every right, so it works whatever the gate's access settings say.

See [Dialing](../dialing.md#redstone) and [The iris](../iris.md#redstone).

## Breaking

Breaking the base, or any block of the gate, breaks the gate apart. A player may break it if they are an operator, if they own the gate and the server setting `owners_break_gates` is on (the default), or if the gate's **Breakable** setting is on. Anyone else sees "This gate can't be broken". Explosions and commands still break gates.

When the base breaks, it drops:

- Itself.
- Its camouflage blocks.
- One {{item:sgcraft:chevron_upgrade}} for each chevron added with a Chevron Upgrade.
- An {{item:sgcraft:iris_upgrade}} if the gate's iris was fitted with one.
- The {{item:sgcraft:pegasus_upgrade}} or {{item:sgcraft:universe_upgrade}} that set the gate's type, if one did.

Chevrons, irises and types the gate came with, or that an operator, a creative player or a command set, drop nothing.

Breaking a ring block instead leaves the base in place with its type, chevrons and camouflage, and the gate forms again when the ring is whole. Breaking a gate while its wormhole is open sets off a breach explosion, strength 10 by default (`breach_explosion`). See [Building gates](../building-gates.md#breach-explosion).

## Gate spawners

Each type has a supergate spawner for creative mode and testing: the {{item:sgcraft:milky_way_supergate_spawner}}, the {{item:sgcraft:pegasus_supergate_spawner}} and the {{item:sgcraft:universe_supergate_spawner}}. Each builds a finished supergate of radius 7 (15 blocks across) with 9 chevrons where you place it, with the base facing you. The player who places it owns the gate. Pick block (middle click) on a formed supergate gives the spawner of its type. See [Building gates](../building-gates.md#gate-spawners).

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
| `/sgcraft camouflage set <slot> <block>` | Op | Camouflage one of the 5 blocks centered on the base, slot 0 to 4 |
| `/sgcraft iris open`, `/sgcraft iris close` | All | Open or close the iris |
| `/sgcraft owner <player>` | All | Hand the gate to another player (owner or op only) |
| `/sgcraft place <type> 9 <radius>` | Op | Build a whole supergate of a radius from 4 to 30, with 9 chevrons, 3 blocks in front of you |

`type` and `chevrons` changes need the gate to be idle. See [Commands](../commands.md#gates) for every command.

## See also

- [Building gates](../building-gates.md#supergates)
- [Supergate Ring Block](supergate-ring.md)
- [Supergate Chevron Block](supergate-chevron.md)
- [Milky Way Stargate Base](stargate-base.md)
- [Power](../power.md#other-worlds-and-supergates)
- [Commands](../commands.md#gates)
