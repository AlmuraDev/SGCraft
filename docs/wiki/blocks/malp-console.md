# MALP Console

The MALP Console is a desk with a screen that you drive MALPs from. It shows the camera feed of a paired MALP and, while someone drives, shows that feed on its monitor to the players around it.

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

1. Place the console. It faces you. The desk is 10 pixels high, with the monitor on the half away from you.
2. Use a {{item:sgcraft:malp}} item on the console to pair it. Chat says "The MALP is paired with this console".
3. Put the MALP down and give it a charged {{item:sgcraft:battery}}. See [MALP](../entities/malp.md).
4. Right-click the console with an empty hand to open the **MALP Console** screen, then choose the MALP from the **PAIRED MALPS** list.

Any number of MALPs can pair with one console, but each MALP pairs with one console at a time. Pairing a MALP with a second console moves it there. With no MALP paired, the screen says "No MALPs are paired. Use a MALP on this console to pair it."

## Screen

When you open the screen, you step up to the console's front (if there is safe ground there within your reach), face it and look 30 degrees down at its keys. Other players see you working the controls. The screen stays open while you stay within your reach of the console, plus 2 blocks.

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

## Signal

The console's radio reaches 256 blocks (`signal_range`). The server checks the signal every 5 ticks.

- **DIRECT:** the console and the MALP are in the same world and within range of each other.
- **RELAYED:** a gate within range of the console has an open wormhole to a gate within range of the MALP. This works whichever end dialed, and through a shut iris. The signal is lost when the wormhole closes.
- **NO SIGNAL:** neither. The MALP stops responding until the signal comes back.

See [The MALP](../malp.md#signal).

## Watchers and the monitor

One player drives at a console at a time. A player who opens a console someone else is at sees that player's feed, with **VIEW ONLY** in the header. A watcher can only take snapshots and close the screen, and doesn't step up to the console.

While the driver has a live feed, the console's monitor shows that feed as a faint green picture to the players nearest it: up to 4 players (`console_spectators`, 0 to 64) within 16 blocks (`spectator_range`, 1 to 64). A spectator can step 2 blocks past the range before the picture stops. The server checks every 10 ticks. Watching costs no energy.

A player at the console who takes damage is taken off it, as if they closed the screen, while `console_damage_interrupts` is on (default `true`).

## Breaking

Breaking the console drops it. The console stores nothing itself: each MALP remembers the position of the console it is paired with.

## Settings

The `malp` object of `config/sgcraft-server.json`:

| Setting | Default | What it does |
| --- | --- | --- |
| `signal_range` | 256 | How far a console reaches a MALP directly, and reaches a gate to relay through, in blocks |
| `console_damage_interrupts` | `true` | A player at a console who takes damage is taken off it |
| `console_spectators` | 4 | How many players near a driven console see its monitor, nearest first (0 to 64) |
| `spectator_range` | 16 | How near a console a player must be to see its monitor, in blocks (1 to 64) |

## Commands

Every MALP command needs permission level 2.

- `/sgcraft malp pair <malp> <pos>` pairs a MALP with the console at a position.
- `/sgcraft malp unpair <malp>` unpairs a MALP from its console.
- `/sgcraft malp signal <malp> [<pos>]` shows how its console, or the console at a position, reaches it.
- `/sgcraft malp console_damage_interrupts`, `console_spectators` and `spectator_range` show or change those settings.

See [Commands](../commands.md#malp-and-views).

## See also

- [The MALP](../malp.md#the-malp-console)
- [MALP](../entities/malp.md)
- [Battery Charger](battery-charger.md)
