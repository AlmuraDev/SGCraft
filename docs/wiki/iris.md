# The iris

An iris is a metal shield across a gate's opening. While it is shut, nothing comes through the gate: travelers arriving through the wormhole die against it.

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

Breaking the gate's base drops the iris upgrade it was fitted with. Operators and players in creative mode can also fit or take off an iris with **Iris fitted** on the **Gate** page of the [configurator](tools.md). An iris fitted that way gives no upgrade back. Taking off an iris that was fitted with an Iris Upgrade gives the upgrade back, unless you are in creative mode.

### Server admins

- `/sgcraft iris fit` and `/sgcraft iris remove` fit or remove an iris (operators).
- The `gate_defaults` setting `sgcraft:iris` gives every new gate players build an iris: `/sgcraft gate_defaults sgcraft:iris "sgcraft:metal"`. An iris that comes with a gate this way, or that a command fits, gives no upgrade back when taken off or broken. See [Settings](settings.md).

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
- The event horizon behind a shut iris gives no light.

A MALP's radio signal still passes through a shut iris. See [The MALP](malp.md).

## The Tollan phase shift device

A living traveler holding a Tollan Phase Shift Device in either hand passes through a shut iris at the far gate, unharmed. The far gate's `phase_shift_passes_iris` property (**Phase shift passes iris**, `true` by default) allows it.

The device has no recipe and isn't found as loot. It is in the creative tab.

## The iris after a wormhole

When the gate's `iris_restores_state` property is on (**Iris shuts again after**, `false` by default), an iris that was shut when the dial started shuts again when the wormhole closes. It only ever closes the iris again: an iris that was open stays as you left it.

## The GDO

The Garage Door Opener (GDO) works irises from a distance. Use it to open its screen on the gate near you, within 6 blocks (the server's `tool_range`).

| Control | Does |
| --- | --- |
| This gate | Opens or closes the near gate's iris |
| The other end | Opens or closes the far gate's iris, once the wormhole is open |
| Disconnect | Shuts the wormhole down |

The screen shows "No iris" for a gate with none, and "No wormhole open" until the wormhole has opened.

```recipe
sgcraft:gdo
```

See [Tools](tools.md) for the PDD, the gate control tablet and the configurator.
