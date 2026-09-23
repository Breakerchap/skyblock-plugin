from __future__ import annotations

import gzip
import io
import json
from pathlib import Path
import struct
from typing import Any

from .model import PaletteEntry, Structure

# Known-valid Minecraft 26.2 data version. Newer servers data-fix older structures.
# Override from the CLI when a newer target data version is known.
DEFAULT_DATA_VERSION = 4903

TAG_END = 0
TAG_BYTE = 1
TAG_INT = 3
TAG_DOUBLE = 6
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

_TYPE_IDS = {
    "byte": TAG_BYTE,
    "int": TAG_INT,
    "double": TAG_DOUBLE,
    "string": TAG_STRING,
    "list": TAG_LIST,
    "compound": TAG_COMPOUND,
}


def _name(value: str) -> bytes:
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _write_payload(out: io.BytesIO, node: tuple[Any, ...]) -> None:
    kind = node[0]
    value = node[1] if len(node) > 1 else None
    if kind == "byte":
        out.write(struct.pack(">b", int(value)))
    elif kind == "int":
        out.write(struct.pack(">i", int(value)))
    elif kind == "double":
        out.write(struct.pack(">d", float(value)))
    elif kind == "string":
        out.write(_name(str(value)))
    elif kind == "list":
        subtype = node[1]
        items = node[2]
        out.write(struct.pack(">b", _TYPE_IDS[subtype]))
        out.write(struct.pack(">i", len(items)))
        for item in items:
            _write_payload(out, item)
    elif kind == "compound":
        for child_name, child in value.items():
            out.write(struct.pack(">b", _TYPE_IDS[child[0]]))
            out.write(_name(child_name))
            _write_payload(out, child)
        out.write(bytes((TAG_END,)))
    else:
        raise ValueError(f"Unsupported NBT node type {kind!r}")


def _list_int(values: list[int]) -> tuple[Any, ...]:
    return ("list", "int", [("int", value) for value in values])


def _list_double(values: list[float]) -> tuple[Any, ...]:
    return ("list", "double", [("double", value) for value in values])


def _palette_key(entry: PaletteEntry) -> tuple[str, tuple[tuple[str, str], ...]]:
    return entry.namespaced_block(), tuple(sorted(entry.state.items()))


def _palette_node(entry: PaletteEntry) -> tuple[Any, ...]:
    data: dict[str, tuple[Any, ...]] = {"Name": ("string", entry.namespaced_block())}
    if entry.state:
        data["Properties"] = (
            "compound",
            {key: ("string", value) for key, value in sorted(entry.state.items())},
        )
    return ("compound", data)


def compile_structure(
    structure: Structure,
    *,
    data_version: int = DEFAULT_DATA_VERSION,
) -> tuple[tuple[Any, ...], dict[str, Any]]:
    bounds = structure.bounds()
    if bounds is None:
        minimum = (0, 0, 0)
        maximum = (-1, -1, -1)
        size = (0, 0, 0)
    else:
        minimum, maximum = bounds
        size = tuple(maximum[i] - minimum[i] + 1 for i in range(3))

    palette: list[PaletteEntry] = []
    palette_index: dict[tuple[str, tuple[tuple[str, str], ...]], int] = {}
    for symbol in sorted(set(structure.blocks.values())):
        entry = structure.palette[symbol]
        key = _palette_key(entry)
        if key not in palette_index:
            palette_index[key] = len(palette)
            palette.append(entry)

    block_nodes: list[tuple[Any, ...]] = []
    for pos, symbol in sorted(structure.blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        entry = structure.palette[symbol]
        relative = [pos[i] - minimum[i] for i in range(3)]
        block_nodes.append((
            "compound",
            {
                "pos": _list_int(relative),
                "state": ("int", palette_index[_palette_key(entry)]),
            },
        ))

    entity_nodes: list[tuple[Any, ...]] = []
    for entity in structure.entities:
        raw_pos = entity.get("pos", [0, 0, 0])
        if not isinstance(raw_pos, list) or len(raw_pos) != 3:
            raise ValueError(f"Entity position must be [x,y,z]: {entity!r}")
        entity_id = str(entity.get("id", "minecraft:pig"))
        pos = [float(raw_pos[i] - minimum[i]) for i in range(3)]
        block_pos = [int(round(v)) for v in pos]
        entity_nodes.append((
            "compound",
            {
                "blockPos": _list_int(block_pos),
                "pos": _list_double(pos),
                "nbt": (
                    "compound",
                    {
                        "id": ("string", entity_id),
                        "Pos": _list_double(pos),
                    },
                ),
            },
        ))

    root = (
        "compound",
        {
            "size": _list_int(list(size)),
            "palette": ("list", "compound", [_palette_node(entry) for entry in palette]),
            "blocks": ("list", "compound", block_nodes),
            "entities": ("list", "compound", entity_nodes),
            "DataVersion": ("int", int(data_version)),
        },
    )

    origin = structure.origin
    placement_offset = [minimum[i] - origin[i] for i in range(3)]
    sidecar = {
        "format": "skystruct-placement/1",
        "name": structure.name,
        "nbt_data_version": int(data_version),
        "source_origin": list(origin),
        "source_bounds": {
            "min": list(minimum),
            "max": list(maximum),
        },
        "nbt_size": list(size),
        "placement_offset": placement_offset,
        "markers": structure.markers,
        "entities": structure.entities,
        "explanation": (
            "To place the blueprint origin at world anchor A, place the NBT structure at "
            "A + placement_offset."
        ),
    }
    return root, sidecar


def write_nbt(path: str | Path, root: tuple[Any, ...]) -> None:
    raw = io.BytesIO()
    raw.write(struct.pack(">b", TAG_COMPOUND))
    raw.write(_name(""))
    _write_payload(raw, root)

    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(target, "wb") as out:
        out.write(raw.getvalue())


def export_structure(
    structure: Structure,
    output: str | Path,
    *,
    data_version: int = DEFAULT_DATA_VERSION,
    write_sidecar: bool = True,
) -> dict[str, Any]:
    root, sidecar = compile_structure(structure, data_version=data_version)
    output = Path(output)
    write_nbt(output, root)
    if write_sidecar:
        sidecar_path = output.with_suffix(".placement.json")
        sidecar_path.write_text(
            json.dumps(sidecar, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
    return sidecar
