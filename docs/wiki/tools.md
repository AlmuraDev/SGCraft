# Tools

Five handheld tools work gates: the PDD dials from your pocket, the gate control tablet works gates from anywhere, the Explorer Dialer Device dials the gates the world built, the GDO works irises and shields, and the configurator changes a gate's settings. The Tollan Phase Shift Device takes you through a shut iris.

## How a tool finds its gate

Use a tool in the air or on a block to open its screen. The PDD, the tablet's **Add this gate**, the Explorer Dialer Device, the GDO and the configurator work on the gate near you, found in this order:

1. The gate you look at, through its ring or its opening, up to 6 blocks away (or `tools.range` if that is more).
2. The gate whose base or [DHD](dhd.md) is nearest your feet, within `tools.range` blocks (6) along each axis. A DHD stands for the gate it is linked to.
3. The gate whose ring is nearest your eyes, within `tools.range` blocks.

The GDO and the configurator stay on the gate they opened on. Their buttons stop working if you move more than 16 blocks from the gate's base (`tools.range` plus 10), and the tool says "Could not detect a gate near you".

Each tool shows what a gate is doing with a colored word: **Idle** (green), **Dialing** (yellow), **Incoming** (gold), **Opening** (yellow), **Connected** (aqua), **Closing** (gray), or **No gate** (red) when no gate is at an address.

## Personal Dialing Device

```infobox
Personal Dialing Device
Icon: sgcraft:pdd
Type: Item
Stacks: 1
Range: 6 blocks (tools.range)
Holds: Its own address book
Dictionary: [Personal Dialing Device](items/pdd.md)
```

The PDD dials the gate near you from a list of addresses kept on the PDD itself. The list travels with the item, so you can hand a PDD to another player with its addresses.

The screen shows:

- The gate near you: "This gate: <address>, <state>". With no gate near you it says "No gate near you. You can still change its addresses". A gate with **Address shown** off shows "address hidden".
- The PDD's addresses, each with a name and what the gate at that address is doing, and "Addresses: <count>". The **Search** box above them narrows the list by name, world or address, and the red **x** empties it.
- **Name** and **Address** boxes. Choosing an entry fills them.
- **Add**, **Rename**, **Remove**, **Up** and **Down** for the list. **Up** and **Down** are off while you search. Names are cut to 48 characters. An entry without a name is named by its address.
- **Dial** dials the address in the box from the gate near you. **Disconnect** closes its wormhole or stops its dial. **Add this gate** adds the near gate to the address book: it puts the gate's address in the box, and in the name box too if that is empty, and saves the entry. **Close** closes the screen.

Dialing needs the `use` right on the gate (see [Access](access.md)). The ring turns for each symbol unless the gate has **Chevrons lock at once** on. A PDD keeps each address in the form that names its world, and dials the shortest form from the gate's world. See [Addresses](addresses.md).

The first time you use a new PDD, it takes the server's default addresses. A default address the server locked can be moved but not renamed or removed: "That address came with the server's defaults and is locked".

```recipe
sgcraft:pdd
```

## Gate control tablet

```infobox
Gate Control Tablet
Icon: sgcraft:gate_control_tablet
Type: Item
Stacks: 1
Range: Any gate on its list, in any world
Holds: A list of gates and its own address book
Dictionary: [Gate Control Tablet](items/gate-control-tablet.md)
```

The tablet is a PDD for many gates. On the left is a list of gates, each with its name, what it is doing, and "Iris shut" when its iris is closed. On the right are the tablet's addresses. Choose a gate and an address, then press **Dial** to dial one from the other, wherever the gate is.

Buttons beside the gates:

- **Add this gate** adds the gate near you to the list, named as the name box says, or by its address. It needs the `use` right on that gate. With no gate near, the screen says "No gate near you to add".
- **Rename**, **Remove**, **Up**, **Down** work on the chosen gate.
- **Save address** adds the chosen gate's address to the addresses on the right, under its name.
- **Configure** opens the chosen gate's [configurator](#stargate-configurator), wherever the gate is. It needs the `configure` right.
- **Open iris** and **Close iris** work the chosen gate's iris. They need the `iris` right. For a gate with a shield they read **Deactivate Shield** and **Activate Shield**, and the list shows "Shield up" while its shield is raised.

Beside the addresses: **Add**, **Rename**, **Remove**, **Up** and **Down**, as on the PDD.

Along the bottom: **Dial** dials the address in the box from the chosen gate. **Disconnect** closes the chosen gate's wormhole or stops its dial. **Close** closes the screen.

The screen also shows the gate near you ("Near you: <address>, <state>"), "Gates: <count>" and "Addresses: <count>". An empty list says "Stand by a gate and add it". Like a PDD, a new tablet takes the server's default addresses the first time you use it.

```recipe
sgcraft:gate_control_tablet
```

## Explorer Dialer Device

```infobox
Explorer Dialer Device
Icon: sgcraft:edd
Type: Item
Stacks: 1
Range: 6 blocks (tools.range), to dial from
Lists: Every generated gate, in every world
Dictionary: [Explorer Dialer Device](items/edd.md)
```

The Explorer Dialer Device lists every gate that world generation built, in every world, sorted by world and then by address. You don't add gates to it: the list comes from the world.

The header reads "Generated Addresses: (N) - Visited: V". Each row shows the name you gave the gate (or "Unnamed gate"), its address in letters and glyphs, its world, and **Visited** in green or **Not visited**. The list refreshes every 3 seconds. The **Search** box above it narrows it by name, world or address, and the red **x** empties it.

- **Dial** dials the chosen gate from the gate near you. Double-clicking an entry dials it too. With no gate near, the screen says "No gate near you to dial from".
- **Disconnect** closes the near gate's wormhole or stops its dial.
- **Rename** gives the chosen gate the name in the **Name** box, up to 40 characters. A blank name clears it.
- **Close** closes the screen.

A generated gate counts as visited when you use the device at it, or when you go through any gate as a player (both ends count, if they are generated). Your visits and names are kept with the world, and survive death. `/sgcraft explorer` shows your list in chat.

```recipe
sgcraft:edd
```

## Garage Door Opener

```infobox
Garage Door Opener
Icon: sgcraft:gdo
Type: Item
Stacks: 1
Range: 6 blocks (tools.range), and the far end of an open wormhole
Dictionary: [Garage Door Opener](items/gdo.md)
```

The GDO works the [iris](iris.md) of the gate near you and, once its wormhole is open, the iris of the gate at the other end. It works a [shield](iris.md#the-shield) the same way.

- **This gate**: the near gate's iris state, with **Open Iris** and **Close Iris**. A gate without an iris shows "No iris".
- **The other end**: the same for the far gate, shown once the wormhole is connected. Until then the screen says "No wormhole open".
- **Disconnect** closes the wormhole. It shows only while the wormhole is opening or open.

For a gate with a shield, the buttons read **Activate Shield** and **Deactivate Shield**, and the state shows as **Shield Active**, **Raising Shield...**, **Lowering Shield...** or **Shield Inactive**.

Each iris needs the `iris` right on its own gate. **Disconnect** needs the `use` right. With no gate near you, the GDO says "Could not detect a gate near you".

To work a shield from a fixed place, use a [Shield Console](blocks/shield-console.md), a block that works the nearest shielded gate within 40 blocks.

```recipe
sgcraft:gdo
```

## Stargate Configurator

```infobox
Stargate Configurator
Icon: sgcraft:configurator
Type: Item
Stacks: 1
Range: 6 blocks (tools.range)
Needs: The configure right on the gate
Dictionary: [Stargate Configurator](items/configurator.md)
```

The configurator changes every setting a gate has. Use it near a gate you have the `configure` right on. Without the right, it says "You aren't allowed to configure this gate". Used on a [MALP](malp.md#the-configurator), it opens the MALP's settings.

The title shows the gate's address ("?" when hidden). Five tabs hold the settings: **Gate**, **Energy**, **Travel**, **Access** and **Other**. Rest the pointer on a setting to read what it does.

A setting the gate has no value of its own for shows in gray, with what applies instead: the server's value, or the setting's default. Leave a number box empty to use that value. Changes wait until you press **Save**, which sends only what changed and shows "Changes saved" or what went wrong. **Revert** drops unsaved changes. **Close** closes the screen.

The gate must be idle to change its type, chevrons or orientation.

> **Note:** In survival you change a gate's type, chevrons and iris with the upgrade items: a gate upgrade, a chevron
> upgrade, an iris upgrade or a shield upgrade. Only operators and players in creative mode set these three freely in the configurator;
> for everyone else they show as locked.

### Gate page

| Setting | Property | Default | What it does |
| --- | --- | --- | --- |
| Type | | | Milky Way, Pegasus or Universe, and Ori for a supergate. Operators and creative players only |
| Chevrons | | | 7, 8 or 9. Operators and creative players only. An Ori supergate always has 9 |
| Iris | `sgcraft:iris` | None | **None**, **Metal Iris** or **Shield**, the kinds the gate's type takes. An Ori supergate takes none. Operators and creative players only. Taking off an iris or shield fitted with its upgrade, or swapping it, gives the upgrade back, unless you are in creative mode |
| Open seconds | `sgcraft:open_ticks` | 300 (6000 ticks) | How long a wormhole stays open before it shuts on its own. 0 for no limit |
| Address shown | `sgcraft:address_visible` | on | Whether players see the gate's address on the gate and in tools |
| Breakable | `sgcraft:breakable` | off | Whether any player can break the gate. Only the owner or an operator can change it |
| Chevrons lock at once | `sgcraft:chevrons_lock_on_dial` | off | Whether a DHD, PDD or tablet locks every chevron at once, with no ring turning |
| See-through horizon | `sgcraft:horizon_transparent` | on | Whether a little of what is behind the open wormhole shows through |
| Ring speed | `sgcraft:ring_speed` | 2 (Pegasus about 7.14) | How fast the gate dials, in degrees a tick, from 0.1 to 360 |
| DHD reach | `sgcraft:dhd_range` | 6 | How far from the base, in blocks, a DHD can stand and still link to the gate, up to 32 |
| Built by | `sgcraft:generated` | Player | Shows whether the world or a player built the gate. It can't be changed here |
| Lies | | | **Upright** for a standing gate, which can't turn. A gate lying flat turns **Flat, face up** or **Flat, face down** |

### Energy page

Only operators change this page's settings, apart from **DHD fuel powers it**. Anyone else sees them grayed out. See [Per-gate settings](power.md#per-gate-settings).

| Setting | Property | Default | What it does |
| --- | --- | --- | --- |
| Needs energy | `sgcraft:requires_power` | on | Whether dialing out costs this gate energy. Off, it dials for free |
| Reserve | `sgcraft:energy_capacity` | 1000 | How much gate energy the gate holds itself |
| Chevron cost, Per chevron | `sgcraft:chevron_cost_enabled`, `sgcraft:chevron_cost` | on, 100 | Energy paid as each chevron locks |
| Opening cost, Opening | `sgcraft:open_cost_enabled`, `sgcraft:open_cost` | on, 4000 | Energy paid as the wormhole forms |
| Tick cost, Per tick | `sgcraft:tick_cost_enabled`, `sgcraft:tick_cost` | on, 1 | Energy paid each tick the wormhole is open |
| DHD fuel powers it | `sgcraft:dhd_fuel` | on | Whether naquadah in linked DHDs powers the gate |
| Per naquadah | `sgcraft:energy_per_naquadah` | 96000 | Gate energy one naquadah gives in a linked DHD |
| Distance factor | `sgcraft:distance_factor_multiplier` | 1.0 | How much more a wormhole costs the further it reaches. 0 for no extra |
| Other worlds | `sgcraft:inter_world_multiplier` | 10.0 | How many times more a dial with an eighth chevron costs, to another world of the group |

The cost defaults are the server's. **Use server's costs** clears the gate's own costs, per naquadah, distance factor and other worlds values. Under the settings, the page shows how many FE from other mods one gate energy is worth. See [Power](power.md).

### Travel page

| Setting | Property | Default | What it does |
| --- | --- | --- | --- |
| One way travel | `sgcraft:one_way_travel` | on | Travelers only go from the dialing gate to the dialed gate |
| Reverse travel kills | `sgcraft:reverse_travel_kills` | off | Stepping into the dialed gate's wormhole while travel is one way kills |
| Close from either end | `sgcraft:close_from_either_end` | on | The dialed gate can close the wormhole too |
| Kawoosh damage | `sgcraft:opening_damage` | on | The burst of a wormhole opening harms what stands in front of the gate |
| Fluid pours through | `sgcraft:spills_fluid` | on | Water and other fluids pour through to the other gate |
| Phase shift passes iris | `sgcraft:phase_shift_passes_iris` | on | Someone holding a [Tollan Phase Shift Device](#tollan-phase-shift-device) passes through this gate's shut iris |
| Iris shuts again after | `sgcraft:iris_restores_state` | off | The iris goes back to how it was before the wormhole opened, once it closes |
| Iris deaths keep items | `sgcraft:preserve_inventory` | off | A player the gate kills keeps what they carry |
| Redstone works iris | `sgcraft:redstone_input` | on | A redstone signal at the base shuts the iris, and the signal ending opens it |
| Redstone signal out | `sgcraft:redstone_output` | on | The gate gives out a redstone signal while it is busy |
| Kawoosh harm | `sgcraft:opening_damage_amount` | 5000 | Harm the kawoosh does each tick. 20 is a player's full health |
| Breach blast | `sgcraft:breach_explosion` | 10 | How strong an explosion goes off if the gate breaks while its wormhole is open. TNT is 4, 0 for none |
| Breach fire | `sgcraft:breach_fire` | on | Whether that explosion sets fires |
| Breach breaks blocks | `sgcraft:breach_breaks_blocks` | on | Whether that explosion breaks blocks |
| Storms send it astray | `sgcraft:lightning_malfunction` | on | Lightning in the rain can send a wormhole this gate dials to another gate |

See [Dialing](dialing.md) and [The iris](iris.md) for what these do in play.

### Access page

The owner, incoming and outgoing settings, default rights, listed players and address rules. See [Access](access.md#the-configurators-access-page).

### Other page

Settings that add-ons give gates, eight to a page, each written as JSON. Leave a box empty for its default, shown in gray. With no add-on settings, the page says "No add-on has given gates any settings".

```recipe
sgcraft:configurator
```

## Tollan Phase Shift Device

```infobox
Tollan Phase Shift Device
Icon: sgcraft:tollan_phase_shift_device
Type: Item
Stacks: 1
Recipe: None
Dictionary: [Tollan Phase Shift Device](items/tollan-phase-shift-device.md)
Get it: Creative tab or commands
```

Hold the device in either hand when you step into a wormhole. If the far gate's iris is shut, you pass through it instead of dying against it, as long as that gate's **Phase shift passes iris** setting is on (it is by default).

The device has no recipe. Get it from the creative tab, or have an operator give it with `/give <player> sgcraft:tollan_phase_shift_device`.

## Server admins

- `tools.range` (6) sets how far from a player the tools look for a gate.
- The default addresses that new PDDs and tablets start with are in `config/sgcraft-default-addresses.json`. Operators change them with `/sgcraft default_addresses add|remove|lock|unlock`. See [Addresses](addresses.md).
- Every configurator setting is also a gate property: `/sgcraft property <id> set <json>` and `/sgcraft property <id> reset` set and clear it, and `/sgcraft gate_defaults` gives gates values without their own. See [Settings](settings.md) and [Commands](commands.md).
