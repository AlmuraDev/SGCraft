<p align="center">
  <a href="https://almuradev.github.io/SGCraft-Docs/">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://almuradev.github.io/SGCraft-Docs/logo.svg">
      <img alt="SGCraft Remake 3.0" src="https://almuradev.github.io/SGCraft-Docs/logo-light.svg" width="520">
    </picture>
  </a>
</p>

<p align="center">
  <b>Stargates for Minecraft 26.3, on NeoForge and Fabric.</b><br>
  Build a ring, dial an address and step through the event horizon.
</p>

<p align="center">
  <a href="https://almuradev.github.io/SGCraft-Docs/install.html"><b>Install</b></a> ·
  <a href="https://almuradev.github.io/SGCraft-Docs/getting-started.html"><b>Getting started</b></a> ·
  <a href="https://almuradev.github.io/SGCraft-Docs/"><b>Wiki</b></a> ·
  <a href="https://github.com/AlmuraDev/SGCraft/releases/tag/snapshot-26.3"><b>Download</b></a> ·
  <a href="https://almuradev.github.io/SGCraft-Docs/faq.html"><b>FAQ</b></a>
</p>

<table align="center">
  <tr>
    <td><img alt="An open Milky Way gate at night" src="https://almuradev.github.io/SGCraft-Docs/scenes/milky-way-open-night.jpg" width="400"></td>
    <td><img alt="An open Pegasus gate at night" src="https://almuradev.github.io/SGCraft-Docs/scenes/pegasus-open-night.jpg" width="400"></td>
  </tr>
  <tr>
    <td><img alt="An open Universe gate at night" src="https://almuradev.github.io/SGCraft-Docs/scenes/universe-open-night.jpg" width="400"></td>
    <td><img alt="An open supergate at night" src="https://almuradev.github.io/SGCraft-Docs/scenes/supergate-open-night.jpg" width="400"></td>
  </tr>
</table>

## Quick start

1. Install NeoForge or Fabric for Minecraft 26.3. On Fabric, add Fabric API too.
2. Download the SGCraft jar for your loader from the
   [latest build](https://github.com/AlmuraDev/SGCraft/releases/tag/snapshot-26.3) and put it in your `mods` folder.
   The NeoForge jar only runs on NeoForge, and the Fabric jar only on Fabric.
3. Mine naquadah, craft the ring blocks, build the ring, set down a DHD and dial.
   [Getting started](https://almuradev.github.io/SGCraft-Docs/getting-started.html) walks through each step.

Launcher steps for the official launcher, Prism, Modrinth and CurseForge, plus server setup, are in
[Installing SGCraft](https://almuradev.github.io/SGCraft-Docs/install.html).

## Where to read more

| You want to | Read |
|---|---|
| Install it on a client or a server | [Installing SGCraft](https://almuradev.github.io/SGCraft-Docs/install.html) |
| Build and dial your first gate | [Getting started](https://almuradev.github.io/SGCraft-Docs/getting-started.html) |
| Know what to work towards | [What to aim for](https://almuradev.github.io/SGCraft-Docs/what-to-aim-for.html) |
| Look up a block, item or recipe | [Blocks](https://almuradev.github.io/SGCraft-Docs/blocks.html), [Items](https://almuradev.github.io/SGCraft-Docs/items.html), [Recipes](https://almuradev.github.io/SGCraft-Docs/recipes.html) |
| Run a server | [Settings](https://almuradev.github.io/SGCraft-Docs/settings.html), [Commands](https://almuradev.github.io/SGCraft-Docs/commands.html), [Access](https://almuradev.github.io/SGCraft-Docs/access.html) |
| Come over from SGCraft 2 on 1.12 | [Changes since SGCraft 2](https://almuradev.github.io/SGCraft-Docs/changes.html) |
| Write an add-on | [API](https://almuradev.github.io/SGCraft-Docs/api.html), [Building from source](https://almuradev.github.io/SGCraft-Docs/building-from-source.html) |
| Fix a problem | [FAQ](https://almuradev.github.io/SGCraft-Docs/faq.html) |

## What you can do

- Build Milky Way, Pegasus and Universe gates, as 5 by 5 rings or as supergates up to 61 blocks across.
- Dial from a DHD, a dialing computer, a handheld dialer, a tablet or a command.
- Cross between worlds. Mobs, items, minecarts and riders go through too.
- Fit an iris or a shield, and open it from a distance with a GDO.
- Power gates with naquadah, ZPMs, or FE from other mods' cables.
- Come across gates already standing in pyramids, temples, ocean monuments, fortresses and villages.
- Send a MALP through first and drive it from a console while its camera feed plays on the screen.
- Fly a puddle jumper through a gate, then use transport rings, Asgard obelisks, Zats and city shields.
- Run every gate from `/sgcraft` commands, so a server never needs a screen open.

## Building from source

You need JDK 25. Then:

```bash
./gradlew build
```

That puts one jar per loader in `neoforge/build/libs` and `fabric/build/libs`. `./gradlew :neoforge:runClient` or
`./gradlew :fabric:runClient` starts a development client. `./gradlew :neoforge:runGameTestServer` runs the game
tests, which place, dial and travel through real gates and check the API's contracts.

The code is split into `api`, the Stargate API that add-ons build against, and `core`, the mod itself, which knows
only vanilla Minecraft. `neoforge` and `fabric` are thin layers that load `core` on each loader. SGCraft depends on no
other mod; links to other mods belong in add-ons built on the API. The wiki has its own repository,
[SGCraft-Docs](https://github.com/AlmuraDev/SGCraft-Docs), and rebuilds whenever this branch changes.

Versions for Minecraft 1.7 to 1.12 live on this repository's other branches.

## Credits and license

SGCraft was created by **gcewing**, who wrote the original mod: the gates, the DHD, dialing and addressing that
everything here grew from. This remake for Minecraft 26.3 is by AlmuraDev and builds on that work.

The source code is under the MIT license; see [LICENSE](LICENSE). The released mod also contains gate model data
under the Creative Commons Attribution-NonCommercial license (see [CREDITS.md](CREDITS.md)), so the mod as
distributed may not be sold or used commercially.
