# MALP Console

The MALP Console is a desk with a wide monitor, three blocks wide and two blocks high, that you drive MALPs from, like the [Dialing Computer](dialing-computer.md) beside it in the SGC. Its screen shows the camera feed of a paired MALP, and its monitor shows a MALP's feed to the players around it, whether or not anyone is at the console.

```infobox
MALP Console
ID: sgcraft:malp_console
Category: MALP
Type: Block
Stackable: Yes (64)
Tool: Pickaxe
Tool tier: Any pickaxe
Hardness: 1.5
Blast resistance: 6
Waterloggable: No
Drops: Itself
Block entity: Yes
Pairs: Any number of MALPs
Signal range: 256 blocks (signal_range)
```

## Obtaining

### Crafting

```recipe
sgcraft:malp_console
```

## Usage

1. Place the console. It faces you. It needs room for all six of its blocks: the block you place it on, one to either side, and the three above. Breaking any of them breaks the whole console. An [office chair](office-chair.md) up to 2 blocks (`chair_reach`) in front of its middle, facing it, lets you sit at it.
2. Give it power: a charged [power unit](../power.md#the-power-unit) or a ZPM block within 6 blocks of its middle. See [Power](#power).
3. Use a {{item:sgcraft:malp}} item on the console to pair it. Chat says "The MALP is paired with this console".
4. Put the MALP down and give it a charged {{item:sgcraft:battery}}. See [MALP](../entities/malp.md).
5. Right-click the console with an empty hand to open the **MALP Console** screen, then choose the MALP from the **PAIRED MALPS** list. Any of its six blocks works.

Any number of MALPs can pair with one console, but each MALP pairs with one console at a time. Pairing a MALP with a second console moves it there. To unpair one, press the **x** beside it in the screen's list; only the MALP's owner and operators can. With no MALP paired, the screen says "No MALPs are paired. Use a MALP on this console to pair it."

## Screen

If a free office chair stands in front of the console, using the console sits you in it; sitting in the chair yourself does the same. Without one, you step up and stand at the console's front, if there is safe ground there within your reach, and other players see you with your hands on its keyboard. Either way you turn to face the monitor and the screen opens. Getting up from the chair closes it, and so does walking away: the screen stays open while you stay within your reach of the console, plus 2 blocks.

The first player to choose a MALP drives it. You drive with your own movement keys:

| Key or control | What it does |
| --- | --- |
| Forward (W), Back (S) | Drive forward or in reverse |
| Left (A), Right (D) | Turn, 4 degrees a tick |
| Left-drag in the feed | Pan and tilt the camera |
| `C` or **Center Camera** | Turn the camera to face straight ahead |
| `L` or **Lamp On** / **Lamp Off** | Turn the headlights on or off |
| **Power Down Feed** / **Power Up Feed** | Switch the feed off or on |
| `P` or **Snapshot** | Save the camera picture to `screenshots/malp/` |
| **Close** or Escape | Leave the console |

The header reads **MALP SYSTEMS ONLINE** for the driver. When the feed has no picture it shows why: **NO SIGNAL**, **NO POWER** or **FEED OFFLINE**. See [The MALP](../malp.md#the-feed-and-readouts) for the readouts.

## Power

The console needs gate energy to run: 0.05 GE a tick (`energy_per_tick`), 1 GE a second, paid once a second while it is switched on. It draws from the [power units](../power.md#the-power-unit) within 6 blocks of the middle of its desk along each axis (`power_reach`), nearest first, then from the ZPM consoles, interface carts and hubs within that reach, nearest first. A full power unit runs it for about 14 hours. When nothing in reach can pay, it has no power. Its monitor goes dark, anyone using it is taken off it, and using it says "The computer has no power". It starts again by itself once there is power, within a second. Turn `requires_power` off to run computers for free.

The console's owner (the player who placed it) and operators can use a {{item:sgcraft:configurator}} on it to turn off its own need for power. A computer placed before computers had owners has none, so only operators can.

Sneak and use the console with an empty hand to switch it off. Its monitor goes dark, anyone using it is taken off it, and it uses no energy. Do the same again to switch it on, or just use it. See [Computers](../power.md#computers).

## Signal

The console's radio reaches 256 blocks (`signal_range`). The server checks the signal every 5 ticks.

- **DIRECT:** the console and the MALP are in the same world and within range of each other.
- **RELAYED:** a gate within range of the console has an open wormhole to a gate within range of the MALP. It goes through only once the wormhole is steady, after its kawoosh. This works whichever end dialed, and through a shut iris. The signal is lost when the wormhole closes.
- **NO SIGNAL:** neither. The MALP stops responding until the signal comes back.

See [The MALP](../malp.md#signal).

## Watchers and the monitor

One player drives at a console at a time. A player who opens a console someone else is at sees that player's feed, with **VIEW ONLY** in the header. A watcher can only take snapshots and close the screen, and doesn't step up to the console.

The console's monitor looks like its screen. On the left it lists the paired MALPs with their batteries, the one it shows marked, and says **CONSOLE IN USE** or **STANDBY**. On the right is the feed in its frame, the shown MALP's name in the header, and while the feed is live, the MALP's readouts and a crosshair. When the feed has no picture, the monitor says why, over static while it looks for a signal or power, or on a plain dark feed:

- **NO FEEDS AVAILABLE:** no MALP is paired.
- **FEED OFFLINE:** the feed was powered down at the console, or no MALP is chosen. A feed powered down stays down, with no one at the console too, until someone powers it up there.
- **SIGNAL LOST:** the console doesn't reach the chosen MALP.
- **NO POWER:** the MALP's battery is flat.

It also shows static while the picture loads, and to players who aren't among those the picture is sent to. Switched off or without power, the monitor is dark.

The monitor shows the picture, as a faint green image, to the players nearest it: up to 8 players (`console_spectators`, 0 to 64) within 16 blocks (`spectator_range`, 1 to 64). A spectator can step 2 blocks past the range before the picture stops. Players in that range are also told the list, the name and the readouts. The server checks every 10 ticks.

- When someone is at the console, the monitor shows their feed while it is live. Watching it costs no energy beyond the driver's feed.
- A player who leaves the console goes on seeing the same feed on the monitor, without it loading again, and gets it back on the screen the same way when they use the console again.
- When no one is at the console, the monitor goes on showing the MALP last chosen at the console, or else the first MALP paired with it, while the console's signal reaches it and it has power. While anyone is near enough to see the monitor, the MALP's chunk stays loaded and the MALP uses its idle energy (`idle_drain`), unless someone drives it from elsewhere, whose feed pays for it. With no one near, nothing is sent and no energy is used.

Each player who sees the monitor costs the server a stream of the world around the MALP, which is why their number is capped. A dialing computer's monitor costs nothing like it: every client already knows what the gate is doing and draws the dialing program itself.

A player at the console who takes damage is taken off it, as if they closed the screen, while `console_damage_interrupts` is on (default `true`).

## Breaking

Breaking any part of the console breaks the whole console and drops it once. Each MALP remembers the position of the console it is paired with; the console itself only remembers the MALP last chosen at it, for its monitor.

## Settings

The `malp` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `signal_range` | 256 | How far a console reaches a MALP directly, and reaches a gate to relay through, in blocks |
| `console_damage_interrupts` | `true` | A player at a console who takes damage is taken off it |
| `console_spectators` | 8 | How many players near a console see its monitor, nearest first (0 to 64) |
| `spectator_range` | 16 | How near a console a player must be to see its monitor, in blocks (1 to 64) |

Its power comes from the `computers` object, shared with the [Dialing Computer](dialing-computer.md#settings): `requires_power` (`true`), `energy_per_tick` (0.05) and `power_reach` (6).

## Commands

Every MALP command needs permission level 2.

- `/sgcraft malp pair <malp> <pos>` pairs a MALP with the console at a position, any of its six blocks.
- `/sgcraft malp unpair <malp>` unpairs a MALP from its console.
- `/sgcraft malp signal <malp> [<pos>]` shows how its console, or the console at a position, reaches it.
- `/sgcraft malp console_damage_interrupts`, `console_spectators` and `spectator_range` show or change those settings.

See [Commands](../commands.md#malp-and-views).

## See also

- [The MALP](../malp.md#the-malp-console)
- [MALP](../entities/malp.md)
- [Battery Charger](battery-charger.md)
