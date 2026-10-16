# Installing SGCraft

SGCraft runs on Minecraft 26.3 with NeoForge or Fabric. This page covers what you need, how to install it in each launcher and on a dedicated server, and what to check when the game won't start.

```infobox
SGCraft 3.0
Minecraft: 26.3
Loaders: NeoForge, Fabric
Java: 25
Side: Client and server
Downloads: [GitHub releases](https://github.com/AlmuraDev/SGCraft/releases)
```

## What you need

There is one SGCraft jar for each loader. Download the one for your loader from the [releases page](https://github.com/AlmuraDev/SGCraft/releases). The NeoForge jar does not run on Fabric, and the Fabric jar does not run on NeoForge.

| Loader | Minecraft | Loader version | Other mods you need |
|--------|-----------|----------------|---------------------|
| NeoForge | 26.3 | 26.3.0.76 or newer | None |
| Fabric | 26.3 | Fabric Loader 0.19.5 or newer | Fabric API for 26.3 (built against 0.162.0+26.3) |

On Fabric, SGCraft carries Team Reborn Energy inside its jar, so power units can take energy from Fabric cables. You do not install Team Reborn Energy yourself.

SGCraft needs Minecraft 26.3 exactly and does not work with other Minecraft versions.

### Client and server

SGCraft must be installed on the server and on every player's client. A player without it can't join a server that has it. Use the same SGCraft version on the server and on every client.

## Java

SGCraft needs Java 25.

- **Playing with a launcher.** The official Minecraft Launcher, Prism Launcher, the Modrinth App and the CurseForge app bring or download their own Java. You don't need to install Java to play.
- **Running a server, or running an installer `.jar`.** You need Java 25 installed on that computer. Eclipse Temurin is a free build: download JDK 25 from [Adoptium](https://adoptium.net/temurin/releases/). The Fabric documentation has step by step Java guides for [Windows, macOS and Linux](https://docs.fabricmc.net/players/installing-java/).

To check which Java you have, open a terminal or command prompt and run:

```
java -version
```

The first line must show version 25 or newer.

## Installing on a client

### Official Minecraft Launcher with NeoForge

1. Install the [Minecraft Launcher](https://www.minecraft.net/en-us/download) and start Minecraft 26.3 once without mods.
2. Close the launcher.
3. Download the NeoForge installer for 26.3 (26.3.0.76 or newer) from [neoforged.net](https://neoforged.net/). Every version's installer is also on the [NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/).
4. Run the installer, choose **Install client** and click **Proceed**.
5. Open the launcher. A NeoForge profile is in the version list. Start it once, then close the game. This creates the `mods` folder.
6. Put the SGCraft NeoForge jar in the `mods` folder of the game directory.
7. Start the NeoForge profile again.

NeoForge's guide for this is [Installing a NeoForge Client](https://docs.neoforged.net/user/docs/client). It also shows how to give NeoForge its own game directory, which keeps your modded worlds apart from vanilla ones.

### Official Minecraft Launcher with Fabric

1. Install the [Minecraft Launcher](https://www.minecraft.net/en-us/download) and start Minecraft 26.3 once without mods.
2. Close the launcher.
3. Download the Fabric installer from [fabricmc.net](https://fabricmc.net/use/installer/). On Windows the `.exe` installer uses the launcher's Java. The `.jar` installer needs Java installed.
4. Run the installer. Choose Minecraft 26.3 and Fabric Loader 0.19.5 or newer, leave **Create profile** checked and click **Install**.
5. Download Fabric API for 26.3 from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
6. Put the Fabric API jar and the SGCraft Fabric jar in the `mods` folder:
  - Windows: `%appdata%\.minecraft\mods`
  - macOS: `~/Library/Application Support/minecraft/mods`
  - Linux: `~/.minecraft/mods`
7. Open the launcher, choose the Fabric profile and click **Play**.

Fabric's guides for this are [Installing Fabric](https://docs.fabricmc.net/players/installing-fabric) and [Installing Mods](https://docs.fabricmc.net/players/installing-mods).

### Prism Launcher

1. Install [Prism Launcher](https://prismlauncher.org/download/) and sign in with your Microsoft account.
2. Create an instance for Minecraft 26.3.
3. In the instance's settings, open **Version** and install NeoForge (26.3.0.76 or newer) or Fabric Loader (0.19.5 or newer).
4. On Fabric, add Fabric API from the instance's **Mods** page. Prism can download it from Modrinth or CurseForge.
5. Add the SGCraft jar for your loader on the **Mods** page, or put it in the instance's `mods` folder.
6. Launch the instance.

Prism downloads a matching Java when it has none. If it starts with the wrong one, set Java 25 in the instance's Java settings.

### Modrinth App

1. Install the [Modrinth App](https://modrinth.com/app) and sign in with your Microsoft account.
2. Create an instance for Minecraft 26.3 with NeoForge or Fabric as its loader.
3. On Fabric, add Fabric API to the instance from the app's content browser.
4. Open the instance's folder and put the SGCraft jar for your loader in its `mods` folder.
5. Play the instance.

### CurseForge app

1. Install the [CurseForge app](https://www.curseforge.com/download/app) and choose Minecraft.
2. Create a custom profile for Minecraft 26.3 with NeoForge or Fabric as its mod loader.
3. On Fabric, add Fabric API to the profile from the app's mod browser.
4. Open the profile's folder and put the SGCraft jar for your loader in its `mods` folder.
5. Play the profile.

> **Tip:** NeoForge lists other launchers that install it on its [launchers page](https://docs.neoforged.net/user/docs/launchers). In any launcher the steps are the same: Minecraft 26.3, the loader, Fabric API on Fabric, then SGCraft in `mods`.

## Installing on a dedicated server

A server needs Java 25 installed. Clients that join need the same SGCraft version (see [Client and server](#client-and-server)).

### NeoForge server

1. Make an empty folder for the server.
2. Download the NeoForge 26.3 installer (26.3.0.76 or newer) from [neoforged.net](https://neoforged.net/) or the [NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/).
3. In that folder, run the installer with `java -jar neoforge-26.3.0.76-installer.jar --installServer`. Use the file name of the installer you downloaded.
4. Start the server once with `run.bat` (Windows) or `./run.sh` (Linux and macOS). It stops on the first run.
5. Open `eula.txt` and set `eula=true` to accept the Minecraft EULA.
6. Put the SGCraft NeoForge jar in the server's `mods` folder.
7. Start the server again.

Memory and other Java options go in `user_jvm_args.txt`. NeoForge's full guide is [Installing a NeoForge Server](https://docs.neoforged.net/user/docs/server).

### Fabric server

1. Make an empty folder for the server.
2. Download the server launcher for Minecraft 26.3 and Fabric Loader 0.19.5 or newer from [fabricmc.net/use/server](https://fabricmc.net/use/server/), and put it in the folder.
3. Start it once with `java -Xmx2G -jar <launcher>.jar nogui`, where `<launcher>.jar` is the file you downloaded. It downloads Minecraft and Fabric Loader, then stops and asks you to accept the EULA.
4. Open `eula.txt` and set `eula=true`.
5. Put Fabric API for 26.3 and the SGCraft Fabric jar in the server's `mods` folder.
6. Start the server again.

The Fabric installer can also install a server: choose its **Server** tab. Fabric's wiki covers that route in [Setting up a Fabric server](https://wiki.fabricmc.net/player:tutorials:server:windows).

## Config files

SGCraft keeps its settings in the `config` folder of the game directory, or of the server's folder:

| File | What it holds |
|------|---------------|
| `config/sgcraft-server.json` | The server's settings: energy, costs, world generation, gate defaults, the MALP. Read as the server starts. On a single player game it is in your own game directory. |
| `config/sgcraft-client.json` | Your own display and sound settings. Clients only. |
| `config/sgcraft-default-addresses.json` | The addresses every new PDD and gate control tablet starts with. |

SGCraft writes each file if it is missing, and a missing setting takes its default. The server file holds only the settings that differ from their defaults, so a fresh one is `{}`. Server admins can change server settings in game with `/sgcraft settings` and other commands, without editing the file. See [Settings](settings.md) for every setting and [Commands](commands.md) for the commands.

## Upgrading from older SGCraft

SGCraft 3.0 is a remake for Minecraft 26.3. Worlds from the SGCraft mod for Minecraft 1.12 and earlier don't carry over: there is no converter for their gates, DHDs, items or addresses. Start a new world, or build your gates again.

Earlier SGCraft versions for Minecraft 1.7 to 1.12 are on the other branches of the [repository](https://github.com/AlmuraDev/SGCraft).

To update SGCraft 3.0 itself, back up your world, then replace the old SGCraft jar in `mods` with the new one. Keep only one SGCraft jar in the folder.

## Troubleshooting

### Wrong Java

The game or server stops at once and the log says the class file version is not supported, or that Java 25 is required. Install Java 25 and start it with that Java. In Prism, the Modrinth App or the CurseForge app, set the instance's Java to 25. On a server, run `java -version` in the server's folder: if it shows an older version, put the full path to Java 25's `java` in your start script.

### Missing Fabric API

On Fabric the game stops before the title screen and says SGCraft requires `fabric-api`. Download Fabric API for Minecraft 26.3 from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api) and put it in `mods`. A dedicated Fabric server needs it too.

### Version mismatch

- **The loader says SGCraft needs another Minecraft version.** SGCraft 3.0 needs Minecraft 26.3. Make an instance or profile for 26.3.
- **The loader is too old.** Update NeoForge to 26.3.0.76 or newer, or Fabric Loader to 0.19.5 or newer.
- **Wrong loader.** The NeoForge jar does not load on Fabric, and the Fabric jar does not load on NeoForge. Download the jar for your loader.
- **You can't join a server.** Install SGCraft on your client, and use the same SGCraft version as the server.

### Other problems

Report bugs on the [issue tracker](https://github.com/AlmuraDev/SGCraft/issues). Include your loader and its version, the SGCraft version and the log file (`logs/latest.log`).
