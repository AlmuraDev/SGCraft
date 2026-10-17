"""Builds the SGCraft wiki: the Markdown pages in docs/wiki become a themed HTML site in build/wiki.

Pages are plain Markdown with a few wiki blocks (infoboxes, crafting recipes, items and addresses drawn in glyphs,
indexes of the dictionary's pages); see docs/wiki/README.md. The glyphs are cut from the mod's own glyph fonts,
recipes are read from the mod's own recipe files, and item icons and names are the game's own, saved by the icon
export, so the wiki shows what the game shows.

The guides sit at the top of docs/wiki. The dictionary has one page for each block, item and entity, in the blocks,
items and entities folders; each folder's index page (blocks.md, items.md, entities.md) lists its pages.

Run from the repository root: python tools/wiki/build.py
After running the icon export (./gradlew :neoforge:runIconExport), refresh the icons the wiki keeps with:
python tools/wiki/build.py --refresh-icons
With --strict, a build with warnings fails, as the GitHub Pages workflow runs it.
Needs Pillow.
"""

import html
import json
import re
import shutil
import sys
from pathlib import Path

from PIL import Image

SOURCE = Path('docs/wiki')
OUT = Path('build/wiki')
FONTS = Path('core/src/main/resources/assets/sgcraft/textures/font')
RECIPES = Path('core/src/main/resources/data')
# The icons and names the wiki keeps, and where the icon export saves every item's
ICONS = SOURCE / 'icons'
EXPORTED = Path('build/icons')
# The pictures the scene export takes in the game, and the share of each picture the wiki keeps, from its middle
SCENES = SOURCE / 'scenes'
SCENE_CROP = {'supergate': 0.9}
STANDARD_CROP = 0.62
SCENE_WIDTH = 960
ITEM_ID = re.compile(r'^[a-z0-9_.-]+:[a-z0-9_./-]+$')
CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@'
CELL = 64
COLUMNS = 10
# Each glyph style, and the color its glyphs are drawn in
GLYPH_STYLES = {'milkyway': 'cfe3ff', 'pegasus': '9fe6ff', 'universe': 'e6edf5'}
LOGO_COLOR = 'e8892b'

# The sidebar, in order: each section's title and its pages. Pages not listed still build, under "More"
NAV = [
    ('Start here', ['home', 'install', 'getting-started', 'faq', 'changes']),
    ('Playing', ['building-gates', 'dialing', 'addresses', 'dhd', 'iris', 'power', 'access', 'tools', 'malp',
                 'worldgen']),
    ('Dictionary', ['blocks', 'items', 'entities', 'recipes']),
    ('Server admins', ['settings', 'commands']),
    ('Add-on developers', ['api', 'building-from-source']),
    ('Stargate', ['stargate-history', 'community']),
]
# The dictionary's folders; each has an index page of the same name at the top of the wiki
DICTIONARY = ['blocks', 'items', 'entities']
# The order the dictionary's indexes and the recipes page group their pages in, by each page's Category
CATEGORIES = ['Gate blocks', 'DHDs', 'Materials', 'Upgrades', 'Tools', 'Power', 'MALP', 'Villagers',
              'Creative only', 'Technical']

# Set while a page renders: the page's name, and the path back to the top of the site from its folder
CURRENT = ''
BASE = ''
# What the first pass learns of each page: its title, summary, category, the items it is about and its icon
META = {}
# The page about each item
PAGE_OF = {}


def asset(path):
    return BASE + path


def cut_glyphs():
    """Writes each glyph style's glyphs as one tinted image per character, and the logo's glyphs in chevron orange."""
    for style, color in GLYPH_STYLES.items():
        font = Image.open(FONTS / f'glyphs_{style}.png').convert('RGBA')
        for tint, folder in ((color, OUT / 'glyphs' / style), (LOGO_COLOR, OUT / 'glyphs' / f'{style}-logo')):
            folder.mkdir(parents=True, exist_ok=True)
            rgb = tuple(int(tint[i:i + 2], 16) for i in (0, 2, 4))
            for i, char in enumerate(CHARS):
                x, y = (i % COLUMNS) * CELL, (i // COLUMNS) * CELL
                cell = font.crop((x, y, x + CELL, y + CELL))
                glyph = Image.new('RGBA', cell.size, rgb + (0,))
                glyph.putalpha(cell.getchannel('A'))
                glyph.save(folder / f'{glyph_name(char)}.png', optimize=True)


def load_names():
    path = ICONS / 'names.json'
    return json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}


NAMES = load_names()


def item_name(item_id):
    return NAMES.get(item_id, item_id.split(':', 1)[-1].replace('_', ' ').capitalize())


def icon(item_id, css='icon'):
    """An item's icon as the game draws it, or nothing if the wiki doesn't keep one."""
    namespace, _, path = item_id.partition(':')
    if not (ICONS / namespace / f'{path}.png').exists():
        return ''
    return (f'<img class="{css}" src="{asset(f"icons/{namespace}/{path}.png")}" '
            f'alt="{html.escape(item_name(item_id))}">')


def page_link(item_id):
    """The link to an item's dictionary page, or nothing if it has none or it is the page being built."""
    page = PAGE_OF.get(item_id)
    return asset(f'{page}.html') if page and page != CURRENT else ''


def item(item_id):
    content = f'{icon(item_id)}{html.escape(item_name(item_id))}'
    target = page_link(item_id)
    if target:
        return f'<a class="item" href="{target}">{content}</a>'
    return f'<span class="item">{content}</span>'


def glyph_name(char):
    return 'origin' if char == '@' else char


def glyphs(address, style):
    symbols = [c for c in address.upper() if c in CHARS]
    images = ''.join(f'<img class="glyph" src="{asset(f"glyphs/{style}/{glyph_name(c)}.png")}" alt="{c}">'
                     for c in symbols)
    return f'<span class="glyphs" title="{html.escape(address)}">{images}</span>'


def slug(text):
    text = re.sub(r'<[^>]+>', '', text).lower()
    text = re.sub(r'[^a-z0-9 -]', '', text)
    return re.sub(r'\s+', '-', text.strip())


def inline(text):
    """Renders the inline Markdown of one run of text."""
    stash = []

    def keep(fragment):
        stash.append(fragment)
        return f'\x00{len(stash) - 1}\x00'

    text = re.sub(r'`([^`]+)`', lambda m: keep(f'<code>{html.escape(m.group(1))}</code>'), text)
    text = re.sub(r'\{\{glyphs(?:-(pegasus|universe))?:([^}]+)\}\}',
                  lambda m: keep(glyphs(m.group(2), m.group(1) or 'milkyway')), text)
    text = re.sub(r'\{\{item:([a-z0-9_.-]+:[a-z0-9_./-]+)\}\}', lambda m: keep(item(m.group(1))), text)
    text = html.escape(text, quote=False)
    text = re.sub(r'!\[([^\]]*)\]\(([^)\s]+)\)', lambda m: keep(
        f'<img src="{link(m.group(2))}" alt="{html.escape(m.group(1))}">'), text)
    text = re.sub(r'\[([^\]]+)\]\(([^)\s]+)\)', lambda m: keep(
        f'<a href="{link(m.group(2))}">{m.group(1)}</a>'), text)
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    text = re.sub(r'(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])', r'<em>\1</em>', text)
    return re.sub(r'\x00(\d+)\x00', lambda m: stash[int(m.group(1))], text)


def link(target):
    if re.match(r'^[a-z][a-z0-9+.-]*:', target):
        return target
    return re.sub(r'\.md(?=$|#)', '.html', target)


def infobox_rows(lines):
    """An infobox's title and its key and value lines, keys in lower case."""
    rows = []
    for line in lines[1:]:
        key, _, value = line.partition(':')
        rows.append((key.strip(), value.strip()))
    return lines[0], rows


def infobox(lines):
    title, rows = infobox_rows(lines)
    shown = []
    picture = ''
    image = ''
    ids = []
    for key, value in rows:
        if key.lower() == 'icon':
            picture = value
            continue
        if key.lower() == 'image':
            image = f'<div class="infobox-image"><img src="{asset(value)}" alt="{html.escape(title)}"></div>'
            continue
        if key.lower() == 'id':
            ids = [i.strip() for i in value.split(',')]
            value = ', '.join(f'`{i}`' for i in ids)
        shown.append(f'<tr><th>{inline(key)}</th><td>{inline(value)}</td></tr>')
    picture = picture or (ids[0] if ids else '')
    picture = image or (f'<div class="infobox-icon">{icon(picture, "icon-large")}</div>' if picture else '')
    return (f'<aside class="infobox"><div class="infobox-title">{inline(title)}</div>{picture}'
            f'<table>{"".join(shown)}</table></aside>')


def ingredient(value):
    """The item an ingredient shows: its first item, or a tag's name."""
    if isinstance(value, list):
        value = value[0]
    if isinstance(value, dict):
        value = value.get('item') or value.get('id') or ('#' + value.get('tag', ''))
    return value


def slot(value):
    if not value:
        return '<div class="slot empty"></div>'
    if value.startswith('#'):
        return f'<div class="slot" title="Any {html.escape(value[1:])}"><span>#</span></div>'
    content = icon(value) or '<span>?</span>'
    target = page_link(value)
    if target:
        content = f'<a href="{target}">{content}</a>'
    return f'<div class="slot" title="{html.escape(item_name(value))}">{content}</div>'


def legend_entry(value):
    if value.startswith('#'):
        return f'<li>Any {html.escape(value[1:])}</li>'
    name = html.escape(item_name(value))
    target = page_link(value)
    if target:
        name = f'<a href="{target}">{name}</a>'
    return f'<li>{icon(value, "icon-small")}{name}</li>'


def load_recipe(recipe_id):
    """A recipe file as the wiki draws it: its kind, its grid of nine ingredients (None for an empty slot), the
    ingredients in the order they first appear, and its result's item and count; None for a kind it can't draw."""
    namespace, _, path = recipe_id.partition(':')
    data = json.loads((RECIPES / namespace / 'recipe' / f'{path}.json').read_text(encoding='utf-8'))
    kind = data['type']
    if kind == 'minecraft:crafting_shaped':
        cells = []
        for row in (data['pattern'] + ['', '', ''])[:3]:
            for letter in (row + '   ')[:3]:
                cells.append(ingredient(data['key'][letter]) if letter.strip() else None)
        result = data['result']
    elif kind == 'minecraft:crafting_shapeless':
        cells = ([ingredient(v) for v in data['ingredients']] + [None] * 9)[:9]
        result = data['result']
    elif kind == 'sgcraft:battery_recharge':
        cells = ['sgcraft:battery', 'sgcraft:naquadah', 'sgcraft:naquadah'] + [None] * 6
        result = {'id': 'sgcraft:battery'}
    else:
        return None
    keys = list(dict.fromkeys(c for c in cells if c))
    return {'kind': kind, 'cells': cells, 'keys': keys, 'result': result['id'], 'count': result.get('count', 1)}


def all_recipes():
    """Every SGCraft recipe's id, in name order."""
    return [f'{path.parent.parent.name}:{path.stem}' for path in sorted(RECIPES.glob('sgcraft/recipe/*.json'))]


def recipe_file(recipe_id):
    """Draws a recipe from the mod's own recipe file, as a crafting table shows it."""
    recipe = load_recipe(recipe_id)
    if recipe is None:
        return f'<p><code>{html.escape(recipe_id)}</code></p>'
    note = ''
    if recipe['kind'] == 'minecraft:crafting_shapeless':
        note = '<div class="recipe-kind">Shapeless: any arrangement</div>'
    elif recipe['kind'] == 'sgcraft:battery_recharge':
        note = ('<div class="recipe-kind">Shapeless: one battery and any number of naquadah, each adding '
                'its charge</div>')
    result = recipe['result']
    amount = f'<span class="count">{recipe["count"]}</span>' if recipe['count'] > 1 else ''
    name = html.escape(item_name(result))
    target = page_link(result)
    if target:
        name = f'<a href="{target}">{name}</a>'
    legend = ''.join(legend_entry(k) for k in recipe['keys'])
    anchor = 'recipe-' + recipe_id.partition(':')[2].replace('_', '-')
    return (f'<figure class="recipe" id="{anchor}"><div class="recipe-table"><div class="grid">'
            f'{"".join(slot(c) for c in recipe["cells"])}</div>'
            f'<div class="arrow">&#10148;</div><div class="result"><div class="result-slot">'
            f'{icon(result, "icon-large")}{amount}</div><div class="result-name">{name}</div></div></div>{note}'
            f'<ul class="legend">{legend}</ul></figure>')


def recipe(lines):
    if len(lines) == 1 and ITEM_ID.match(lines[0].strip()):
        return recipe_file(lines[0].strip())
    head = lines[0]
    shapeless = head.lower().startswith('shapeless:')
    if shapeless:
        head = head[len('shapeless:'):].strip()
    grid = []
    key = {}
    for line in lines[1:]:
        match = re.match(r'^\s*(\S)\s*=\s*(.+)$', line)
        if match:
            key[match.group(1)] = match.group(2).strip()
        else:
            grid.append(line)
    cells = []
    if shapeless:
        items = [k for k in key]
        letters = (items + [' '] * 9)[:9]
    else:
        rows = (grid + ['', '', ''])[:3]
        letters = [(row + '   ')[i] for row in rows for i in range(3)]
    for letter in letters:
        if letter.strip() and letter in key:
            cells.append(f'<div class="slot" title="{html.escape(key[letter])}"><span>{html.escape(letter)}</span></div>')
        else:
            cells.append('<div class="slot empty"></div>')
    legend = ''.join(f'<li><span class="slot-key">{html.escape(k)}</span>{inline(v)}</li>' for k, v in key.items())
    kind = '<div class="recipe-kind">Shapeless</div>' if shapeless else ''
    return (f'<figure class="recipe"><div class="recipe-table"><div class="grid">{"".join(cells)}</div>'
            f'<div class="arrow">&#10148;</div><div class="result">{inline(head)}</div></div>{kind}'
            f'<ul class="legend">{legend}</ul></figure>')


def category_order(category):
    return CATEGORIES.index(category) if category in CATEGORIES else len(CATEGORIES)


def grouped(pages):
    """Pages grouped by their category, in the wiki's category order, each group's pages by title."""
    groups = {}
    for name in pages:
        groups.setdefault(META[name]['category'] or 'Other', []).append(name)
    return [(category, sorted(names, key=lambda n: META[n]['title'].lower()))
            for category, names in sorted(groups.items(), key=lambda g: category_order(g[0]))]


def index(lines, headings):
    """A dictionary folder's pages as cards, grouped by category. The first line names the folder; each further line
    starting with + names another page to list with them."""
    folder = lines[0].strip()
    pages = [n for n in META if n.startswith(folder + '/')]
    pages += [line.strip()[1:].strip() for line in lines[1:] if line.strip().startswith('+')]
    pages = [n for n in pages if n in META]
    out = []
    for category, names in grouped(pages):
        anchor = slug(category)
        headings.append((anchor, html.escape(category)))
        out.append(f'<h2 id="{anchor}"><a class="anchor" href="#{anchor}">{html.escape(category)}</a></h2>')
        cards = []
        for name in names:
            meta = META[name]
            picture = icon(meta['icon'], 'icon-large') if meta['icon'] else ''
            if meta['image']:
                picture = f'<img class="card-image" src="{asset(meta["image"])}" alt="">'
            cards.append(f'<div class="card"><a class="card-icon" href="{asset(name + ".html")}">{picture}</a>'
                         f'<div class="card-text"><a class="card-title" href="{asset(name + ".html")}">'
                         f'{html.escape(meta["title"])}</a><div class="card-summary">'
                         f'{inline(plain(meta["summary"]))}</div></div></div>')
        out.append(f'<div class="cards">{"".join(cards)}</div>')
    return '\n'.join(out)


def plain(text):
    """Markdown text with its links reduced to their words, for text that sits inside a link of its own."""
    return re.sub(r'(?<!!)\[([^\]]+)\]\([^)\s]+\)', r'\1', text)


def recipes(headings):
    """Every SGCraft recipe, grouped by the category of the page about what it makes."""
    groups = {}
    for recipe_id in all_recipes():
        recipe = load_recipe(recipe_id)
        page = PAGE_OF.get(recipe['result']) if recipe else None
        groups.setdefault(META[page]['category'] if page else 'Other', []).append(recipe_id)
    out = []
    for category, ids in sorted(groups.items(), key=lambda g: category_order(g[0])):
        anchor = slug(category)
        headings.append((anchor, html.escape(category)))
        out.append(f'<h2 id="{anchor}"><a class="anchor" href="#{anchor}">{html.escape(category)}</a></h2>')
        out.append(f'<div class="recipes">{"".join(recipe_file(i) for i in ids)}</div>')
    return '\n'.join(out)


def uses():
    """For each item, the items whose recipes take it."""
    found = {}
    for recipe_id in all_recipes():
        recipe = load_recipe(recipe_id)
        if recipe:
            for key in recipe['keys']:
                if key != recipe['result']:
                    found.setdefault(key, []).append(recipe['result'])
    return {key: list(dict.fromkeys(results)) for key, results in found.items()}


JAVA_KEYWORDS = {
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'continue', 'default', 'do',
    'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'if', 'implements', 'import',
    'instanceof', 'int', 'interface', 'long', 'new', 'package', 'private', 'protected', 'public', 'record', 'return',
    'sealed', 'short', 'static', 'super', 'switch', 'this', 'throw', 'throws', 'try', 'var', 'void', 'volatile',
    'while', 'yield', 'true', 'false', 'null',
}
# Groovy's own words, as a Gradle build file uses them
GROOVY_KEYWORDS = JAVA_KEYWORDS | {'def', 'apply', 'plugins', 'dependencies', 'repositories'}
# One token of Java or Groovy: a comment, a string, an annotation, a number or a word
JAVA_TOKEN = re.compile(r'(?P<comment>//[^\n]*|/\*.*?\*/)|(?P<string>"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\')'
                        r'|(?P<annotation>@[A-Za-z_]\w*)|(?P<number>\b\d[\d_]*(?:\.\d+)?[LlFfDd]?\b)'
                        r'|(?P<word>\b[A-Za-z_]\w*\b)', re.S)
JSON_TOKEN = re.compile(r'(?P<key>"(?:\\.|[^"\\])*"(?=\s*:))|(?P<string>"(?:\\.|[^"\\])*")'
                        r'|(?P<number>-?\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b)|(?P<keyword>\b(?:true|false|null)\b)')


def highlight(code, lang):
    """A code block's text as HTML, its tokens colored for Java, Groovy or JSON; other text as it is."""
    pattern = JAVA_TOKEN if lang in ('java', 'groovy') else JSON_TOKEN if lang == 'json' else None
    if pattern is None:
        return html.escape(code)
    keywords = GROOVY_KEYWORDS if lang == 'groovy' else JAVA_KEYWORDS
    out = []
    last = 0
    for match in pattern.finditer(code):
        out.append(html.escape(code[last:match.start()]))
        kind, text = match.lastgroup, match.group()
        if kind == 'word':
            kind = 'keyword' if text in keywords else 'type' if text[0].isupper() else None
        out.append(f'<span class="tok-{kind}">{html.escape(text)}</span>' if kind else html.escape(text))
        last = match.end()
    out.append(html.escape(code[last:]))
    return ''.join(out)


def gallery(lines):
    """Pictures side by side, each line a picture's path and, after a |, its caption."""
    figures = []
    for line in lines:
        path, _, caption = (part.strip() for part in line.partition('|'))
        figures.append(f'<figure><a href="{asset(path)}"><img src="{asset(path)}" alt="{html.escape(caption)}" '
                       f'loading="lazy"></a><figcaption>{inline(caption)}</figcaption></figure>')
    return f'<div class="gallery">{"".join(figures)}</div>'


def table(rows):
    def cells(row):
        return [c.strip() for c in row.strip().strip('|').split('|')]

    head = ''.join(f'<th>{inline(c)}</th>' for c in cells(rows[0]))
    body = ''.join('<tr>' + ''.join(f'<td>{inline(c)}</td>' for c in cells(row)) + '</tr>' for row in rows[2:])
    return f'<div class="table-wrap"><table><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table></div>'


def listing(lines):
    """Renders a list, with one nested level for lines indented by two or more spaces."""
    ordered = bool(re.match(r'^\s*\d+\.', lines[0]))
    tag = 'ol' if ordered else 'ul'
    out = [f'<{tag}>']
    nested = None
    for line in lines:
        indent = len(line) - len(line.lstrip())
        item = re.sub(r'^\s*(?:[-*]|\d+\.)\s+', '', line)
        if indent >= 2:
            if nested is None:
                nested_tag = 'ol' if re.match(r'^\s*\d+\.', line) else 'ul'
                out[-1] = out[-1][:-len('</li>')]
                out.append(f'<{nested_tag}>')
                nested = nested_tag
            out.append(f'<li>{inline(item)}</li>')
        else:
            if nested:
                out.append(f'</{nested}></li>')
                nested = None
            out.append(f'<li>{inline(item)}</li>')
    if nested:
        out.append(f'</{nested}></li>')
    out.append(f'</{tag}>')
    return ''.join(out)


def render(markdown):
    """Renders a page; returns its title, summary, body and headings."""
    lines = markdown.replace('\r\n', '\n').split('\n')
    out = []
    headings = []
    title = None
    summary = None
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith('```'):
            lang = line[3:].strip()
            block = []
            i += 1
            while i < len(lines) and not lines[i].startswith('```'):
                block.append(lines[i])
                i += 1
            i += 1
            if lang == 'infobox':
                out.append(infobox([b for b in block if b.strip()]))
            elif lang == 'recipe':
                out.append(recipe([b for b in block if b.strip()]))
            elif lang == 'index':
                out.append(index([b for b in block if b.strip()], headings))
            elif lang == 'recipes':
                out.append(recipes(headings))
            elif lang == 'gallery':
                out.append(gallery([b for b in block if b.strip()]))
            elif lang == 'layout':
                out.append(f'<pre class="code layout"><code>{html.escape(chr(10).join(block))}</code></pre>')
            else:
                css = f' class="language-{lang}"' if lang else ''
                out.append(f'<pre class="code"><code{css}>{highlight(chr(10).join(block), lang)}</code></pre>')
            continue
        heading = re.match(r'^(#{1,4})\s+(.*)$', line)
        if heading:
            level = len(heading.group(1))
            text = inline(heading.group(2).strip())
            if level == 1 and title is None:
                title = heading.group(2).strip()
            else:
                anchor = slug(heading.group(2))
                if level == 2:
                    headings.append((anchor, text))
                out.append(f'<h{level} id="{anchor}"><a class="anchor" href="#{anchor}">{text}</a></h{level}>')
            i += 1
            continue
        if line.startswith('|') and i + 1 < len(lines) and re.match(r'^\|?\s*:?-{2,}', lines[i + 1]):
            rows = []
            while i < len(lines) and lines[i].startswith('|'):
                rows.append(lines[i])
                i += 1
            out.append(table(rows))
            continue
        if re.match(r'^\s*(?:[-*]|\d+\.)\s+', line):
            items = []
            while i < len(lines) and lines[i].strip() and (re.match(r'^\s*(?:[-*]|\d+\.)\s+', lines[i])
                                                            or lines[i].startswith('  ')):
                if re.match(r'^\s*(?:[-*]|\d+\.)\s+', lines[i]):
                    items.append(lines[i])
                else:
                    items[-1] += ' ' + lines[i].strip()
                i += 1
            out.append(listing(items))
            continue
        if line.startswith('>'):
            quote = []
            while i < len(lines) and lines[i].startswith('>'):
                quote.append(lines[i][1:].strip())
                i += 1
            text = ' '.join(quote)
            kind = re.match(r'^\*\*(Note|Tip|Warning):\*\*', text)
            css = f'callout {kind.group(1).lower()}' if kind else 'quote'
            out.append(f'<div class="{css}">{inline(text)}</div>')
            continue
        para = []
        while i < len(lines) and lines[i].strip() and not re.match(r'^(#{1,4}\s|```|>|\||\s*(?:[-*]|\d+\.)\s)',
                                                                       lines[i]):
            para.append(lines[i].strip())
            i += 1
        text = ' '.join(para)
        if summary is None and title is not None and not out:
            summary = text
            out.append(f'<p class="summary">{inline(text)}</p>')
        else:
            out.append(f'<p>{inline(text)}</p>')
    return title or 'Untitled', summary or '', '\n'.join(out), headings


def describe(markdown):
    """What a page tells the rest of the wiki: its title, summary, category, items and icon."""
    title, summary, _, _ = render(markdown)
    meta = {'title': title, 'summary': summary, 'category': '', 'ids': [], 'icon': '', 'image': ''}
    box = re.search(r'^```infobox\n(.*?)^```', markdown, re.S | re.M)
    if box:
        _, rows = infobox_rows([b for b in box.group(1).split('\n') if b.strip()])
        for key, value in rows:
            if key.lower() == 'category':
                meta['category'] = value
            elif key.lower() == 'id':
                meta['ids'] = [i.strip() for i in value.split(',')]
            elif key.lower() == 'icon':
                meta['icon'] = value
            elif key.lower() == 'image':
                meta['image'] = value
    meta['icon'] = meta['icon'] or (meta['ids'][0] if meta['ids'] else '')
    return meta


def logo():
    """The SGCraft 3.0 logo: a gate ring beside the wordmark, and REMAKE in Milky Way glyphs around the point of
    origin, seven symbols as a gate's address has."""
    chevrons = ''.join(f'<path d="M-9 -130 L9 -130 L4 -110 L-4 -110 Z" transform="rotate({a})"/>'
                       for a in (0, 40, 80, 120, -40, -80, -120))
    remake = []
    for i, char in enumerate('REM@AKE'):
        x = 376 + 42 * i
        if char == '@':
            remake.append(f'<image x="{x - 3}" y="193" width="40" height="40" '
                          f'href="{asset("glyphs/milkyway/origin.png")}"/>')
        else:
            remake.append(f'<image x="{x}" y="196" width="34" height="34" '
                          f'href="{asset(f"glyphs/milkyway-logo/{char}.png")}"/>')
    remake = ''.join(remake)
    return f'''<svg class="logo" viewBox="80 14 600 290" role="img" aria-label="SGCraft Remake 3.0">
<g transform="translate(250 165)">
<circle r="116" fill="none" stroke="#3a4250" stroke-width="22"/>
<circle r="116" fill="none" stroke="#5b6574" stroke-width="2" stroke-dasharray="4 10"/>
<circle r="102" fill="#0d3b66"/><circle r="78" fill="#1565a8"/><circle r="50" fill="#3d8fd6"/>
<circle r="24" fill="#9ccbf2"/><g fill="#e8892b">{chevrons}</g></g>
<text x="519" y="168" text-anchor="middle" font-family="Michroma, sans-serif" font-size="43" fill="#e9eef5"
letter-spacing="5">SGCRAFT</text>
<rect x="371" y="184" width="296" height="2" fill="#e8892b"/>{remake}
<text x="519" y="262" text-anchor="middle" font-family="Orbitron, sans-serif" font-size="13" fill="#8fa3bb"
letter-spacing="8">REMAKE · 3.0</text></svg>'''


def search_index():
    """Every page's title, link and section, for the search box."""
    section = {}
    for title, names in NAV:
        for name in names:
            section[name] = title
    entries = []
    for name, meta in sorted(META.items(), key=lambda m: m[1]['title'].lower()):
        folder = name.split('/')[0] if '/' in name else ''
        where = META[folder]['title'] if folder in META else section.get(name, 'More')
        entries.append({'t': meta['title'], 'u': name + '.html', 's': where})
    return json.dumps(entries, separators=(',', ':'))


def page(name, title, summary, body, headings, pages):
    folder = name.split('/')[0] if '/' in name else ''
    nav = []
    listed = set()
    for section, names in NAV:
        links = []
        for other in names:
            if other in pages:
                listed.add(other)
                current = ' class="current"' if other in (name, folder) else ''
                links.append(f'<li><a href="{asset(other + ".html")}"{current}>'
                             f'{html.escape(pages[other][0])}</a></li>')
        nav.append(f'<h3>{section}</h3><ul>{"".join(links)}</ul>')
    extra = [other for other in sorted(pages) if other not in listed and '/' not in other]
    if extra:
        links = ''.join(f'<li><a href="{asset(o + ".html")}">{html.escape(pages[o][0])}</a></li>' for o in extra)
        nav.append(f'<h3>More</h3><ul>{links}</ul>')
    toc = ''
    if len(headings) > 2:
        items = ''.join(f'<li><a href="#{a}">{t}</a></li>' for a, t in headings)
        toc = f'<nav class="toc"><div class="toc-title">On this page</div><ol>{items}</ol></nav>'
    # The contents sit after the summary, beside the infobox if the page has one
    marker = '<p class="summary">'
    if toc and marker in body:
        end = body.index('</p>', body.index(marker)) + len('</p>')
        body = body[:end] + toc + body[end:]
    elif toc:
        body = toc + body
    crumbs = f'<a href="{asset("home.html")}">SGCraft Wiki</a> <span>›</span> '
    if folder in pages:
        crumbs += f'<a href="{asset(folder + ".html")}">{html.escape(pages[folder][0])}</a> <span>›</span> '
    crumbs += html.escape(title)
    description = html.escape(re.sub(r'[`*\[\]]|\(.*?\.md[^)]*\)', '', summary))
    return f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)} · SGCraft Wiki</title>
<meta name="description" content="{description}">
<link rel="icon" href="{asset("glyphs/milkyway-logo/origin.png")}">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Michroma&family=Orbitron:wght@500;700&family=Barlow:wght@400;500;600&display=swap" rel="stylesheet">
<link rel="stylesheet" href="{asset("wiki.css")}">
</head>
<body>
<header class="masthead"><a class="brand" href="{asset("home.html")}">{logo()}</a>
<div class="masthead-text"><div class="wiki-name">SGCraft Wiki</div>
<div class="search-box"><input class="search" type="search" placeholder="Search the wiki" aria-label="Search the wiki">
<ul class="search-results" hidden></ul></div></div></header>
<div class="layout">
<nav class="sidebar">{"".join(nav)}</nav>
<main class="content">
<div class="breadcrumb">{crumbs}</div>
<h1 class="page-title">{html.escape(title)}</h1>
<article>{body}</article>
<footer class="foot">SGCraft is made by AlmuraDev, building on the original SGCraft by gcewing.
Stargate is a trademark of MGM; this is a fan project with no tie to MGM.</footer>
</main>
</div>
<script>
const BASE = '{BASE}';
const PAGES = {search_index()};
const box = document.querySelector('.search');
const results = document.querySelector('.search-results');
box.addEventListener('input', () => {{
  const q = box.value.trim().toLowerCase();
  document.querySelectorAll('.sidebar li').forEach(li => {{
    li.style.display = !q || li.textContent.toLowerCase().includes(q) ? '' : 'none';
  }});
  const found = q ? PAGES.filter(p => p.t.toLowerCase().includes(q)).slice(0, 12) : [];
  results.replaceChildren(...found.map(p => {{
    const li = document.createElement('li');
    const a = document.createElement('a');
    a.href = BASE + p.u;
    a.textContent = p.t;
    const where = document.createElement('span');
    where.textContent = p.s;
    a.append(where);
    li.append(a);
    return li;
  }}));
  results.hidden = found.length === 0;
}});
box.addEventListener('keydown', e => {{
  const first = results.querySelector('a');
  if (e.key === 'Enter' && first) location.href = first.href;
  if (e.key === 'Escape') {{ box.value = ''; box.dispatchEvent(new Event('input')); }}
}});
</script>
</body>
</html>
'''


def markdown_pages():
    """Every page's name (its path under docs/wiki without .md) and its file."""
    return {path.relative_to(SOURCE).with_suffix('').as_posix(): path
            for path in sorted(SOURCE.rglob('*.md')) if path.name != 'README.md' and ICONS not in path.parents}


def refresh_icons():
    """Keeps the icons and names of every SGCraft item, and of every other item the pages or recipes name."""
    exported = json.loads((EXPORTED / 'names.json').read_text(encoding='utf-8'))
    wanted = {i for i in exported if i.startswith('sgcraft:')}
    text = ' '.join(p.read_text(encoding='utf-8') for p in markdown_pages().values())
    for path in RECIPES.glob('*/recipe/*.json'):
        text += ' ' + path.read_text(encoding='utf-8')
    wanted |= {i for i in re.findall(r'[a-z0-9_.-]+:[a-z0-9_./-]+', text) if i in exported}
    if ICONS.exists():
        shutil.rmtree(ICONS)
    for item_id in sorted(wanted):
        namespace, _, path = item_id.partition(':')
        target = ICONS / namespace / f'{path}.png'
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(EXPORTED / namespace / f'{path}.png', target)
    names = {i: exported[i] for i in sorted(wanted)}
    (ICONS / 'names.json').write_text(json.dumps(names, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(f'Kept the icons of {len(wanted)} items in {ICONS}')
    refresh_scenes()


def refresh_scenes():
    """Keeps the scene export's pictures: entities as they are, see-through, and the world pictures cut to their
    middle, where the gate stands, and made smaller."""
    exported = EXPORTED / 'scenes'
    if not exported.exists():
        return
    if SCENES.exists():
        shutil.rmtree(SCENES)
    SCENES.mkdir(parents=True)
    for path in sorted(exported.glob('*.png')):
        picture = Image.open(path)
        if picture.mode == 'RGBA' and picture.getextrema()[3][0] < 255:
            picture.crop(picture.getbbox()).save(SCENES / path.name, optimize=True)
            continue
        share = SCENE_CROP.get(path.stem.rsplit('-', 1)[0], STANDARD_CROP)
        width, height = picture.size
        crop_width, crop_height = round(width * share), round(height * share)
        left, top = (width - crop_width) // 2, (height - crop_height) // 2
        picture = picture.convert('RGB').crop((left, top, left + crop_width, top + crop_height))
        picture = picture.resize((SCENE_WIDTH, round(crop_height * SCENE_WIDTH / crop_width)), Image.LANCZOS)
        picture.save(SCENES / f'{path.stem}.jpg', quality=88, optimize=True)
    print(f'Kept {len(list(SCENES.iterdir()))} scenes in {SCENES}')


def used_in(item_ids, used):
    """The section listing what an item goes into, or nothing if no recipe takes it."""
    results = list(dict.fromkeys(r for i in item_ids for r in used.get(i, [])))
    if not results:
        return ''
    entries = ''.join(f'<li>{item(r)}</li>' for r in sorted(results, key=item_name))
    return (f'<h2 id="used-in"><a class="anchor" href="#used-in">Used in</a></h2>'
            f'<ul class="used-in">{entries}</ul>')


def check(sources):
    """Warns of each link to a page or anchor the site lacks, and of each page whose item has a recipe the page
    doesn't draw."""
    problems = []
    anchors = {}
    for path in OUT.rglob('*.html'):
        anchors[path.resolve()] = set(re.findall(r'id="([^"]+)"', path.read_text(encoding='utf-8')))
    for path in OUT.rglob('*.html'):
        text = path.read_text(encoding='utf-8')
        for target in re.findall(r'(?:href|src)="([^"]+)"', text):
            if re.match(r'^[a-z][a-z0-9+.-]*:', target):
                continue
            file, _, anchor = target.partition('#')
            resolved = (path.parent / file).resolve() if file else path.resolve()
            if not resolved.exists():
                problems.append(f'{path.relative_to(OUT)}: no {file}')
            elif anchor and resolved in anchors and anchor not in anchors[resolved]:
                problems.append(f'{path.relative_to(OUT)}: no #{anchor} in {file or "this page"}')
    made = {}
    for recipe_id in all_recipes():
        recipe = load_recipe(recipe_id)
        if recipe:
            made.setdefault(recipe['result'], []).append(recipe_id)
    for name, meta in META.items():
        if '/' not in name:
            continue
        drawn = set(re.findall(r'^```recipe\n\s*([a-z0-9_.-]+:[a-z0-9_./-]+)\s*\n```', sources[name], re.M))
        for item_id in meta['ids']:
            for recipe_id in made.get(item_id, []):
                if recipe_id not in drawn:
                    problems.append(f'{name}: no recipe block for {recipe_id}')
    for item_id in NAMES:
        if item_id.startswith('sgcraft:') and item_id not in PAGE_OF:
            problems.append(f'no dictionary page names {item_id}')
    for problem in sorted(set(problems)):
        print('WARNING', problem)
    return len(set(problems))


def main():
    global NAMES, CURRENT, BASE
    if '--refresh-icons' in sys.argv:
        refresh_icons()
        NAMES = load_names()
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    cut_glyphs()
    shutil.copy(Path(__file__).with_name('wiki.css'), OUT / 'wiki.css')
    for folder in ('assets', 'icons', 'scenes'):
        if (SOURCE / folder).exists():
            shutil.copytree(SOURCE / folder, OUT / folder)
    sources = {name: path.read_text(encoding='utf-8').replace('\r\n', '\n')
               for name, path in markdown_pages().items()}
    for name, text in sources.items():
        META[name] = describe(text)
        # Items link to their dictionary pages; the guides only describe them
        if '/' in name:
            for item_id in META[name]['ids']:
                PAGE_OF.setdefault(item_id, name)
    used = uses()
    rendered = {}
    for name, text in sources.items():
        CURRENT = name
        BASE = '../' * name.count('/')
        title, summary, body, headings = render(text)
        section = used_in(META[name]['ids'], used) if '/' in name else ''
        if section:
            body += section
            headings.append(('used-in', 'Used in'))
        rendered[name] = (title, summary, body, headings)
    for name, (title, summary, body, headings) in rendered.items():
        CURRENT = name
        BASE = '../' * name.count('/')
        target = OUT / f'{name}.html'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(page(name, title, summary, body, headings, rendered), encoding='utf-8', newline='\n')
    (OUT / 'index.html').write_text('<!doctype html><meta http-equiv="refresh" content="0; url=home.html">',
                                    encoding='utf-8')
    print(f'Built {len(rendered)} pages into {OUT}')
    warnings = check(sources)
    print(f'{warnings} warnings')
    if warnings and '--strict' in sys.argv:
        sys.exit(1)


if __name__ == '__main__':
    main()
