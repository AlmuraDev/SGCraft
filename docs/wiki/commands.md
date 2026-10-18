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
| `/sgcraft type <type>` | Op | Yes | Change the gate's type: `sgcraft:milky_way`, `sgcraft:pegasus`, `sgcraft:universe` or, for a supergate, `sgcraft:ori` |
| `/sgcraft chevrons <1-9>` | Op | Yes | Set how many chevrons the gate has |
| `/sgcraft orientation` | All | Yes | Whether the gate stands or lies flat, and which way it faces |
| `/sgcraft orientation face_up` | Op | Yes | Turn a flat gate face up |
| `/sgcraft orientation face_down` | Op | Yes | Turn a flat gate face down |
| `/sgcraft camouflage` | All | Yes | The five camouflage blocks on the gate's bottom row, left to right |
| `/sgcraft camouflage set <slot> <block>` | Op | Yes | Camouflage one block (slot 0 is the left of the bottom row, seen from the front, to 4 at the right) |
| `/sgcraft camouflage clear [<slot>]` | Op | Yes | Clear one slot, or every slot; what was there is lost |
| `/sgcraft tokra` | Op | Yes | Spawn a Tok'ra in front of the gate, or say "There's no room in front of the gate for a Tok'ra to stand" |

`place` takes up to 9 chevrons (9 unless given) and a radius of `2` for a standard gate, or `4` to `30` for a [supergate](building-gates.md). `sgcraft:ori` builds an [Ori supergate](building-gates.md#ori-supergates), radius 7 unless given. A type built at a size it doesn't come in fails with a message naming the type, such as "Ori gates can't be built at that size". `type` gives the same message when it would turn a standard gate Ori. The last word is `standing` (the default), `face_up` or `face_down`. A flat gate's ring runs away from you. If the spot isn't clear, the gate goes in the nearest clear spot within 3 blocks. You own the gate you place.

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
| `/sgcraft iris` | All | Yes | Whether the gate has an iris or a shield, and its state |
| `/sgcraft iris open` | All | Yes | Open the iris, or lower the shield |
| `/sgcraft iris close` | All | Yes | Close the iris, or raise the shield |
| `/sgcraft iris fit`, `/sgcraft iris fit metal` | Op | Yes | Fit a metal iris |
| `/sgcraft iris fit shield` | Op | Yes | Fit a shield |
| `/sgcraft iris remove` | Op | Yes | Take the iris or shield off |

A shield is raised by closing it and lowered by opening it. See [Iris](iris.md) and [The shield](iris.md#the-shield).

## Energy and power

| Command | Level | Gate | What it does |
| --- | --- | --- | --- |
| `/sgcraft energy` | All | Yes | How much energy the gate holds, of how much, and whether it dials without it |
| `/sgcraft energy add <amount>` | Op | Yes | Add energy to the gate's store, up to its capacity |
| `/sgcraft energy set <amount>` | Op | Yes | Set the energy in the gate's store |
| `/sgcraft power [<pos>]` | Op | No | The gate energy (GE) in the power unit you look at (within 8 blocks), or at a position, with the FE that is worth: "The power unit at <pos> holds <held> of <capacity> GE (<fe> FE)" |
| `/sgcraft power [<pos>] set <ge>` | Op | No | Set a power unit's GE, kept within its capacity. It replies "The power unit at <pos> now holds <held> GE" |
| `/sgcraft costs` | Op | No | The server's dial costs for gates without their own, the eighth and ninth chevron multipliers, the FE rates and the energy converter's rate |
| `/sgcraft costs <kind> enabled <bool>` | Op | No | Turn one cost on or off |
| `/sgcraft costs <kind> energy <amount>` | Op | No | Set how much one cost takes, before distance |
| `/sgcraft costs inter_world_multiplier <times>` | Op | No | Set how many times more a dial with an eighth chevron costs |
| `/sgcraft costs other_group_multiplier <times>` | Op | No | Set how many times more again a dial with a ninth chevron costs |
| `/sgcraft costs fe_in_per_ge <fe>` | Op | No | Set how many FE buy one gate energy coming in (saved to `energy.bridge_rates.fe_in_per_ge`) |
| `/sgcraft costs fe_out_per_ge <fe>` | Op | No | Set how many FE one gate energy gives going out (saved to `energy.bridge_rates.fe_out_per_ge`) |
| `/sgcraft costs converter_rate <ge>` | Op | No | Set the most gate energy an [energy converter](blocks/energy-converter.md) turns into FE each tick (saved to `energy.converter_rate`) |

The cost kinds are `per_chevron`, `open` and `per_tick`. Cost changes apply to dials from then on and are saved to the settings file. The multipliers show as "An eighth chevron costs 10 times as much, and a ninth 10 times as much again" with the defaults. The FE rates show as "80 FE buy one gate energy coming in, and one gate energy gives 80 FE going out", and the converter as "An energy converter turns up to 62.5 gate energy a tick into FE". See [Power](power.md#other-worlds-and-supergates).

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

Ore changes apply to chunks generated or loaded from then on and are saved. See [World generation](worldgen.md).

## Addresses

| Command | Level | What it does |
| --- | --- | --- |
| `/sgcraft book` | All | Your own address book |
| `/sgcraft book add <name> <address>` | All | Add an address to your book; put a name with spaces in quotes |
| `/sgcraft book remove <address>` | All | Remove an address from your book |
| `/sgcraft explorer` | All | The generated gates, and which of them you have visited, as the [Explorer Dialer Device](items/edd.md) lists them |
| `/sgcraft explorer <player>` | Op | Another player's list |
| `/sgcraft explorer forget <player>` | Op | Forget what a player has found: their visits and their names for gates |
| `/sgcraft default_addresses` | Op | The addresses new PDDs and tablets start with |
| `/sgcraft default_addresses add <name> <address>` | Op | Add a default address, or rename it if it is there |
| `/sgcraft default_addresses remove <address>` | Op | Remove a default address |
| `/sgcraft default_addresses lock <address>` | Op | Holders can't rename or remove it |
| `/sgcraft default_addresses unlock <address>` | Op | Holders may rename or remove it |
| `/sgcraft addresses` | Op | Each world's address layout (cell size and point of origin), group and symbol, its gate count, and what changes after a restart |
| `/sgcraft addresses cell_chunks <chunks> [<world>]` | Op | Set how many chunks wide each addressed cell is, for every world or one |
| `/sgcraft addresses point_of_origin <symbol> [<world>]` | Op | Set the point of origin: a letter, a digit, `giza` for `@`, or `none` |
| `/sgcraft addresses clear <world> [<part>]` | Op | Give a world the server's cell size and point of origin again, or only one: `cell_chunks` or `point_of_origin` |
| `/sgcraft addresses groups` | Op | Each group, its symbol and how many of its 39 worlds it has |
| `/sgcraft addresses group create <name> [<symbol>]` | Op | Make a group, with a symbol or, left out or `auto`, one SGCraft chooses |
| `/sgcraft addresses group remove <name>` | Op | Remove a group; its worlds go to the default group |
| `/sgcraft addresses group symbol <name> <symbol>` | Op | Set a group's symbol: a letter, a digit, `giza` for `@`, or `auto` |
| `/sgcraft addresses group assign <world> <group>` | Op | Put a world in a group, or in `default` |
| `/sgcraft addresses secret_layout true` | Op | Turn on the secret layout for every world, so no one can work out where a gate is from its address. `false` turns it off |

Your book keeps each address in the form that names its world, so it dials from any world. Default address changes reach PDDs and tablets used for the first time after the change. Address layout and group changes take effect when the server next starts. A layout change changes every gate address in the worlds it reaches, and a group change their addresses of 8 and 9 symbols. Each warns about the worlds whose gates it reaches. See [Changing the layout](addresses.md#changing-the-layout).

## Shields

Every command here needs level 2.

| Command | What it does |
| --- | --- |
| `/sgcraft shields` | The server's shield settings |
| `/sgcraft shields <name> <value>` | Set one: `gate_energy_per_tick`, `console_range`, `city_energy_per_million_blocks`, `city_max_radius`, `city_power_reach` or `city_keeps_chunks_loaded` (saved) |
| `/sgcraft shields city <pos>` | The city shield generator at a position in your world: whether its shield is up, its size, its cost and the energy in its power units |
| `/sgcraft shields city <pos> raise` | Raise its shield |
| `/sgcraft shields city <pos> lower` | Lower its shield |
| `/sgcraft shields city <pos> size <width> <above> <below>` | Set how far its shield reaches out all around, up and down |

A position with no generator says "There is no city shield generator there". `raise` without the energy says "There isn't enough energy around the generator to raise a shield that size". A gate's shield is worked with the [iris commands](#iris). See [City Shield Generator](blocks/city-shield-generator.md) and [Settings](settings.md#shields).

## ZPMs

Every command here needs level 2.

| Command | What it does |
| --- | --- |
| `/sgcraft zpm` | The server's ZPM settings |
| `/sgcraft zpm <name> <value>` | Set one: `capacity`, `search_range`, `tainted_explosion`, `chest_chance` or `tainted_chance` (saved) |
| `/sgcraft zpm give [tainted]` | Give you a full ZPM, or a tainted one |
| `/sgcraft zpm energy <ge>` | Set the GE in the ZPM in your main hand, from 0 up to `zpm.capacity`. It replies "The ZPM holds N GE" |

`energy` without a ZPM in your main hand says "Hold a ZPM in your main hand". See [Power](power.md#zpms) and [Settings](settings.md#zpms).

## Dialing computers

Every command here needs level 2.

| Command | What it does |
| --- | --- |
| `/sgcraft computers` | The server's dialing computer settings |
| `/sgcraft computers link_range <blocks>` | Set how far from a computer, 1 to 64 blocks, the gate it dials may be (saved) |
| `/sgcraft computers chair_reach <blocks>` | Set how far in front of a computer, 1 to 4 blocks, its chair may stand (saved) |
| `/sgcraft computers requires_power <true\|false>` | Set whether dialing computers and MALP consoles need gate energy to run (saved) |
| `/sgcraft computers energy_per_tick <energy>` | Set how much gate energy a computer uses each tick; it may be a fraction (saved) |
| `/sgcraft computers power_reach <blocks>` | Set how far from a computer, 1 to 16 blocks along each axis, its power may stand (saved) |
| `/sgcraft computers history_size <addresses>` | Set how many of the addresses last dialed from a computer it keeps, 0 to 10 (saved) |

See [Dialing Computer](blocks/dialing-computer.md) and [Settings](settings.md#dialing-computers).

## MALP and views

Every command here needs level 2. Name a MALP by its name or the start of its UUID, as `malp list` shows it. Put a name with spaces in quotes. A command about a MALP whose chunk isn't loaded loads it and asks you to run it again.

| Command | What it does |
| --- | --- |
| `/sgcraft malp list` | Every MALP: where it was last seen, its battery or that it needs no power, its console and its owner |
| `/sgcraft malp pair <malp> <pos>` | Pair a MALP with the console at a position in your world |
| `/sgcraft malp unpair <malp>` | Unpair a MALP from its console |
| `/sgcraft malp battery <malp> <ge>` | Set the charge of a MALP's battery in GE, giving it one if it has none. It replies "<malp>'s battery holds <held> of <capacity> GE" |
| `/sgcraft malp power_required <malp> [<bool>]` | Show or set whether a MALP runs on its battery |
| `/sgcraft malp poweroff <malp>` | Power down the feed of everyone watching a MALP |
| `/sgcraft malp recall <malp>` | Bring a MALP to you |
| `/sgcraft malp center <malp>` | Turn a MALP's camera to face the way it drives |
| `/sgcraft malp signal <malp> [<pos>]` | How its console, or the console at a position, reaches a MALP: directly, through which gates, or not at all |
| `/sgcraft malp pair_tablet <malp>` | Pair a MALP with the MALP tablet in your hand, whoever owns the MALP |
| `/sgcraft malp unpair_tablet <malp>` | Take a MALP off the tablet in your hand |
| `/sgcraft malp console_damage_interrupts [<bool>]` | Show or set whether damage takes a player off a console or tablet (saved) |
| `/sgcraft malp console_spectators [<0-64>]` | Show or set how many players near a console see its monitor (saved) |
| `/sgcraft malp spectator_range [<1-64>]` | Show or set how near a console those players must be, in blocks (saved) |
| `/sgcraft malp tablet_drain [<ge>]` | Show or set the GE a tick a tablet uses while its feed is live (saved) |
| `/sgcraft battery` | The charge of the battery in your main hand: "Your battery holds <held> of <capacity> GE" |
| `/sgcraft battery set <ge>` | Set the charge of the battery in your main hand, in GE |
| `/sgcraft view list` | Who is watching a remote view, what, where, and on which display |
| `/sgcraft view stop <players>` | Stop every remote view those players have |

See [MALP](malp.md).

## Server settings

| Command | Level | What it does |
| --- | --- | --- |
| `/sgcraft settings` | Op | `gates.kawoosh_breaks_unbreakable`, `gates.owners_break_gates`, `gates.generated_gates_breakable` and `gates.wormholes_jump` |
| `/sgcraft settings <name> <bool>` | Op | Set one of those four |
| `/sgcraft settings screen` | Op | Open the [settings screen](settings.md#the-settings-screen) |
| `/sgcraft config` | Op | Every setting that is one value, by its key, with its value after a restart where that differs |
| `/sgcraft config <key>` | Op | One setting: its value, what it does, its default, and whether it waits for a restart |
| `/sgcraft config <key> <value>` | Op | Set one, such as `/sgcraft config zpm.capacity 30000000` |
| `/sgcraft gate_defaults` | Op | The values gates have when they have none of their own |
| `/sgcraft gate_defaults <property> <json>` | Op | Set one, such as `sgcraft:open_ticks 12000` |
| `/sgcraft gate_defaults <property> reset` | Op | Clear one, so gates have the property's default again |
| `/sgcraft chunk_loading_range [<0-8>]` | Op | Show or set how many chunks around a busy gate stay loaded |
| `/sgcraft log_events [<bool>]` | Op | Show or set whether gate events go to the server log |

These changes apply at once and are saved to `config/sgcraft-server.json`, except the `remote_view` and `worldgen.village_gates` settings, which take effect when the server next starts. `/sgcraft gate_defaults` also takes `sgcraft:chevrons` (7 to 9), and `sgcraft:iris` and `sgcraft:chevrons` reach gates as they first form. See [Settings](settings.md).

## Shared results

Commands that change a gate answer with one of these when nothing happens:

| Message | Why |
| --- | --- |
| Nothing changed | The gate already had that value |
| You aren't allowed to do that | You lack the right, or the change needs the gate's owner or an operator |
| Something stopped it | An add-on canceled the change |
| That isn't possible now. A gate must be idle to change it | The gate is busy, or the change can't apply to it |

If a setting can't be written to its file, the command says the change applies until the server stops.
