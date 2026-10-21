# Naquadah Generator

The Naquadah Generator burns naquadah to power the gates near it, as in SGCraft 2.2.0. It comes in two marks. The Mark I is steady and makes the most of its fuel. The Mark II makes energy six times as fast but gets only half of each piece's worth.

```infobox
Naquadah Generator
ID: sgcraft:naquadah_generator_mk1, sgcraft:naquadah_generator_mk2
Category: Power
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 4
Blast resistance: 20
Waterloggable: No
Drops: Itself; the fuel in its slots
Block entity: Yes
Light: 7 while it runs
Range: 4 blocks from a gate's base along each axis
```

## Obtaining

### Crafting

The Mark I:

```recipe
sgcraft:naquadah_generator_mk1
```

The Mark II is made from a Mark I:

```recipe
sgcraft:naquadah_generator_mk2
```

## Usage

1. Place the generator within 4 blocks of a gate's base along each axis. It faces you.
2. Use it to open its screen, and put naquadah or naquadah alloy ingots in its four fuel slots. Hoppers can fill the slots too, but can't take anything out.
3. Press **On**. Its arms swing out. While it has energy or fuel, its crystal, the rod across its middle and the lights around its dome glow and pulse, and it hums.

Switched on, it burns its fuel a piece at a time to fill its store, as fast as its mark allows. It burns nothing while its store is full, so fuel isn't wasted while no gate draws on it. Switched off, its arms draw in, and it neither burns fuel nor gives energy.

| | Mark I | Mark II |
| --- | --- | --- |
| Makes | 4 GE a tick | 24 GE a tick |
| Holds | 20,000 GE | 60,000 GE |
| Gets of each piece's worth | All of it | Half |
| One naquadah lasts | 24,000 ticks (20 minutes) | 2,000 ticks (100 seconds) |
| Crystal | Red | Blue |

A piece of naquadah is worth 96,000 GE and a naquadah alloy ingot 384,000 GE, before the mark's share.

## Powering gates

A gate draws on the generators within 4 blocks of its base, the nearest first, after its DHD fuel and power units and before any ZPMs. It takes their GE as it takes any other, so a generator also keeps a gate's shield up. See [Power](../power.md#where-a-gate-draws-energy).

## FE

The generator gives no FE of its own. An [Energy Converter](energy-converter.md) placed against it turns its GE into FE for other mods' machines.

## Screen

The screen shows, beside the fuel slots:

- Whether it is **Running**, has **No fuel**, or is **Switched off**.
- The GE it holds ready and the most it holds, with a gauge beside the slots.
- How much it makes a tick.
- How long its fuel lasts at full output, counting what is left of the piece it is burning.
- **On** or **Off**, which switches it.

## Settings

The `generators` section of the server's settings:

| Setting | Default | Meaning |
| --- | --- | --- |
| `naquadah_energy` | 96000 | What a piece of naquadah is worth, in GE, before the mark's share |
| `ingot_energy` | 384000 | What a naquadah alloy ingot is worth |
| `mark_1.energy_per_tick` | 4 | GE the Mark I makes a tick |
| `mark_1.capacity` | 20000 | GE the Mark I holds |
| `mark_1.efficiency` | 1.0 | The share of its fuel's worth the Mark I gets |
| `mark_2.energy_per_tick` | 24 | GE the Mark II makes a tick |
| `mark_2.capacity` | 60000 | GE the Mark II holds |
| `mark_2.efficiency` | 0.5 | The share of its fuel's worth the Mark II gets |

See [Settings](../settings.md#naquadah-generators).
