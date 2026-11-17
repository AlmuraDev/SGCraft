# Recipes

All recipes are shaped (3x3 crafting table) unless marked shapeless. A server can turn any of them off in the
`recipes` section of its config. "Alloy" means a Naquadah Alloy Ingot (or anything in the `ingotNaquadahAlloy` ore
dictionary). Chiseled and Smooth Sandstone are the decorative sandstone variants.

There are no smelting recipes. The **Stargate Core Crystal** and **Stargate Controller Crystal** come from dungeon,
stronghold library, spawn bonus and village blacksmith chests, and from Tok'ra villagers; their crafting recipes
(below) are **off by default**.

## Materials

**Naquadah Alloy Ingot** (shapeless): Iron Ingot + Naquadah

**Naquadah Alloy Block**
```
Alloy Alloy Alloy
Alloy Alloy Alloy
Alloy Alloy Alloy
```
Back to 9 ingots: one Naquadah Alloy Block, alone.

**Stargate Core Crystal** (off by default)
```
Lapis     Lapis     Redstone
Redstone  Diamond   Lapis
Lapis     Redstone  Lapis
```

**Stargate Controller Crystal** (off by default)
```
Redstone    Orange Dye  Orange Dye
Orange Dye  Diamond     Redstone
Orange Dye  Orange Dye  Redstone
```

## The gate

**Stargate Ring Block**
```
Chiseled Sandstone  Chiseled Sandstone  Chiseled Sandstone
Alloy               Alloy               Alloy
Smooth Sandstone    Smooth Sandstone    Smooth Sandstone
```

**Stargate Chevron Block**
```
Chiseled Sandstone  Glowstone Dust  Chiseled Sandstone
Alloy               Ender Pearl     Alloy
Smooth Sandstone    Redstone        Smooth Sandstone
```

**Stargate Base**
```
Chiseled Sandstone  Redstone      Chiseled Sandstone
Alloy               Eye of Ender  Alloy
Smooth Sandstone    Core Crystal  Smooth Sandstone
```

**Stargate Controller (DHD)**
```
Stone Button  Stone Button        Stone Button
Obsidian      Ender Pearl         Obsidian
Obsidian      Controller Crystal  Obsidian
```

## Supergate

**Supergate Ring Block** (makes 4)
```
Ring Block  Alloy        Ring Block
Alloy       Alloy Block  Alloy
Ring Block  Alloy        Ring Block
```
(Ring Block = Stargate Ring Block)

**Supergate Chevron Block**: a Supergate Ring Block above a Stargate Chevron Block.

**Supergate Base**
```
Supergate Ring  Nether Star    Supergate Ring
Alloy           Stargate Base  Alloy
Supergate Ring  Alloy Block    Supergate Ring
```

## Upgrades

**Stargate Chevron Upgrade**
```
Glowstone Dust  .            Glowstone Dust
Ender Pearl     Alloy        Ender Pearl
Redstone        .            Redstone
```

**Stargate Iris Blade**
```
.           Iron Ingot  Iron Ingot
Iron Ingot  Charcoal    .
Iron Ingot  .           .
```

**Stargate Iris Upgrade**: 8 Iris Blades round a Redstone.

**Stargate Shield Upgrade**
```
Alloy     Lapis         Alloy
Redstone  Core Crystal  Redstone
Alloy     Lapis         Alloy
```

**Pegasus Type Upgrade Crystal**: either of
```
Glowstone Dust  Lapis Block   Glowstone Dust        Glowstone Dust  Redstone      Glowstone Dust
Redstone        Core Crystal  Redstone              Lapis Block     Core Crystal  Lapis Block
Glowstone Dust  Lapis Block   Glowstone Dust        Glowstone Dust  Redstone      Glowstone Dust
```
The same recipes with a Pegasus Upgrade Crystal in the middle instead of the Core Crystal make 2. (A server can
switch the Core Crystal for a Naquadah Alloy Block; there's then no copying recipe.)

**Universe Type Upgrade Crystal**
```
Ender Pearl      Diamond       Ender Pearl
Chevron Upgrade  Core Crystal  Chevron Upgrade
Ender Pearl      Diamond       Ender Pearl
```

The Invulnerability Upgrade has no recipe (creative only).

## Power

**SG Power Unit**
```
Emerald      Glass Pane     Emerald
Core Crystal Iron Block     Core Crystal
Iron Block   Emerald Block  Iron Block
```

**ZPM Console**
```
Emerald             Glass Pane  Emerald
Controller Crystal  Iron Block  Core Crystal
Iron Block          Gold Block  Iron Block
```

**ZPM Interface Cart** (without IC2)
```
Controller Crystal  Iron Block     Redstone
Redstone            SG Power Unit  Redstone
Redstone            Alloy Block    Controller Crystal
```

**ZPM Hub** (without IC2)
```
Controller Crystal  Iron Block   Redstone
Redstone            ZPM Console  Redstone
ZPM Console         Alloy Block  ZPM Console
```

With IC2 installed, the Cart and Hub use IC2 parts instead: Electronic Circuits for the crystals, an Advanced
Machine Casing for the Iron Block, Copper Cable for Redstone, a Machine Block for the Alloy Block, and (Cart only) an
IC2 Stargate Power Unit in the middle.

**Naquadah Generator Mark I**
```
Iron Ingot  Alloy         Iron Ingot
Alloy       Core Crystal  Alloy
Iron Ingot  Alloy         Iron Ingot
```

**Naquadah Generator Mark II**
```
Alloy  Diamond      Alloy
Alloy  Mark I       Alloy
Alloy  Alloy Block  Alloy
```

The Zero Point Module itself has no recipe; it's found in chests at generated gates.

## Dialers and controls (need MalisisCore)

**Personal Dialer Device**
```
Redstone            Compass         Redstone
Naquadah            Clock           Naquadah
Controller Crystal  Book and Quill  Core Crystal
```

**Explorer Dialer Device**: as the PDD, with an Empty Map instead of the Clock.

**Handheld GDO**
```
Redstone            Clock           Redstone
Controller Crystal  Lever           Core Crystal
Naquadah            Book and Quill  Naquadah
```

**Shield Console**
```
Iron Ingot  Stone Button        Iron Ingot
Redstone    Controller Crystal  Redstone
Iron Ingot  Iron Ingot          Iron Ingot
```

**City Shield Generator**
```
Alloy Block   Shield Upgrade  Alloy Block
Core Crystal  Nether Star     Core Crystal
Alloy Block   Shield Upgrade  Alloy Block
```

The Configurator has no recipe.

## Transport

**Transport Ring Platform (Floor)**
```
Iron Block  Alloy        Iron Block
Alloy       Ender Pearl  Alloy
Iron Block  Alloy        Iron Block
```
A Floor platform alone turns into a Ceiling platform, and back.

**Ring Control Panel**
```
Gold Ingot  Redstone  Gold Ingot
Gold Ingot  Alloy     Gold Ingot
```

**Ring Remote**
```
.           Redstone  .
Gold Ingot  Alloy     Gold Ingot
.           Gold Ingot .
```

**Asgard Transport Obelisk**
```
.             Chiseled Sandstone  .
Stone Bricks  Ender Pearl         Stone Bricks
Stone Bricks  Alloy               Stone Bricks
```

**Asgard Knotwork Obelisk**: the same with Smooth Sandstone instead of Stone Bricks.

## Weapons

**Zat**
```
.            Alloy     Core Crystal
Alloy        Redstone Block  Alloy
Purple Dye   Alloy     .
```

## Computers

**ComputerCraft Stargate Interface** (ComputerCraft)
```
Stone  Alloy     Stone
Stone  Redstone  Stone
Stone  Stone     Stone
```

**OpenComputers Stargate Interface** (OpenComputers)
```
Iron Ingot  Alloy                Iron Ingot
Cable       Microchip (Tier 1)   Cable
Iron Ingot  Printed Circuit Board Iron Ingot
```

## IC2

**Ridiculously Large Capacitor**: 3 Copper Plates, 3 Rubber, 3 Copper Plates (rows).

**IC2 Stargate Power Unit**
```
Capacitor       Copper Cable   Capacitor
Copper Cable    Machine Block  Copper Cable
Capacitor       Circuit        Capacitor
```
