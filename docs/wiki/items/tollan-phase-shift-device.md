# Tollan Phase Shift Device

The Tollan Phase Shift Device takes you through a shut iris. Hold it as you step into a wormhole, and you pass through the far gate's iris instead of dying against it.

```infobox
Tollan Phase Shift Device
ID: sgcraft:tollan_phase_shift_device
Category: Creative only
Type: Item
Stackable: No
Held in: Either hand
Works at: Gates with Phase shift passes iris on (the default)
```

## Obtaining

The device has no recipe, isn't found as loot and isn't traded. It is in the SGCraft creative tab. An operator can give it with `/give <player> sgcraft:tollan_phase_shift_device`.

## Usage

1. Hold the device in your main hand or your off hand.
2. Step into an open wormhole.
3. If the far gate's iris is shut, you pass through it unharmed and arrive as if it were open.

The device works only in a hand. In the rest of your inventory it does nothing.

## Behavior

- It works for any living traveler holding it, so a mob holding it passes through as well. Dropped items and other things that aren't alive still hit the iris.
- The far gate decides. Its **Phase shift passes iris** setting (the `sgcraft:phase_shift_passes_iris` property, on by default) must be on. With it off, a traveler holding the device dies against the shut iris like anyone else.
- Without the device, a traveler who arrives at a shut iris dies with the message "got splattered against a stargate iris". A creative player doesn't travel and sees "Destination blocked by iris".
- The device keeps no data on the item.

## Server admins

Turn off **Phase shift passes iris** on a gate's [configurator](configurator.md) Travel page, or run `/sgcraft property sgcraft:phase_shift_passes_iris set false` on the gate, to stop the device at that gate. `/sgcraft gate_defaults` sets it for every gate without a value of its own.

## See also

- [The Tollan phase shift device](../iris.md#the-tollan-phase-shift-device)
- [What a shut iris does](../iris.md#what-a-shut-iris-does)
- [Tools](../tools.md#tollan-phase-shift-device)
- {{item:sgcraft:gdo}}, to open an iris from a distance
