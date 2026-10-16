# Changes since SGCraft 2

SGCraft 3.0 rebuilds SGCraft 2.1.0, the Minecraft 1.12.2 version, for Minecraft 26.3 on NeoForge and Fabric. This page lists what is new, what works differently, what 2.1.0 had that 3.0 does not, and what happens to old worlds.

## Platforms

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Minecraft | 1.12.2 | 26.3 |
| Loader | Forge for 1.12.2, built against 14.23.5.2838 | NeoForge 26.3.0.76 or newer, or Fabric Loader 0.19.5 or newer |
| Java | 8 | 25 |
| Other mods | Optional: MalisisCore for the PDD, GDO and configurator, SpongeForge for permission nodes, and IndustrialCraft 2, ComputerCraft and OpenComputers for their integrations | None on NeoForge. Fabric API on Fabric |
| Jars | One Forge jar | One jar for each loader |

On Fabric, SGCraft carries Team Reborn Energy inside its jar, so its power unit takes energy from Fabric cables. See [Installing SGCraft](install.md).

## New in 3.0

- **The MALP.** A {{item:sgcraft:malp}} is a probe you drive through a gate from a {{item:sgcraft:malp_console}} or a {{item:sgcraft:malp_tablet}}. You watch its camera live, and the world around it streams to you out to 32 blocks across and 24 up and down by default. The radio reaches it directly within 256 blocks, or through an open wormhole. It runs on a {{item:sgcraft:battery}}, which you charge in a {{item:sgcraft:battery_charger}}, in a power unit's new battery slot, or with naquadah in the crafting grid. See [The MALP](malp.md).
- **The gate control tablet.** A {{item:sgcraft:gate_control_tablet}} keeps a list of gates. From anywhere, in any world, it dials from them, works their irises and opens their configurators. See [Gate control tablet](tools.md#gate-control-tablet).
- **Gate owners.** A gate belongs to the player whose block finished it. The owner has every right on it and decides who may do what. Gates in 2.1.0 had no owner. See [Owners](access.md#owners).
- **The `/sgcraft` commands.** 2.1.0 had one command, `/sgspawn`, which built a gate. 3.0's `/sgcraft` covers dialing, the iris, energy, gate properties, rights, owners, address rules, camouflage, world generation, naquadah ore, address books, the MALP and server settings, so admins can run every gate without a screen. `/sgcraft place` replaces `/sgspawn`. See [Commands](commands.md).
- **Your own address book.** `/sgcraft book` keeps addresses on the server for each player, apart from any PDD or tablet. See [Address books](addresses.md#address-books).
- **Fluid through the wormhole.** A gate standing in water, lava or another fluid pours it out of the far gate. See [Fluid through the wormhole](dialing.md#fluid-through-the-wormhole).
- **An add-on API.** 2.1.0 had no API library. Other mods could only listen for one Forge event, fired when a gate formed or broke. 3.0 publishes `sgcraft-api`, which add-ons build against on both loaders to find and dial gates, change their properties, add energy sources, rights, address books and gate types, and listen to events. See [API](api.md).

## Changed

### Addresses

Every address is new. A gate rebuilt where it stood in 2.1.0 gets a different address.

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Symbols | 36: `A` to `Z` and `0` to `9` | 37: `A` to `Z`, `0` to `9` and `@`, the point of origin |
| Lengths | 7 for a gate in the same dimension, 9 for any dimension | 7 for the same world, 8 for any world of the group, 9 for any world of any group |
| Made of | 7 position symbols, then 2 for the dimension | 6 position symbols, then the world's symbol, then the group's symbol, ending with `@` |
| Written as | `ABCD-EFG-HI` | `XOBF-R3@`, `XOBF-R3A-@` or `XOBF-R3A-@@` |
| Reach of the grid | 139,967 chunks (about 2,239,472 blocks) out from the center each way | About 373,248 blocks out each way with the default point of origin |
| Chevrons at the dialed gate | Had to be at least the length of the dialing gate's address, so a dial from another dimension needed 9 chevrons at both ends | Only the dialing gate needs chevrons |

Worlds are sorted into groups, each with its own symbol. SGCraft has one group, `default`. See [Addresses](addresses.md) and [World symbols and groups](addresses.md#world-symbols-and-groups).

### Chevrons and upgrades

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Chevrons | 7, or 9 with the upgrade | 7, 8 or 9 |
| {{item:sgcraft:chevron_upgrade}} | One upgrade took a gate from 7 chevrons to 9 | Each upgrade adds one chevron, up to 9. Breaking the base gives back one upgrade for each chevron added with one |
| Who can fit an upgrade | Any player, on any gate, idle or busy | A player with the gate's configure right, on an idle gate |
| {{item:sgcraft:pegasus_upgrade}} and {{item:sgcraft:universe_upgrade}} | Lost when the base broke | Given back when the base breaks. The gate keeps its type when the ring breaks and forms again |
| Configurator | Set the type, the chevron upgrade and the iris upgrade for anyone it opened for | Only operators and players in creative mode set the type, chevrons and iris. Everyone else uses the upgrade items |

See [Chevrons and upgrades](building-gates.md#chevrons-and-upgrades) and [Gate type upgrades](building-gates.md#gate-type-upgrades).

### Power and fuel

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Opening cost | `energyPerFuelItem` divided by `gateOpeningsPerFuelItem`: 96000 / 24 = 4000 | `dial_costs.open`, 4000 |
| Cost each tick open | `energyPerFuelItem` spread over `minutesOpenPerFuelItem` (80 minutes): 1 a tick | `dial_costs.per_tick`, 1 |
| Cost per chevron | None | `dial_costs.per_chevron`, 100 for each symbol dialed |
| Turning a cost off | Not possible on its own | Each cost has an `enabled` switch, on the server and on each gate |
| Other worlds | `interDimensionMultiplier` (4.0) was stored on each gate, but the cost to another dimension was multiplied by `distanceFactorMultiplier` (1.0) in its place, so it cost no more by default | `inter_world_multiplier`, 4.0, applies |
| Power unit | "SG Power Unit", taking Forge Energy | {{item:sgcraft:power_unit}}, taking FE on NeoForge and Team Reborn Energy on Fabric, with a battery slot |

What stayed the same: one naquadah gives 96000 gate energy, the distance factor follows the same formula, supergates cost 8 times as much, a power unit holds 4,000,000 FE at 80 FE to one gate energy, and a gate draws from power units within 4 blocks of its base. See [Power](power.md).

### The DHD

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Blocks | One, the Stargate Controller, for every gate type. It took the Pegasus look beside a Pegasus gate | {{item:sgcraft:dhd}} for Milky Way gates and {{item:sgcraft:dhd_pegasus}} for Pegasus gates. Universe gates have no DHD |
| Linking | To a gate whose base stood within a box around the DHD: 5 blocks to each side, 1 up or down, 6 to the front and back | To the nearest gate of its type within the gate's `dhd_range`, 6 blocks in a straight line by default, 0 to 32 for each gate |
| Fuel slots | 4 | 5 |

See [The DHD](dhd.md).

### Rights and access

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Players | A Player Access List on each gate, by player name, with gate, iris and admin access. By default everyone had all three | Rights on each gate, by player UUID: `sgcraft:use`, `sgcraft:iris` and `sgcraft:configure`. By default players have use and iris, and configure is the owner's |
| Admins | SpongeForge permission nodes, such as `sgcraft.admin`, `sgcraft.gui.configurator`, `sgcraft.gui.pdd`, `sgcraft.gui.gdo` and one for each configurator setting, when SpongeForge was installed | Operators (permission level 2) have every right. There are no permission nodes |
| Other gates | A Gate Address Access List with default incoming and outgoing switches | `allow_incoming`, `allow_outgoing` and address rules for single addresses |
| Breaking gates | Two gate settings: whether players may break player gates, and generated gates | Operators may, owners may while `owners_break_gates` is on, and anyone may when the gate's `breakable` property is on |

See [Access](access.md).

### Tools

| Tool | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| {{item:sgcraft:configurator}} | "Configurator". No recipe, and only with MalisisCore installed | Has a recipe. Five tabs (Gate, Energy, Travel, Access, Other) and the configure right to open it. It also opens a MALP's settings |
| {{item:sgcraft:pdd}} | "Personal Dialer Device", only with MalisisCore. Default addresses in `config/SGCraft/pdd.yml` | Built in. Default addresses in `config/sgcraft-default-addresses.json`, shared with the gate control tablet. It dials the shortest address that reaches the gate |
| {{item:sgcraft:gdo}} | "Handheld GDO", only with MalisisCore | "Garage Door Opener", built in. Same jobs: the near iris, the far iris once the wormhole is open, and disconnect |
| {{item:sgcraft:tollan_phase_shift_device}} | Worked for players holding it in the off hand | Works for any living traveler holding it in either hand, while the far gate's `phase_shift_passes_iris` is on |

See [Tools](tools.md).

### Gates and wormholes

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Gate settings | The `stargate` section's values were copied into each gate when it was made, so a change to the file reached only new gates, or every gate with `force-default-configs` | Each gate holds only the values set on it. `gate_defaults` gives a value to every gate without its own, at once |
| Lightning | Always on, 6 in 100. Sent the wormhole to a generated gate of the same world from `config/SGCraft/generator.yml` | 6 in 100, sent to a random generated gate the dialing gate can reach and may dial. The `lightning_malfunction` property turns it off for each gate |
| Breach explosion | One server setting for all gates: `explosionRadius` (10), `explosionFlame` and `explosionSmoke` | Gate properties `breach_explosion` (10), `breach_fire` and `breach_breaks_blocks` |
| Kawoosh damage | `transientDamageRate` (5000) for all gates | The gate property `opening_damage_amount` (5000) |

Supergates (radius 4 to 30), flat gates facing up or down, camouflage, gate spawners, the iris, the GDO, chunk loading and one way travel were already in 2.1.0, and 3.0 keeps them. See [Building gates](building-gates.md), [Dialing](dialing.md) and [The iris](iris.md).

### World generation and the Tok'ra

| | SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- | --- |
| Structure gates | Keys for each structure in the `generaton` section, such as `pyramid_addon_chance` | `structure_gates` entries, each with a structure template a data pack can replace |
| ZPM chests | Generated gates could come with a chest holding a ZPM (`*_zpm_chest_chance`, 15) | None |
| Village gates | A gate piece in village generation, which built a gate at a chance of `village_addon_chance` (25) | A gate plaza in the village's pool of houses (`weight` 2). One gate at most for each village, and none within 80 blocks of another |
| Tok'ra | A villager profession, spawned at generated gates where the structure's `*_spawn_tokra` key allowed it | A villager without a job can take a DHD as its job site and become a Tok'ra. `tokra_worlds` turns them on or off for each world. Trades in the data pack trade set `sgcraft:tokra/level_1` |
| Chest loot | Village blacksmith chests | Village weaponsmith and toolsmith chests |

The same six structures get gates (desert pyramids, jungle temples, ocean monuments, Nether fortresses, swamp huts and igloos), each at a 25% chance, with 9 chevrons one time in four. Igloo gates are still Pegasus gates. Naquadah ore generates as before, with the same defaults. The Tok'ra still trade 8 emeralds and a diamond for a core crystal, and 16 emeralds and a diamond for a controller crystal. See [World generation](worldgen.md).

### Recipes

Recipes are data pack recipes now. 2.1.0 let you turn each one off in the `recipes` section of its config file. In 3.0 a data pack changes or removes them.

- **New:** {{item:sgcraft:stargate_base_pegasus}} and {{item:sgcraft:stargate_base_universe}}, made from a Milky Way base (2.1.0 had them in the creative tab only), the {{item:sgcraft:dhd_pegasus}}, the {{item:sgcraft:configurator}}, the {{item:sgcraft:gate_control_tablet}}, and the MALP's {{item:sgcraft:malp}}, {{item:sgcraft:malp_console}}, {{item:sgcraft:malp_tablet}}, {{item:sgcraft:battery}} and {{item:sgcraft:battery_charger}}.
- **On by default:** {{item:sgcraft:core_crystal}} and {{item:sgcraft:controller_crystal}}. 2.1.0 had these recipes off unless you turned them on.
- **Gone:** the naquadah recipe from coal, a slime ball and blaze powder, which 2.1.0 had off by default, and the second shape of the Pegasus upgrade recipe.

The gate blocks, supergate blocks, Milky Way DHD, upgrades, iris parts, power unit, PDD and GDO keep their 2.1.0 recipes. See [Recipes](recipes.md).

### Item names

| SGCraft 2.1.0 | SGCraft 3.0 |
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

2.1.0 kept its settings in `config/sgcraft.cfg`, a Forge config file, and in YAML files in `config/SGCraft/`. 3.0 uses three JSON files: `config/sgcraft-server.json`, `config/sgcraft-client.json` and `config/sgcraft-default-addresses.json`. Many server settings can also be changed in game with `/sgcraft` commands, which write the file. See [Settings](settings.md).

| SGCraft 2.1.0 | SGCraft 3.0 |
| --- | --- |
| `stargate.energyPerFuelItem` | `energy_per_naquadah` |
| `stargate.gateOpeningsPerFuelItem`, `stargate.minutesOpenPerFuelItem` | `dial_costs.open`, `dial_costs.per_tick` |
| `stargate.distanceFactorMultiplier`, `stargate.interDimensionMultiplier` | `distance_factor_multiplier`, `inter_world_multiplier` |
| `supergate.energyMultiplier` | `supergate_energy_multiplier` |
| `fp-power.energyBufferSize` | `power_unit_capacity` |
| `fp-power.fpPerSGEnergyUnit` (80) | `bridge_rates` with `sgcraft:forge_energy` (0.0125, one in 80) |
| `stargate.toolSearchRange` | `tool_range` |
| `stargate.wormholeCanDestroyUnbreakableBlocks` | `kawoosh_breaks_unbreakable` |
| `options.chunkLoadingRange`, `options.logStargateEvents` | `chunk_loading_range`, `log_stargate_events` |
| `options.enableNaquadahOre`, `options.addOresToExistingWorlds` and the `naquadah` section | The `naquadah_ore` object |
| The other per-gate keys of the `stargate`, `iris`, `dhd`, `gate-access` and `player-access` sections | `gate_defaults`, by gate property, such as `sgcraft:open_ticks` (in ticks, where 2.1.0 used `secondsToStayOpen`) |
| `stargate.irisUpgrade`, `stargate.chevronUpgrade` | `gate_defaults` with `sgcraft:iris` and `sgcraft:chevrons` |
| The `generaton` section | `structure_gates` and `village_gates` |
| `config/SGCraft/tokra.yml` (written by 2.1.0 but not used) | `tokra_worlds` |
| `config/SGCraft/pdd.yml` | `config/sgcraft-default-addresses.json` |
| `client.useHDEventHorizonTexture`, `client.saveAddressToClipboard`, `stargate.soundVolume` | `hd_event_horizon`, `copy_address_on_open` and `sound_volume` in `config/sgcraft-client.json` |
| The `recipes` section | None. Use a data pack |

## Not ported yet

These parts of 2.1.0 are not in SGCraft 3.0.

| SGCraft 2.1.0 | In 3.0 |
| --- | --- |
| ComputerCraft: the ComputerCraft Stargate Interface block | Not ported |
| OpenComputers: the OpenComputers Stargate Interface block, and gates that passed wireless network messages through an open wormhole | Not ported |
| IndustrialCraft 2: the IC2 Stargate Power Unit, which took EU, and the Ridiculously Large Capacitor | Not ported. The power unit takes FE, or Team Reborn Energy on Fabric |
| ZPMs: the Zero Point Module, the ZPM Console, and the ZPM Interface Cart and ZPM Hub (both with IC2), along with the routes in `config/SGCraft/zpm.yml` that needed a ZPM and the ZPM chests at generated gates | Not part of SGCraft 3.0 |
| SpongeForge permission nodes | Not ported. Rights and operator levels decide who may do what (see [Access](access.md)) |

SGCraft 3.0 has no computer mod integration. Add-ons can build one on the [API](api.md), which lets them find, dial and change gates and listen to their events. ZPMs are not part of the base mod. The API also lets an add-on give gates an energy source of its own (see [Example: an energy source](api.md#example-an-energy-source)).

2.1.0 had no RedstoneFlux or Mystcraft code: its power unit took Forge Energy, which 3.0's power unit still takes on NeoForge.

## Upgrading a world

1.12 worlds and gates don't carry over. Opening a 1.12 world in Minecraft 26.3 does not bring its SGCraft blocks, items or gates along, and SGCraft 3.0 has no converter: it reads none of 2.1.0's gates, DHDs, items, PDD lists or addresses. Block and item IDs have changed too.

- Start a new world, or build your gates again.
- Write down the addresses you need again after you rebuild. Because the address format changed, each gate gets a new address, even in the same spot.
- Settings don't carry over either. 3.0 does not read `config/sgcraft.cfg` or the files in `config/SGCraft/`. It writes its JSON files with their defaults the first time it starts. Use the [configuration table](#configuration-files) above to set them again.

See [Upgrading from older SGCraft](install.md#upgrading-from-older-sgcraft).
