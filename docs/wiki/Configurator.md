# Configurator

The Configurator changes a gate's settings. Right-click within 6 blocks of a merged gate (without sneaking). You must
be the gate's admin; on a Sponge server you also need `sgcraft.gui.configurator`, and each setting has its own
permission (`sgcraft.configurator.<setting>`), greyed out if you don't have it. Settings are saved when you press
**Save** while still holding the Configurator at the gate. Needs MalisisCore.

The Configurator is also used to link a DHD by hand (see [Addresses and Dialing](Addresses-and-Dialing#the-dhd-stargate-controller)),
to set who may use an obelisk or ring platform, and to view a ZPM device's link.

## Numbers

| Setting | What it does | Range |
|---|---|---|
| Seconds to Stay Open | how long a dialed connection lasts; 0 = until closed | 0-50,000 |
| Gate Rotation Speed | how fast the ring turns when dialing (Milky Way) | 1-10 |
| Energy Buffer Size | how much energy the gate stores itself | 0-5,000,000 |
| Energy per Naquadah | gate units each Naquadah is worth | 0-250,000 |
| Openings per Naquadah | how many openings one Naquadah pays for | 1-50,000 |
| Distance Factor Multiplier | scales how much distance and other worlds add to costs (see [Power](Power)) | 0-1,000 |
| Inter-Dimensional Multiplier | shown, but currently has no effect | 0-1,000 |

## Gate style

Milky Way, Pegasus, Universe, or (supergates only) Ori. An Ori gate locks the settings that don't apply to it.

## Switches

| Setting | What it does |
|---|---|
| One Way Travel Only | only the dialing end transports |
| None / Titanium Iris / Shield | which barrier the gate has |
| Chevron Upgrade | 9 chevrons |
| Reverse Wormhole Kills | entering the receiving end kills you |
| Close from Either End | the receiving end may close the connection |
| Preserve Inventory on Iris Death | keep your items if you die on the iris or going backwards |
| No Input Power Required | dials are free |
| Chevrons lock when dialed | chevrons lock as each symbol is entered (see [Addresses and Dialing](Addresses-and-Dialing#chevrons-lock-as-dialed)) |
| Return to Previous Iris State | re-close the iris after a connection if it was closed before |
| Transient Damage | the kawoosh hurts entities and removes blocks |
| Event Horizon Transparency | see-through event horizon |
| Use DHD as Fuel Source | the DHD's naquadah can pay for dials |
| Allow Redstone Output | the gate gives a redstone signal while active |
| Allow Redstone Input | a redstone signal to the base closes the iris |
| Player can destroy player stargate | survival players may break this gate if a player built it |
| Player can destroy generated stargate | survival players may break this gate if the world generated it |
| Display Gate Address | show the address on the gate, PDD and Shield Console |
| World Generated Stargate | treat the gate as world-generated |
| Invulnerable | only creative players can break it |
| Show on Dynmap | a marker for the gate on the server's Dynmap |
| Show Address on Dynmap | include the address in the marker's label (only with Show on Dynmap) |

**Load Defaults** fills the screen with the server's defaults; nothing changes until you press Save.

## Access lists

The two buttons under **Access Control Systems** open the gate's access lists (you need to be its admin; on Sponge,
also `sgcraft.gui.gateAccessMenu` / `sgcraft.gui.playerAccessMenu`).

- **Player Access List.** Per player: **Gate** (may dial and travel), **Iris** (may work the iris and GDO), **Admin**
  (may configure the gate). Players not on the list get the defaults set at the top of the screen. Adding yourself
  makes you an admin.
- **Gate Address Access List.** Per address (full 9 symbols): may it dial **in**, and may this gate dial **out** to it.
  Addresses not on the list get the defaults. Up to 256 entries.
- **Linked ZPMs** lists the ZPM devices linked to the gate and lets you unlink them.

On a Sponge server, `sgcraft.admin` bypasses all of these.
