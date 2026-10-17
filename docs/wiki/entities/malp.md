# MALP

The MALP (Mobile Analytic Laboratory Probe) is a six-wheeled probe you drive through a stargate from a console or a tablet, watching its camera to see what is on the other side. You carry it as an item and put it down as an entity.

```infobox
MALP
ID: sgcraft:malp
Image: scenes/malp.png
Category: MALP
Type: Entity and item
Size: 1.0 wide, 0.9 tall
Health: Breaks when its damage passes 40 (each hit adds 10 times its damage, and the count falls by 1 a tick)
Speed: 0.15 blocks a tick (3 blocks a second), turns 4 degrees a tick
Stackable: No
Power: Battery (GE)
Step height: 1 block
```

## Obtaining

### Crafting

```recipe
sgcraft:malp
```

## Usage

1. Use the MALP item on a block face. The MALP goes down on that face, facing the way you face. It won't go down where it doesn't fit.
2. Use the MALP with an empty hand to open its battery slot, and put in a charged {{item:sgcraft:battery}}.
3. Use the MALP item on a {{item:sgcraft:malp_console}} to pair them, or sneak and use a {{item:sgcraft:malp_tablet}} on the MALP.
4. Open the console or tablet and choose the MALP from the **PAIRED MALPS** list to drive it. See [Driving](../malp.md#driving).

To pick the MALP up, sneak and use it with an empty hand. A name tag renames it. A new MALP is called `MALP` and the first four characters of its UUID, such as `MALP 3F2A`.

## Behavior

- The MALP never moves by itself. It drives only while someone at its console or tablet holds the controls, and only while it has power. A drive order lasts 4 ticks, so it stops as soon as the driver lets go.
- Its camera mast pans 120 degrees either way and tilts 45 degrees up or down, apart from the body. The lamp lights what the camera sees while the MALP has power.
- It goes through gates like any other traveler and keeps its battery, console pairing, name, owner and settings on the far side.
- The server remembers where each MALP was last seen, so its console finds it while its chunk is unloaded.
- You can push it, and it slows down in water.

## Ownership

The player who first puts a MALP down owns it. A MALP item that already has an owner keeps that owner. The owner and operators change its settings with a {{item:sgcraft:configurator}} and pair tablets with it. A MALP with no owner, such as one summoned with `/summon`, is managed by operators only. See [The configurator](../malp.md#the-configurator).

## Power

The MALP runs on the battery in its slot. A battery holds 12,500 gate energy (GE), set by `battery_capacity`.

| Use | Cost |
| --- | --- |
| A live feed from its driver | 0.125 GE a tick (`idle_drain`) |
| Driving | 1.25 GE for each block covered (`drive_drain`) |
| Watchers and monitor spectators | Nothing extra |

Rolling on or being pushed costs nothing. With a flat battery or none, the MALP shows **NO POWER** and doesn't move. Its **Requires power** setting (on by default) can be turned off with the configurator or `/sgcraft malp power_required`, and then it uses no energy and works with a flat battery or none. See [Batteries and charging](../malp.md#batteries-and-charging).

## Camera feed

A driver at a console or tablet sees a live picture from the MALP's camera while three things hold: the console or tablet reaches the MALP, the MALP has power, and a tablet has GE in its own battery.

- The radio reaches 256 blocks (`signal_range`), directly in the same world, or relayed through an open wormhole between a gate near the radio and a gate near the MALP. The signal passes a shut iris. See [Signal](../malp.md#signal).
- One player drives a MALP at a time. Others can watch from their own console or tablet, and up to 4 players within 16 blocks of a driven console see the feed on its monitor. See [Watchers and the console monitor](../malp.md#watchers-and-the-console-monitor).
- The feed shows the world around the MALP out to 32 blocks across and 24 blocks up and down by default. See [Remote view](../settings.md#remote-view).

## The world around a watched MALP

A MALP someone drives makes the world near it act as if a player stood there.

- Its chunk and the eight around it keep running, and chunks 3 out stay loaded. They stay loaded for 600 ticks (`linger_ticks`) after the last person stops watching.
- Mobs wander and despawn by the MALP as they do by a player. Despawn distances are measured from the nearest watched MALP when it is nearer than any player.
- Mobs spawn around it as around a player, in running chunks within 8 chunks (128 blocks) of it. The mob cap grows by those chunks only.
- In the End, a watched MALP within 192 blocks of (0, 128, 0) keeps the dragon fight's arena running.

See [The world around a watched MALP](../malp.md#the-world-around-a-watched-malp).

## Damage

The MALP is a vehicle, like a minecart or boat, and has no hearts. Hits add to a damage count, and it breaks when the count passes 40. A hit from a player in creative mode removes it at once with no drops.

- Lava, fire and cactus hurt it as they would a creature.
- Explosions hurt it, except an explosion a mob causes while the `mob_griefing` game rule is off.
- Mobs don't attack it.
- The kawoosh of an opening wormhole hurts it, a shut iris at the far end destroys it, and a one-way wormhole can destroy it. See [Damage](../malp.md#damage).

## Drops

| How | What you get |
| --- | --- |
| Picked up (sneak and use with an empty hand) | The MALP item, with its battery inside, console pairing, name, owner and settings |
| Destroyed | The MALP item, with its console pairing, name, owner and settings, and its battery as a separate item |
| Hit by a creative player | Nothing |

A destroyed MALP drops nothing when the `entity_drops` game rule is off. The item's tooltip shows the paired console, the battery's charge, and "Needs no power" when **Requires power** is off.

> **Note:** A MALP you pick up and put down again is a new entity to tablets. Pair your tablets with it again. Its console pairing stays.

## Data components

The MALP item keeps the MALP in these components:

| Component | Holds |
| --- | --- |
| `minecraft:container` | The battery |
| `sgcraft:malp_console` | The console it is paired with |
| `sgcraft:malp_owner` | Its owner's UUID |
| `sgcraft:malp_options` | Its settings, left out when they are the defaults |
| `minecraft:custom_name` | Its name |

## Commands

Every MALP command needs permission level 2. Name a MALP by its name or the start of its UUID, as `/sgcraft malp list` shows it.

| Command | What it does |
| --- | --- |
| `/sgcraft malp list` | Every MALP, where it was last seen, its battery, console and owner |
| `/sgcraft malp pair <malp> <pos>` | Pair it with the console at a position |
| `/sgcraft malp unpair <malp>` | Unpair it from its console |
| `/sgcraft malp battery <malp> <ge>` | Set its battery's charge in GE, giving it a battery if it has none |
| `/sgcraft malp power_required <malp> [<true/false>]` | Show or set whether it runs on its battery |
| `/sgcraft malp poweroff <malp>` | Power down the feed of everyone watching it |
| `/sgcraft malp recall <malp>` | Bring it to you |
| `/sgcraft malp center <malp>` | Turn its camera to face ahead |
| `/sgcraft malp signal <malp> [<pos>]` | How its console, or the console at a position, reaches it |
| `/sgcraft malp pair_tablet <malp>` | Pair it with the tablet in your hand |
| `/sgcraft malp unpair_tablet <malp>` | Take it off the tablet in your hand |

See [MALP and views](../commands.md#malp-and-views) for the rest, and [Settings](../settings.md#malp) for the `malp` settings.

## See also

- [The MALP guide](../malp.md)
- [Driving](../malp.md#driving)
- [The feed and readouts](../malp.md#the-feed-and-readouts)
- [Batteries and the battery charger](../power.md#batteries-and-the-battery-charger)
- [What a shut iris does](../iris.md#what-a-shut-iris-does)
