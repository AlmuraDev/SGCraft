# Battery

The Battery stores gate energy (GE) for the MALP and the MALP tablet. You charge it in a power unit, a battery charger or the crafting grid.

```infobox
Battery
ID: sgcraft:battery
Category: Power
Type: Item
Stackable: No
Capacity: 12,500 GE (`battery_capacity`)
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

A battery shows its charge as a green bar on the item. Its tooltip shows the exact charge as "<held> / <capacity> GE (<percent>%)", such as "6,250 / 12,500 GE (50%)".

## Charging

| Where | How fast | Notes |
| --- | --- | --- |
| A {{item:sgcraft:power_unit}}'s battery slot | 12.5 GE a tick (`power_unit_charge_rate`) | Takes GE from the unit's store |
| A {{item:sgcraft:battery_charger}} | 12.5 GE a tick (`charger_charge_rate`) | The charger burns naquadah or takes FE from cables |
| The crafting grid | At once | 96,000 GE for each {{item:sgcraft:naquadah}} (`energy.per_naquadah`), so one piece fills it |
| Other mods' chargers | As fast as they charge | On NeoForge it is a NeoForge energy item. On Fabric it is a Team Reborn Energy item. Each 80 FE adds 1 GE |

### Recharging with naquadah

```recipe
sgcraft:battery_recharge
```

Put one battery and any number of naquadah in the crafting grid, in any arrangement. Nothing else may be in the grid. The battery comes out holding 96,000 GE more for each naquadah, up to its capacity, and the naquadah is used up. One naquadah fills an empty battery.

- A full battery doesn't craft, so no naquadah is lost on it.
- Energy past the capacity is lost. One naquadah gives far more than a battery holds, so use one piece at a time.
- Two batteries in the grid don't craft.

## Behavior

- Other mods see a battery's charge as FE, 80 FE for each GE it holds. They can take FE out of a battery as well as put it in.
- The capacity is the server's `battery_capacity` setting. If the server lowers it, a battery counts as holding no more than the new capacity.
- A MALP that is destroyed drops its battery. A MALP you pick up keeps its battery in the item.

## Data components

| Component | What it holds |
| --- | --- |
| `sgcraft:energy` | The GE the battery holds. A battery without it holds none |

## Commands

These need permission level 2.

- `/sgcraft battery` shows the charge of the battery in your main hand: "Your battery holds <held> of <capacity> GE".
- `/sgcraft battery set <ge>` sets it in GE, kept within the capacity.
- `/sgcraft malp battery <malp> <ge>` sets the charge of a MALP's battery in GE, giving it one if it has none. It replies "<malp>'s battery holds <held> of <capacity> GE".

## Server admins

The battery settings are in the `malp` section of `config/sgcraft-server.json`, except what naquadah gives, which is `energy.per_naquadah`:

| Setting | Default | What it does |
| --- | --- | --- |
| `malp.battery_capacity` | 12500 | GE a battery holds |
| `malp.power_unit_charge_rate` | 12.5 | GE a tick a power unit puts into its battery |
| `malp.charger_charge_rate` | 12.5 | GE a tick a battery charger puts into its battery |
| `energy.per_naquadah` | 96000 | GE one naquadah gives a battery, crafted in or burned in a charger, the same as it gives a gate |

## See also

- [Batteries and the battery charger](../power.md#batteries-and-the-battery-charger)
- [Batteries and charging](../malp.md#batteries-and-charging)
- [MALP settings](../settings.md#malp)
- [Commands](../commands.md#malp-and-views)
