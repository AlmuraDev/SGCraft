# Addresses

Every gate has an address: the symbols you dial to reach it. An address has 7, 8 or 9 symbols, and the longer forms reach gates in other worlds.

## Symbols

There are 37 symbols: the letters `A` to `Z`, the digits `0` to `9`, and `@`, the point of origin (Giza). Gates and DHDs draw them as glyphs. Each gate type has its own:

| Type | Glyphs |
| --- | --- |
| Milky Way | {{glyphs:ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@}} |
| Pegasus | {{glyphs-pegasus:ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789}} |
| Universe | {{glyphs-universe:ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789}} |

The Pegasus gate has no `@` glyph of its own, so it borrows the Milky Way one.

An address is written in groups: 4 symbols, a dash, 3 symbols, then a dash before any symbols after the seventh. For example `XOBF-R3@` or `XOBF-R3A-@@`. When you type an address, case, dashes and spaces don't matter: `xobf r3a@` is the same as `XOBF-R3A-@`.

## Address lengths

| Symbols | Made of | Reaches |
| --- | --- | --- |
| 7 | 6 position symbols, `@` | A gate in the dialing gate's own world |
| 8 | 6 position symbols, the world's symbol, `@` | A gate in any world of the dialing gate's group, its own world included |
| 9 | 6 position symbols, the world's symbol, the group's symbol, `@` | A gate in any world of any group |

For a gate whose position symbols are `XOBFR3`, in the world with symbol `A`:

| Form | Address | Dials from |
| --- | --- | --- |
| 7 symbols | {{glyphs:XOBF-R3@}} `XOBF-R3@` | World `A` only |
| 8 symbols | {{glyphs:XOBF-R3A-@}} `XOBF-R3A-@` | Any world in the default group |
| 9 symbols | {{glyphs:XOBF-R3A-@@}} `XOBF-R3A-@@` | Any world |

The same 7 symbols reach a different gate from another world. The 8 and 9 symbol forms name their world, so they reach the same gate from anywhere they work, including the gate's own world.

To see a gate's address, right-click its base or any of its ring blocks, or look at it and run `/sgcraft address`. The command shows the address from this world and from other worlds.

## Why a gate can't dial a longer address

A gate locks one chevron for each symbol, so it can't dial an address with more symbols than it has chevrons. A gate forms with 7 chevrons, so it reaches only its own world. It fails a longer address with "This gate doesn't have enough chevrons for that address".

Add a [chevron upgrade](building-gates.md#chevrons-and-upgrades) for each extra symbol: 8 chevrons to reach other worlds of the group, 9 to reach other groups. A dial with an eighth chevron costs 10 times as much energy, and one with a ninth 100 times. See [Power](power.md#other-worlds-and-supergates).

## Point of origin

Every address ends with the point of origin, `@`, which the top chevron locks last. The server setting `addresses.point_of_origin` sets it, for every world or for one. An address dialed from a world must end with that world's point of origin, or it reaches no gate.

### Server admins

- `addresses.point_of_origin` is `"@"` by default. Set it to `""` for addresses without one: the seventh symbol then becomes a position symbol, and the grid is 6 times as wide each way.
- Changing the point of origin changes every address. See [Changing the layout](#changing-the-layout).

## Cells and the grid

Each world is divided into cells, 8 chunks (128 blocks) across by default. A gate's address is the address of the cell holding its base.

- The 6 position symbols give 36 to the power 6 codes, a grid 46,656 cells square, centered on x 0, z 0. With the default 8 chunk cells that covers about 2,985,984 blocks out from the center in each direction.
- A gate beyond the grid has no address.
- The codes are scrambled, so gates near each other don't get addresses that look alike.
- `@` is never a position symbol.
- Two gates in one cell share an address, and a dial reaches only one of them. Build one gate per cell.

### Cell size

The server setting `addresses.cell_chunks` sets how many chunks wide and long each cell is, from 1 to 256, for every world or, under `addresses.worlds`, for one. Bigger cells make the grid reach further with the same symbols, at the cost of more space between gates:

| `cell_chunks` | Cell | Grid reaches, each way from x 0, z 0 |
| --- | --- | --- |
| `1` | 16 blocks | 373,248 blocks |
| `2` | 32 blocks | 746,496 blocks |
| `4` | 64 blocks | 1,492,992 blocks |
| `8` (default) | 128 blocks | 2,985,984 blocks |

### Changing the layout

Changing `cell_chunks` or `point_of_origin` changes every gate address in the worlds it reaches. Old addresses in address books, default addresses, PDDs and tablets then reach another gate or none, and a bigger cell can put two gates in one cell. Set the layout before players build gates.

The server records each world's layout. When a world that has gates starts with a different one, the server log shows a large `STARGATE ADDRESSES HAVE CHANGED` warning naming each such world, how many gates it has, and its old and new layout. The warning shows once, as the new layout is then recorded. To get the old addresses back, set the old values again and restart. A world without gates changes quietly.

Operators can change the layout in game. The change is written to the settings file and takes effect when the server next starts. The command warns, in chat and in the server log, about each world whose gates' addresses it would change:

- `/sgcraft addresses` shows each world's layout, how many gates it has, and its layout after a restart if that differs.
- `/sgcraft addresses cell_chunks <chunks> [<world>]`
- `/sgcraft addresses point_of_origin <symbol> [<world>]`, where the symbol is a letter, a digit, `giza` (or `"@"`) for `@`, or `none`.
- `/sgcraft addresses clear <world>` gives a world the server's layout again.

## World symbols and groups

Worlds are sorted into groups. SGCraft has one group, `default`, whose symbol is `@`. That is why a 9 symbol address in the default group ends in `@@`.

Each world gets a symbol within its group, `A`, `B`, `C` and so on, in the order the server first sees the worlds. A world's symbol never changes once given. The server log records each one, as "World ... has the symbol ...".

## Address books

You keep addresses in address books.

| Book | Where it lives | How to use it |
| --- | --- | --- |
| Your own book | The server's data, so it lasts whether you are online or not | `/sgcraft book`, `/sgcraft book add <name> <address>`, `/sgcraft book remove <address>` |
| A PDD's book | On the PDD item, so it travels with it | The PDD screen |
| A gate control tablet's book | On the tablet item | The tablet screen |

Names are up to 48 characters. See [Tools](tools.md) for the PDD and tablet screens.

A book keeps each address in its lasting form: the shortest form that names its world, so it dials from any world. When you dial an entry with a tool, the tool sends the shortest address that reaches the same gate from the dialing gate's world. A 7 chevron gate can dial an entry for a gate in its own world this way. An address you type is dialed as you typed it.

### Hidden addresses

When a gate's `address_visible` property is off (**Address shown** in the configurator), its base screen shows "... Gate Address Hidden ..." and the PDD shows "address hidden". The gate can still be dialed by anyone who knows the address.

### Default addresses

Every new PDD and gate control tablet starts with the server's default addresses. It takes them the first time it is used. After that its addresses are its holder's, and emptying it leaves it empty.

A locked default address stays on the item: its holder can move it in the list, but can't rename or remove it.

#### Server admins

The list lives in `config/sgcraft-default-addresses.json`, a list of entries:

```json
[
  { "address": "XOBF-R3A-@", "name": "Home base", "locked": true }
]
```

`locked` is optional and `false` by default. Operators can change the list in game:

- `/sgcraft default_addresses` lists it.
- `/sgcraft default_addresses add <name> <address>`
- `/sgcraft default_addresses remove <address>`
- `/sgcraft default_addresses lock <address>` and `unlock <address>`
