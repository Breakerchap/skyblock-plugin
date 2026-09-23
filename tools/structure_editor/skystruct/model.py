from __future__ import annotations

from dataclasses import dataclass, field
import json
from pathlib import Path
from typing import Any, Iterable

FORMAT = "skystruct/1"
AIR = "."
Pos = tuple[int, int, int]


@dataclass(frozen=True)
class PaletteEntry:
    block: str
    color: str | None = None
    state: dict[str, str] = field(default_factory=dict)

    @staticmethod
    def from_json(value: Any) -> "PaletteEntry":
        if isinstance(value, str):
            return PaletteEntry(value)
        if not isinstance(value, dict) or "block" not in value:
            raise ValueError(f"Invalid palette entry: {value!r}")
        return PaletteEntry(
            block=str(value["block"]),
            color=value.get("color"),
            state={str(k): str(v) for k, v in value.get("state", {}).items()},
        )

    def to_json(self) -> dict[str, Any]:
        result: dict[str, Any] = {"block": self.block}
        if self.color:
            result["color"] = self.color
        if self.state:
            result["state"] = dict(sorted(self.state.items()))
        return result

    def namespaced_block(self) -> str:
        return self.block if ":" in self.block else f"minecraft:{self.block}"


@dataclass
class Structure:
    name: str
    palette: dict[str, PaletteEntry]
    blocks: dict[Pos, str] = field(default_factory=dict)
    origin: Pos = (0, 0, 0)
    entities: list[dict[str, Any]] = field(default_factory=list)
    markers: list[dict[str, Any]] = field(default_factory=list)
    meta: dict[str, Any] = field(default_factory=dict)

    @staticmethod
    def load(path: str | Path) -> "Structure":
        return Structure.from_json(json.loads(Path(path).read_text(encoding="utf-8")))

    @staticmethod
    def from_json(data: dict[str, Any]) -> "Structure":
        if data.get("format") != FORMAT:
            raise ValueError(f"Expected format {FORMAT!r}, got {data.get('format')!r}")

        palette = {
            symbol: PaletteEntry.from_json(value)
            for symbol, value in data.get("palette", {}).items()
        }
        if AIR in palette or " " in palette:
            raise ValueError("'.' and space are reserved for air")
        for symbol in palette:
            if len(symbol) != 1:
                raise ValueError(f"Palette symbols must be one character: {symbol!r}")

        blocks: dict[Pos, str] = {}
        for layer in data.get("layers", []):
            y = int(layer["y"])
            x0 = int(layer.get("x", 0))
            z0 = int(layer.get("z", 0))
            rows = layer.get("rows", [])
            if not isinstance(rows, list):
                raise ValueError("layer.rows must be a list")
            for rz, row in enumerate(rows):
                if not isinstance(row, str):
                    raise ValueError("layer rows must be strings")
                for rx, symbol in enumerate(row):
                    if symbol in (AIR, " "):
                        continue
                    if symbol not in palette:
                        raise ValueError(
                            f"Unknown symbol {symbol!r} at {(x0 + rx, y, z0 + rz)}"
                        )
                    blocks[(x0 + rx, y, z0 + rz)] = symbol

        raw_origin = data.get("origin", [0, 0, 0])
        if not isinstance(raw_origin, list) or len(raw_origin) != 3:
            raise ValueError("origin must be [x,y,z]")
        origin: Pos = tuple(int(v) for v in raw_origin)  # type: ignore[assignment]

        return Structure(
            name=str(data.get("name", "unnamed")),
            palette=palette,
            blocks=blocks,
            origin=origin,
            entities=list(data.get("entities", [])),
            markers=list(data.get("markers", [])),
            meta=dict(data.get("meta", {})),
        )

    def save(self, path: str | Path) -> None:
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(
            json.dumps(self.to_json(), indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )

    def to_json(self) -> dict[str, Any]:
        return {
            "format": FORMAT,
            "name": self.name,
            "origin": list(self.origin),
            "palette": {
                symbol: entry.to_json()
                for symbol, entry in sorted(self.palette.items())
            },
            "layers": self._layers_json(),
            "entities": self.entities,
            "markers": self.markers,
            "meta": self.meta,
        }

    def _layers_json(self) -> list[dict[str, Any]]:
        if not self.blocks:
            return []
        ys = sorted({y for _, y, _ in self.blocks})
        result: list[dict[str, Any]] = []
        for y in ys:
            positions = [p for p in self.blocks if p[1] == y]
            min_x = min(p[0] for p in positions)
            max_x = max(p[0] for p in positions)
            min_z = min(p[2] for p in positions)
            max_z = max(p[2] for p in positions)
            rows: list[str] = []
            for z in range(min_z, max_z + 1):
                chars = [self.blocks.get((x, y, z), AIR) for x in range(min_x, max_x + 1)]
                rows.append("".join(chars).rstrip(AIR))
            while rows and not rows[-1]:
                rows.pop()
            result.append({"y": y, "x": min_x, "z": min_z, "rows": rows})
        return result

    def set(self, pos: Pos, symbol: str) -> None:
        if symbol in (AIR, " "):
            self.blocks.pop(pos, None)
            return
        if symbol not in self.palette:
            raise KeyError(f"Unknown palette symbol {symbol!r}")
        self.blocks[pos] = symbol

    def get(self, pos: Pos) -> str:
        return self.blocks.get(pos, AIR)

    def bounds(self) -> tuple[Pos, Pos] | None:
        if not self.blocks:
            return None
        xs = [x for x, _, _ in self.blocks]
        ys = [y for _, y, _ in self.blocks]
        zs = [z for _, _, z in self.blocks]
        return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))

    def size(self) -> Pos:
        bounds = self.bounds()
        if bounds is None:
            return (0, 0, 0)
        lo, hi = bounds
        return tuple(hi[i] - lo[i] + 1 for i in range(3))  # type: ignore[return-value]

    def positions(self, symbol: str | None = None) -> Iterable[Pos]:
        if symbol is None:
            return self.blocks.keys()
        return (p for p, value in self.blocks.items() if value == symbol)

    def translate(self, dx: int, dy: int, dz: int) -> None:
        self.blocks = {
            (x + dx, y + dy, z + dz): symbol
            for (x, y, z), symbol in self.blocks.items()
        }
        ox, oy, oz = self.origin
        self.origin = (ox + dx, oy + dy, oz + dz)
        for collection in (self.entities, self.markers):
            for item in collection:
                pos = item.get("pos")
                if isinstance(pos, list) and len(pos) == 3:
                    item["pos"] = [pos[0] + dx, pos[1] + dy, pos[2] + dz]

    def fill_box(self, a: Pos, b: Pos, symbol: str, hollow: bool = False) -> int:
        min_x, max_x = sorted((a[0], b[0]))
        min_y, max_y = sorted((a[1], b[1]))
        min_z, max_z = sorted((a[2], b[2]))
        count = 0
        for x in range(min_x, max_x + 1):
            for y in range(min_y, max_y + 1):
                for z in range(min_z, max_z + 1):
                    if hollow and not (
                        x in (min_x, max_x)
                        or y in (min_y, max_y)
                        or z in (min_z, max_z)
                    ):
                        continue
                    self.set((x, y, z), symbol)
                    count += 1
        return count


NEIGHBORS_6: tuple[Pos, ...] = (
    (1, 0, 0), (-1, 0, 0),
    (0, 1, 0), (0, -1, 0),
    (0, 0, 1), (0, 0, -1),
)


def add_pos(a: Pos, b: Pos) -> Pos:
    return a[0] + b[0], a[1] + b[1], a[2] + b[2]
