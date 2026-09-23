from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

from .analyze import analyze, format_report
from .model import PaletteEntry, Structure
from .nbt import DEFAULT_DATA_VERSION, export_structure
from .ops import organic_island, prune_small_components, replace_random, scatter_on_top
from .render import render_turntable


def _pos(values: list[str]) -> tuple[int, int, int]:
    if len(values) != 3:
        raise argparse.ArgumentTypeError("position needs exactly x y z")
    return tuple(int(v) for v in values)  # type: ignore[return-value]


def _load(path: str) -> Structure:
    return Structure.load(path)


def _save(structure: Structure, path: str) -> None:
    structure.save(path)
    print(f"Saved {path}")


def cmd_new(args: argparse.Namespace) -> None:
    palette = {
        "g": PaletteEntry("minecraft:grass_block", "#6f9f45"),
        "d": PaletteEntry("minecraft:dirt", "#8a6545"),
        "s": PaletteEntry("minecraft:stone", "#818181"),
    }
    Structure(args.name, palette).save(args.file)
    print(f"Created {args.file}")


def cmd_analyze(args: argparse.Namespace) -> None:
    report = analyze(_load(args.file))
    if args.json:
        print(json.dumps(report.to_json(), indent=2))
    else:
        print(format_report(report))
    if args.fail_on_detached and report.detached_blocks:
        raise SystemExit(2)


def cmd_render(args: argparse.Namespace) -> None:
    outputs = render_turntable(_load(args.file), args.output, tile=args.tile)
    for output in outputs:
        print(output)


def cmd_export(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    version = args.data_version
    if version is None:
        version = int(structure.meta.get("data_version", DEFAULT_DATA_VERSION))
    sidecar = export_structure(structure, args.output, data_version=version)
    print(f"Wrote vanilla structure: {args.output}")
    print(f"Wrote placement metadata: {Path(args.output).with_suffix('.placement.json')}")
    print(f"Placement offset from anchor: {sidecar['placement_offset']}")


def cmd_set(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    structure.set((args.x, args.y, args.z), args.symbol)
    _save(structure, args.file)


def cmd_fill(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    count = structure.fill_box(
        (args.x1, args.y1, args.z1),
        (args.x2, args.y2, args.z2),
        args.symbol,
        hollow=args.hollow,
    )
    _save(structure, args.file)
    print(f"Changed {count} block positions")


def cmd_palette(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    state: dict[str, str] = {}
    for pair in args.state:
        if "=" not in pair:
            raise SystemExit(f"Invalid state {pair!r}; expected key=value")
        key, value = pair.split("=", 1)
        state[key] = value
    structure.palette[args.symbol] = PaletteEntry(args.block, args.color, state)
    _save(structure, args.file)


def cmd_island(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    count = organic_island(
        structure,
        radius=args.radius,
        top_y=args.top,
        surface=args.surface,
        soil=args.soil,
        stone=args.stone,
        seed=args.seed,
        fullness=args.fullness,
        min_depth=args.min_depth,
        max_depth=args.max_depth,
        hanging_spikes=args.spikes,
        clear_existing=args.clear,
    )
    _save(structure, args.file)
    print(f"Baked {count} explicit terrain blocks")


def cmd_prune(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    removed = prune_small_components(structure, keep_at_least=args.minimum)
    _save(structure, args.file)
    print(f"Removed {removed} block(s) from tiny detached components")


def cmd_replace_random(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    changed = replace_random(
        structure,
        args.source,
        args.target,
        chance=args.chance,
        seed=args.seed,
        only_exposed=args.exposed,
    )
    _save(structure, args.file)
    print(f"Replaced {changed} block(s)")


def cmd_scatter(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    below = set(args.on) if args.on else None
    placed = scatter_on_top(
        structure,
        args.symbol,
        count=args.count,
        seed=args.seed,
        require_below=below,
    )
    _save(structure, args.file)
    print(f"Scattered {placed} block(s)")


def cmd_layer(args: argparse.Namespace) -> None:
    structure = _load(args.file)
    positions = [(x, z, symbol) for (x, y, z), symbol in structure.blocks.items() if y == args.y]
    if not positions:
        print(f"No blocks at y={args.y}")
        return
    min_x = min(x for x, _, _ in positions)
    max_x = max(x for x, _, _ in positions)
    min_z = min(z for _, z, _ in positions)
    max_z = max(z for _, z, _ in positions)
    print(f"y={args.y}, x={min_x}..{max_x}, z={min_z}..{max_z}")
    for z in range(min_z, max_z + 1):
        print("".join(structure.get((x, args.y, z)) for x in range(min_x, max_x + 1)))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="skystruct",
        description="Headless structure authoring, diagnostics, preview rendering and Minecraft NBT export.",
    )
    sub = parser.add_subparsers(required=True)

    p = sub.add_parser("new", help="create an empty blueprint")
    p.add_argument("file")
    p.add_argument("--name", default="New Structure")
    p.set_defaults(func=cmd_new)

    p = sub.add_parser("analyze", help="diagnose disconnected, flat or repetitive geometry")
    p.add_argument("file")
    p.add_argument("--json", action="store_true")
    p.add_argument("--fail-on-detached", action="store_true")
    p.set_defaults(func=cmd_analyze)

    p = sub.add_parser("render", help="render four isometric PNG previews")
    p.add_argument("file")
    p.add_argument("-o", "--output", default="preview")
    p.add_argument("--tile", type=int, default=18)
    p.set_defaults(func=cmd_render)

    p = sub.add_parser("export-nbt", help="compile blueprint to a real vanilla .nbt structure")
    p.add_argument("file")
    p.add_argument("-o", "--output", required=True)
    p.add_argument("--data-version", type=int)
    p.set_defaults(func=cmd_export)

    p = sub.add_parser("set", help="set one block")
    p.add_argument("file")
    p.add_argument("x", type=int); p.add_argument("y", type=int); p.add_argument("z", type=int)
    p.add_argument("symbol")
    p.set_defaults(func=cmd_set)

    p = sub.add_parser("fill", help="fill a box")
    p.add_argument("file")
    for name in ("x1","y1","z1","x2","y2","z2"):
        p.add_argument(name, type=int)
    p.add_argument("symbol")
    p.add_argument("--hollow", action="store_true")
    p.set_defaults(func=cmd_fill)

    p = sub.add_parser("palette", help="add/update a palette symbol")
    p.add_argument("file"); p.add_argument("symbol"); p.add_argument("block")
    p.add_argument("--color")
    p.add_argument("--state", action="append", default=[])
    p.set_defaults(func=cmd_palette)

    p = sub.add_parser("organic-island", help="bake an irregular connected floating island into explicit blocks")
    p.add_argument("file")
    p.add_argument("--radius", type=int, required=True)
    p.add_argument("--top", type=int, default=0)
    p.add_argument("--surface", required=True)
    p.add_argument("--soil", required=True)
    p.add_argument("--stone", required=True)
    p.add_argument("--seed", type=int, required=True)
    p.add_argument("--fullness", type=float, default=0.72)
    p.add_argument("--min-depth", type=int, default=2)
    p.add_argument("--max-depth", type=int, default=10)
    p.add_argument("--spikes", type=float, default=0.06)
    p.add_argument("--clear", action="store_true")
    p.set_defaults(func=cmd_island)

    p = sub.add_parser("prune-components", help="remove tiny disconnected components")
    p.add_argument("file")
    p.add_argument("--minimum", type=int, default=4)
    p.set_defaults(func=cmd_prune)

    p = sub.add_parser("replace-random", help="seeded palette variation")
    p.add_argument("file"); p.add_argument("source"); p.add_argument("target")
    p.add_argument("--chance", type=float, required=True)
    p.add_argument("--seed", type=int, required=True)
    p.add_argument("--exposed", action="store_true")
    p.set_defaults(func=cmd_replace_random)

    p = sub.add_parser("scatter", help="scatter blocks on exposed top surfaces")
    p.add_argument("file"); p.add_argument("symbol")
    p.add_argument("--count", type=int, required=True)
    p.add_argument("--seed", type=int, required=True)
    p.add_argument("--on", action="append", default=[])
    p.set_defaults(func=cmd_scatter)

    p = sub.add_parser("layer", help="print an editable ASCII view of one Y layer")
    p.add_argument("file"); p.add_argument("y", type=int)
    p.set_defaults(func=cmd_layer)
    return parser


def main(argv: list[str] | None = None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        args.func(args)
    except (ValueError, KeyError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        raise SystemExit(2) from exc
