# Commands

The `/sgcraft` command runs everything SGCraft does without a screen, so players and server admins can work gates, MALPs and settings from chat or the server console.

## Permission levels

Each command below lists the permission level it needs:

- **All:** any player (level 0). The gate still checks the player's [rights](access.md): dialing and closing need `use`, the iris needs `iris`, and changing rights or the owner needs the gate's owner or an operator.
- **Op:** permission level 2 or higher, which operators and command blocks have.

Commands from the server console, command blocks and other non-player sources have every right on every gate.

In these tables, `<value>` is required and `[value]` is optional. `<bool>` is `true` or `false`.

## Choosing a gate

Most commands act on one gate. They pick it this way:

1. Your selected gate, wherever you are, if you have one.
2. Otherwise, the gate you look at, through its ring or its opening, up to 64 blocks away.

`/sgcraft select` selects the gate you look at. Operators can give a position instead: `/sgcraft select <pos>` selects the gate in the address region (one address cell) that holds that position, in your world. A selection lasts until you run `/sgcraft deselect` or the server stops. The server console has a selection of its own.

`address` and `info` show the gate you look at. Add `--selected` to show your selected gate instead.

Operators can also name a gate by any block of it, in their own world, with `/sgcraft gate <pos> <command>`. It takes every command marked "Yes" in the **Gate** column below. For example, `/sgcraft gate 100 64 -20 iris close`.

Chat output is clickable. A gate's position puts a teleport command in your chat box, an address copies itself, **[Dial]** puts a dial command in your chat box and **[Select]** selects that gate.

## Gates

| Command | Level | Gate | What it does |
| --- | --- | --- | --- |
| `/sgcraft address [--selected]` | All | Yes | The gate's type, position and address, from its own world and from the other worlds of its group, in letters and glyphs |
| `/sgcraft info [--selected]` | All | Yes | The gate's type, position, state, chevrons, owner and connection |
| `/sgcraft selected` | All | No | `info` for your selected gate |
| `/sgcraft select` | All | No | Select the gate you look at |
| `/sgcraft select <pos>` | Op | No | Select the gate in the address region holding a position |
| `/sgcraft deselect` | All | No | Clear your selection |
| `/sgcraft list [<world>]` | Op | No | Every gate in your world, or in another, with addresses and **[Select]** links |
| `/sgcraft place <type> [<chevrons>] [<radius>] [<lying>]` | Op | No | Build a whole gate 3 blocks in front of you, facing you |
| `/sgcraft type <type>` | Op | Yes | Change the gate's type: `sgcraft:milky_way`, `sgcraft:pegasus` or `sgcraft:universe` |
| `/sgcraft chevrons <1-9>` | Op | Yes | Set how many chevrons the gate has |
| `/sgcraft orientation` | All | Yes | Whether the gate stands or lies flat, and which way it faces |
| `/sgcraft orientation face_up` | Op | Yes | Turn a flat gate face up |
| `/sgcraft orientation face_down` | Op | Yes | Turn a flat gate face down |
| `/sgcraft camouflage` | All | Yes | The five camouflage blocks on the gate's bottom row, left to right |
| `/sgcraft camouflage set <slot> <block>` | Op | Yes | Camouflage one block (slot 0 is the left of the bottom row, seen from the front, to 4 at the right) |
| `/sgcraft camouflage clear [<slot>]` | Op | Yes | Clear one slot, or every slot; what was there is lost |
| `/sgcraft tokra` | Op | Yes | A Tok'ra stands in front of the gate |

`place` takes up to 9 chevrons (9 unless given) and a radius of `2` for a standard gate, or `4` to `30` for a [supergate](building-gates.md). The last word is `standing` (the default), `face_up` or `face_down`. A flat gate's ring runs away from you. If the spot isn't clear, the gate goes in the nearest clear spot within 3 blocks. You own the gate you place.

`type`, `chevrons` and `orientation` changes need the gate to be idle. Only a gate lying flat can turn. `/sgcraft orientation` returns 1 for standing, 2 for face up and 3 for face down, for command blocks to test.

## Dialing

| Command | Level | Gate | What it does |
| --- | --- | --- | --- |
| `/sgcraft dial <address>` | All | Yes | Dial an address, turning the ring for each symbol |
| `/sgcraft dial <address> --quick` | All | Yes | Dial with every chevron locking at once (`-q` works too) |
| `/sgcraft disconnect` | All | Yes | Stop dialing, or close the wormhole |

Type the address with or without dashes, in any case, such as `/sgcraft dial ab12-cde`. The command reports when the wormhole opens, or why the dial failed. See [Dialing](dialing.md) for every failure message.

## Iris

| Command | Level | Gate | What it does |
| --- | --- | --- | --- |
| `/sgcraft iris` | All | Yes | Whether the gate has an iris, and its state |
| `/sgcraft iris open` | All | Yes | Open the iris |
| `/sgcraft iris close` | All | Yes | Close the iris |
| `/sgcraft iris fit` | Op | Yes | Fit a metal iris |
| `/sgcraft iris remove` | Op | Yes | Take the iris off |

See [Iris](iris.md).

## Energy and power

| Command | Level | Gate | What it does |
| --- | --- | --- | --- |
| `/sgcraft energy` | All | Yes | How much energy the gate holds, of how much, and whether it dials without it |
| `/sgcraft energy add <amount>` | Op | Yes | Add energy to the gate's store, up to its capacity |
| `/sgcraft energy set <amount>` | Op | Yes | Set the energy in the gate's store |
| `/sgcraft power [<pos>]` | Op | No | The FE and gate energy in the power unit you look at (within 8 blocks), or at a position |
| `/sgcraft power [<pos>] set <fe>` | Op | No | Set a power unit's FE, kept within its capacity |
| `/sgcraft costs` | Op | No | The server's dial costs for gates without their own |
| `/sgcraft costs <kind> enabled <bool>` | Op | No | Turn one cost on or off |
| `/sgcraft costs <kind> energy <amount>` | Op | No | Set how much one cost takes, before distance |

The cost kinds are `per_chevron`, `open` and `per_tick`. Cost changes apply to dials from then on and are saved to the settings file. See [Power](power.md).

## Properties and rights

| Command | Level | Gate | What it does |
| --- | --- | --- | --- |
| `/sgcraft property` | All | Yes | Every property of the gate and its value |
| `/sgcraft property <id>` | All | Yes | One property's value |
| `/sgcraft property <id> set <json>` | Op | Yes | Set the gate's own value, written as JSON |
| `/sgcraft property <id> reset` | Op | Yes | Clear the gate's own value, so it has the default again |
| `/sgcraft rights` | All | Yes | The owner, the default rights and each listed player's rights |
| `/sgcraft rights default <rights>` | All | Yes | Set the rights of players not listed (owner or op only) |
| `/sgcraft rights <player> <rights>` | All | Yes | Set one player's rights (owner or op only) |
| `/sgcraft owner` | All | Yes | Who owns the gate |
| `/sgcraft owner <player>` | All | Yes | Hand the gate to another player (owner or op only) |
| `/sgcraft owner --none` | All | Yes | Leave the gate with no owner, so operators manage it (owner or op only) |
| `/sgcraft rule` | Op | Yes | The gate's incoming and outgoing defaults and each address rule |
| `/sgcraft rule set <incoming> <outgoing> <address>` | Op | Yes | Allow or refuse connections with one address (two bools) |
| `/sgcraft rule remove <address>` | Op | Yes | Remove an address's rule |

Write rights as a comma list with no spaces: `use,iris,configure`, `use,iris`, `use` or `none`. For a player, `reset` takes them off the list, so they get the default rights. Players must have been seen on the server.

Property IDs are listed in [Settings](settings.md#gate-properties). For example, `/sgcraft property sgcraft:open_ticks set 12000` or `/sgcraft property sgcraft:one_way_travel set false`. See [Access](access.md) for rights and address rules.

## Worldgen and ore

| Command | Level | What it does |
| --- | --- | --- |
| `/sgcraft worldgen` | Op | Each structure set to have a gate, its chance and its template |
| `/sgcraft worldgen always <bool>` | Op | Every such structure gets a gate until the server stops, for trying them with `/place structure` (not saved) |
| `/sgcraft worldgen tokra` | Op | The worlds set to have or not have Tok'ra at generated gates |
| `/sgcraft worldgen tokra <world> <bool>` | Op | Set whether Tok'ra stand at gates generated in a world (saved) |
| `/sgcraft ore` | Op | How naquadah ore generates, and how many old chunks wait for ore |
| `/sgcraft ore enable_naquadah_ore <bool>` | Op | Turn naquadah ore on or off |
| `/sgcraft ore add_ores_to_existing_worlds <bool>` | Op | Give ore to chunks made without it as they load |
| `/sgcraft ore <count> <number>` | Op | Set `gen_under_lava_odds`, `max_nodes_under_lava`, `gen_isolated_odds` or `max_isolated_nodes` |
| `/sgcraft ore dimension_ids add <world>` | Op | Add a world ore generates in |
| `/sgcraft ore dimension_ids remove <world>` | Op | Remove a world ore generates in |
| `/sgcraft ore chunk [<x z>]` | Op | Whether your chunk, or the chunk holding a position, has had its ore |
| `/sgcraft ore chunk [<x z>] generate` | Op | Give that loaded chunk ore now |
| `/sgcraft ore chunk [<x z>] unmark` | Op | Have that chunk get ore again the next time it loads, while `add_ores_to_existing_worlds` is on |
| `/sgcraft tokra` | Op | A Tok'ra stands in front of the chosen gate |

Ore changes apply to chunks generated or loaded from then on and are saved. See [World generation](worldgen.md).

## Addresses

| Command | Level | What it does |
| --- | --- | --- |
| `/sgcraft book` | All | Your own address book |
| `/sgcraft book add <name> <address>` | All | Add an address to your book; put a name with spaces in quotes |
| `/sgcraft book remove <address>` | All | Remove an address from your book |
| `/sgcraft default_addresses` | Op | The addresses new PDDs and tablets start with |
| `/sgcraft default_addresses add <name> <address>` | Op | Add a default address, or rename it if it is there |
| `/sgcraft default_addresses remove <address>` | Op | Remove a default address |
| `/sgcraft default_addresses lock <address>` | Op | Holders can't rename or remove it |
| `/sgcraft default_addresses unlock <address>` | Op | Holders may rename or remove it |

Your book keeps each address in the form that names its world, so it dials from any world. Default address changes reach PDDs and tablets used for the first time after the change. See [Addresses](addresses.md).

## MALP and views

Every command here needs level 2. Name a MALP by its name or the start of its UUID, as `malp list` shows it. Put a name with spaces in quotes. A command about a MALP whose chunk isn't loaded loads it and asks you to run it again.

| Command | What it does |
| --- | --- |
| `/sgcraft malp list` | Every MALP: where it was last seen, its battery or that it needs no power, its console and its owner |
| `/sgcraft malp pair <malp> <pos>` | Pair a MALP with the console at a position in your world |
| `/sgcraft malp unpair <malp>` | Unpair a MALP from its console |
| `/sgcraft malp battery <malp> <fe>` | Set the charge of a MALP's battery, giving it one if it has none |
| `/sgcraft malp power_required <malp> [<bool>]` | Show or set whether a MALP runs on its battery |
| `/sgcraft malp poweroff <malp>` | Power down the feed of everyone watching a MALP |
| `/sgcraft malp recall <malp>` | Bring a MALP to you |
| `/sgcraft malp center <malp>` | Turn a MALP's camera to face the way it drives |
| `/sgcraft malp signal <malp> [<pos>]` | How its console, or the console at a position, reaches a MALP: directly, through which gates, or not at all |
| `/sgcraft malp pair_tablet <malp>` | Pair a MALP with the MALP tablet in your hand, whoever owns the MALP |
| `/sgcraft malp unpair_tablet <malp>` | Take a MALP off the tablet in your hand |
| `/sgcraft malp console_damage_interrupts [<bool>]` | Show or set whether damage takes a player off a console or tablet (saved) |
| `/sgcraft malp console_spectators [<0-64>]` | Show or set how many players near a driven console see its monitor (saved) |
| `/sgcraft malp spectator_range [<1-64>]` | Show or set how near a console those players must be, in blocks (saved) |
| `/sgcraft malp tablet_drain [<fe>]` | Show or set the FE a tick a tablet uses while its feed is live (saved) |
| `/sgcraft battery` | The charge of the battery in your main hand |
| `/sgcraft battery set <fe>` | Set the charge of the battery in your main hand |
| `/sgcraft view list` | Who is watching a remote view, what, where, and on which display |
| `/sgcraft view stop <players>` | Stop every remote view those players have |

See [MALP](malp.md).

## Server settings

| Command | Level | What it does |
| --- | --- | --- |
| `/sgcraft settings` | Op | `kawoosh_breaks_unbreakable`, `owners_break_gates` and `generated_gates_breakable` |
| `/sgcraft settings <name> <bool>` | Op | Set one of those three |
| `/sgcraft gate_defaults` | Op | The values gates have when they have none of their own |
| `/sgcraft gate_defaults <property> <json>` | Op | Set one, such as `sgcraft:open_ticks 12000` |
| `/sgcraft gate_defaults <property> reset` | Op | Clear one, so gates have the property's default again |
| `/sgcraft chunk_loading_range [<0-8>]` | Op | Show or set how many chunks around a busy gate stay loaded |
| `/sgcraft log_events [<bool>]` | Op | Show or set whether gate events go to the server log |

These changes apply at once and are saved to `config/sgcraft-server.json`. `gate_defaults` also takes `sgcraft:chevrons` (7 to 9), and `sgcraft:iris` and `sgcraft:chevrons` reach gates as they first form. See [Settings](settings.md).

## Shared results

Commands that change a gate answer with one of these when nothing happens:

| Message | Why |
| --- | --- |
| Nothing changed | The gate already had that value |
| You aren't allowed to do that | You lack the right, or the change needs the gate's owner or an operator |
| Something stopped it | An add-on canceled the change |
| That isn't possible now. A gate must be idle to change it | The gate is busy, or the change can't apply to it |

If a setting can't be written to its file, the command says the change applies until the server stops.
