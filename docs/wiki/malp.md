# MALP

The MALP (Mobile Analytic Laboratory Probe) is a six-wheeled probe you drive through a stargate from a console or a tablet, to see what waits on the other side before anyone steps through.

```infobox
MALP
Icon: sgcraft:malp
ID: sgcraft:malp
Type: Entity and item
Stacks: 1
Size: 1.0 wide, 0.9 tall
Speed: 0.15 blocks a tick (3 blocks a second)
Power: Battery (GE)
Recipe: [Crafting](#crafting)
Dictionary: [MALP](entities/malp.md)
```

## What it is

The MALP has a camera mast that pans 120 degrees either way and tilts 45 degrees up or down, a dish, a grapple arm and two headlights. It never moves by itself, only while someone drives it from a [MALP console](#the-malp-console) or a [MALP tablet](#the-malp-tablet), and only while its battery holds gate energy (GE), unless it is set to need no power.

It goes through gates like any other traveler. It keeps its battery, its console pairing, its name, its owner and its settings on the far side. The server remembers where each MALP was last seen, so a console finds it even while its chunk is unloaded.

## Placing, owning and picking up

- To place a MALP, use the MALP item on a block face. It goes down on that face, facing the way you face, and won't go down where it doesn't fit.
- The player who first puts a MALP down owns it. A MALP item that already has an owner keeps that owner.
- A new MALP is called `MALP` and the first four characters of its UUID, such as `MALP 3F2A`. Use a name tag on it, or change its name in its configurator screen, to rename it. Its name shows above it all the time, not only while you look at it.
- Use the MALP with an empty hand to open its battery slot. The screen shows the battery's charge (or "Needs no power") and the console it is paired with. Stay within 8 blocks to keep it open.
- To pick it up, sneak and use the MALP with an empty hand. The item keeps its battery, console pairing, name, owner and settings. Its tooltip shows the paired console and the battery's charge, or that it needs no power.
- Only the owner and operators can pick up a MALP, rename it with a name tag or open its battery slot. Anyone else is told "This MALP isn't yours". Anyone can handle a MALP with no owner.

> **Note:** A MALP you pick up and put down again is a new entity to tablets. Pair your [tablets](#the-malp-tablet) with it again. Its console pairing stays.

## Batteries and charging

```infobox
Battery
Icon: sgcraft:battery
ID: sgcraft:battery
Type: Item
Stacks: 1
Holds: 12,500 GE
Recipe: [Crafting](#crafting)
Dictionary: [Battery](items/battery.md)
```

A battery holds up to 12,500 GE (`malp.battery_capacity`). A green bar on the item shows its charge, and the tooltip shows the exact GE. The creative tab has an empty battery and a full one. Other mods' chargers can fill a battery, 80 FE for each GE, and draw FE out of it: on NeoForge it is a NeoForge energy item, and on Fabric a Team Reborn Energy item.

You can charge a battery three ways:

| Where | How fast | Notes |
| --- | --- | --- |
| [Power unit](power.md) battery slot | 12.5 GE a tick (`power_unit_charge_rate`) | Takes GE from the unit's store. |
| Battery charger | 12.5 GE a tick (`charger_charge_rate`) | See below. |
| Crafting grid | At once | A battery with naquadah in any arrangement. Each naquadah adds 96,000 GE (`energy.per_naquadah`), so one piece fills a battery and the rest is lost. A full battery doesn't craft, so no naquadah is wasted on it. |

### Battery charger

```infobox
Battery Charger
Icon: sgcraft:battery_charger
ID: sgcraft:battery_charger
Type: Block
Slots: 1 battery, 1 naquadah
Store: 192,000 GE
Recipe: [Crafting](#crafting)
Dictionary: [Battery Charger](blocks/battery-charger.md)
```

The battery charger has a bay for a battery and a slot for naquadah. Its own store holds two naquadah's worth of GE (192,000 GE by default). It burns one naquadah only when its store has room for all 96,000 GE of it, and it passes GE from the store into the battery. FE cables can fill the store too, 80 FE for each GE.

### What a MALP uses

| Use | Cost |
| --- | --- |
| A live feed from its driver | 0.125 GE a tick (`idle_drain`), once per MALP however many people watch |
| Driving | 1.25 GE for each block covered (`drive_drain`) |
| Watchers and monitor spectators | Nothing extra |
| A tablet showing a live feed | 0.025 GE a tick from the tablet's own battery (`tablet_drain`), paid every 20 ticks |

Rolling on after you let go, or being pushed, costs nothing. A MALP with a flat battery or no battery shows **NO POWER** and doesn't move.

A MALP's **Requires power** setting is on by default. Turned off (with the [configurator](#the-configurator) or `/sgcraft malp power_required`), the MALP uses no energy and works with a flat battery or none. Its readout then says **NEEDS NO POWER**.

## The MALP console

```infobox
MALP Console
Icon: sgcraft:malp_console
ID: sgcraft:malp_console
Type: Block
Pairs: Any number of MALPs
Recipe: [Crafting](#crafting)
Dictionary: [MALP Console](blocks/malp-console.md)
```

The console is a desk three blocks wide with a wide monitor, two blocks high, like the [Dialing Computer](blocks/dialing-computer.md). It faces the player who placed it.

- To pair a MALP, use the MALP item on the console. Any number of MALPs can pair with one console, but each MALP pairs with one console at a time. Pairing a MALP with a second console moves it there.
- To open the feed, use the console with an empty hand. With a free [office chair](blocks/office-chair.md) in front of it, you sit in the chair; otherwise you step up to its front (if there is safe ground there within your reach). Either way you face its monitor. Other players see you with your hands on its keyboard, or sitting.
- The screen stays open while you stay within your reach of the console, plus 2 blocks.

## The MALP tablet

```infobox
MALP Tablet
Icon: sgcraft:malp_tablet
ID: sgcraft:malp_tablet
Type: Item
Stacks: 1
Power: Its own battery
Recipe: [Crafting](#crafting)
Dictionary: [MALP Tablet](items/malp-tablet.md)
```

The tablet is a MALP console in your hand.

- To pair a MALP, sneak and use the tablet on the MALP. Only the MALP's owner or an operator can do this. The MALP stays paired with its console too, and any number of tablets can pair with one MALP. The server keeps which tablets it paired with which MALPs, so a tablet's list edited in creative mode reaches no MALP it wasn't paired with.
- To open the feed, use the tablet in the air. The list on the left shows the paired MALPs. The **x** beside each one takes it off the tablet.
- To open the battery slot, sneak and use the tablet in the air. The tablet needs a charged battery to show a feed. The screen shows **TABLET BATTERY** with its charge, or **NO TABLET BATTERY**. With a flat battery the feed shows **NO TABLET POWER**.
- Keep the tablet in the hand you used it from, in the same world, or the screen closes. Other players see you holding it out with both hands.

## Driving

Open a console or tablet, then choose a MALP from the **PAIRED MALPS** list. The first player to choose a MALP drives it. Driving uses your own movement keys, so it follows your key settings.

| Key or control | What it does |
| --- | --- |
| Forward (W) | Drive forward |
| Back (S) | Drive in reverse |
| Left (A), Right (D) | Turn, 4 degrees a tick |
| Left-drag in the feed | Pan and tilt the camera (0.6 degrees for each pixel) |
| `C` or **Center Camera** | Turn the camera back to face straight ahead |
| `L` or **Lamp On** / **Lamp Off** | Turn the headlights on or off |
| **Power Down Feed** / **Power Up Feed** | Switch the feed off or on |
| `P` or **Snapshot** | Save the camera picture |
| **Close** or Escape | Leave the console or tablet |

A drive order lasts 4 ticks, so the MALP stops as soon as you let go. It climbs steps one block high. The lamp lights the view only while the MALP has power. **Center Camera** and the lamp work only while the feed is live.

**Snapshot** saves the MALP's picture alone, at the feed's shape, to `screenshots/malp/` named for the date and time. The feed flashes, the header says **SNAPSHOT SAVED** and chat links the file. With no picture to save, it says **NO PICTURE**. The game's own screenshot key (F2) also works on this screen. Watchers can take snapshots too.

## The feed and readouts

The feed fills the right of the screen. Its header reads **MALP SYSTEMS ONLINE** for the driver, or **VIEW ONLY** for a watcher, with **VIDEO FEED ONLINE** or **VIDEO FEED LOST**. When there is no picture, the feed shows why:

| Feed state | Meaning |
| --- | --- |
| Live picture | The radio reaches the MALP and everything has power. |
| **NO SIGNAL** | The console or tablet doesn't reach the MALP. See [Signal](#signal). |
| **NO POWER** | The MALP's battery is flat or missing, and it needs power. |
| **FEED OFFLINE** | The feed was powered down, or the player you were watching left. |
| **NO TABLET POWER** | The tablet's own battery is flat or missing. |

Small lines of readouts run along the bottom of the feed:

- `POS` with the MALP's X, Y and Z, its `CHUNK`, and the nearest gate in its world within the signal's range: `GATE`, its address, how far it is in meters and which way it lies, such as `GATE SJVC-5QC-@ 8m W`. A gate that hides its address shows `HIDDEN`. With no gate in range it says `NO GATE IN RANGE`.
- `WORLD`, `BIOME`, `TEMP` and `TIME`. The temperature is the biome's base temperature times 25, in degrees C. The time is a 24-hour clock where a new day (tick 0) is 06:00.
- `LIGHT`, `HOSTILES n/32m` (hostile mobs within 32 meters), `BATTERY` with its percent (or `NEEDS NO POWER`), and `SIGNAL` (`DIRECT` or `RELAYED`). The line turns red when the battery is at 10% or less.
- `ROUTE`, when the signal is relayed: the gates it goes through.

A box in the feed's top right corner gives the MALP's `HEADING`: the compass point it faces and its bearing in degrees from north, such as `W 270`.

## Signal

The console's radio, or the radio of a tablet in your hands, reaches 256 blocks (`signal_range`). The server checks the signal every 5 ticks.

- **DIRECT:** the radio and the MALP are in the same world and within range of each other.
- **RELAYED:** a gate within range of the radio has an open wormhole to a gate within range of the MALP. The signal goes through only once the wormhole is steady, after its kawoosh. This works whichever end dialed, and through a shut [iris](iris.md). The signal is lost when the wormhole closes.
- **NO SIGNAL:** neither. The MALP stops responding until the signal comes back.

> **Tip:** Dial the far world first (see [Dialing](dialing.md)), drive the MALP through, and keep the wormhole open while you look around. Watch the gate's open time (`open_ticks`, 6000 ticks by default).

## Watchers and the console monitor

One player drives each MALP at a time. Other players can watch:

- A player who opens a console someone else is at sees that player's feed. They can only take snapshots and close the screen, and they don't step up to the console.
- A player can choose, from their own console or tablet, a MALP someone else drives. They need their own signal (and tablet power) to see it, and they can switch to another MALP.

When the player you watch leaves or switches MALP, your feed goes to **FEED OFFLINE**.

The console's monitor shows a feed to the players nearest it as a faint green picture: up to 8 players (`console_spectators`, 0 to 64) within 16 blocks (`spectator_range`, 1 to 64). A spectator can step 2 blocks past the range before the picture stops. The server checks every 10 ticks. While someone drives at the console, it shows their live feed, which costs no extra energy. With no one at it, it shows the MALP last chosen there, while anyone is near: that MALP's chunk stays loaded and it uses its idle energy, unless someone drives it. With no one near, nothing is sent. A tablet has no monitor. See [MALP Console](blocks/malp-console.md#watchers-and-the-monitor).

## The world around a watched MALP

A MALP counts as watched while someone drives it from a console or tablet, and while a console's monitor shows it to players nearby with no one at the console. Players who watch a driver's feed add nothing of their own, since the driver's MALP already counts. A watched MALP makes the world around it behave as if a player stood there:

- The chunks as far out from the MALP as its camera sees keep running, so the mobs in sight move: 3 chunks out with the default view of 32 blocks (`remote_view.horizontal_radius`), and chunks 5 out stay loaded, wherever the MALP is. They stay loaded, and the MALP still counts as watched, for 600 ticks (`linger_ticks`) after its driver leaves it, or for 40 ticks after its monitor last showed it.
- Mobs wander and despawn by the MALP the way they do by a player. Distances for despawning are measured from the nearest watched MALP when it is nearer than any player.
- Mobs spawn around the MALP as around a player: in running chunks within 8 chunks (128 blocks) of it, with the mob cap growing by those chunks, and with spawn distances measured from the MALP.
- In the End, a MALP within 192 blocks of the island's center (0, 128, 0) keeps the dragon fight's arena running, as a player there would.

The feed shows the world around the MALP out to 32 blocks across and 24 blocks up and down by default, with its sky, its time of day and its weather: rain or snow falls around the camera as it does around a player there, by the biome, and stops under roofs. Server admins can change how far it reaches in [settings](settings.md#remote-view).

## Damage

The MALP takes damage like a minecart or boat: hit it enough and it breaks. It also takes damage from:

- explosions, lava, fire and cactus;
- the kawoosh of an opening wormhole in front of it (5000 a tick), when the gate's `opening_damage` is on and its iris isn't shut;
- arriving at a shut iris, which destroys it as it would any traveler;
- entering the receiving end of a one-way wormhole, when the dialing gate's `reverse_travel_kills` is on.

A destroyed MALP drops its MALP item and, beside it, its battery. The item keeps the console pairing, name, owner and settings.

A player at a console or tablet who takes damage is taken off it, as if they closed the screen (`console_damage_interrupts`, on by default).

## The configurator

Use a [configurator](tools.md) on a MALP to open its settings. Only the MALP's owner and operators can, and they must stay within their reach plus 4 blocks.

The screen shows the owner (or that it has none, so only operators configure it) and the console it is paired with. It has a **Requires power** toggle and these buttons:

- **Save** applies the changes.
- **Revert** goes back to the saved settings.
- **Unpair** unpairs the MALP from its console at once, without waiting for **Save**. Anyone driving it from there loses it.
- **Close** leaves without saving.

## Crafting

```recipe
sgcraft:malp
```

```recipe
sgcraft:malp_console
```

```recipe
sgcraft:malp_tablet
```

```recipe
sgcraft:battery
```

```recipe
sgcraft:battery_charger
```

## Server admins

### Commands

Every MALP command needs permission level 2. MALPs are named by their name or the start of their UUID, as `/sgcraft malp list` shows them. A command about a MALP whose chunk isn't loaded loads it and asks you to run it again.

| Command | What it does |
| --- | --- |
| `/sgcraft malp list` | Every MALP: where it was last seen, battery or "needs no power", console and owner |
| `/sgcraft malp pair <malp> <pos>` | Pair a MALP with the console at a position |
| `/sgcraft malp unpair <malp>` | Unpair a MALP from its console |
| `/sgcraft malp battery <malp> <ge>` | Set its battery's charge in GE, giving it a battery if it has none |
| `/sgcraft malp power_required <malp> [<true/false>]` | Show or set whether it runs on its battery |
| `/sgcraft malp poweroff <malp>` | Power down the feed of everyone watching it |
| `/sgcraft malp recall <malp>` | Bring it to you |
| `/sgcraft malp center <malp>` | Turn its camera to face ahead |
| `/sgcraft malp signal <malp> [<pos>]` | How its console, or the console at a position, reaches it |
| `/sgcraft malp pair_tablet <malp>` | Pair it with the tablet in your hand, whoever owns it |
| `/sgcraft malp unpair_tablet <malp>` | Take it off the tablet in your hand |
| `/sgcraft battery [set <ge>]` | Show or set the charge of the battery in your main hand |

`/sgcraft malp console_damage_interrupts`, `console_spectators`, `spectator_range` and `tablet_drain` show or change those settings and save them to the file. See [Commands](commands.md#malp-and-views) for the full list, including `/sgcraft view`.

### Settings

The `malp` section of `config/sgcraft-server.json`. What one naquadah gives a battery or a charger is `energy.per_naquadah` (96000), the same as a DHD.

| Key | Default | Meaning |
| --- | --- | --- |
| `battery_capacity` | `12500` | Most GE a battery holds |
| `power_unit_charge_rate` | `12.5` | GE a tick a power unit puts into a battery |
| `charger_charge_rate` | `12.5` | GE a tick a battery charger puts into a battery |
| `idle_drain` | `0.125` | GE a tick a MALP uses while its feed is live |
| `drive_drain` | `1.25` | GE a MALP uses for each block it drives |
| `signal_range` | `256` | Blocks a radio reaches, directly or to a gate |
| `linger_ticks` | `600` | Ticks a MALP's chunks stay loaded after its driver leaves it |
| `console_damage_interrupts` | `true` | Damage takes a player off a console or tablet |
| `console_spectators` | `8` | Players near a console who see its monitor (0 to 64) |
| `spectator_range` | `16` | Blocks from a console to see its monitor (1 to 64) |
| `tablet_drain` | `0.025` | GE a tick a tablet uses while its feed is live |

The `remote_view` group sets how much of the world the feed shows. See [Settings](settings.md#malp) and [Remote view](settings.md#remote-view).
