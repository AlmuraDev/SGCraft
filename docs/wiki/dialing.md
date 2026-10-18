# Dialing

Dialing locks one chevron for each symbol of an address and opens a wormhole to the gate at that address. This page covers how a dial runs, what can stop it, what crosses the wormhole and how it closes.

## Ways to dial

- A [DHD](dhd.md) next to the gate. Milky Way and Pegasus gates each have their own.
- A [dialing computer](blocks/dialing-computer.md), from its chair or standing at it. It dials slowly, quickly or as the gate is set to. See [The dialing computer](#the-dialing-computer).
- A [PDD or gate control tablet](tools.md), from an address book.
- An [Explorer Dialer Device](items/edd.md), to the gates the world built.
- The command `/sgcraft dial <address>`, at the gate you look at or have selected. See [Commands](commands.md).

## The dialing computer

A [dialing computer](blocks/dialing-computer.md) dials the nearest gate within 32 blocks, as the SGC's does. You sit in an [office chair](blocks/office-chair.md) in front of it to work it, or stand at it. Each computer keeps a dial mode. **Gate's dial** follows the gate's `chevrons_lock_on_dial`, as a DHD does. **Slow dial** (the standard mode) and **Fast dial** (the quick mode) override it. Its monitor shows the dialing program to everyone nearby: the ring turning, the chevrons locking red, and each symbol flying to its box.

## Dial modes

| Mode | What happens |
| --- | --- |
| Standard | The gate dials one symbol at a time. A Milky Way gate turns its glyph ring to bring each symbol to the top, a Universe gate turns its whole ring, and a Pegasus gate runs a light around its screens. Each chevron locks in turn. |
| Quick | Every chevron locks at once and no ring moves. All the chevron costs are paid together. |

The DHD, the PDD and the tablet dial in standard mode, or in quick mode when the gate's `chevrons_lock_on_dial` property is on (**Chevrons lock at once** in the [configurator](tools.md)). With the command, add `--quick` or `-q` for quick mode. An [Ori supergate](building-gates.md#ori-supergates) always dials in standard mode, linking its ring segments one after another.

Both gates lock each chevron in the same tick. A dialed Pegasus gate fills with light as the dial goes on, and is full when the last chevron locks.

## Timings

There are 20 ticks in a second.

| Step | Milky Way and Universe | Pegasus |
| --- | --- | --- |
| Speed while dialing | Ring turns 2 degrees a tick | Light passes one screen (10 degrees) in 1.4 ticks, about 7.14 degrees a tick |
| Each symbol | At least half a turn, so at least 90 ticks at the default speed | As far as the light runs to the next chevron |
| Rest after each chevron locks | 24 ticks | 10 ticks |
| Gates wait for each other after the last chevron | 10 ticks | 10 ticks |
| Wormhole forms | 30 ticks | 10 ticks |
| Kawoosh | 20 ticks | 20 ticks |
| Wormhole stays open | Up to 6000 ticks (5 minutes) | Up to 6000 ticks (5 minutes) |
| Wormhole closes | 40 ticks | 40 ticks |

A Milky Way or Universe gate always turns the long way around. The gate property `ring_speed` (0.1 to 360 degrees a tick) changes a gate's speed, and `open_ticks` changes how long it stays open. `open_ticks` of `0` keeps it open with no limit.

## What can stop a dial

The gate checks these in order. The first one that fails stops the dial, and the message shows to the player who dialed.

| Check | Result | Message |
| --- | --- | --- |
| The dialing gate still exists | `GATE_GONE` | A gate was broken while dialing |
| The dialing gate is idle | `BUSY` | This gate is busy |
| You may use the dialing gate | `DENIED` | You aren't allowed to dial that |
| Some address has that many symbols (7, 8 or 9) | `INVALID_ADDRESS` | No address has that many symbols. Addresses have 7, 8 or 9 |
| The gate has at least as many chevrons as the address has symbols | `NOT_ENOUGH_CHEVRONS` | This gate doesn't have enough chevrons for that address |
| A gate stands at that address | `NO_GATE` | There is no gate at that address |
| The address isn't the dialing gate's own | `SAME_GATE` | That is this gate's own address |
| The gate at that address is idle | `TARGET_BUSY` | The gate at that address is busy |
| The two gates' types can connect (an Ori supergate connects only to another) | `INCOMPATIBLE` | This gate can't connect to that type of gate |
| The dialing gate may call out to it, and it takes calls from the dialing gate | `DENIED` | You aren't allowed to dial that |
| No add-on stopped the dial | `CANCELED` | Something stopped the dial |
| The gate has the energy for every chevron and the opening | `NOT_ENOUGH_ENERGY` | This gate doesn't have the energy |

An address that doesn't end with the point of origin reaches no gate, so it fails with `NO_GATE`. The energy check is skipped for a gate that needs no power, and for creative players when the server's `energy.free_in_creative` is on. See [Power](power.md) and [Access](access.md).

A dial can also end after it starts:

| Result | Message | Cause |
| --- | --- | --- |
| `NOT_ENOUGH_ENERGY` | This gate doesn't have the energy | The energy ran out while chevrons locked or as the wormhole formed. |
| `INTERRUPTED` | The dial was interrupted | The dial was stopped partway, such as by a disconnect. |
| `GATE_GONE` | A gate was broken while dialing | Either gate was broken. |

When you dial a gate from the tablet's gate list, the tablet first works out its address. If no address reaches that gate from your world, you see "No address reaches that gate from here" (`UNREACHABLE`).

A dial that works ends with "The wormhole to ... is open".

## The kawoosh

As the wormhole opens, it bursts out of the front of both gates for 20 ticks. While the gate's `opening_damage` property is on (the default):

- Every living thing and every MALP in front of the gate takes 5000 damage a tick (`opening_damage_amount`), with the death message "was torn apart by an event horizon". For a standard gate the area is 3 blocks wide, 3 tall and 5 deep in front of the opening.
- On its first tick the kawoosh clears the blocks in its way: 3 across, 3 up and 4 deep from the ring for a standard gate, at most 4096 blocks for a supergate. It never clears gate blocks or fluids. It clears unbreakable blocks only when the server setting `gates.kawoosh_breaks_unbreakable` is on.

A shut iris keeps the kawoosh in. See [The iris](iris.md).

## Traveling through the wormhole

Anything that crosses the event horizon from the front, through the opening, travels: players, mobs, dropped items, minecarts and boats. Fishing hooks don't. Riders travel with what they ride.

- You come out of the front of the far gate, turned to face out of it, moving as you went in.
- Every player in a stack of riders needs the right to use both gates. Otherwise the travel stops with "You aren't allowed to travel through this gate".
- Between gates of different sizes, or a standing gate and a flat one, travelers come out inside the far opening where they went in, scaled to its size. A player on foot comes out standing on the far gate's floor.
- A flat gate facing up takes travelers falling in from above. One facing down takes travelers going up into it.

### One way travel

By default a wormhole goes one way, from the dialing gate to the dialed gate. The dialing gate's `one_way_travel` property (`true` by default) sets this. Stepping into the dialed end does nothing.

When the dialing gate's `reverse_travel_kills` property is on (`false` by default), anything entering the dialed end dies instead. A player who dies this way loses what they carry, unless the dialed gate's `preserve_inventory` property or the `keepInventory` game rule keeps it. Creative players are only told "You can't travel backwards through a one way wormhole".

## Closing the wormhole

The wormhole closes when:

- Someone disconnects it: the DHD's dome, the PDD, the tablet, the GDO or `/sgcraft disconnect`. This needs the right to use the gate. From the dialed end, it works only while the dialing gate's `close_from_either_end` property is on (the default).
- It has been open for `open_ticks` (6000 ticks, 5 minutes, by default).
- The dialing gate runs out of energy for the cost of each tick open.

## Redstone

- While a gate does anything other than stand idle, its base gives out a full redstone signal of 15 (`redstone_output`, on by default).
- A redstone signal arriving at the base shuts the gate's iris, and the signal going opens it (`redstone_input`, on by default). See [The iris](iris.md#redstone).

## Lightning

Once the wormhole has opened, rain at the dialing gate may bring lightning down on it. The chance is 6 in 100, checked once per wormhole. The rain is looked for above the top of a standing gate's ring, or above the base of a flat one.

The bolt is only for show: it hurts nothing and starts no fires. It sends the wormhole astray to a random generated gate: one that is idle, that the dialing gate can reach and may dial, and that takes calls from it. The gate that was dialed shuts down, and the new gate opens at once with every chevron locked. If no gate fits, nothing changes.

The gate property `lightning_malfunction` (on by default, **Storms send it astray** in the configurator) turns this off.

## Wormhole jumps

Two gates in one [address cell](addresses.md#cells-and-the-grid) share an address, and a dial reaches only one of them. If that gate is broken while its wormhole is open, by an explosion or any other way, the wormhole jumps to the other gate in the cell, as on the show. The other gate opens at once with every chevron locked, and the dialing gate stays connected. The broken gate still explodes as an open gate does.

The wormhole jumps only to a gate the dialing gate could have dialed: one that is idle, of a type the two gates connect between, and that the gates' rights allow. With more than one, it takes the nearest to the broken gate. If none fits, the wormhole closes as usual. Breaking the dialing gate always closes it.

### Server admins

`gates.wormholes_jump` (on by default) turns this off. Set it with `/sgcraft settings wormholes_jump <true|false>`.

## Fluid through the wormhole

A gate standing in water, lava or another fluid sends it through the wormhole. The far gate pours it out of its opening, more the deeper the first gate is in the fluid.

- Both gates need their `spills_fluid` property on (the default).
- Fluid goes the same way travelers do: only from the dialing gate while travel is one way, both ways otherwise.
- Fluid only pours into air. A far gate standing in fluid of its own pours nothing.
- A small body of fluid, 4096 source blocks or fewer, drains as it goes. A bigger one, such as the sea, never runs out.
- Fluid poured into the front of the sending gate's event horizon, such as from a bucket, goes through too.
- A shut iris at either end stops the fluid.
- The fluid the far gate poured out is taken away when the wormhole closes.

## Chunk loading

While a gate is busy, from the first chevron until the wormhole closes, both gates keep their chunks loaded. Each keeps `gates.chunk_loading_range` chunks around its own loaded too (1 by default, 0 to 8), plus enough to cover a supergate's ring.

### Server admins

- `/sgcraft chunk_loading_range` shows or sets the range.
- `/sgcraft log_events` turns on the server log of gate events, including lightning reroutes (`gates.log_events`).
- `/sgcraft settings <key> <bool>` sets the server's switches, such as `gates.kawoosh_breaks_unbreakable`. See [Settings](settings.md).
