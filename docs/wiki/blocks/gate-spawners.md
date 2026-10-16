# Gate spawners

Gate spawners are seven creative blocks that each build a finished gate where you place them: a Milky Way, Pegasus or Universe gate, as a stargate or a supergate, or an Ori supergate. They are meant for testing and building.

```infobox
Gate spawners
ID: sgcraft:milky_way_stargate_spawner, sgcraft:milky_way_supergate_spawner, sgcraft:pegasus_stargate_spawner, sgcraft:pegasus_supergate_spawner, sgcraft:universe_stargate_spawner, sgcraft:universe_supergate_spawner, sgcraft:ori_supergate_spawner
Icon: sgcraft:milky_way_stargate_spawner
Category: Creative only
Type: Block
Stackable: Yes (64)
Tool: None
Hardness: 1.5
Blast resistance: 6
Waterloggable: Yes
Drops: Nothing
Block entity: Yes
Builds: A gate with 9 chevrons; a supergate of radius 7
```

## Kinds

| Spawner | ID | Builds |
| --- | --- | --- |
| {{item:sgcraft:milky_way_stargate_spawner}} | `sgcraft:milky_way_stargate_spawner` | A Milky Way stargate |
| {{item:sgcraft:pegasus_stargate_spawner}} | `sgcraft:pegasus_stargate_spawner` | A Pegasus stargate |
| {{item:sgcraft:universe_stargate_spawner}} | `sgcraft:universe_stargate_spawner` | A Universe stargate |
| {{item:sgcraft:milky_way_supergate_spawner}} | `sgcraft:milky_way_supergate_spawner` | A Milky Way supergate |
| {{item:sgcraft:pegasus_supergate_spawner}} | `sgcraft:pegasus_supergate_spawner` | A Pegasus supergate |
| {{item:sgcraft:universe_supergate_spawner}} | `sgcraft:universe_supergate_spawner` | A Universe supergate |
| {{item:sgcraft:ori_supergate_spawner}} | `sgcraft:ori_supergate_spawner` | An Ori supergate |

The Ori supergate comes only as a supergate, so it has no stargate spawner. The Ori Supergate Spawner is the only way to get an Ori supergate without a command or the configurator. See [Ori supergates](../building-gates.md#ori-supergates).

## Obtaining

Spawners have no recipe. They are only in the SGCraft creative tab, or you can get one with `/give`.

### Pick block

Pick block (middle click) on a block of a finished gate gives the spawner that builds a gate like it: the stargate or supergate spawner of the gate's type. An Ori supergate gives the Ori Supergate Spawner.

## Usage

Place a spawner where you want the gate's base. On the next tick the spawner is gone and the gate stands in its place:

- The base goes where the spawner was, facing you, and the ring rises from it. The gate always stands upright.
- A stargate has 9 chevrons. A supergate has 9 chevrons and a radius of 7, 15 blocks across. An Ori supergate is the same size and always reaches as far as 9 chevrons.
- The player who placed the spawner owns the gate. See [Access](../access.md#owners).
- The spawner builds the gate alone, with no DHD. Place a {{item:sgcraft:dhd}} or {{item:sgcraft:dhd_pegasus}} next to a Milky Way or Pegasus gate yourself. A Universe gate and an Ori supergate have no DHD.
- Blocks in the gate's space are destroyed without drops. Another gate in the way stops the build.
- A spawner placed in water keeps the water, so the gate stands in water.

If the gate can't be built, the spawner is still gone and you see "The gate couldn't be built" with the reason.

The 9 chevrons came with the gate, so breaking the gate later drops no chevron upgrades for them. See [Building gates](../building-gates.md#breaking-a-gate).

No server setting turns spawners off.

## In structures

The world's gate templates hold spawners too. Each builds its gate once the world runs, facing the way the structure turned it. A gate built this way is a generated gate with no owner. See [World generation](../worldgen.md#generated-gates).

## Commands

`/sgcraft place <type> [<chevrons>] [<radius>] [<lying>]` builds a gate as a spawner does, with a choice of chevrons, size and whether the gate lies flat. It needs operator rights. See [Commands](../commands.md#gates).

## See also

- [Building gates](../building-gates.md#gate-spawners)
- [Building gates: supergates](../building-gates.md#supergates)
- [The DHD](../dhd.md)
