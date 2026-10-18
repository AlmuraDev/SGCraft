# Building from source

This page is for contributors and add-on authors. It covers building SGCraft with Gradle, how the project is laid out, the code style, building this wiki, and sending changes.

Add-on authors who only need the API should read [API](api.md) first. An add-on builds against the `sgcraft-api` library and does not need the mod's source.

## What you need

- JDK 25. Eclipse Temurin 25 from [Adoptium](https://adoptium.net/temurin/releases/) works. Gradle's toolchain support finds an installed JDK 25 and can download one if it finds none.
- Git, from [git-scm.com](https://git-scm.com/downloads).

You need nothing else. The Gradle wrapper in the repository downloads the right Gradle version (9.7.1) on first use.

## Getting the source

```
git clone https://github.com/AlmuraDev/SGCraft.git
cd SGCraft
```

The Minecraft 26.3 remake is on its own branch. The earlier versions for Minecraft 1.7 to 1.12 are on other branches. Check out the remake's branch before you build.

## Gradle tasks

Run every task through the wrapper from the repository root: `./gradlew` on Linux and macOS, `gradlew.bat` (or `.\gradlew`) on Windows.

| Task | What it does |
|------|--------------|
| `./gradlew build` | Compiles everything, checks license headers and builds the jars. |
| `./gradlew :neoforge:runClient` | Starts a NeoForge development client with SGCraft loaded. |
| `./gradlew :fabric:runClient` | Starts a Fabric development client with SGCraft loaded. |
| `./gradlew :neoforge:runServer` | Starts a NeoForge development server. |
| `./gradlew :fabric:runServer` | Starts a Fabric development server. |
| `./gradlew :neoforge:runGameTestServer` | Runs the game tests on a test server, then stops. |
| `./gradlew licenseCheck` | Fails if a source file lacks its license header. Same as `checkLicenses`. |
| `./gradlew licenseFormat` | Adds or fixes the license header on every source file. Same as `updateLicenses`. |
| `./gradlew :neoforge:runIconExport` | Renders every item's icon into `docs/wiki/icons` for the wiki. |

The game tests place real gates, dial them, send entities through and check the API's contracts. Run them before you send a change that touches gates, dialing, energy or the API.

Each run keeps its game directory in the loader's project: `neoforge/runs/client`, `neoforge/runs/server`, `neoforge/runs/gametest`, `fabric/runs/client` and `fabric/runs/server`. These folders are not committed.

### Where the jars land

`./gradlew build` puts one mod jar per loader, with its sources and javadoc jars beside it:

| Project | Folder | Mod jar |
|---------|--------|---------|
| NeoForge | `neoforge/build/libs` | `sgcraft-neoforge-26.3-<version>.jar` |
| Fabric | `fabric/build/libs` | `sgcraft-fabric-26.3-<version>.jar` |
| API | `api/build/libs` | `sgcraft-api-26.3-<version>.jar` |

The version comes from `version` in `gradle.properties` (`26.3.0.0` at the time of writing). The Fabric jar carries Team Reborn Energy inside it. The API jar is the library add-ons compile against. It is not a mod and does not go in `mods`.

## Project layout

| Folder | What it holds |
|--------|---------------|
| `api` | The Stargate API. Add-ons build against this alone. Its contracts are versioned on their own. |
| `core` | The mod itself, written against vanilla Minecraft only. Gates, dialing, items, world generation, commands, the MALP. |
| `neoforge` | The thin layer that loads `core` on NeoForge, and the game tests. |
| `fabric` | The thin layer that loads `core` on Fabric. |
| `build-logic` | Gradle convention plugins shared by the projects: Java version, jar manifest, metadata expansion, license headers. |
| `tools` | Scripts used to make the mod's art and data (gate models, glyphs, DHD art, templates), and the wiki builder in `tools/wiki`. |
| `docs/wiki` | The Markdown pages of this wiki. |

Each loader's jar compiles the `api` and `core` sources into itself, so a release is one jar per loader. The versions of Minecraft, NeoForge, Fabric Loader, Fabric API and Team Reborn Energy are set in `gradle.properties`.

SGCraft does not depend on any other mod, and `core` does not know about any other mod. Integrations with other mods belong in add-ons built on the API.

## Code style

Pull requests must follow these rules.

- Use American English everywhere: identifiers, comments, javadoc, docs, lang files, commit messages and in-game text. Write color, gray, center, meter, behavior, neighbor, license, dialing, dialed, dialer, traveler, canceled. Text quoted from third-party works keeps its own spelling, and so do Minecraft's own names (such as `BlockBehaviour`).
- Every method and constructor parameter is `final`, in interfaces too.
- Call instance methods as `this.method()` and static methods as `ClassName.staticMethod()`, even inside the same class.
- Import each class by name, with no star imports.
- Every Java source file starts with the MIT license header from `HEADER.txt`. Files that carry on gcewing's work use `HEADER-gcewing.txt` and are listed in `build-logic/src/main/groovy/multiloader-common.gradle`. Run `./gradlew licenseFormat` to add headers; `./gradlew build` fails without them.
- Keep lines to 120 characters or fewer.
- API javadoc states the contract an implementor must keep. It does not describe how SGCraft's own implementation works.

## Building the wiki

The wiki is the Markdown in `docs/wiki`. A Python script turns it into a themed HTML site.

1. Install Python 3 from [python.org](https://www.python.org/downloads/) and [Pillow](https://pypi.org/project/pillow/) with `python -m pip install Pillow`.
2. From the repository root, run `python tools/wiki/build.py`.
3. Open `build/wiki/index.html` in a browser.

The script cuts the address glyphs from the mod's own glyph fonts, so it needs the repository's `core` resources. It deletes and rewrites `build/wiki` on every run.

Item icons come from the game. Run `./gradlew :neoforge:runIconExport` to render every item's icon into `docs/wiki/icons`, then build the wiki again. Run it after you add an item or change its look.

To write or change a page, follow the format of the pages already there: a `#` title, a one or two sentence summary right after it, and sentence case headings. Pages support infoboxes, crafting recipe grids and `{{glyphs:...}}` addresses.

## Contributing

- Report bugs and ideas on the [issue tracker](https://github.com/AlmuraDev/SGCraft/issues). For a bug, give the loader and its version, the SGCraft version, the steps that cause it and the log (`logs/latest.log`).
- To send a pull request, fork the [repository](https://github.com/AlmuraDev/SGCraft), make your change on a branch of your own and open a pull request against the remake's branch.
- Before you open it, run `./gradlew build` and `./gradlew :neoforge:runGameTestServer`, and check your change on both loaders with `runClient` if it touches anything a player sees.
- A new feature that admins would change also needs a `/sgcraft` command, so a server can run it without a screen. See [Commands](commands.md).

SGCraft's code is under the MIT license (see `LICENSE`). The gate model data is under its sources' own licenses, listed in `CREDITS.md`.
