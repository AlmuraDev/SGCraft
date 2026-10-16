# Tok'ra

The Tok'ra are villagers who keep watch over gates the world builds and trade the crystals a gate is made from. A Tok'ra works at a DHD.

```infobox
Tok'ra
ID: sgcraft:tokra
Icon: sgcraft:dhd
Category: Villagers
Type: Villager profession
Job site: Milky Way DHD or Pegasus DHD
Trades: 2
```

## Obtaining

### At generated gates

When the world builds a gate, one Tok'ra spawns in front of it. It stands at the nearest spot 2 to 5 blocks out from the gate and up to 2 blocks up that has solid ground and room for a villager. If there is no such spot, no Tok'ra spawns. A Tok'ra spawned this way never despawns.

Gates at these structures get a Tok'ra by default:

| Structure | Tok'ra |
| --- | --- |
| Desert pyramid | Yes |
| Jungle temple | Yes |
| Swamp hut | Yes |
| Igloo | Yes |
| Village gate plaza | Yes |
| Ocean monument | No |
| Nether fortress | No |

See [Structure gates](../worldgen.md#structure-gates) and [Village gates](../worldgen.md#village-gates).

### From any villager

A villager with no job can take any DHD as its job site and become a Tok'ra. No other profession takes a DHD. See [the DHD guide](../dhd.md#tokra).

## Trades

A Tok'ra has trades at the first level only. It offers 2 trades from the trade set `sgcraft:tokra/level_1`:

| Wants | Gives | Uses | Villager XP | Reputation discount |
| --- | --- | --- | --- | --- |
| 8 {{item:minecraft:emerald}} and 1 {{item:minecraft:diamond}} | 1 {{item:sgcraft:core_crystal}} | 12 | 5 | 0.05 |
| 16 {{item:minecraft:emerald}} and 1 {{item:minecraft:diamond}} | 1 {{item:sgcraft:controller_crystal}} | 12 | 5 | 0.05 |

A data pack can change the trades. The trade set reads the trades from the tag `#sgcraft:tokra/level_1`, which lists `sgcraft:tokra/1/core_crystal` and `sgcraft:tokra/1/controller_crystal` in `data/sgcraft/villager_trade/`.

## Server admins

- `tokra_worlds` (default `{"minecraft:overworld": true}`) turns Tok'ra on or off at each world's generated gates, by world ID. A world set to `false` gets none. A world left out gets them wherever its structure settings say.
- Each structure gate and the village gates have a `tokra` field (default `true`). See [Structure gates](../settings.md#structure-gates) and [Village gates](../settings.md#village-gates).
- `/sgcraft worldgen tokra` lists the worlds set off. `/sgcraft worldgen tokra <world> <true|false>` sets one and saves it.
- `/sgcraft tokra` (permission level 2) puts a Tok'ra in front of your selected gate, or the one you look at. It says "There's no room in front of the gate for a Tok'ra to stand" when no spot fits.

## See also

- [Tok'ra villagers](../worldgen.md#tokra-villagers)
- [The DHD](../dhd.md#tokra)
- [Commands](../commands.md#worldgen-and-ore)
