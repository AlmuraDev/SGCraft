# Server Administration

## Files

| File | What it holds |
|---|---|
| `config/sgcraft.cfg` | the main config (sections below) |
| `config/SGCraft/zpm.yml` | which world-to-world routes need a ZPM |
| `config/SGCraft/pdd.yml` | the address list every new PDD starts with |
| `config/SGCraft/generator.yml` | addresses of world-generated gates (the EDD's list) |
| `config/SGCraft/general.yml` | every gate's address |
| `config/SGCraft/world-names.properties` | world names, so dialers can name worlds that aren't loaded |

### zpm.yml

```yaml
worlds:
  world:
    DIM-1: 0
    DIM1: 1
```

For each world (by name), the worlds it dials into and a multiplier. A multiplier above 0 means that route needs ZPM
power, and is added to the dial's distance factor (see [Power](Power)).

### pdd.yml

```yaml
addresses:
  ABCD-EFG-HI:
    name: Home
    locked: true
    autoClose: false
```

`locked` entries can't be removed from a player's PDD.

## sgcraft.cfg

Most `[stargate]` settings are the **defaults for new gates**; each gate's own copy is changed with the
[Configurator](Configurator). `force-default-configs = true` re-applies the config to every existing gate on load.

| Section | Setting | Default | Meaning |
|---|---|---|---|
| stargate | energyPerFuelItem | 96000 | gate units per Naquadah |
| | gateOpeningsPerFuelItem | 24 | openings per Naquadah |
| | minutesOpenPerFuelItem | 80 | minutes open per Naquadah |
| | secondsToStayOpen | 300 | 0 = until closed |
| | oneWayTravel / reverseWormholeKills / closeFromEitherEnd | true / false / true | |
| | maxEnergyBuffer | 1000 | |
| | distanceFactorMultiplier | 1.0 | |
| | gateType | 1 | 1 Milky Way, 2 Pegasus, 3 Universe, 4 Ori |
| | chevronsLockOnDial / returnToPreviousIrisState | false / false | |
| | irisUpgrade / chevronUpgrade / requiresNoPower | false / false / false | |
| | transientDamage / transparency / allowRedstoneOutput | true / true / true | |
| | displayGateAddress | true | |
| | transientDamageRate | 5000 | kawoosh damage |
| | wormholeCanDestroyUnbreakableBlocks | false | |
| | explosionRadius / explosionFlame / explosionSmoke | 10 / true / true | breaking a connected gate's base |
| | repairGeneratedGates | true | rebuild a broken generated gate when it's dialed |
| | resetIsGenerated | true | re-flag gates whose address is in generator.yml |
| | toolSearchRange | 6 | how far the PDD, EDD and Configurator look for a gate |
| | zpmSearchRange | 6 | how far gates and city shields look for ZPMs |
| | eventHorizonLightLevel | 12 | light from an open gate (0-15) |
| iris | preserveInventory / allowRedstoneInput | false / true | |
| gate-access | defaultAllowIncoming / defaultAllowOutgoing | true / true | |
| player-access | defaultAllowGateAccess / IrisAccess / AdminAccess | true / true / true | |
| dhd | linkRangeX / linkRangeY / linkRangeZ | 5 / 1 / 6 | how far a DHD looks for a gate |
| | maxEnergyBuffer | 2500 | |
| | useDHDFuelSource | true | |
| shield | energyPerTick / consoleRange | 1.0 / 40 | |
| supergate | energyMultiplier | 8.0 | |
| options | chunkLoadingRange | 1 | chunks kept loaded round an active gate |
| | logStargateEvents | false | log gates being built and broken |
| | gateSpawnerBlocks | true | creative test-spawner blocks |
| | enableNaquadahOre | true | |
| naquadah | worlds | `*, !the_nether, !the_end` | where ore generates: dimension numbers, world names, `overworld`/`the_nether`/`the_end`, `*`; `!` excludes |
| | addToExistingChunks | true | add ore to chunks generated before the mod |
| | genUnderLavaOdds / maxNodesUnderLava | 1 / 16 | |
| | genIsolatedOdds / maxIsolatedNodes | 1 / 4 | |
| naquadah-generator | naquadahEnergy / ingotEnergy | 96000 / 384000 | |
| | markI/markII EnergyPerTick, Capacity, Efficiency | 4, 20000, 1.0 / 24, 60000, 0.5 | |
| city-shield | energyPerMillionBlocksPerTick / maxRadius / keepChunksLoaded | 0.333 / 128 / true | |
| rings | energyPerUse / energyPerBlock / energyCapacity | 20000 / 20 / 200000 | FE |
| | maxDistance / energyPerNaquadah | 2000 / 100000 | 0 = no limit |
| zat | range / cooldownTicks / stunTicks / stunDamage / secondShotKills | 30 / 20 / 200 / 4 / true | |
| computers | addressLookupCooldownMillis / maxAddressLookups / addressLookupWindowSeconds / addressLookupLockoutSeconds | 1000 / 20 / 60 / 600 | |
| opencomputers | wirelessRebroadcastStrength | 50 | |
| fp-power | energyBufferSize / fpPerSGEnergyUnit | 4000000 / 80 | SG Power Unit; FE per gate unit |
| ic2 | energyBufferSize / euPerSGEnergyUnit / safeInputRate / PowerTETier / fePerEU | 1000000 / 20 / 2048 / 5 / 4 | |
| block-harvest | dhdBlock / sgBaseBlock / sgRingBlock | false | whether they drop when broken |
| client | useHDEventHorizonTexture / saveAddressToClipboard / showPowerValuesOnGui | true / false / true | |
| debug | dialSync | false | log dial timing (chatty) |
| generaton | `<structure>_addon`, `_addon_chance`, `_chevron_upgrade_chance`, `_zpm_chest_chance`, `_spawn_tokra` | true, 25, 25, 15, true | per structure: village, nether, oceanmon, pyramid, igloo, jungle, swamp |
| loot.* | chance, rolls, item weights and counts | | per SGCraft chest loot table |
| recipes | one switch per recipe | true | see [Recipes](Recipes) |

## Commands

| Command | Who | What it does |
|---|---|---|
| `/sgpower` | anyone with access to the gate | at a gate or DHD (within 8 blocks): every power source the gate can use, with amounts in FE and gate units, and why the DHD isn't counted if it isn't |
| `/sgspawn <stargate\|supergate> [milkyway\|pegasus\|universe\|ori] [size]` | operators | builds a finished gate 3 blocks in front of you; size (odd, 9-61) and Ori are for supergates |

## Permissions (Sponge)

Permission nodes only work on a SpongeForge server. Without Sponge every node is granted, and only the gates' own
access lists restrict anything.

| Node | Allows |
|---|---|
| `sgcraft.admin` | everything: bypasses gate, iris and admin access lists, obelisk and ring access, Configurator setting nodes |
| `sgcraft.gui.configurator` | opening the Configurator (also needs gate admin access) |
| `sgcraft.configurator.<setting>` | changing each Configurator setting: secondsToStayOpen, ringRotationSpeed, maxEnergyBuffer, energyPerFuelItem, gateOpeningsPerFuelItem, distanceFactorMultiplier, interDimensionalMultiplier, oneWayTravel, hasIrisUpgrade, hasChevronUpgrade, gateType, reverseWormholeKills, closeFromEitherEnd, preserveInventory, noPowerRequired, chevronsLockOnDial, returnToPreviousIrisState, transientDamage, transparency, orientation, useDHDFuelSource, allowRedstoneOutput, allowRedstoneInput, canPlayerBreakPlayerGate, canPlayerBreakGeneratedGate, displayGateAddress, isGenerated, invulnerable, dynmap |
| `sgcraft.gui.gateAccessMenu` | the Gate Address Access List |
| `sgcraft.gui.playerAccessMenu` | the Player Access List |
| `sgcraft.gui.pdd` | using a PDD |
| `sgcraft.gui.pdd.edit` | changing PDD entries |
| `sgcraft.gui.edd` | using an EDD |
| `sgcraft.gui.gdo` | using a GDO |
| `sgcraft.gui.shieldconsole` | using a Shield Console |
| `sgcraft.gui.cityshield` | using a City Shield Generator |

## World-generated gates

Gates are added to some desert pyramids, swamp huts, igloos (Pegasus gates), jungle temples, nether fortresses, ocean
monuments and villages, as set in the `generaton` section. Each comes with a DHD holding some Naquadah, and may have
the Chevron Upgrade, a chest with a ZPM, and a Tok'ra. Their addresses go into `generator.yml`, which is the EDD's
list.

When a generated gate is dialed and its ring has been broken, it's rebuilt (`repairGeneratedGates`). If it's gone
altogether, it's taken off the list.

## Naquadah ore in existing worlds

With `naquadah.addToExistingChunks` on, chunks generated before SGCraft was added get ore the first time they load,
a few chunks per tick. In those chunks ore only replaces natural stone.

## Chunk loading

An active gate keeps its own chunks, and `chunkLoadingRange` chunks round it, loaded until it's idle again. A raised
City Shield keeps its chunks loaded if `city-shield.keepChunksLoaded` is on; for big shields, raise the limits in
Forge's `forgeChunkLoading.cfg`.

## Dynmap

With Dynmap installed, gates whose **Show on Dynmap** setting is on appear in a "Stargates" layer, labelled
"Stargate" or with their address. Dynmap keeps the markers itself; they update when a gate's settings change, when
it's built or broken, and when its chunk loads.
