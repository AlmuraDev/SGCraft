# Credits

## Original author

SGCraft was created by **gcewing**, who wrote the original mod for Minecraft 1.7 to 1.10: the gates, the DHD,
dialling, gate addressing and the rendering that every later version grew from. The original code is under the
MIT licence, and gcewing's copyright stays in [LICENSE](LICENSE). Classes in this remake that carry over gcewing's
design say so in their header.

## Gate models

SGCraft's gates are built from measurements of these models. The data measured from each one
(the files listed under it) is under that model's licence, as given on its page, not SGCraft's.
The paths are where the data lives in this remake, under `common/src/main/resources/assets/sgcraft`.

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

The gates' sounds (`sounds`) and the event horizon's texture (`textures/gate/event_horizon.png`) come from the
1.12 mod as they were, along with the timings, the order chevrons lock in and the wave simulation of the
wormhole's surface, which gcewing designed.
