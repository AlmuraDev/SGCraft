# Gate Control Tablet

The Gate Control Tablet is a PDD for many gates. It keeps a list of gates and an address book, and it dials, closes, works the iris of and configures any gate on its list, in any world.

```infobox
Gate Control Tablet
ID: sgcraft:gate_control_tablet
Category: Tools
Type: Item
Stackable: No
Range: Any gate on its list, in any world. Adding a gate: 6 blocks (`tool_range`)
Right needed: `use` to add, dial or disconnect, `iris` for the iris, `configure` to configure
Holds: A list of gates and its own address book
```

## Obtaining

### Crafting

The recipe uses a {{item:sgcraft:pdd}}. The PDD's addresses don't carry over to the tablet.

```recipe
sgcraft:gate_control_tablet
```

## Usage

1. Stand near a gate and use the tablet, in the air or on a block. Its screen opens.
2. Press **Add this gate** to put the gate near you on the list. Do this at each gate you want to work.
3. Later, from anywhere, choose a gate on the left and an address on the right, then press **Dial**.

The tablet works from either hand. **Add this gate** finds the gate near you the same way every tool does: the gate you look at within 6 blocks (or `tool_range` if that is more), else the gate whose base or {{item:sgcraft:dhd}} is nearest your feet, else the gate whose ring is nearest your eyes, each within `tool_range` blocks (6). Every other button works on the gate chosen in the list, however far away it is.

## Screen

The screen has two lists side by side, with buttons beside each and a row along the bottom.

- At the top: the gate near you ("Near you: <address>, <state>", with "?" for a hidden address), or "No gate near you to add".
- On the left, the tablet's gates. Each row shows the gate's name, what it is doing and its address. "Iris shut" shows in red when its iris is shut or moving. An empty list says "Stand by a gate and add it". Under it, "Gates: <count>".
- On the right, the tablet's addresses. Each row shows the name, the address in letters, what the gate at that address is doing, and the address in the chosen gate's glyphs. The chosen gate's own address is left out, as a gate can't dial itself. Under it, "Addresses: <count>".
- A **Gate name** box for the gate list, and **Name** and **Address** boxes for the address book. Choosing a gate puts its name in the gate name box. Choosing an address fills the name and address boxes.

The top gate is chosen when the screen opens. The state of each gate shows as a colored word: **Idle** (green), **Dialing** (yellow), **Incoming** (gold), **Opening** (yellow), **Connected** (aqua), **Closing** (gray), or **No gate** (red). The screen updates once a second.

### Gate buttons

| Button | What it does |
| --- | --- |
| **Add this gate** | Adds the gate near you to the end of the list, named as the gate name box says. With the box empty, it is named by its address, or by its position when it has no address. It needs the `use` right on that gate |
| **Rename** | Names the chosen gate as the gate name box says |
| **Remove** | Takes the chosen gate off the list |
| **Up**, **Down** | Move the chosen gate one place in the list |
| **Save address** | Adds the chosen gate's address to the address book, under the gate's name. It works only when the gate's address is shown |
| **Configure** | Opens the chosen gate's {{item:sgcraft:configurator}} screen, wherever the gate is. It needs the `configure` right |
| **Open iris**, **Close iris** | Open or shut the chosen gate's iris. They work only on a gate with an iris and need the `iris` right |

Gate names are cut to 48 characters. A gate can be on the list once: a second add says "This tablet already has that gate".

### Address buttons

**Add**, **Rename**, **Remove**, **Up** and **Down** work on the address book as on a PDD. An entry without a name is named by its address. Names are cut to 48 characters.

### Bottom row

| Button | What it does |
| --- | --- |
| **Dial** | Dials the address in the box from the chosen gate. It works while that gate is idle and the box holds a whole address |
| **Disconnect** | Closes the chosen gate's wormhole or stops its dial. It works while the gate isn't idle |
| **Close** | Closes the screen |

If a listed gate is broken, its row shows **No gate**. Only **Rename**, **Remove**, **Up** and **Down** still work on it.

## Address book

The tablet keeps its addresses on the item, the same way a PDD does.

- Each address is kept in its lasting form: the shortest form that names its world, so it dials from any world. An address you add is turned into that form from the world you stand in.
- When you dial an entry, the tablet sends the shortest address that reaches the same gate from the chosen gate's world. A gate with 7 chevrons can dial an entry for a gate in its own world this way.
- An address you type that isn't on the list is dialed as you typed it.
- Each entry's state is read from the chosen gate's world, or from yours when the list is empty.

The first time you use a new tablet, it takes the server's default addresses. A default address the server locked can be moved but not renamed or removed. See [Default addresses](../addresses.md#default-addresses).

## Configuring from afar

**Configure** opens the chosen gate's configurator in place of the tablet's screen. Its **Save** works while you hold the tablet and the gate is still on its list, in any world. See {{item:sgcraft:configurator}}.

## Rights

| Action | Right on the gate |
| --- | --- |
| Add this gate | `use` |
| Dial, Disconnect | `use` |
| Open iris, Close iris | `iris` |
| Configure | `configure` |

Renaming, removing and moving gates and addresses need no rights, as they change only the tablet. See [Access](../access.md#rights).

## Data components

| Component | What it holds |
| --- | --- |
| `sgcraft:tablet_gates` | The listed gates, in list order: each gate base's world and position, and its name |
| `sgcraft:addresses` | The addresses, each with its name, in list order |
| `sgcraft:locked_addresses` | The default addresses that came locked |

## See also

- [Tools](../tools.md#gate-control-tablet)
- [How a tool finds its gate](../tools.md#how-a-tool-finds-its-gate)
- [Addresses](../addresses.md#address-books)
- [Access](../access.md)
- {{item:sgcraft:pdd}}
