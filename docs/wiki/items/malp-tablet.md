# MALP Tablet

The MALP Tablet is a MALP console in your hand. It drives and watches the MALPs paired with it, and it runs on a battery inside it.

```infobox
MALP Tablet
ID: sgcraft:malp_tablet
Category: MALP
Type: Item
Stackable: No
Power: Its own battery, 0.025 gate energy (GE) a tick while it shows a live feed (`tablet_drain`)
Range: 256 blocks to the MALP, or to a gate that relays (`signal_range`)
Pairs: Any number of MALPs
```

## Obtaining

### Crafting

```recipe
sgcraft:malp_tablet
```

## Usage

1. Sneak and use the tablet in the air to open its battery slot. Put in a charged {{item:sgcraft:battery}}.
2. Sneak and use the tablet on a {{item:sgcraft:malp}} to pair them.
3. Use the tablet in the air to open its feed screen. Choose a MALP from the **PAIRED MALPS** list to drive or watch it.

Using the tablet on a MALP without sneaking opens the feed screen, as using it in the air does.

## Pairing

- Only the MALP's owner or an operator can pair a MALP with a tablet. Anyone else sees "You aren't allowed to pair this MALP".
- The tablet says "<name> is paired with this tablet", or "<name> is already paired with this tablet".
- The MALP stays paired with its {{item:sgcraft:malp_console}}. Any number of tablets can pair with one MALP, and one tablet can pair with any number of MALPs.
- The **x** beside a MALP on the feed screen takes it off the tablet.
- When the screen opens, MALPs the server no longer has leave the list, such as those picked up or destroyed. A MALP you pick up and put down again is a new MALP to the tablet, so pair it again.

## Feed screen

The feed screen is the same as a console's. The list on the left shows the paired MALPs. With none it says "No MALPs are paired. Sneak and use this tablet on a MALP to pair it."

When the screen opens, the tablet chooses the MALP you watched from it before, if no one else drives it. Otherwise it chooses the first MALP on its list that no one else drives. If someone else drives every one, you watch the first.

The first player to choose a MALP drives it. A player who chooses a MALP someone else drives watches it.

| Key or control | What it does |
| --- | --- |
| Forward (W), Back (S) | Drive forward or in reverse |
| Left (A), Right (D) | Turn, 4 degrees a tick |
| Left-drag in the feed | Pan and tilt the camera |
| `C` or **Center Camera** | Turn the camera back to face ahead |
| `L` or **Lamp On** / **Lamp Off** | Turn the headlights on or off |
| **Power Down Feed** / **Power Up Feed** | Switch the feed off or on |
| `P` or **Snapshot** | Save the camera picture to `screenshots/malp/` |
| **Close** or Escape | Leave the tablet |

See [Driving](../malp.md#driving) and [The feed and readouts](../malp.md#the-feed-and-readouts).

### Keeping the screen open

- Keep the tablet in the hand you used it from, in the same world, or the screen closes.
- Taking damage takes you off the tablet, as if you closed the screen (`console_damage_interrupts`, on by default).
- Other players see you holding the tablet out with both hands.
- A tablet has no monitor, so players around you don't see the feed.

## Power

The tablet needs a charged battery to show a feed. While its screen shows a live feed it uses 0.025 GE a tick from its own battery (`tablet_drain`), whether you drive or watch. It pays every 20 ticks. This is separate from what the MALP uses from its own battery.

The feed screen shows **TABLET BATTERY** with its percent, or **NO TABLET BATTERY**. With a flat battery the feed shows **NO TABLET POWER**.

### Battery slot

Sneak and use the tablet in the air to open its battery slot. The slot takes only batteries. The screen shows the battery's charge ("Battery <percent>%" or "No battery") and "Paired MALPs: <count>". It stays open while the tablet stays in the hand you used it from, and the tablet's own inventory slot can't be clicked or swapped with a number key while it is open.

Take the battery out to charge it in a {{item:sgcraft:power_unit}}, a {{item:sgcraft:battery_charger}} or the crafting grid.

## Signal

The radio of a tablet in your hands reaches 256 blocks (`signal_range`). The server checks it every 5 ticks.

- **DIRECT**: you and the MALP are in the same world and within range of each other.
- **RELAYED**: a gate within range of you has an open wormhole to a gate within range of the MALP. This works whichever end dialed, and through a shut iris.
- **NO SIGNAL**: neither. The MALP stops responding until the signal comes back.

## Tooltip

The tablet's tooltip shows "Paired MALPs: <count>" and its battery's charge ("Battery: <percent>%"), or "No battery".

## Data components

| Component | What it holds |
| --- | --- |
| `sgcraft:tablet_malps` | The paired MALPs, by UUID, in the order they were paired |
| `minecraft:container` | The battery in the tablet |

## Commands

These need permission level 2.

| Command | What it does |
| --- | --- |
| `/sgcraft malp pair_tablet <malp>` | Pair a MALP with the tablet in your hand, whoever owns the MALP |
| `/sgcraft malp unpair_tablet <malp>` | Take a MALP off the tablet in your hand |
| `/sgcraft malp tablet_drain [<ge>]` | Show or set the GE a tick a tablet uses while its feed is live, saved to the settings file. It replies "A MALP tablet uses <ge> GE a tick while it shows a live feed" |
| `/sgcraft malp console_damage_interrupts [<bool>]` | Show or set whether damage takes a player off a console or tablet |

## See also

- [The MALP tablet](../malp.md#the-malp-tablet)
- [Signal](../malp.md#signal)
- [Watchers and the console monitor](../malp.md#watchers-and-the-console-monitor)
- [MALP settings](../settings.md#malp)
- [Commands](../commands.md#malp-and-views)
