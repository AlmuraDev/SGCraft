# Changes since SGCraft 2

SGCraft 3.0 rebuilds SGCraft 2.2.0, the latest Minecraft 1.12.2 version, for Minecraft 26.3 on NeoForge and Fabric. This page lists what is new, what works differently, what 2.2.0 had that 3.0 does not, and what happens to old worlds.

## Platforms

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Minecraft | 1.12.2 | 26.3 |
| Loader | Forge for 1.12.2, built against 14.23.5.2838 | NeoForge 26.3.0.76 or newer, or Fabric Loader 0.19.5 or newer |
| Java | 8 | 25 |
| Other mods | Optional: MalisisCore for the PDD, GDO, configurator, Explorer Dialer Device, shield console and city shield generator, SpongeForge for permission nodes, and IndustrialCraft 2, ComputerCraft and OpenComputers for their integrations | None on NeoForge. Fabric API on Fabric |
| Jars | One Forge jar | One jar for each loader |

On Fabric, SGCraft carries Team Reborn Energy inside its jar, so its power unit takes energy from Fabric cables. See [Installing SGCraft](install.md).

## New in 3.0

- **The MALP.** A {{item:sgcraft:malp}} is a probe you drive through a gate from a {{item:sgcraft:malp_console}} or a {{item:sgcraft:malp_tablet}}. You watch its camera live, and the world around it streams to you out to 32 blocks across and 24 up and down by default. The radio reaches it directly within 256 blocks, or through an open wormhole. It runs on a {{item:sgcraft:battery}}, which you charge in a {{item:sgcraft:battery_charger}}, in a power unit's new battery slot, or with naquadah in the crafting grid. The console is a desk three blocks wide, worked from an {{item:sgcraft:office_chair}} or standing, whose wide monitor shows a MALP's feed to the players near it even with no one at it. See [The MALP](malp.md).
- **The SGC dialing computer.** A {{item:sgcraft:dialing_computer}} three blocks wide dials the nearest gate from an {{item:sgcraft:office_chair}} in front of it, or standing at it: as the gate's settings say, or slowly with the ring turning and each chevron locking, or quickly. Its monitor shows the dialing program to everyone nearby, and it sounds an alarm while a wormhole comes in. The dialing computer and the MALP console run on gate energy from power units or ZPMs near them, and go dark while that energy runs short. Sneaking and using one with an empty hand switches it off or on. See [Dialing Computer](blocks/dialing-computer.md).
- **The gate control tablet.** A {{item:sgcraft:gate_control_tablet}} keeps a list of gates. From anywhere, in any world, it dials from them, works their irises and opens their configurators. See [Gate control tablet](tools.md#gate-control-tablet).
- **Gate owners.** A gate belongs to the player whose block finished it. The owner has every right on it and decides who may do what. Gates in 2.2.0 had no owner. See [Owners](access.md#owners).
- **The `/sgcraft` commands.** 2.2.0 had one command, `/sgspawn`, which built a gate. 3.0's `/sgcraft` covers dialing, the iris, energy, gate properties, rights, owners, address rules, camouflage, world generation, naquadah ore, address books, the MALP and server settings, so admins can run every gate without a screen. `/sgcraft place` replaces `/sgspawn`. See [Commands](commands.md).
- **Address layout settings.** In 2.2.0 every address named one chunk. 3.0 uses cells 8 chunks across by default, and the `addresses` settings set the cell size and point of origin for every world or for one. The server warns loudly when a change alters the addresses of existing gates. See [Changing the layout](addresses.md#changing-the-layout).
- **World groups.** 2.2.0 put every world in one group. In 3.0 the `addresses` settings sort worlds into up to 39 groups of up to 39 worlds, each group named by its own symbol in 9 symbol addresses. See [World symbols and groups](addresses.md#world-symbols-and-groups).
- **The settings screen.** Operators set each world's point of origin, cell size and group, make and remove groups, and change the rest of the server's settings in a screen opened with `/sgcraft settings screen` or from the mod list's Config button. Every player changes their own client settings there too. `/sgcraft config` changes the same settings by command. 2.2.0 had no settings screen. See [The settings screen](settings.md#the-settings-screen).
- **Wormhole jumps.** When the gate an open wormhole reaches breaks, the wormhole jumps to another gate in the same address cell, as on the show. See [Wormhole jumps](dialing.md#wormhole-jumps).
- **Your own address book.** `/sgcraft book` keeps addresses on the server for each player, apart from any PDD or tablet. See [Address books](addresses.md#address-books).
- **Fluid through the wormhole.** A gate standing in water, lava or another fluid pours it out of the far gate. See [Fluid through the wormhole](dialing.md#fluid-through-the-wormhole).
- **The energy converter.** An {{item:sgcraft:energy_converter}} turns the GE in a ZPM console, interface cart, hub, power unit or battery charger into FE for other mods, up to 62.5 GE (5,000 FE) a tick. See [Energy Converter](blocks/energy-converter.md).
- **An add-on API.** 2.2.0 had no API library. Other mods could only listen for one Forge event, fired when a gate formed or broke. 3.0 publishes `sgcraft-api`, which add-ons build against on both loaders to find and dial gates, change their properties, add energy sources, rights, address books and gate types, and listen to events. See [API](api.md).

## Carried over from 2.2.0

SGCraft 2.2.0 added these, and 3.0 has them in the base mod, with the same recipes except where noted:

- **The gate shield.** A {{item:sgcraft:shield_upgrade}} fits an energy shield in place of the metal iris. It rises and falls in half a second, stops travelers as a shut iris does, and uses 1 gate energy a tick while it is up. The GDO, the gate control tablet, redstone and commands work it as they work an iris. See [The shield](iris.md#the-shield).
- **The shield console.** A {{item:sgcraft:shield_console}} raises and lowers the shield of the nearest shielded gate within 40 blocks. See [Shield Console](blocks/shield-console.md).
- **The Ori supergate.** A gate type built only as a supergate, which connects only to other Ori supergates. Its ring of segments links one after another as it dials, over about 22 seconds, timed to its dialing sound. It takes no iris, shield or upgrades. Get one with the {{item:sgcraft:ori_supergate_spawner}}, a command or the configurator. See [Ori supergates](building-gates.md#ori-supergates).
- **The Explorer Dialer Device.** An {{item:sgcraft:edd}} lists every generated gate, in every world, marks the ones you have visited and dials them from the gate near you. See [Explorer Dialer Device](items/edd.md).
- **The city shield.** A {{item:sgcraft:city_shield_generator}} raises a dome around itself, up to 128 blocks each way, that keeps things and projectiles out, keeps outside explosions from harming what is inside, and keeps its chunks loaded. It runs on the power units and ZPMs within 6 blocks of it and the 3 ZPMs in its own slots. See [City Shield Generator](blocks/city-shield-generator.md).
- **ZPMs.** The {{item:sgcraft:zpm}} holds 26,843,546 gate energy (GE). It has no recipe: chests beside generated gates hold them, and one in ten is tainted and explodes when drawn on. See [Zero Point Module](items/zpm.md).
- **The ZPM console, interface cart and hub.** The {{item:sgcraft:zpm_console}} holds one ZPM, the {{item:sgcraft:zpm_interface_cart}} one and the {{item:sgcraft:zpm_hub}} three. Gates, city shield generators within 6 blocks and energy converters draw on them. The cart and the hub need no IndustrialCraft 2, and have new recipes. The console keeps its recipe. See [Power](power.md#zpms).
- **ZPM chests.** 15% of generated gates have a chest beside them holding a ZPM. See [ZPM chests](worldgen.md#zpm-chests).

In 2.2.0 the Explorer Dialer Device, shield console and city shield generator needed MalisisCore, and the ZPM interface cart and hub needed IndustrialCraft 2. In 3.0 they need no other mod.

## Changed

### Addresses

Every address is new. A gate rebuilt where it stood in 2.2.0 gets a different address.

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Symbols | 36: `A` to `Z` and `0` to `9` | 39, as on the Milky Way gate's ring: `A` to `Z`, `0` to `9`, `@`, the point of origin, and `+` and `.` |
| Lengths | 7 for a gate in the same dimension, 9 for any dimension | 7 for the same world, 8 for any world of the group, 9 for any world of any group |
| Made of | 7 position symbols, then 2 for the dimension | 6 position symbols, then the world's symbol, then the group's symbol, ending with `@` |
| Written as | `ABCD-EFG-HI` | `XOBF-R3@`, `XOBF-R3A-@` or `XOBF-R3A-@@` |
| Reach of the grid | 139,967 chunks (about 2,239,472 blocks) out from the center each way | About 3,511,808 blocks out each way with the default 8 chunk cells and point of origin |
| Chevrons at the dialed gate | Had to be at least the length of the dialing gate's address, so a dial from another dimension needed 9 chevrons at both ends | Only the dialing gate needs chevrons |

Worlds are sorted into groups, each with its own symbol. SGCraft has one group, `default`. See [Addresses](addresses.md) and [World symbols and groups](addresses.md#world-symbols-and-groups).

### Chevrons and upgrades

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Chevrons | 7, or 9 with the upgrade | 7, 8 or 9 |
| {{item:sgcraft:chevron_upgrade}} | One upgrade took a gate from 7 chevrons to 9 | Each upgrade adds one chevron, up to 9. Breaking the base gives back one upgrade for each chevron added with one |
| Who can fit an upgrade | Any player, on any gate, idle or busy | A player with the gate's configure right, on an idle gate |
| {{item:sgcraft:pegasus_upgrade}} and {{item:sgcraft:universe_upgrade}} | Lost when the base broke | Given back when the base breaks. The gate keeps its type when the ring breaks and forms again |
| Configurator | Set the type, the chevron upgrade and the iris upgrade for anyone it opened for | Only operators and players in creative mode set the type, chevrons and iris. Everyone else uses the upgrade items |

See [Chevrons and upgrades](building-gates.md#chevrons-and-upgrades) and [Gate type upgrades](building-gates.md#gate-type-upgrades).

### Power and fuel

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Opening cost | `energyPerFuelItem` divided by `gateOpeningsPerFuelItem`: 96000 / 24 = 4000 | `energy.dial_costs.open`, 4000 |
| Cost each tick open | `energyPerFuelItem` spread over `minutesOpenPerFuelItem` (80 minutes): 1 a tick | `energy.dial_costs.per_tick`, 1 |
| Cost per chevron | None | `energy.dial_costs.per_chevron`, 100 for each symbol dialed |
| Turning a cost off | Not possible on its own | Each cost has an `enabled` switch, on the server and on each gate |
| Other worlds | `interDimensionMultiplier` (4.0) was stored on each gate, but the cost to another dimension was multiplied by `distanceFactorMultiplier` (1.0) in its place, so it cost no more by default | `energy.multipliers.inter_world`, 10.0, applies to a dial with an eighth chevron. A ninth chevron multiplies that by `energy.multipliers.other_group`, 10.0, so 100 times in all |
| Drawing from sources | Evenly from every source | One after another: DHD fuel, then power units, then ZPMs, the nearest of each first |
| Power unit | "SG Power Unit", taking Forge Energy | {{item:sgcraft:power_unit}}, taking FE on NeoForge and Team Reborn Energy on Fabric, with a battery slot |
| What stores hold | The power unit held FE, and a ZPM held its own units | Power units, ZPMs, batteries and battery chargers all hold GE. FE moves only where SGCraft meets other mods, at two rates: 80 FE buy one GE coming in (`energy.bridge_rates.fe_in_per_ge`), and one GE gives 80 FE going out (`energy.bridge_rates.fe_out_per_ge`). Cables fill power units and chargers, other mods' chargers fill batteries, and energy converters give FE |

What stayed the same: one naquadah gives 96000 gate energy, the distance factor follows the same formula, supergates cost 8 times as much, a full power unit is worth 4,000,000 FE (it holds 50,000 GE, at 80 FE to one), and a gate draws from power units within 4 blocks of its base. See [Power](power.md).

### ZPMs

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| What a ZPM holds | Its own units of energy | GE. A full ZPM holds 26,843,546 GE (`zpm.capacity`), which gates and city shields take as any other |
| Interface cart and hub | Needed IndustrialCraft 2, gave 1 EU for each of a ZPM's units (or 4 FE for each EU), and were made from IC2 parts | Hold GE for gates, city shields and energy converters, with new recipes |
| FE from ZPM blocks | The ZPM console gave FE, and the cart and hub gave IC2 EU | The ZPM blocks hold GE and give no FE themselves. An {{item:sgcraft:energy_converter}} turns any SGCraft store's GE into FE, with separate in and out rates |
| Supergates | Ran on ZPMs alone and needed three of them | Take any power. ZPMs are large stores of energy that any gate may draw on, and a supergate's base holds three |
| The Overworld to the End | Needed a ZPM, from `zpm.yml`, which worked only on SpongeForge servers | Needs nothing special. A dial with an eighth chevron costs 10 times as much, and one with a ninth 100 times |
| Energy from a cart or hub | Gates got 80 times more gate energy than the cart or hub drew from its ZPMs, a bug | Gates get one GE for each GE drawn. A hub holding more than one ZPM makes them go further |
| ZPM chests | A chance for each structure type | One chance, `zpm.chest_chance` (15), for every generated gate |

See [Power](power.md#zpms).

### The DHD

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Blocks | One, the Stargate Controller, for every gate type. It took the Pegasus look beside a Pegasus gate | {{item:sgcraft:dhd}} for Milky Way gates and {{item:sgcraft:dhd_pegasus}} for Pegasus gates. Universe gates have no DHD |
| Linking | To a gate whose base stood within a box around the DHD: 5 blocks to each side, 1 up or down, 6 to the front and back | To the nearest gate of its type within the gate's `dhd_range`, 6 blocks in a straight line by default, 0 to 32 for each gate |
| Fuel slots | 4 | 5 |

See [The DHD](dhd.md).

### Rights and access

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Players | A Player Access List on each gate, entered by player name and matched by UUID once the player uses the gate, with gate, iris and admin access. By default everyone had all three | Rights on each gate, by player UUID: `sgcraft:use`, `sgcraft:iris` and `sgcraft:configure`. By default players have use and iris, and configure is the owner's |
| Admins | SpongeForge permission nodes, such as `sgcraft.admin`, `sgcraft.gui.configurator`, `sgcraft.gui.pdd`, `sgcraft.gui.gdo` and one for each configurator setting, when SpongeForge was installed | Operators (permission level 2) have every right. There are no permission nodes |
| Other gates | A Gate Address Access List with default incoming and outgoing switches | `allow_incoming`, `allow_outgoing` and address rules for single addresses |
| Breaking gates | Two gate settings: whether players may break player gates, and generated gates | Operators may, owners may while `gates.owners_break_gates` is on, and anyone may when the gate's `breakable` property is on |

See [Access](access.md).

### Tools

| Tool | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| {{item:sgcraft:configurator}} | "Configurator". No recipe, and only with MalisisCore installed | Has a recipe. Five tabs (Gate, Energy, Travel, Access, Other) and the configure right to open it. It also opens a MALP's settings |
| {{item:sgcraft:pdd}} | "Personal Dialer Device", only with MalisisCore. Default addresses in `config/SGCraft/pdd.yml` | Built in. Default addresses in `config/sgcraft-default-addresses.json`, shared with the gate control tablet. It dials the shortest address that reaches the gate |
| {{item:sgcraft:gdo}} | "Handheld GDO", only with MalisisCore | "Garage Door Opener", built in. Same jobs: the near iris, the far iris once the wormhole is open, and disconnect |
| {{item:sgcraft:tollan_phase_shift_device}} | Worked for players holding it in the off hand | Works for any living traveler holding it in either hand, while the far gate's `phase_shift_passes_iris` is on |

See [Tools](tools.md).

### Gates and wormholes

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Gate settings | The `stargate` section's values were copied into each gate when it was made, so a change to the file reached only new gates, or every gate with `force-default-configs` | Each gate holds only the values set on it. `gates.defaults` gives a value to every gate without its own, at once |
| Lightning | Always on, 6 in 100. Sent the wormhole to a generated gate of the same world from `config/SGCraft/generator.yml` | 6 in 100, sent to a random generated gate the dialing gate can reach and may dial. The `lightning_malfunction` property turns it off for each gate |
| Breach explosion | One server setting for all gates: `explosionRadius` (10), `explosionFlame` and `explosionSmoke` | Gate properties `breach_explosion` (10), `breach_fire` and `breach_breaks_blocks` |
| Kawoosh damage | `transientDamageRate` (5000) for all gates | The gate property `opening_damage_amount` (5000) |

Supergates (radius 4 to 30), flat gates facing up or down, camouflage, gate spawners, the iris, the GDO, chunk loading and one way travel were already in 2.2.0, and 3.0 keeps them. See [Building gates](building-gates.md), [Dialing](dialing.md) and [The iris](iris.md).

### World generation and the Tok'ra

| | SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- | --- |
| Structure gates | Keys for each structure in the `generaton` section, such as `pyramid_addon_chance` | `worldgen.structure_gates` entries, each with a structure template a data pack can replace |
| ZPM chests | Generated gates could come with a chest holding a ZPM, at a chance for each structure type (`*_zpm_chest_chance`, 15) | One chance for every generated gate, in structures and villages (`zpm.chest_chance`, 15). One chest ZPM in ten is tainted (`zpm.tainted_chance`) |
| Village gates | A gate piece in village generation, which built a gate at a chance of `village_addon_chance` (25) | A gate plaza in the village's pool of houses (`weight` 2). One gate at most for each village, and none within 80 blocks of another |
| Tok'ra | A villager profession, spawned at generated gates where the structure's `*_spawn_tokra` key allowed it | A villager without a job can take a DHD as its job site and become a Tok'ra. `worldgen.tokra_worlds` turns them on or off for each world. Trades in the data pack trade set `sgcraft:tokra/level_1` |
| Chest loot | Village blacksmith chests | Village weaponsmith and toolsmith chests |

The same six structures get gates (desert pyramids, jungle temples, ocean monuments, Nether fortresses, swamp huts and igloos), each at a 25% chance, with 9 chevrons one time in four. Igloo gates are still Pegasus gates. Naquadah ore generates as before, with the same defaults. The Tok'ra still trade 8 emeralds and a diamond for a core crystal, and 16 emeralds and a diamond for a controller crystal. See [World generation](worldgen.md).

### Recipes

Recipes are data pack recipes now. 2.2.0 let you turn each one off in the `recipes` section of its config file. In 3.0 a data pack changes or removes them.

- **New:** {{item:sgcraft:stargate_base_pegasus}} and {{item:sgcraft:stargate_base_universe}}, made from a Milky Way base (2.2.0 had them in the creative tab only), the {{item:sgcraft:dhd_pegasus}}, the {{item:sgcraft:configurator}}, the {{item:sgcraft:gate_control_tablet}}, and the MALP's {{item:sgcraft:malp}}, {{item:sgcraft:malp_console}}, {{item:sgcraft:malp_tablet}}, {{item:sgcraft:battery}} and {{item:sgcraft:battery_charger}}, and the {{item:sgcraft:energy_converter}}.
- **On by default:** {{item:sgcraft:core_crystal}} and {{item:sgcraft:controller_crystal}}. 2.2.0 had these recipes off unless you turned them on.
- **Gone:** the naquadah recipe from coal, a slime ball and blaze powder, which 2.2.0 had off by default, and the second shape of the Pegasus upgrade recipe.

The gate blocks, supergate blocks, Milky Way DHD, upgrades, iris parts, power unit, ZPM console, PDD and GDO keep their 2.2.0 recipes. The {{item:sgcraft:zpm_interface_cart}} and {{item:sgcraft:zpm_hub}} have new recipes, as 2.2.0's used IndustrialCraft 2 parts. See [Recipes](recipes.md).

### Item names

| SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- |
| Stargate Ring Segment (ring and chevron) | {{item:sgcraft:stargate_ring}} and {{item:sgcraft:stargate_chevron}} |
| Stargate Base | {{item:sgcraft:stargate_base}} |
| Stargate Controller | {{item:sgcraft:dhd}} |
| SG Power Unit | {{item:sgcraft:power_unit}} |
| Naquadah Alloy Block | {{item:sgcraft:naquadah_block}} |
| Stargate Iris Blade | {{item:sgcraft:iris_blade}} |
| Pegasus Type Upgrade Crystal | {{item:sgcraft:pegasus_upgrade}} |
| Universe Type Upgrade Crystal | {{item:sgcraft:universe_upgrade}} |
| Personal Dialer Device | {{item:sgcraft:pdd}} |
| Handheld GDO | {{item:sgcraft:gdo}} |
| Configurator | {{item:sgcraft:configurator}} |
| Milky Way Stargate (Test Spawner) and the others | Milky Way Stargate Spawner and the others. See [Gate spawners](blocks/gate-spawners.md) |

### Configuration files

2.2.0 kept its settings in `config/sgcraft.cfg`, a Forge config file, and in YAML files in `config/SGCraft/`. 3.0 uses three JSON files: `config/sgcraft-server.json`, `config/sgcraft-client.json` and `config/sgcraft-default-addresses.json`. Many server settings can also be changed in game with `/sgcraft` commands, which write the file. See [Settings](settings.md).

`sgcraft-server.json` is grouped into sections: `energy`, `gates`, `tools`, `worldgen`, `remote_view`, `malp`, `shields` and `zpm`. The table below names each setting by its path, such as `energy.multipliers.inter_world`. The file holds only the settings that differ from their defaults, so a fresh one is `{}`.

| SGCraft 2.2.0 | SGCraft 3.0 |
| --- | --- |
| `stargate.energyPerFuelItem` | `energy.per_naquadah`, which a MALP battery's naquadah gives too |
| `stargate.gateOpeningsPerFuelItem`, `stargate.minutesOpenPerFuelItem` | `energy.dial_costs.open`, `energy.dial_costs.per_tick` |
| `stargate.distanceFactorMultiplier`, `stargate.interDimensionMultiplier` | `energy.multipliers.distance_factor`, `energy.multipliers.inter_world` |
| `supergate.energyMultiplier` | `energy.multipliers.supergate` |
| `fp-power.energyBufferSize` | `energy.power_unit_capacity`, now in GE: 50,000 is 4,000,000 FE |
| `fp-power.fpPerSGEnergyUnit` (80) | `energy.bridge_rates.fe_in_per_ge` (80) |
| `stargate.toolSearchRange` | `tools.range` |
| `stargate.wormholeCanDestroyUnbreakableBlocks` | `gates.kawoosh_breaks_unbreakable` |
| `options.chunkLoadingRange`, `options.logStargateEvents` | `gates.chunk_loading_range`, `gates.log_events` |
| `options.enableNaquadahOre`, `options.addOresToExistingWorlds` and the `naquadah` section | The `worldgen.naquadah_ore` object |
| The other per-gate keys of the `stargate`, `iris`, `dhd`, `gate-access` and `player-access` sections | `gates.defaults`, by gate property, such as `sgcraft:open_ticks` (in ticks, where 2.2.0 used `secondsToStayOpen`) |
| `stargate.irisUpgrade`, `stargate.chevronUpgrade` | `gates.defaults` with `sgcraft:iris` and `sgcraft:chevrons` |
| `shield.energyPerTick`, `shield.consoleRange` | `shields.gate_energy_per_tick`, `shields.console_range` |
| `city-shield.energyPerMillionBlocksPerTick`, `city-shield.maxRadius`, `city-shield.keepChunksLoaded` | `shields.city_energy_per_million_blocks`, `shields.city_max_radius`, `shields.city_keeps_chunks_loaded` |
| The `*_zpm_chest_chance` keys of the `generaton` section | `zpm.chest_chance` |
| The `generaton` section | `worldgen.structure_gates` and `worldgen.village_gates` |
| `config/SGCraft/tokra.yml` (written by 2.2.0 but not used) | `worldgen.tokra_worlds` |
| `config/SGCraft/pdd.yml` | `config/sgcraft-default-addresses.json` |
| `client.useHDEventHorizonTexture`, `client.saveAddressToClipboard`, `stargate.soundVolume` | `hd_event_horizon`, `copy_address_on_open` and `sound_volume` in `config/sgcraft-client.json` |
| The `recipes` section | None. Use a data pack |

## Not ported yet

These parts of 2.2.0 are not in SGCraft 3.0.

| SGCraft 2.2.0 | In 3.0 |
| --- | --- |
| ComputerCraft: the ComputerCraft Stargate Interface block | Not ported |
| OpenComputers: the OpenComputers Stargate Interface block, and gates that passed wireless network messages through an open wormhole | Not ported |
| IndustrialCraft 2: the IC2 Stargate Power Unit, which took EU, the Ridiculously Large Capacitor, and EU from the ZPM interface cart and hub | Not ported. The power unit takes FE, or Team Reborn Energy on Fabric, and an energy converter gives FE from the ZPM interface cart, hub or any other SGCraft store |
| SpongeForge permission nodes | Not ported. Rights and operator levels decide who may do what (see [Access](access.md)) |

SGCraft 3.0 has no computer mod integration. Add-ons can build one on the [API](api.md), which lets them find, dial and change gates and listen to their events. The API also lets an add-on give gates an energy source of its own (see [Example: an energy source](api.md#example-an-energy-source)).

2.2.0 had no RedstoneFlux or Mystcraft code: its power unit took Forge Energy, which 3.0's power unit still takes on NeoForge.

## Upgrading a world

1.12 worlds and gates don't carry over. Opening a 1.12 world in Minecraft 26.3 does not bring its SGCraft blocks, items or gates along, and SGCraft 3.0 has no converter: it reads none of 2.2.0's gates, DHDs, items, PDD lists or addresses. Block and item IDs have changed too.

- Start a new world, or build your gates again.
- Write down the addresses you need again after you rebuild. Because the address format changed, each gate gets a new address, even in the same spot.
- Settings don't carry over either. 3.0 does not read `config/sgcraft.cfg` or the files in `config/SGCraft/`. It writes its JSON files with their defaults the first time it starts. Use the [configuration table](#configuration-files) above to set them again.

See [Upgrading from older SGCraft](install.md#upgrading-from-older-sgcraft).
