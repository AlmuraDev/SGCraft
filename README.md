# SGCraft

Stargates for Minecraft: build a gate, dial another one from its DHD and step through the event horizon.
Milky Way, Pegasus and Universe gates, from standard size up to supergates.

SGCraft was created by **gcewing**, who wrote the original mod and the gates, DHD, dialing and addressing that
everything here grew from. This version is a remake for Minecraft 26.3 by AlmuraDev, built on that work.

## Loaders

| Loader   | Minecraft |
|----------|-----------|
| NeoForge | 26.3      |
| Fabric   | 26.3      |

Earlier versions for Minecraft 1.7 to 1.12 are on the other branches of this repository.

## Projects

- `api`: the Stargate API. Add-ons build against this alone; its contracts are versioned on their own.
- `common`: the mod, written against vanilla Minecraft only.
- `neoforge`, `fabric`: thin layers that load `common` on each loader.

SGCraft does not depend on or know about any other mod. Integrations with other mods belong in add-ons built on
the API.

## Building

Needs JDK 25. `./gradlew build` puts one jar per loader in `neoforge/build/libs` and `fabric/build/libs`.
`./gradlew :neoforge:runClient` or `./gradlew :fabric:runClient` starts a development client.
`./gradlew :neoforge:runGameTestServer` runs the game tests, which place, dial and travel through real gates on a
test server and check the API's contracts.

## License

The source code is under the MIT license; see [LICENSE](LICENSE). The released mod also contains gate model data
under the Creative Commons Attribution-NonCommercial license (see [CREDITS.md](CREDITS.md)), so the mod as
distributed may not be sold or used commercially.
