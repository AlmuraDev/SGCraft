# The iris

An iris closes a gate's opening. While it is shut, nothing comes through the gate: travelers arriving through the wormhole die against it. A gate has no iris, the metal iris or the [shield](#the-shield), an energy barrier that rises in half a second and uses energy while it is up.

```infobox
Metal Iris
Icon: sgcraft:iris_upgrade
ID: sgcraft:metal
Fitted with: Stargate Iris Upgrade
Opens or closes in: 70 ticks (3.5 seconds)
Right needed: sgcraft:iris
Recipe: [below](#fitting-an-iris)
Dictionary: [Stargate Iris Upgrade](items/iris-upgrade.md)
```

## Fitting an iris

Use a Stargate Iris Upgrade on any block of a gate, or in the air while you look at its ring from within 6 blocks. You need the right to configure the gate. The upgrade is used up, and the new iris starts open.

| Message | Meaning |
| --- | --- |
| Iris fitted | The iris is on. |
| This gate already has an iris | The gate has one. |
| You can't fit an iris to this gate | You lack the right, or something stopped it. |

```recipe
sgcraft:iris_blade
```

```recipe
sgcraft:iris_upgrade
```

Breaking the gate's base drops the iris upgrade it was fitted with. Operators and players in creative mode can also choose **None**, **Metal Iris** or **Shield** with **Iris** on the **Gate** page of the [configurator](tools.md#gate-page). An iris fitted that way gives no upgrade back. Taking off an iris that was fitted with an Iris Upgrade gives the upgrade back, unless you are in creative mode.

An Iris Upgrade also replaces a shield. See [Swapping the iris and the shield](#swapping-the-iris-and-the-shield).

### Server admins

- `/sgcraft iris fit` (or `/sgcraft iris fit metal`) and `/sgcraft iris remove` fit or remove an iris (operators).
- The `gates.defaults` setting `sgcraft:iris` gives every new gate players build an iris: `/sgcraft gate_defaults sgcraft:iris "sgcraft:metal"`. An iris that comes with a gate this way, or that a command fits, gives no upgrade back when taken off or broken. See [Settings](settings.md).

## Opening and closing

The iris takes 70 ticks to open or close. Told to turn back partway, it reverses from where it is, without a jump. Its state is saved, so it carries on after a restart.

You can work the iris with:

- The [GDO](#the-gdo), at the gate near you or at the other end of an open wormhole.
- The [gate control tablet](tools.md), from anywhere.
- `/sgcraft iris open` and `/sgcraft iris close`. `/sgcraft iris` on its own shows the iris's state.
- Redstone at the gate's base (see [Redstone](#redstone)).

A player needs the `sgcraft:iris` right on the gate. By default everyone has it, along with the right to use the gate. See [Access](access.md).

## Redstone

While the gate's `redstone_input` property is on (the default, **Redstone works iris** in the configurator):

- A redstone signal arriving at the base shuts the iris.
- The signal going opens it.

Redstone has every right, so it works whatever the gate's access settings say.

## What a shut iris does

The iris stops travelers once it is half shut.

- The iris is solid. Unseen plates fill the opening where the blades are, so you can't walk into the gate from either side.
- Anything that comes through the wormhole into a gate whose iris is shut hits it. Living things die with the message "got splattered against a stargate iris", and other things, such as dropped items, are destroyed. The gate plays the iris hit sound.
- A creative player doesn't travel, and sees "Destination blocked by iris".
- A player killed by the iris loses what they carry, rather than dropping it. The far gate's `preserve_inventory` property (**Iris deaths keep items**, `false` by default) or the `keepInventory` game rule keeps it.
- A gate whose iris is shut keeps its kawoosh in, so it harms nothing and clears no blocks.
- A shut iris at either end stops fluid going through. See [Dialing](dialing.md#fluid-through-the-wormhole).
- The event horizon behind a shut iris gives no light in front of the gate. Its back is lit instead, as in SGCraft 2.2.0, by unseen light blocks in the air just behind the opening, at the server's `gates.event_horizon_light`. They go when the iris opens or the wormhole closes, and any left behind take themselves away within a few seconds. Anything can be placed into them.

A MALP's radio signal still passes through a shut iris. See [The MALP](malp.md).

## The shield

```infobox
Shield
Icon: sgcraft:shield_upgrade
ID: sgcraft:shield
Fitted with: Stargate Shield Upgrade
Raises or lowers in: 10 ticks (half a second)
Energy: 1 a tick while rising or raised
Right needed: sgcraft:iris
Dictionary: [Stargate Shield Upgrade](items/shield-upgrade.md)
```

The shield is a second kind of iris: a disc of energy across the gate's opening. Raising it is closing the iris, and lowering it is opening the iris, so everything that works an iris works the shield.

```gallery
scenes/shield-day.jpg | Shield raised, by day
scenes/shield-night.jpg | Shield raised, by night
scenes/shield-open-day.jpg | Shield raised, gate open, by day
scenes/shield-open-night.jpg | Shield raised, gate open, by night
```

### Fitting a shield

Use a Stargate Shield Upgrade on any block of a gate, or in the air while you look at its ring from within 6 blocks. You need the right to configure the gate. The upgrade is used up.

```recipe
sgcraft:shield_upgrade
```

| Message | Meaning |
| --- | --- |
| Shield fitted | The shield is on. |
| This gate already has a shield | The gate has one. |
| This type of gate takes no iris or shield | The gate is an Ori supergate. |
| You can't fit a shield to this gate | You lack the right, or something stopped it. |

Breaking the gate's base drops the shield upgrade it was fitted with, but not a shield the gate came with. Operators and players in creative mode can choose **Shield** with **Iris** on the configurator's **Gate** page. Taking a shield off with the configurator, or swapping it for the metal iris, gives the upgrade back, unless you are in creative mode.

### Swapping the iris and the shield

A gate has one or the other, never both.

- A Shield Upgrade used on a gate with the metal iris replaces the iris, but only while the iris is open. You get the Iris Upgrade back, unless the gate came with its iris.
- An Iris Upgrade used on a gate with a shield replaces the shield the same way, while the shield is lowered, and gives the Shield Upgrade back.
- If the iris is shut or the shield is up, you see "Open the iris or lower the shield before replacing it".

### Raising and lowering

The shield rises or falls in 10 ticks (half a second). The metal iris takes 70. Work it with:

- The [GDO](#the-gdo) and the [gate control tablet](tools.md#gate-control-tablet). Their buttons read **Activate Shield** and **Deactivate Shield**, and the GDO shows **Shield Active**, **Raising Shield...**, **Lowering Shield...** or **Shield Inactive**. The tablet's gate list shows "Shield up" for a gate whose shield is raised.
- The [Shield Console](blocks/shield-console.md), a block that works the nearest shielded gate within 40 blocks.
- `/sgcraft iris close` raises it and `/sgcraft iris open` lowers it.
- Redstone at the base: a signal raises it, and the signal going lowers it.
- The [API](api.md), as for any iris.

Each needs the `sgcraft:iris` right on the gate, as for the metal iris.

### Energy

The shield uses 1 gate energy a tick while it rises or stays raised (`shields.gate_energy_per_tick`, which may be a fraction). It takes this from the gate's own store and from the gate's energy sources: naquadah in its DHD, then power units, then ZPMs near it, the nearest of each first.

- You can't raise the shield without enough energy for one tick: "The gate doesn't have the energy to raise its shield".
- The shield falls when the energy runs out.
- A gate with `requires_power` off (**Needs energy** in the configurator) runs its shield for free.

See [Power](power.md#the-gate-shield).

### What a raised shield does

A raised shield stops travelers exactly as a shut iris does. They die against it, with the same rules for what they carry and for the Tollan Phase Shift Device.

Unlike a shut metal iris, the shield doesn't darken the gate. The event horizon ripples on behind it and its light shines through. The shield is drawn as a shimmering disc of blue light, brighter toward its rim, that fades in and out as it rises and falls, with its own raising and lowering sounds.

## The Tollan phase shift device

A living traveler holding a Tollan Phase Shift Device in either hand passes through a shut iris at the far gate, unharmed. The far gate's `phase_shift_passes_iris` property (**Phase shift passes iris**, `true` by default) allows it.

The device has no recipe and isn't found as loot. It is in the creative tab.

## The iris after a wormhole

When the gate's `iris_restores_state` property is on (**Iris shuts again after**, `false` by default), an iris that was shut when the dial started shuts again when the wormhole closes. It only ever closes the iris again: an iris that was open stays as you left it.

## The GDO

The Garage Door Opener (GDO) works irises from a distance. Use it to open its screen on the gate near you, within 6 blocks (the server's `tools.range`).

| Control | Does |
| --- | --- |
| This gate | Opens or closes the near gate's iris |
| The other end | Opens or closes the far gate's iris, once the wormhole is open |
| Disconnect | Shuts the wormhole down |

The screen shows "No iris" for a gate with none, and "No wormhole open" until the wormhole has opened. For a gate with a shield, the buttons read **Activate Shield** and **Deactivate Shield**.

```recipe
sgcraft:gdo
```

See [Tools](tools.md) for the PDD, the gate control tablet and the configurator.
