# Supergate Chevron Block

The Supergate Chevron Block is the segment of a supergate's ring that carries a chevron. Every supergate takes 9 of them, whatever its size, at the 9 chevron angles around its ring.

```infobox
Supergate Chevron Block
ID: sgcraft:supergate_chevron
Category: Gate blocks
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Light: 12 while a lit chevron stands on it, in a formed gate
Waterloggable: Yes
Drops: Itself
Block entity: No
```

## The finished gate

```gallery
scenes/supergate-day.jpg | Idle, by day
scenes/supergate-night.jpg | Idle, by night
scenes/supergate-open-day.jpg | Open, by day
scenes/supergate-open-night.jpg | Open, by night
```

## Obtaining

### Crafting

```recipe
sgcraft:supergate_chevron
```

## Usage

A supergate's ring is every block a circle passes through, with a radius of 4 to 30 blocks from its center to the base. The 9 blocks of the ring nearest the 9 chevron angles must be Supergate Chevron Blocks, and the rest {{item:sgcraft:supergate_ring}}. The {{item:sgcraft:supergate_base}} sits at the bottom. The chevron angles are 40 degrees apart, with one at the top of the ring.

- The space inside the ring doesn't need to be clear. Only the ring blocks are checked.
- The gate forms when you place the last block of the ring, or the base next to a complete ring.
- Supergate Chevron Blocks only make supergates. A standard gate needs {{item:sgcraft:stargate_chevron}}.

The ring is hard to lay out by hand. To see where the chevron blocks go, an operator can run `/sgcraft place sgcraft:milky_way 9 10`, which builds a supergate of radius 10, or place a supergate spawner. See [Building gates](../building-gates.md#supergates) for the full build.

## Chevrons and chevron blocks

A supergate always has 9 chevron blocks, but the number of chevrons it has is a setting of the gate, kept in its base. It forms with 7 chevrons, and each {{item:sgcraft:chevron_upgrade}} adds one, up to 9. The chevrons limit the addresses the gate can dial:

| Chevrons | Can dial |
| --- | --- |
| 7 | Gates in the same world |
| 8 | Also gates in other worlds of the same group |
| 9 | Also gates in other groups |

See [Building gates](../building-gates.md#chevrons-and-upgrades).

## Behavior

- Once the gate forms, its chevron blocks merge into it and the gate draws as one model. A merged block lets light through and doesn't hide the faces of the blocks beside it.
- Right-click a chevron block of a formed supergate to open the gate's **Stargate Address** screen. See [Supergate Base](supergate-base.md#the-address-screen).
- Gate tools and upgrades work on the gate when you use them on any of its ring blocks.
- Pick block (middle click) on a block of a formed supergate gives the supergate spawner of its type.
- A chevron block holds water, so a supergate built under water stands in water.

## Light

Each lit chevron lights the ring block nearest it, at level 12, which is most often its chevron block. Chevrons light as they lock while the gate dials and go dark when the gate shuts down. On a Universe gate, every chevron stays lit while the gate is active. An unformed chevron block gives no light.

## Breaking

Breaking any block of a formed supergate breaks the gate apart, and the gate forms again when you put the ring back. A player may break a block of a formed gate if they are an operator, if they own the gate and `gates.owners_break_gates` is on (the default), or if the gate's **Breakable** setting is on. Anyone else sees "This gate can't be broken". Explosions and commands still break gates. See [Building gates](../building-gates.md#breaking-a-gate).

## See also

- [Building gates](../building-gates.md#supergates)
- [Supergate Ring Block](supergate-ring.md)
- [Supergate Base](supergate-base.md)
- [Lock order](../building-gates.md#lock-order)
