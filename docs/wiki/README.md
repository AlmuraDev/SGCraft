# SGCraft wiki

The wiki lives in `docs/wiki/` as Markdown. `tools/wiki/build.py` renders it into a themed HTML site. Every page is
one `.md` file named in lower case with hyphens, such as `building-gates.md`.

The wiki has two parts:
- **Guides** at the top of `docs/wiki/`, which explain how to play, run a server or write an add-on.
- **The dictionary**, one page for each block, item and entity, in `blocks/`, `items/` and `entities/`. A page's
  file name is its registry path with hyphens: `sgcraft:stargate_base_pegasus` is `blocks/stargate-base-pegasus.md`.
  The index pages `blocks.md`, `items.md` and `entities.md` list them, and `recipes.md` shows every recipe.

## Dictionary pages
- The title is the in-game name. The summary follows it, then an infobox with `ID` and `Category` rows.
- `ID` names the registry entry (several, comma separated, for a page about a family such as the gate spawners).
  Every mention of that item elsewhere (`{{item:...}}`, recipe slots and legends) links to the page, and the
  builder adds a "Used in" section listing what the recipes make from it.
- `Category` groups the page on its index and on the recipes page. Use one of the `CATEGORIES` in `build.py`:
  Gate blocks, DHDs, Materials, Upgrades, Tools, Power, MALP, Villagers, Creative only, Technical.
- Sections, leaving out empty ones: Obtaining (Crafting and other sources), Usage, then what the thing needs
  (Behavior, Power, Rights, Settings, Commands), then See also, linking the guides.
- Draw every recipe that makes the item. The builder warns when one is missing.
- Link to guides from a dictionary page with `../`: `[the DHD guide](../dhd.md#fuel)`.

## Page shape
- The first line is the page title as `# Title`.
- The second paragraph (right after the title) is a one or two sentence summary of the page. It is shown larger.
- Use `##` and `###` headings in sentence case ("Dialing another world", not "Dialing Another World").
- Link to other pages with their file name: `[Dialing](dialing.md)`, `[the iris](iris.md#redstone)`.
- Anchors are the heading text in lower case with spaces as hyphens and other punctuation dropped.

## Supported Markdown
Paragraphs, `**bold**`, `*italic*`, `` `code` ``, links, images, bullet lists (`-`, one nested level with two
spaces), numbered lists (`1.`), tables (GitHub pipe tables with a header row), fenced code blocks (```` ```json ````),
and blockquotes. A blockquote that starts with `**Note:**`, `**Tip:**` or `**Warning:**` is drawn as a callout box.

## Wiki blocks
Infobox, floated right like a wiki's item box. First line is the title, then `key: value` lines. Use it at the top of
pages about one thing (an item, a block, a gate type):

```infobox
MALP
ID: sgcraft:malp
Category: MALP
Type: Entity and item
Stackable: No
Power: Battery, FE
```

An infobox with an `ID` row shows that item's icon; an `Icon: sgcraft:malp` row picks another.
An `Image: scenes/tokra.png` row shows a picture instead, and the page's card on its index uses it too.

Gallery, pictures side by side with captions, one per line as `path | caption`:

```gallery
scenes/milky-way-day.jpg | Idle, by day
scenes/milky-way-night.jpg | Idle, by night
scenes/milky-way-open-day.jpg | Open, by day
scenes/milky-way-open-night.jpg | Open, by night
```

Code blocks tagged `java`, `groovy` or `json` are colored.

Index of a dictionary folder, as cards grouped by category. The first line names the folder; a line starting with
`+` adds a page from another folder:

```index
items
+entities/malp
```

All recipes, grouped by category, as on `recipes.md`:

```recipes
```

Crafting recipe, drawn as a crafting table shows it, with the game's own icons and names. Give the recipe's id; the
builder reads the recipe file in `core/src/main/resources/data`, so the page always matches the game:

```recipe
sgcraft:stargate_ring
```

Item: `{{item:sgcraft:naquadah}}` or `{{item:minecraft:diamond}}` shows the item's icon and in-game name inline.

Glyphs: `{{glyphs:XOBF-R3A@}}` draws an address in the mod's Milky Way glyphs, inline.
`{{glyphs-pegasus:...}}` and `{{glyphs-universe:...}}` use the other gate types' glyphs.

## Writing style (strict)
- American English everywhere: color, gray, center, meter, behavior, neighbor, dialing, dialed, traveler, canceled.
- Plain and direct. Second person ("you"). Short sentences. Say what a thing does and how to use it.
- No filler, no marketing, no hype words (seamless, powerful, robust, simply, just, easy, unlock, leverage).
- Do not use em dashes or en dashes as punctuation. Use commas, colons, parentheses or separate sentences.
- No rhetorical questions, no "In this page you will learn". No emoji.
- Numbers: give exact defaults and units. Settings and commands in `code`.
- Facts only. Every fact must come from the facts files or the source code. When unsure, read the code in
  `core/src/main/java/com/almuradev/sgcraft/mod/` and `api/`. Do not invent features.
- Write for two readers: players (how to build and use things) and server admins (settings, commands). Mark admin
  parts with a "Server admins" subsection where it helps.

## Building the site
- `python tools/wiki/build.py` builds the site into `build/wiki`. Open `build/wiki/home.html`, or serve the folder.
- Item icons and names come from the game. `./gradlew :neoforge:runIconExport` starts a client that draws every
  item's icon into `build/icons` and closes itself. Then `python tools/wiki/build.py --refresh-icons` keeps the
  icons the wiki uses (every SGCraft item and each other item the pages or recipes name) in `docs/wiki/icons`.
- The same export then draws the entities the wiki pictures (the Tok'ra) on a see-through background, and builds
  each gate type in its flat world to photograph it by day and by night. `--refresh-icons` keeps those pictures in
  `docs/wiki/scenes`, the world ones cut to the gate and saved as JPEG.
- The sidebar order is the `NAV` list in `tools/wiki/build.py`.
- The build checks every link and anchor, warns of a dictionary page missing a recipe its item has, and of an
  SGCraft item with no dictionary page. Fix every warning before publishing.
