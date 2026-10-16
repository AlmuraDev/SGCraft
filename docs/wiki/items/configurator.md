# Stargate Configurator

The Stargate Configurator changes every setting a gate has, over five pages. Used on a MALP, it opens the MALP's settings.

```infobox
Stargate Configurator
ID: sgcraft:configurator
Category: Tools
Type: Item
Stackable: No
Range: The gate near you, 6 blocks (`tool_range`). A MALP within your reach
Right needed: `configure` on the gate. For a MALP, its owner or an operator
```

## Obtaining

### Crafting

```recipe
sgcraft:configurator
```

## Usage

1. Stand near a gate you have the `configure` right on and use the configurator, in the air or on a block.
2. Choose a page: **Gate**, **Energy**, **Travel**, **Access** or **Other**.
3. Change what you want, then press **Save**.

The configurator finds the gate near you the same way every tool does:

1. The gate you look at, through its ring or its opening, up to 6 blocks away (or `tool_range` if that is more).
2. The gate whose base or {{item:sgcraft:dhd}} is nearest your feet, within `tool_range` blocks (6) along each axis. A DHD stands for the gate it is linked to.
3. The gate whose ring is nearest your eyes, within `tool_range` blocks.

With no gate near you it says "Could not detect a gate near you". Without the right it says "You aren't allowed to configure this gate".

The screen stays on the gate it opened on. **Save** stops working once you are more than 16 blocks from the gate's base (`tool_range` plus 10). A {{item:sgcraft:gate_control_tablet}} can open the configurator of any gate on its list, and then **Save** works from anywhere while you hold the tablet.

## Screen

The title shows the gate's address: "Configurator: <address>", or "?" when the address is hidden. Rest the pointer on a setting to read what it does.

A setting the gate has no value of its own for shows in gray, with what applies instead: the server's value for gates, or the setting's default. Leave a number box empty to use that value.

| Button | What it does |
| --- | --- |
| **Save** | Sends only what changed. The screen shows "Changes saved" in green, or what went wrong in red |
| **Revert** | Drops the changes you haven't saved |
| **Close** | Closes the screen |

The gate must be idle to change its type, chevrons or orientation.

## Upgrades and the configurator

In survival you change a gate's type, chevrons and iris with upgrade items: a {{item:sgcraft:pegasus_upgrade}} or {{item:sgcraft:universe_upgrade}}, a {{item:sgcraft:chevron_upgrade}}, or a {{item:sgcraft:iris_upgrade}}. In the configurator these three show as locked, and the tip names the item that changes them.

Operators and players in creative mode set them freely. Taking off an iris that was fitted with an iris upgrade gives the upgrade back, unless you are in creative mode. An iris set with the configurator gives nothing back.

## Gate page

| Setting | Property | Default | What it does |
| --- | --- | --- | --- |
| Type | | | Milky Way, Pegasus or Universe. Operators and creative players only |
| Chevrons | | | 7, 8 or 9. Operators and creative players only |
| Iris fitted | `sgcraft:iris` | off | Fits a metal iris or takes it off. Operators and creative players only |
| Open seconds | `sgcraft:open_ticks` | 300 (6000 ticks) | How long a wormhole stays open before it shuts on its own. 0 for no limit |
| Address shown | `sgcraft:address_visible` | on | Whether players see the gate's address on the gate and in tools |
| Breakable | `sgcraft:breakable` | off | Whether any player can break the gate. Only the owner or an operator changes it |
| Chevrons lock at once | `sgcraft:chevrons_lock_on_dial` | off | Whether a DHD, PDD or tablet locks every chevron at once, with no ring turning |
| See-through horizon | `sgcraft:horizon_transparent` | on | Whether a little of what is behind the open wormhole shows through |
| Ring speed | `sgcraft:ring_speed` | 2 (Pegasus about 7.14) | How fast the gate dials, in degrees a tick, from 0.1 to 360 |
| DHD reach | `sgcraft:dhd_range` | 6 | How far from the base, in blocks, a DHD can stand and still link to the gate, up to 32 |
| Built by | `sgcraft:generated` | Player | Whether the world or a player built the gate. It can't be changed here |
| Lies | | | **Upright** for a standing gate, which can't turn. A gate lying flat turns **Flat, face up** or **Flat, face down** |

## Energy page

| Setting | Property | Default | What it does |
| --- | --- | --- | --- |
| Needs energy | `sgcraft:requires_power` | on | Whether dialing out costs this gate energy |
| Reserve | `sgcraft:energy_capacity` | 1000 | How much gate energy the gate holds itself |
| Chevron cost, Per chevron | `sgcraft:chevron_cost_enabled`, `sgcraft:chevron_cost` | on, 100 | Energy paid as each chevron locks |
| Opening cost, Opening | `sgcraft:open_cost_enabled`, `sgcraft:open_cost` | on, 4000 | Energy paid as the wormhole forms |
| Tick cost, Per tick | `sgcraft:tick_cost_enabled`, `sgcraft:tick_cost` | on, 1 | Energy paid each tick the wormhole is open |
| DHD fuel powers it | `sgcraft:dhd_fuel` | on | Whether naquadah in linked DHDs powers the gate |
| Per naquadah | `sgcraft:energy_per_naquadah` | 96000 | Gate energy one naquadah gives in a linked DHD |
| Distance factor | `sgcraft:distance_factor_multiplier` | 1.0 | How much more a wormhole costs the further it reaches. 0 for no extra |
| Other worlds | `sgcraft:inter_world_multiplier` | 4.0 | How many times more a wormhole to another world costs |

The cost defaults are the server's. **Use server's costs** clears the gate's own costs, per naquadah, distance factor and other worlds values. Under the settings the page says "Amounts are gate energy, before distance: one is 80 FE from a power unit", with the rate the server uses. See [Power](../power.md#per-gate-settings).

## Travel page

| Setting | Property | Default | What it does |
| --- | --- | --- | --- |
| One way travel | `sgcraft:one_way_travel` | on | Travelers only go from the dialing gate to the dialed gate |
| Reverse travel kills | `sgcraft:reverse_travel_kills` | off | Stepping into the dialed gate's wormhole while travel is one way kills |
| Close from either end | `sgcraft:close_from_either_end` | on | The dialed gate can close the wormhole too |
| Kawoosh damage | `sgcraft:opening_damage` | on | The burst of a wormhole opening harms what stands in front of the gate |
| Fluid pours through | `sgcraft:spills_fluid` | on | Water and other fluids pour through to the other gate |
| Phase shift passes iris | `sgcraft:phase_shift_passes_iris` | on | Someone holding a {{item:sgcraft:tollan_phase_shift_device}} passes through this gate's shut iris |
| Iris shuts again after | `sgcraft:iris_restores_state` | off | The iris goes back to how it was before the wormhole opened, once it closes |
| Iris deaths keep items | `sgcraft:preserve_inventory` | off | A player the iris kills keeps what they carry |
| Redstone works iris | `sgcraft:redstone_input` | on | A redstone signal can shut the iris |
| Redstone signal out | `sgcraft:redstone_output` | on | The gate gives out a redstone signal while it is busy |
| Kawoosh harm | `sgcraft:opening_damage_amount` | 5000 | Harm the kawoosh does each tick. 20 is a player's full health |
| Breach blast | `sgcraft:breach_explosion` | 10 | How strong an explosion goes off if the gate breaks while its wormhole is open. TNT is 4, 0 for none |
| Breach fire | `sgcraft:breach_fire` | on | Whether that explosion sets fires |
| Breach breaks blocks | `sgcraft:breach_breaks_blocks` | on | Whether that explosion breaks blocks |
| Storms send it astray | `sgcraft:lightning_malfunction` | on | Lightning in the rain can send a wormhole this gate dials to another gate |

## Access page

- At the top: "Owner: <name>", or "Owner: none, so operators manage it". A player who doesn't manage the gate also sees "Only the owner or an operator changes rights".
- **Incoming allowed** and **Outgoing allowed**.
- **All: use**, **All: iris** and **All: configure**: the default rights. Only the owner or an operator changes them.
- The listed players, five at a time, each with **use**, **iris** and **configure** switches and an **x** button that takes them off the list. **<** and **>** turn the pages.
- **Player name** and **List player**: lists a player with the gate's current default rights. **List player** saves at once, with any other changes you made. The player must have been seen on the server.
- The address rules, each with an **In** and an **Out** switch and an **x** button. Type an address in **Address** and press **Add rule** to add a rule that allows both ways.

See [The configurator's Access page](../access.md#the-configurators-access-page).

## Other page

Settings that add-ons give gates, eight to a page, each written as JSON. Leave a box empty for its default, shown in gray. With no add-on settings, the page says "No add-on has given gates any settings".

## MALP settings

Use the configurator on a {{item:sgcraft:malp}} to open the **MALP Configurator** screen. Only the MALP's owner and operators can. Anyone else is told "You aren't allowed to configure this MALP". Stay within your reach of the MALP plus 4 blocks, or the screen's buttons say "That MALP isn't near you".

The screen shows the owner ("Owner: <name>", or "Owner: none, so only operators configure it") and the console the MALP is paired with, or "Not paired".

| Control | What it does |
| --- | --- |
| **Requires power** | Whether the MALP runs on its battery. Off, it uses no FE and works with a flat battery or none |
| **Save** | Applies the change |
| **Revert** | Goes back to the saved settings |
| **Unpair** | Unpairs the MALP from its console at once, without waiting for **Save**. Anyone driving it from there loses it |
| **Close** | Leaves without saving |

See [The MALP's configurator](../malp.md#the-configurator).

## Rights

Opening and saving need the `configure` right on the gate. The right doesn't cover the default rights, the listed players, **Breakable** or the owner: only the owner, an operator or something that isn't a player changes those. See [Managed settings](../access.md#managed-settings).

The configurator keeps no data on the item.

## Server admins

Every setting is also a gate property. `/sgcraft property <id> set <json>` and `/sgcraft property <id> reset` set and clear one, and `/sgcraft gate_defaults` gives gates values without their own. See [Gate properties](../settings.md#gate-properties) and [Commands](../commands.md#properties-and-rights).

## See also

- [Tools](../tools.md#stargate-configurator)
- [Access](../access.md)
- [Power](../power.md)
- [Building gates](../building-gates.md#chevrons-and-upgrades)
- [The MALP](../malp.md#the-configurator)
