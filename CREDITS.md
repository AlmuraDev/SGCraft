# Credits

## Original author

SGCraft was created by **gcewing**, who wrote the original mod for Minecraft 1.7 to 1.10: the gates, the DHD,
dialing, gate addressing and the rendering that every later version grew from. The original code is under the
MIT license, and gcewing's copyright stays in [LICENSE](LICENSE). Classes in this remake that carry over gcewing's
design say so in their header.

## Gate models

SGCraft's gates are built from measurements of these models. The data measured from each one
(the files listed under it) is under that model's license, as given on its page, not SGCraft's.
The paths are where the data lives in this remake, under `core/src/main/resources/assets/sgcraft`.

### Milky Way Stargate | Stargate SG-1

- By Anquietas System (@anquietas-sys) on Sketchfab:
  https://sketchfab.com/3d-models/milky-way-stargate-stargate-sg-1-517b56e322a548ac81dd8454dbbd95bf
- License: as on that page (Creative Commons Attribution-NonCommercial)
- Used for: the shape of the Milky Way gate, `models/gate/milkyway_gate.txt`, measured from it by the tools in
  `tools/milkyway` of the 1.12 mod

### Stargate (Pegasus Version)

- By David Gian-Cursio, from his OBJ conversion kit of the gate from "Stargate Atlantis":
  https://www.gian-cursio.net/2021/07/stargate-2020-sga-showcase
- License: CC0, public domain (as its read me gives it)
- Used for: the shape of the Pegasus gate, its glyphs and its screens' crystal,
  `models/gate/pegasus_gate.txt` and `pegasus_glyphs.txt`, and the textures in `textures/gate/pegasus`
  drawn from it, by the tools in `tools/pegasus` of the 1.12 mod

### Destiny Stargate | Stargate: Universe

- By Anquietas System (@anquietas-sys) on Sketchfab:
  https://sketchfab.com/3d-models/destiny-stargate-stargate-universe-dbeae0025fdf4642bf68fe1619ddde95
- License: as on that page (Creative Commons Attribution-NonCommercial)
- Used for: the shape of the Universe gate, `models/gate/universe_gate.txt`, and its symbols and the masks of
  its chevrons' lights and paint (`glyphs.png`, `chevron_lights.png`, `chevron_glow.png` and
  `chevron_paint.png` in `textures/gate/universe`), measured from it by the tools in `tools/universe` of the
  1.12 mod. `glyphs_cut.png` and `chevron_lights_cut.png` are made from `glyphs.png` and
  `chevron_lights.png` and share their license.

## Sounds and the event horizon

The gates' sounds (`sounds`) and the event horizon's textures (`textures/gate/event_horizon.png`, and
`event_horizon_plain.png`, the 1.12 mod's `eventhorizon.png`) come from the 1.12 mod as they were, along with the timings, the order chevrons lock in and the wave simulation of the
wormhole's surface, which gcewing designed.

### Keyboard Soundpack 1

- By unicaegames, recorded on a Cherry KC 1000:
  https://opengameart.org/content/keyboard-soundpack-1-typing-and-single-keystrokes
- License: CC0, public domain
- Used for: the dialing computer's key presses, `sounds/computer_key_1.ogg` to `computer_key_6.ogg` and
  `computer_key_big_1.ogg` and `computer_key_big_2.ogg`, cut from its single keystrokes by
  `tools/computer/import_keys.py`

## Iris, items and naquadah

The iris's textures (`textures/gate/*/iris.png`) and sounds (`sounds/iris_open.ogg`, `iris_close.ogg` and
`iris_hit.ogg`, made mono), the textures of the items and of naquadah (`textures/item`, and `naquadah_ore.png` and
`naquadah_block.png` in `textures/block`) and the recipes come from the 1.12 mod. The iris's blades are drawn
with gcewing's geometry, and its timing, the energy costs and the DHD's fuel follow the original's.

## The gate base screen

The base screen's art (`textures/gui/gate/base.png` and `address.png`) comes from the 1.12 mod's `sg_gui.png` and
`sg_gui_noinventory.png`, and the screen and its camouflage slots are laid out as gcewing's were.

## Supergates

The supergate blocks' textures (`supergateblock.png`, `supergatering.png`, `supergatechevron.png` and
`supergatebase_front.png` in `textures/block`) and recipes come from the 1.12 mod. A supergate's ring of blocks, its
sizes, the way it is drawn as a standard gate scaled up, its opening, its kawoosh and its extra energy cost follow
the original's `GateShape`.

The gate spawners' textures (`textures/block/*_spawner.png`) come from the 1.12 mod's `gatespawner_*.png`, and the
spawners work as its gate spawner blocks did.

The chevron upgrade's texture (`textures/item/chevron_upgrade.png`) and recipe come from the 1.12 mod's
`sgchevronupgrade`. It adds one chevron at a time, where the original's added two at once.

The Tollan phase shift device's texture (`textures/item/tollan_phase_shift_device.png`) comes from the 1.12 mod, and
the device takes its holder through a shut iris as the original's did, held in either hand where the original
took the off hand only.

The power unit's textures (`textures/block/power_unit_*.png`), its screen's art (`textures/gui/power_unit.png`,
widened to the width the original's screen drew it at) and its recipe come from the 1.12 mod's `sgpowerunit` and
`power_gui.png`, and it stores and gives energy as the original's did.

The Tok'ra's skin (`textures/entity/villager/profession/tokra.png`, and the same in `zombie_villager`) comes from
the 1.12 mod's `textures/skins/tokra.png`, and the Tok'ra trade the crystals the original's did, at the original's
prices.

## SGCraft 2.2.0's shields, Ori supergate and explorer dialer

The gate shield, the shield console, the city shield generator, the Ori supergate and the explorer dialer follow the
1.12 mod's version 2.2.0, as AlmuraDev added them there. From it come the shield upgrade's texture
(`textures/item/shield_upgrade.png`, the 1.12 mod's `sgShieldUpgrade.png`), the shield console's and the city shield
generator's textures (`textures/block/shield_console_*.png` and `city_shield_generator_*.png`), the explorer dialer's
texture (`textures/item/edd.png`), the Ori supergate's metal (`textures/gate/ori/ring.png`, the 1.12 mod's
`tileentity/ori/stargate.png`) and its spawner's texture (`textures/block/ori_supergate_spawner.png`), and the sounds
of the shield rising and falling (`sounds/shield_activation_atlantis.ogg` and `shield_deactivation.ogg`), of the
city shield rising and falling (`sounds/city_shield_up.ogg` and `city_shield_down.ogg`, the 1.12 mod's
`city_shield_on.ogg` and `city_shield_off.ogg`), of the Ori supergate dialing (`sounds/ori/dial.ogg`, the 1.12 mod's
`supergate.ogg`) and of a Pegasus DHD's symbol pressed (`sounds/pegasus/dhd_button.ogg`, the 1.12 mod's
`sga_dhd_button.ogg`). The recipes, the timings, the shapes and colors of
the shields and of the Ori supergate's ring, and the city shield's energy use follow the 1.12 mod's. The city shield
runs on ZPMs and power units here, where the 1.12 mod's ran on ZPMs alone.

## ZPMs

The ZPM, the ZPM console, the ZPM interface cart and the ZPM hub come from the 1.12 mod. The ZPM's texture
(`textures/item/zpm.png`) comes from it, and so do the models of the console, the cart and the hub
(`models/block/zpm_console.json`, `zpm_console_active.json`, `zpm_hub.json`, `zpm_hub_1.json`, `zpm_hub_2.json`,
`zpm_hub_3.json`, `zpm_interface_cart.json` and `zpm_interface_cart_active.json`), which Dockter made for the 1.12
mod with MrCrayfish's Model Creator. Their textures (`textures/block/zpm_side.png`, `zpm_top.png`,
`zpm_interface_cart_*.png`, `1_console.png`, `2_console.png`, `4_console.png`, `5_console.png` and
`6_console.png`) and the screens' art (`textures/gui/zpm_gui.png` and `zpm_hub_gui.png`) come from the 1.12 mod
too. The ZPM's energy units, the supergate's need for three ZPMs, the routes that need a ZPM and the ZPM chests
follow the 1.12 mod's version 2.2.0.
