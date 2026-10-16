# Garage Door Opener

The Garage Door Opener (GDO) opens and closes the iris of the gate near you and, once its wormhole is open, the iris of the gate at the other end. It can also close the wormhole.

```infobox
Garage Door Opener
ID: sgcraft:gdo
Category: Tools
Type: Item
Stackable: No
Range: The gate near you, 6 blocks (`tools.range`), and the far end of its open wormhole
Right needed: `iris` on each gate for its iris, `use` to disconnect
```

## Obtaining

### Crafting

```recipe
sgcraft:gdo
```

## Usage

1. Stand near a gate and use the GDO, in the air or on a block. Its screen opens on that gate.
2. Press **Open Iris** or **Close Iris** under **This gate**.
3. Once the wormhole is open, the **The other end** side shows the far gate. Its buttons work that gate's iris.

The GDO finds the gate near you the same way every tool does:

1. The gate you look at, through its ring or its opening, up to 6 blocks away (or `tools.range` if that is more).
2. The gate whose base or {{item:sgcraft:dhd}} is nearest your feet, within `tools.range` blocks (6) along each axis. A DHD stands for the gate it is linked to.
3. The gate whose ring is nearest your eyes, within `tools.range` blocks.

With no gate near you, the GDO says "Could not detect a gate near you" and opens nothing.

The screen stays on the gate it opened on. Its buttons stop working once you are more than 16 blocks from that gate's base (`tools.range` plus 10), and the GDO says "Could not detect a gate near you".

## Screen

The screen has two sides:

- **This gate**: the near gate's address in letters and glyphs ("????-???-??" when hidden), its iris state ("Iris: open", "Iris: closing", "Iris: closed" or "Iris: opening"), and what the gate is doing. A gate without an iris shows "No iris".
- **The other end**: the same for the far gate, shown once the wormhole is connected. Until then the side says "No wormhole open".

For a gate with a [shield](../iris.md#the-shield), the side shows **Shield Active**, **Raising Shield...**, **Lowering Shield...** or **Shield Inactive**, and the buttons read **Activate Shield** and **Deactivate Shield**.

The screen updates twice a second.

| Button | What it does |
| --- | --- |
| **Open Iris** | Opens that side's iris. It works while the iris is fully shut |
| **Close Iris** | Shuts that side's iris. It works while the iris is fully open |
| **Disconnect** | Closes the near gate's wormhole or stops its dial. It works while the gate is dialing, opening, connected or has an incoming wormhole |
| **Close** | Closes the screen |

An iris that is moving takes no orders until it stops. Errors show in red, such as "That gate has no iris" or "The wormhole isn't open".

## Rights

Each iris needs the `iris` right on its own gate, so you can only work the far iris if that gate lets you. **Disconnect** needs the `use` right on the near gate. See [Access](../access.md#rights).

The GDO keeps no data on the item.

## See also

- [Tools](../tools.md#garage-door-opener)
- [The iris](../iris.md#the-gdo)
- [Opening and closing the iris](../iris.md#opening-and-closing)
- [How a tool finds its gate](../tools.md#how-a-tool-finds-its-gate)
- {{item:sgcraft:iris_upgrade}}, which fits an iris
