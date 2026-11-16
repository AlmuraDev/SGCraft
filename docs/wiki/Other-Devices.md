# Other Devices

## Zero Point Modules

The Zero Point Module (ZPM) is the mod's biggest power source. ZPMs aren't crafted; they turn up in chests at
world-generated gates. A ZPM's tooltip shows its remaining power. A ZPM enchanted with **Infinity** is "tainted" and
explodes when a device draws on it.

ZPMs go in:

| Device | ZPMs | Notes |
|---|---|---|
| ZPM Console | 1 | gives Forge Energy to neighbours; powers gates within 6 blocks or linked |
| ZPM Interface Cart | 1 | gives Forge Energy (and EU with IC2) to neighbours |
| ZPM Hub | 3 | as the cart; the ZPMs share each draw |
| Supergate base | 3 | the supergate's own slots |
| City Shield Generator | 3 | see below |

How to link a ZPM device to a gate: see [Power](Power#linking-a-zpm-device). Right-click a device with the
[Configurator](Configurator) to see or remove its link.

## City Shield Generator

Raises a dome-shaped energy shield around itself. Needs MalisisCore.

- Right-click it (within 8 blocks) to set the shield's **Width**, **Above** and **Below** (3 to 128 blocks; 32, 24
  and 8 by default), see its volume, running cost and remaining time, and **Activate** or **Deactivate** it.
- Nothing can come in from outside: players and mobs are pushed back, arrows and fireballs are destroyed. Things
  already inside can leave. Explosions outside don't harm anything inside.
- It runs on **ZPMs only**: its own 3 slots plus ZPM Consoles within 6 blocks. The cost depends on the volume; three
  full ZPMs keep a shield of almost 5 million blocks up for about 30 days. It won't raise without 10 seconds of
  power, and drops when the ZPMs run dry. It takes at least 5 seconds to form.
- While up it keeps the chunks it covers loaded (server setting).
- Hoppers can put ZPMs in but not take them out. Breaking the generator drops its ZPMs with their power.

## Shield Console

Raises or lowers the shield of the nearest gate with a Shield Upgrade, within 40 blocks. Right-click it to see the
gate's address and status. You need iris access at the gate. Raising the shield needs enough power for the shield
(unless the gate needs no power). Needs MalisisCore.

## Naquadah Generators

Burn Naquadah (96,000 gate units each) or Naquadah Alloy Ingots (384,000 each) for power. Needs MalisisCore.

| | Output per tick | Store | Fuel efficiency |
|---|---|---|---|
| Mark I | 4 gate units (320 FE) | 20,000 | 100% |
| Mark II | 24 gate units (1,920 FE) | 60,000 | 50% |

Right-click to switch it on or off and fill its 4 fuel slots (hoppers can add fuel). While on, it powers a gate within
4 blocks of the gate's base and pushes Forge Energy into neighbouring blocks. It only burns fuel when its store isn't
full.

## Zat

A stun weapon. Right-click to charge and fire (three-quarters of a second); it reaches 30 blocks.

- The first hit does a little damage and stuns for 10 seconds: slowness, weakness, mining fatigue and nausea.
- A second hit on a stunned target kills it (server setting).
- It respects PvP settings and creative mode. One second between shots.

## Tok'ra

Tok'ra villagers appear at some world-generated gates (not ocean monuments). They trade:

- 8 Emeralds + 1 Diamond: Stargate Core Crystal
- 16 Emeralds + 1 Diamond: Stargate Controller Crystal

## Tollan Phase Shift Device

Held in the off-hand, lets a player pass safely through a closed iris or shield. No recipe.
