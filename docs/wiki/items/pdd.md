# Personal Dialing Device

The Personal Dialing Device (PDD) is a handheld dialer. It dials the gate near you from a list of addresses kept on the PDD itself.

```infobox
Personal Dialing Device
ID: sgcraft:pdd
Category: Tools
Type: Item
Stackable: No
Range: The gate near you, 6 blocks (`tools.range`)
Right needed: `use` on the gate, to dial or disconnect
Holds: Its own address book, up to 200 addresses
```

## Obtaining

### Crafting

```recipe
sgcraft:pdd
```

## Usage

1. Stand near a gate and use the PDD, in the air or on a block. Its screen opens.
2. Choose an address from the list, or type one in the **Address** box.
3. Press **Dial**. The gate near you dials the address.

The PDD works from either hand. It looks for a gate the same way every tool does:

1. The gate you look at, through its ring or its opening, up to 6 blocks away (or `tools.range` if that is more).
2. The gate whose base or {{item:sgcraft:dhd}} is nearest your feet, within `tools.range` blocks (6) along each axis. A DHD stands for the gate it is linked to.
3. The gate whose ring is nearest your eyes, within `tools.range` blocks.

The PDD finds the gate again each time you press a button, so you can walk from gate to gate with the screen open. You can change the PDD's addresses with no gate near you.

## Screen

The screen shows, from the top:

- The gate near you: "This gate: <address>, <state>", with the address in letters and in the gate's glyphs. With no gate near you it says "No gate near you. You can still change its addresses". A gate with **Address shown** off shows "address hidden".
- The PDD's addresses, one to a row: the name, the address in letters and glyphs, and what the gate at that address is doing. The glyphs are those of the near gate's type. Under the list, "Addresses: <count>".
- A **Name** box (up to 48 characters) and an **Address** box (up to 11 characters). Choosing an entry in the list fills both.
- Two rows of buttons.

The state of each gate shows as a colored word: **Idle** (green), **Dialing** (yellow), **Incoming** (gold), **Opening** (yellow), **Connected** (aqua), **Closing** (gray), or **No gate** (red) when no gate is at an address. The screen updates once a second.

### Buttons

| Button | What it does |
| --- | --- |
| **Add** | Adds the address in the box under the name in the box. An entry without a name is named by its address |
| **Rename** | Renames the entry whose address is in the box to the name in the box. Choosing an entry fills both boxes |
| **Remove** | Takes the entry whose address is in the box off the list |
| **Up**, **Down** | Move the chosen entry one place in the list |
| **Dial** | Dials the address in the box from the gate near you. It works while that gate is idle and the box holds a whole address |
| **Disconnect** | Closes the near gate's wormhole or stops its dial. It works while the gate isn't idle |
| **Add this gate** | Puts the near gate's address in the box, and in the name box if that is empty, then adds it. It works only when the gate's address is shown |
| **Close** | Closes the screen |

Errors show in red on the screen, such as "This PDD already has that address", "That isn't an address", "There is no gate near you" or the reason a dial failed.

## Address book

The PDD keeps its addresses on the item. They go wherever the PDD goes, so you can hand a PDD to another player with its addresses.

- Each address is kept in its lasting form: the shortest form that names its world, so it dials from any world. An address you add is turned into that form from the world you stand in.
- When you dial an entry, the PDD sends the shortest address that reaches the same gate from the dialing gate's world. A gate with 7 chevrons can dial an entry for a gate in its own world this way.
- An address you type that isn't on the list is dialed as you typed it.
- Each entry's state is read from the near gate's world, or from yours when no gate is near.

See [Address books](../addresses.md#address-books).

### Default addresses

The first time you use a new PDD, it takes the server's default addresses. After that its addresses are yours, and emptying the list leaves it empty. A default address the server locked can be moved but not renamed or removed: "That address came with the server's defaults and is locked".

## Rights

Dialing and **Disconnect** need the `use` right on the near gate. Without it the dial fails with "You aren't allowed to dial that". Changing the PDD's own list needs no rights. See [Access](../access.md#rights).

The ring turns for each symbol, unless the gate has **Chevrons lock at once** on. Then every chevron locks at once.

## Data components

| Component | What it holds |
| --- | --- |
| `sgcraft:addresses` | The addresses, each with its name, in list order. Kept even when empty, so an emptied PDD doesn't take the defaults again |
| `sgcraft:locked_addresses` | The default addresses that came locked |

## Server admins

- `tools.range` (default 6) sets how far from a player the tools look for a gate.
- The default addresses are in `config/sgcraft-default-addresses.json`. Operators change them with `/sgcraft default_addresses add|remove|lock|unlock`. Changes reach PDDs used for the first time after the change.

## See also

- [Tools](../tools.md#personal-dialing-device)
- [How a tool finds its gate](../tools.md#how-a-tool-finds-its-gate)
- [Addresses](../addresses.md#address-books)
- [Default addresses](../addresses.md#default-addresses)
- [Dialing](../dialing.md)
- {{item:sgcraft:gate_control_tablet}}, a PDD for many gates
