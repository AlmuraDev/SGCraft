# Credits

## Original author

SGCraft was created by **gcewing**, who wrote the original mod for Minecraft 1.7 to 1.10: the gates, the DHD,
dialling, gate addressing and the rendering that every later version grew from. The original code is under the
MIT licence, and gcewing's copyright stays in [LICENSE](LICENSE). Classes in this remake that carry over gcewing's
design say so in their header.

## Gate models

SGCraft's gates are built from measurements of these models. The data measured from each one
(the files listed under it) is under that model's licence, as given on its page, not SGCraft's.
The paths are where the data lives in this remake, under `core/src/main/resources/assets/sgcraft`.

### Milky Way Stargate | Stargate SG-1

- By Anquietas System (@anquietas-sys) on Sketchfab:
  https://sketchfab.com/3d-models/milky-way-stargate-stargate-sg-1-517b56e322a548ac81dd8454dbbd95bf
- Licence: as on that page (Creative Commons Attribution-NonCommercial)
- Used for: the shape of the Milky Way gate, `models/gate/milkyway_gate.txt`, measured from it by the tools in
  `tools/milkyway` of the 1.12 mod

### Stargate (Pegasus Version)

- By David Gian-Cursio, from his OBJ conversion kit of the gate from "Stargate Atlantis":
  https://www.gian-cursio.net/2021/07/stargate-2020-sga-showcase
- Licence: CC0, public domain (as its read me gives it)
- Used for: the shape of the Pegasus gate, its glyphs and its screens' crystal,
  `models/gate/pegasus_gate.txt` and `pegasus_glyphs.txt`, and the textures in `textures/gate/pegasus`
  drawn from it, by the tools in `tools/pegasus` of the 1.12 mod

### Destiny Stargate | Stargate: Universe

- By Anquietas System (@anquietas-sys) on Sketchfab:
  https://sketchfab.com/3d-models/destiny-stargate-stargate-universe-dbeae0025fdf4642bf68fe1619ddde95
- Licence: as on that page (Creative Commons Attribution-NonCommercial)
- Used for: the shape of the Universe gate, `models/gate/universe_gate.txt`, and its symbols and the masks of
  its chevrons' lights and paint (`glyphs.png`, `chevron_lights.png`, `chevron_glow.png` and
  `chevron_paint.png` in `textures/gate/universe`), measured from it by the tools in `tools/universe` of the
  1.12 mod. `glyphs_cut.png` and `chevron_lights_cut.png` are made from `glyphs.png` and
  `chevron_lights.png` and share their licence.

## Sounds and the event horizon

The gates' sounds (`sounds`) and the event horizon's textures (`textures/gate/event_horizon.png`, and
`event_horizon_plain.png`, the 1.12 mod's `eventhorizon.png`) come from the 1.12 mod as they were, along with the timings, the order chevrons lock in and the wave simulation of the
wormhole's surface, which gcewing designed.

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
