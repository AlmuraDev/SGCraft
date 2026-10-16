# FAQ

Short answers to the questions players and server admins ask most. Each answer links to the page with the details.

## Installing

### Which loaders does SGCraft run on

NeoForge and Fabric, on Minecraft 26.3. Each loader has its own jar. Fabric also needs Fabric API. See [Installing SGCraft](install.md).

### Does it have to be on the server and the client

Yes. Install it on the server and on every client that joins, with the same SGCraft version. See [Installing SGCraft](install.md#client-and-server).

### Can I keep my 1.12 world

No. Worlds from the SGCraft mod for Minecraft 1.12 and earlier don't carry over. There is no converter. See [Upgrading from older SGCraft](install.md#upgrading-from-older-sgcraft).

### Where are the config files

In the `config` folder of the game or server directory: `sgcraft-server.json`, `sgcraft-client.json` and `sgcraft-default-addresses.json`. See [Settings](settings.md).

## Gates and dialing

### Why can't my gate dial another world

A gate forms with 7 chevrons, and a 7 symbol address reaches only gates in the same world. An address to another world has 8 symbols, so the dialing gate needs 8 chevrons. Fit a [Stargate Chevron Upgrade](building-gates.md#chevrons-and-upgrades) to add one. A gate holds up to 9.

The gate you dial doesn't need extra chevrons. Only the dialing gate does. See [Addresses](addresses.md#why-a-gate-cant-dial-a-longer-address).

### Where do I find my gate's address

Use the gate's base or any ring block to open the **Stargate Address** screen. You can also run `/sgcraft address` while you look at the gate, or point a PDD at it. If the owner turned off the gate's `address_visible` property, the PDD shows "address hidden" in place of it. See [Addresses](addresses.md).

### My gate has no address

A gate gets an address only inside the address grid, about 373,248 blocks out from the world's center each way with the default settings. Only one gate in each chunk gets an address. See [Cells and the grid](addresses.md#cells-and-the-grid).

### Why does the Universe gate have no DHD

Universe gates have no DHD in SGCraft. Dial one with a [PDD or a gate control tablet](tools.md), or with `/sgcraft dial <address>`. See [The DHD](dhd.md#kinds).

### Why did my dial fail

The gate tells you why. The common reasons:

- "This gate doesn't have enough chevrons for that address": add chevron upgrades.
- "There is no gate at that address": nothing is built there, or the address is mistyped.
- "This gate doesn't have the energy": fuel the DHD or connect a power unit.
- "You aren't allowed to dial that": the gate's rights or address rules stop you.
- "The gate at that address is busy": the other gate already has a wormhole or a dial running.

See [What can stop a dial](dialing.md#what-can-stop-a-dial) for the full list.

### How do I close the wormhole

Press the dome in the middle of the DHD, use the disconnect button of a PDD, tablet or GDO, or run `/sgcraft disconnect`. By default either end can close it. A wormhole also closes by itself after 6000 ticks (5 minutes), the `open_ticks` property, or when the dialing gate runs out of energy. See [Closing the wormhole](dialing.md#closing-the-wormhole).

### My dial went to the wrong gate

When it rains at the dialing gate, a dial can be struck by lightning (6% chance) and land at a random generated gate instead. The `lightning_malfunction` property turns this off. See [Lightning](dialing.md#lightning).

### What can go through a gate

Players, mobs, dropped items, minecarts and anything riding them. Players need the use right at both gates. See [Traveling through the wormhole](dialing.md#traveling-through-the-wormhole).

## Dying in the gate

### Why did I die going through

Three things kill a traveler:

- The kawoosh: when a wormhole opens, it bursts out of the front of both gates for 20 ticks (1 second), and anything alive in front of the opening takes 5000 damage a tick. Stand to the side until the burst is over. See [The kawoosh](dialing.md#the-kawoosh).
- A shut iris at the far end: arriving at a gate whose iris is shut kills you, and you lose what you carry unless the gate's `preserve_inventory` property or the `keepInventory` game rule keeps it. See [The iris](iris.md#what-a-shut-iris-does).
- Going the wrong way: a wormhole runs one way, from the dialing gate to the dialed gate. Stepping into the dialed end does nothing by default, but if the dialing gate's `reverse_travel_kills` property is on, it kills you. See [One way travel](dialing.md#one-way-travel).

### Can I get through a shut iris

Yes, with a Tollan Phase Shift Device in your hand, while the gate's `phase_shift_passes_iris` property is on (the default). The device has no recipe. See [The Tollan phase shift device](iris.md#the-tollan-phase-shift-device).

## Power

### How do I power a gate

Use one of these, or both:

- Naquadah in the DHD. Sneak and use the DHD to open its fuel slots. One naquadah gives 96,000 gate energy.
- A Stargate Power Unit within 4 blocks of the gate's base, charged by cables.

A standard dial in one world costs 100 energy per chevron, 4000 to open and 1 a tick while open, more over long distances and to other worlds. See [Power](power.md).

### Can I turn power off

Yes. Set the gate's `requires_power` property to `false` with the configurator or `/sgcraft property`, or set it for every new gate in `gate_defaults`. See [Free dialing](power.md#free-dialing).

### Does it work with other mods' power

Yes, through the power unit. On NeoForge it takes FE (NeoForge energy) from other mods' cables. On Fabric it takes energy from cables that use Team Reborn Energy, which SGCraft bundles. 80 FE makes one gate energy by default. Batteries for the MALP are FE items on both loaders, so other mods' chargers can fill them. See [FE and gate energy](power.md#fe-and-gate-energy).

## Breaking and building

### Can I break generated gates

Not by default. Gates the world built have no owner, and only operators can break them while the server setting `generated_gates_breakable` is `false`. Explosions and commands still break them. See [Breaking a gate](building-gates.md#breaking-a-gate).

### Can I break my own gate

Yes, while the server setting `owners_break_gates` is `true` (the default). Breaking a gate with an open wormhole sets off a breach explosion at its center. See [Breach explosion](building-gates.md#breach-explosion).

### Where do I find naquadah

Under lava below sea level in the Overworld, in small nodes deep underground, in some chest loot, and in the DHD of every generated gate. See [World generation](worldgen.md#naquadah-ore).

### Where are gates already built

At desert pyramids, jungle temples, ocean monuments, Nether fortresses, swamp huts and igloos, each with a 25% chance, and on plazas in some villages. See [Structure gates](worldgen.md#structure-gates) and [Village gates](worldgen.md#village-gates).

## Multiplayer and permissions

### Who can use my gate

You own the gates you build. The owner and operators can do everything. Other players get the gate's default rights: use (dial, close, travel) and iris. Configure is owner only unless you give it. Set rights per player in the configurator's **Access** page or with `/sgcraft rights`. See [Access](access.md#who-has-which-rights).

### Can I stop other gates dialing mine

Yes. Turn off the gate's `allow_incoming` property, or add address rules that allow or block single addresses. See [Address rules](access.md#address-rules).

### Can admins run gates without a screen

Yes. The `/sgcraft` commands cover dialing, the iris, energy, properties, rights and server settings. See [Commands](commands.md).

### Do gates keep chunks loaded

A gate keeps the chunks around it loaded while it is dialing or connected, so the far end works with no player near it. The server setting `chunk_loading_range` (1 by default) sets how far. See [Chunk loading](dialing.md#chunk-loading).

## Add-ons

### Can I write an add-on

Yes. Build against the `sgcraft-api` library. See [API](api.md) and [Building from source](building-from-source.md).
