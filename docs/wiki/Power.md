# Power

A gate pays once to open a wormhole and then every tick to keep it open. Only the **dialing** gate pays. Costs are
shown in FE (Forge Energy); internally the gate counts "gate units", and 1 gate unit = 80 FE by default.

## What a dial costs

With the default settings, a standard gate pays:

| | Gate units | FE |
|---|---|---|
| To open | 4,000 | 320,000 |
| To stay open, per second | 20 | 1,600 |

That's one Naquadah (worth 96,000 gate units) per 24 openings, or per 80 minutes open. Both figures are then
multiplied by a **distance factor**:

- It grows with the distance between the gates, slowly at first: a nearby gate costs about the base amount, and a gate
  at the far edge of the address range costs up to 15 times as much. The gate's **Distance Factor Multiplier**
  scales this.
- Dialing **another world** multiplies it again by the Distance Factor Multiplier, and adds the route's ZPM
  multiplier, if the server sets one (see below).
- Supergates multiply it by 8.

If the gate can't pay, the dial fails with a message giving the cost to open, the cost per second, and what's
available, in FE. `/sgpower` at the gate lists every source it can see.

**No Input Power Required** (a [Configurator](Configurator) setting) makes a gate's dials free.

## What can pay

Everything within **4 blocks** of the gate's base, in any direction, counts, plus anything linked to the gate:

| Source | Notes |
|---|---|
| The gate's own buffer | up to the gate's **Energy Buffer Size** (1,000 gate units by default); filled from the other sources |
| DHD fuel | Naquadah in the linked DHD, if **Use DHD as Fuel Source** is on; not used for routes that need a ZPM |
| SG Power Unit | takes Forge Energy (FE/RF) from cables or generators; holds 4,000,000 FE |
| IC2 Stargate Power Unit | takes IC2 EU (IC2 only) |
| Naquadah Generator Mk I / Mk II | while switched on (see [Other Devices](Other-Devices)) |
| ZPM Console | holds one ZPM; also counts within 6 blocks |
| ZPM Interface Cart / ZPM Hub | 1 or 3 ZPMs; found nearby only with IC2 installed, otherwise link them |
| Linked ZPM devices | any ZPM Console, Cart or Hub in the same world linked to the gate |
| Supergate ZPM slots | supergates only |

The cost is shared evenly across the sources.

### Linking a ZPM device

Sneak-right-click the ZPM Console, Interface Cart or Hub, then click any block of a merged gate in the same world
within 60 seconds. Sneak-click the device again to cancel. A gate's [Configurator](Configurator) lists its linked
ZPMs and can unlink them.

## Routes that need a ZPM

The server can mark travel between two worlds as needing a ZPM, in `config/SGCraft/zpm.yml`. On those routes the
dialing gate must have ZPM power (the DHD's naquadah doesn't count), and the connection closes if the ZPMs run out.
See [Server Administration](Server-Administration#zpmyml).

## Supergates

Supergates run on ZPMs only and need **3 ZPMs** at each end, in their ZPM slots, nearby or linked. They shut down if
fewer than 3 are left. The supergate's screen shows ZPMs in use, ZPM energy, the cost to open and per tick, and how
many openings and how long open the ZPMs allow.
