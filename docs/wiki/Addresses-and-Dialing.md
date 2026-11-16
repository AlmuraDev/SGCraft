# Addresses and Dialing

## Addresses

An address is written `XXXX-XXX-XX` using the symbols `A-Z` and `0-9`.

- The first **7 symbols** give the gate's position (its chunk). Dialing a gate in the same world needs only these 7.
- The last **2 symbols** give the world. Dialing another world needs all 9, and a gate with the Chevron Upgrade.
- There can be one gate per chunk. A gate's address never changes while it stays in that chunk.
- Dashes and letter case don't matter when entering an address.
- A gate set not to display its address shows "Gate Address Hidden" instead.

## The DHD (Stargate Controller)

- **Linking.** Place the DHD near the gate: within 5 blocks either side, 1 up or down and 6 forwards or back. It links
  to the nearest merged gate that has no DHD. To link by hand, sneak-right-click the gate's base with a Configurator,
  then sneak-right-click the DHD (you must be the gate's admin).
- **Dialing.** Click the top of the DHD. Click the symbols (letters on the outer ring, digits on the inner) or type
  them; Backspace removes one. Click the centre dome or press Enter to dial. Clicking the dome while the gate is open
  closes it.
- **Fuel.** Click the lower half of the DHD (admins only) to open its 4 fuel slots, which take Naquadah.
- You must be at the gate or within 10 blocks of its DHD.

## The PDD (Personal Dialer Device)

A handheld dialer with your own address list. Needs MalisisCore.

- **Right-click** near a gate (within 6 blocks) to open the list and dial. Away from a gate it opens for editing only.
- **Sneak-right-click** at a gate to add its address to the list (or edit its entry).
- Buttons: **+** add, **-** remove, **?** edit, **Dial**, **Reset**, **Disconnect**, **Close**. Double-click an entry
  to dial it. Entries can be moved up and down.
- The search box matches names, world names and addresses; the red **x** clears it.
- **Auto Close** on an entry closes the screen once the gate starts opening.
- Entries must be full 9-symbol addresses. A list holds up to 200 entries. A new PDD starts with the server's default
  list (`config/SGCraft/pdd.yml`); "locked" entries from it can't be removed.

## The EDD (Explorer Dialer Device)

Like the PDD, but its list is every **world-generated** gate on the server, sorted by world. You can't add or remove
entries, but you can name them. It remembers which gates you have visited (by standing at them with the EDD, or by
travelling through them); this stays with you through death and onto a new EDD. Needs MalisisCore.

## The GDO (Handheld GDO)

Controls the iris from a distance. Within 6 blocks of a gate: open or close its iris, disconnect it, and, while a
wormhole is open, open or close the **other** gate's iris (you need iris access at that gate). Needs MalisisCore.

## What happens in a dial

1. **Dialing.** Each symbol is turned to and locked. A Milky Way gate spins its ring and dips the top chevron for
   each symbol; a Pegasus gate runs a light round the ring instead.
2. **Opening.** Both gates open together, with the "kawoosh". If **Transient Damage** is on, the kawoosh hurts
   anything standing in front of the gate and removes blocks in its path (never gate or DHD blocks).
3. **Connected.** Walk in at the dialing end to arrive at the other. By default travel is **one way**: stepping into
   the receiving end does nothing (or kills, if **Reverse Wormhole Kills** is on).
4. **Closing.** After **Seconds to Stay Open** (300 by default, 0 = until closed), when the dialing gate can't pay to
   keep it open, or when someone closes it. The receiving end can close it only if **Close from Either End** is on.

### Chevrons lock as dialed

With **Chevrons lock when dialed** set on a gate, each symbol locks a chevron as you enter it and the ring doesn't
spin. The PDD and EDD close as soon as you dial; the gate then enters a symbol every 0.75 seconds. The receiving gate
locks its own chevrons as the dial arrives.

### The iris

- Anyone arriving at a gate whose iris (or shield) is closed is killed. Their inventory is lost unless the gate has
  **Preserve Inventory on Iris Death** on or the world has `keepInventory`. Mobs and items are destroyed.
- A **Tollan Phase Shift Device** held in the off-hand lets a player pass through a closed iris.
- **Return to Previous Iris State** closes the iris again after a connection if it was closed before.
- The iris can be worked from the GDO, a computer, or a redstone signal to the base (powered closes it), if
  **Allow Redstone Input** is on.

### Redstone

With **Allow Redstone Output** on, the gate's blocks give a full redstone signal whenever the gate isn't idle
(dialing, open or closing).

### Access

A gate can restrict who may use it and which addresses may dial in or out; see [Configurator](Configurator).
Travelling through needs gate access at both ends.

### Weather

When a wormhole forms at a gate in the rain, there's a small chance of lightning sending it to a random generated gate
instead.
