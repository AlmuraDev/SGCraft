# City Shield Generator

The City Shield Generator raises a dome of energy around itself, powered by the power units and ZPMs near it and the ZPMs in its own slots. While the shield is up, things outside can't come in, projectiles stop at it, and explosions outside don't reach what is inside.

```infobox
City Shield Generator
ID: sgcraft:city_shield_generator
Category: Power
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 5
Blast resistance: 2000
Drops: Itself and its ZPMs
Block entity: Yes
Power: Power units within 6 blocks along each axis (city_power_reach), its 3 ZPM slots, and ZPM consoles, carts and hubs within 6 blocks (zpm.search_range)
Size: 3 to 128 blocks each way (city_max_radius)
Rights: Its owner and operators
```

```gallery
scenes/city-shield-day.jpg | Raised, by day
scenes/city-shield-night.jpg | Raised, by night
```

## Obtaining

### Crafting

Craft it from 4 {{item:sgcraft:naquadah_block}}, 2 {{item:sgcraft:shield_upgrade}}, 2 {{item:sgcraft:core_crystal}} and 1 {{item:minecraft:nether_star}}:

```recipe
sgcraft:city_shield_generator
```

## Usage

1. Place the generator. You own it.
2. Give it energy: place charged {{item:sgcraft:power_unit}}s or ZPM consoles, carts or hubs within 6 blocks of it along each axis, or put {{item:sgcraft:zpm}}s in its own slots.
3. Right-click the generator to open its screen.
4. Set the size, then press **Activate**.

Only the owner and operators work the generator. Anyone else sees its screen and state, with "Only the generator's owner can work it".

## Screen

| Control | What it does |
| --- | --- |
| **Width (all around)** | How far the shield reaches out from the generator, every way across, 3 to 128 blocks |
| **Above generator** | How far it reaches up, 3 to 128 blocks |
| **Below generator** | How far it reaches down, 3 to 128 blocks |
| **Activate** | Raises the shield |
| **Apply** | Resizes the shield to the numbers in the boxes, keeping it up |
| **Deactivate** | Lowers the shield |

The screen also shows:

- **Volume (blocks):** how much space the shield holds.
- **Energy per tick:** what the shield costs to keep up.
- **Energy available:** the energy in the power units and ZPMs it draws on, in green, or in red when they hold none.
- **Runs for:** how long that energy keeps the shield up, in the two largest units of months, weeks, days, hours, minutes and seconds.
- **ZPMs:** three ZPM slots above your inventory, which take only {{item:sgcraft:zpm}}s.

## Shape

The shield is two half-ellipsoids that meet at the generator's level: the top half reaches **Above generator** up and the bottom half reaches **Below generator** down, and both reach **Width** out all around. The default size is 32 out, 24 up and 8 down.

## Power

The generator runs on these, and draws on them in this order:

1. the ZPMs in its own three slots, evenly,
2. the power units within 6 blocks of it along each axis (`shields.city_power_reach`), nearest first,
3. the ZPMs in ZPM consoles, interface carts and hubs within 6 blocks of it along each axis (`zpm.search_range`), nearest first.

It takes from the next source only when the one before runs short. It takes energy in the same gate energy that gates use. A full power unit holds 50,000 GE. A full ZPM holds 26,843,546 GE. A tainted ZPM in the generator's own slots doesn't explode, but one in a console, cart or hub near it does when the shield draws on it.

The shield costs 0.333 energy a tick for every million blocks it holds (`shields.city_energy_per_million_blocks`):

| Size (out, up, down) | Volume | Energy a tick | One full power unit lasts | One full ZPM lasts |
| --- | --- | --- | --- | --- |
| 32, 24, 8 (default) | About 68,600 blocks | About 0.023 | About 30 hours | About 680 days |
| 128, 128, 128 (largest) | About 8.8 million blocks | About 2.9 | About 14 minutes | About 5 days |

- Raising the shield needs 10 seconds' worth of energy.
- The shield falls when its power units and ZPMs run dry.

See [Power](../power.md#the-city-shield).

## While the shield is up

- Things moving in from outside are pushed back and bounced off.
- Projectiles, such as arrows and fireballs, are stopped.
- Things inside can leave.
- Explosions outside don't harm the blocks or things inside. Explosions inside work as normal.
- It doesn't stop teleports: moves of more than 10 blocks in one tick.

### Chunk loading

A raised shield keeps the chunks under it loaded and running, on one ticket at the generator. This is saved with the world, so the shield carries on after a restart. The chunks are released when the shield is lowered, runs out of power or the generator is broken. `shields.city_keeps_chunks_loaded` (on by default) turns this off.

## Looks and sound

The shield is drawn as a dome of blue light, brighter where you see it edge on. As it rises it spreads up and over from the generator's level, bright at its spreading edge, over about 5 seconds, and draws back the same way as it falls. It plays the shield's raising and lowering sounds, heard about 64 blocks away.

## Settings

The `shields` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `city_energy_per_million_blocks` | 0.333 | Energy a tick a city shield costs for every million blocks it holds |
| `city_max_radius` | 128 | The most a city shield reaches each way, in blocks, up to 256 |
| `city_power_reach` | 6 | How far from the generator, along each axis, its power units may stand |
| `city_keeps_chunks_loaded` | `true` | A raised city shield keeps the chunks under it loaded |

See [Settings](../settings.md#shields). How far the ZPM consoles, carts and hubs it draws on may stand is `zpm.search_range` (6). See [Settings](../settings.md#zpms).

## Commands

Operators can use these:

- `/sgcraft shields city <pos>` shows the generator at a position: whether its shield is up, its size, its cost and the energy it can draw on.
- `/sgcraft shields city <pos> raise` raises its shield. Without the energy it says "There isn't enough energy around the generator to raise a shield that size".
- `/sgcraft shields city <pos> lower` lowers it.
- `/sgcraft shields city <pos> size <width> <above> <below>` sets its size.

See [Commands](../commands.md#shields).

## See also

- [Power](../power.md#the-city-shield)
- [Stargate Power Unit](power-unit.md)
- [Zero Point Module](../items/zpm.md)
- [Stargate Shield Upgrade](../items/shield-upgrade.md)
