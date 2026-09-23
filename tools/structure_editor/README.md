# SkyStruct structure editor

SkyStruct is a **text-first, headless Minecraft structure editor** built specifically for the Skyblock exploration islands.

The important design rule is that procedural helpers are authoring tools only. They **bake their result into explicit block coordinates**. The final blueprint is individual blocks, not an ellipsoid/radius formula that gets rerun in production.

It uses only Python's standard library.

## Why this exists

The old Java structure builders were difficult to judge visually. SkyStruct gives the authoring loop we actually need:

1. edit/generate explicit blocks,
2. run structural diagnostics,
3. render four PNG views,
4. inspect the images,
5. change the blueprint,
6. repeat,
7. compile the approved blueprint to a real vanilla Minecraft `.nbt` structure.

## Run it

From the repository root:

```powershell
cd tools\structure_editor
python -m skystruct --help
```

No `pip install` is required.

Try the included example:

```powershell
python -m skystruct analyze examples/tiny_island.skystruct.json
python -m skystruct render examples/tiny_island.skystruct.json -o preview/tiny
python -m skystruct export-nbt examples/tiny_island.skystruct.json -o build/tiny_island.nbt
```

The render command creates:

```text
preview/tiny/view_0.png
preview/tiny/view_1.png
preview/tiny/view_2.png
preview/tiny/view_3.png
```

## Optional visual editor

Open `tools/structure_editor/web/index.html` directly in a modern browser. It is a single offline HTML file with no CDN or install.

It can:

- drag/drop or load any `.skystruct.json`;
- paint and erase individual blocks by Y layer;
- show the previous layer as a ghost;
- edit the palette;
- use variable brush sizes;
- pan/zoom the layer grid;
- rotate/zoom an isometric preview;
- show block count, bounds and disconnected-component count live;
- undo/redo;
- save the exact same JSON format used by the CLI.

The browser editor intentionally does **not** have a separate save format. NBT compilation still goes through the tested Python compiler.

## Blueprint format

A `.skystruct.json` file has:

- a one-character block palette;
- an explicit list of Y layers;
- an authoring origin/anchor;
- optional entity spawn definitions;
- optional plugin markers;
- free-form metadata.

Example:

```json
{
  "format": "skystruct/1",
  "name": "Example",
  "origin": [0, 0, 0],
  "palette": {
    "g": {"block": "minecraft:grass_block", "color": "#6f9f45"},
    "d": {"block": "minecraft:dirt", "color": "#8a6545"}
  },
  "layers": [
    {
      "y": 0,
      "x": -2,
      "z": -1,
      "rows": [
        ".gg.",
        "gggg",
        ".ggg"
      ]
    }
  ],
  "entities": [],
  "markers": [],
  "meta": {}
}
```

`.` and spaces are air. Negative authoring coordinates are fine.

## Commands useful to the model

### Diagnostics

```powershell
python -m skystruct analyze build.skystruct.json
```

It reports disconnected components, singleton floating blocks, unusually flat terrain, boxy footprints and suspiciously repeated vertical columns.

To make detached geometry fail a scripted iteration:

```powershell
python -m skystruct analyze build.skystruct.json --fail-on-detached
```

### Preview

```powershell
python -m skystruct render build.skystruct.json -o preview/build
```

Four isometric views are produced without Minecraft, Pillow, Blender or a browser. A red cross marks the blueprint origin. Blocks in components detached from the largest connected build are rendered bright red by default, making accidental floaters obvious from the preview itself. Use `--no-highlight-detached` only when intentionally reviewing satellite chunks.

### Anchor and transforms

The logical origin is where the plugin should consider the landmark's anchor. Change it without moving blocks:

```powershell
python -m skystruct origin build.skystruct.json 0 0 0
```

Move the entire authored coordinate system, including origin/entities/markers:

```powershell
python -m skystruct translate build.skystruct.json 4 -2 7
```

Rotate clockwise in 90-degree steps around the origin:

```powershell
python -m skystruct rotate build.skystruct.json --turns 1
```

Rotation updates geometry plus common Minecraft block-state orientation such as `facing`, horizontal log `axis`, rotatable `rotation`, and rail `shape`, instead of merely moving block coordinates.

### Entities and plugin markers

Entities can be part of the exported vanilla structure:

```powershell
python -m skystruct entity build.skystruct.json minecraft:axolotl 2 1 -3
```

Plugin-only semantic locations stay in the placement sidecar:

```powershell
python -m skystruct marker build.skystruct.json discovery 0 2 0 --data "{\"radius\":22}"
```

This lets the final visual build stay independent from hard-coded Java coordinates for things such as discovery points, trader/map targets, special spawn points or scripted interactions.

### Explicit editing

```powershell
python -m skystruct set build.skystruct.json 3 4 -2 g
python -m skystruct fill build.skystruct.json -4 -2 -4 4 -1 4 d
python -m skystruct layer build.skystruct.json 0
```

### Palette

```powershell
python -m skystruct palette build.skystruct.json m minecraft:moss_block --color "#5b8f3d"
python -m skystruct palette build.skystruct.json l minecraft:oak_log --state axis=y
```

### Organic terrain starting point

```powershell
python -m skystruct organic-island build.skystruct.json ^
  --radius 15 --top 0 --surface g --soil d --stone s ^
  --seed 41277 --fullness 0.68 --max-depth 11 --clear
```

This grows an irregular connected footprint, varies its topography and underside, and **writes all resulting blocks into the JSON**. Subsequent work edits those blocks directly. It is not an ellipsoid stored as parameters.

### Seeded texture variation

```powershell
python -m skystruct replace-random build.skystruct.json s t --chance 0.14 --seed 9 --exposed
```

### Remove obvious accidental floaters

```powershell
python -m skystruct prune-components build.skystruct.json --minimum 4
```

Do not run this blindly if the design intentionally contains disconnected satellite chunks.

## Real Minecraft NBT export

```powershell
python -m skystruct export-nbt build.skystruct.json -o build/lush_hollow.nbt
```

This writes a **gzip-compressed vanilla structure-template NBT**, containing:

- `size`
- `palette`
- `blocks`
- `entities`
- `DataVersion`

It also writes:

```text
build/lush_hollow.placement.json
```

The sidecar matters because authoring is centred around an arbitrary origin while vanilla NBT block coordinates start at the structure's minimum corner.

If its `placement_offset` is:

```json
[-14, -8, -12]
```

and the Skyblock landmark anchor is world location `A`, Paper should place the NBT at:

```text
A + (-14, -8, -12)
```

Then the blueprint's logical `origin` lands exactly on `A`.

The current default NBT DataVersion is the known-valid Minecraft 26.2 value `4903`; newer Paper versions can data-fix older structure templates. A known current value can be supplied explicitly:

```powershell
python -m skystruct export-nbt island.skystruct.json -o island.nbt --data-version 4903
```

## Paper loading

The generated `.nbt` files are intended to be bundled into the plugin and loaded with Paper's `StructureManager`, e.g. via `loadStructure(InputStream)`. The eventual runtime loader will read the matching placement sidecar, add its offset to the configured island anchor, and call `Structure.place(...)`.

That means **the editor output is the production structure**, not merely a reference image that later has to be manually recreated in Java.

## Tests

From `tools/structure_editor`:

```powershell
python -m unittest discover -s tests -v
```
