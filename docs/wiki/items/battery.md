# Battery

The Battery stores FE for the MALP and the MALP tablet. You charge it in a power unit, a battery charger or the crafting grid.

```infobox
Battery
ID: sgcraft:battery
Category: Power
Type: Item
Stackable: No
Capacity: 1,000,000 FE (`battery_capacity`)
Used by: MALP, MALP tablet
Charged by: Power unit, battery charger, naquadah, other mods' chargers
```

## Obtaining

### Crafting

A new battery is empty.

```recipe
sgcraft:battery
```

### Creative tab

The SGCraft creative tab has two batteries: an empty one, and right after it a full one, holding as much as the server's batteries hold.

## Usage

Put a charged battery in the battery slot of a {{item:sgcraft:malp}} (use the MALP with an empty hand) or of a {{item:sgcraft:malp_tablet}} (sneak and use the tablet in the air). Gates don't use batteries.

A battery shows its charge as a green bar on the item. Its tooltip shows the exact charge as "<held> / <capacity> FE (<percent>%)".

## Charging

| Where | How fast | Notes |
| --- | --- | --- |
| A {{item:sgcraft:power_unit}}'s battery slot | 1000 FE a tick (`power_unit_charge_rate`) | Takes FE from the unit's store |
| A {{item:sgcraft:battery_charger}} | 1000 FE a tick (`charger_charge_rate`) | The charger burns naquadah or takes FE from cables |
| The crafting grid | At once | 100,000 FE for each {{item:sgcraft:naquadah}} (`fe_per_naquadah`) |
| Other mods' chargers | As fast as they charge | On NeoForge it is a NeoForge energy item. On Fabric it is a Team Reborn Energy item |

### Recharging with naquadah

```recipe
sgcraft:battery_recharge
```

Put one battery and any number of naquadah in the crafting grid, in any arrangement. Nothing else may be in the grid. The battery comes out holding 100,000 FE more for each naquadah, up to its capacity, and the naquadah is used up.

- A full battery doesn't craft, so no naquadah is lost on it.
- FE past the capacity is lost. A battery holding 950,000 FE gains only 50,000 FE from one naquadah, so add only as many as it has room for.
- Two batteries in the grid don't craft.

## Behavior

- Other mods can take FE out of a battery as well as put it in.
- The capacity is the server's `battery_capacity` setting. If the server lowers it, a battery counts as holding no more than the new capacity.
- A MALP that is destroyed drops its battery. A MALP you pick up keeps its battery in the item.

## Data components

| Component | What it holds |
| --- | --- |
| `sgcraft:energy` | The FE the battery holds. A battery without it holds none |

## Commands

These need permission level 2.

- `/sgcraft battery` shows the charge of the battery in your main hand: "Your battery holds <held> of <capacity> FE".
- `/sgcraft battery set <fe>` sets it, kept within the capacity.
- `/sgcraft malp battery <malp> <fe>` sets the charge of a MALP's battery, giving it one if it has none.

## Server admins

The battery settings are in the `malp` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `battery_capacity` | 1000000 | FE a battery holds |
| `power_unit_charge_rate` | 1000 | FE a tick a power unit puts into its battery |
| `charger_charge_rate` | 1000 | FE a tick a battery charger puts into its battery |
| `fe_per_naquadah` | 100000 | FE one naquadah gives a battery, crafted in or burned in a charger |

## See also

- [Batteries and the battery charger](../power.md#batteries-and-the-battery-charger)
- [Batteries and charging](../malp.md#batteries-and-charging)
- [MALP settings](../settings.md#malp)
- [Commands](../commands.md#malp-and-views)
