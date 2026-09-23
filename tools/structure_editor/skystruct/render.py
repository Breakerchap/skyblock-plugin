from __future__ import annotations

from dataclasses import dataclass
import math
from pathlib import Path
import struct
import zlib

from .analyze import connected_components
from .model import PaletteEntry, Pos, Structure

RGB = tuple[int, int, int]

DEFAULT_COLORS: dict[str, str] = {
    "grass_block": "#6f9f45", "moss_block": "#5b8f3d", "moss_carpet": "#6ea44c",
    "dirt": "#8a6545", "coarse_dirt": "#76573d", "rooted_dirt": "#8b6a4b",
    "stone": "#818181", "cobblestone": "#727272", "deepslate": "#53545a",
    "tuff": "#6f7772", "sand": "#dccb8f", "sandstone": "#d9c58a",
    "red_sand": "#b76735", "water": "#3f75c5", "clay": "#9ca6b5",
    "oak_log": "#73532f", "oak_planks": "#ad8550", "oak_leaves": "#3f7d34",
    "jungle_log": "#755335", "jungle_leaves": "#3f8433",
    "spruce_log": "#55412b", "spruce_leaves": "#315f3e",
    "mangrove_log": "#70413c", "mangrove_leaves": "#4b7d3d",
    "mycelium": "#6c655f", "podzol": "#6b4d2d",
    "prismarine": "#6fae9f", "prismarine_bricks": "#63a997",
    "dark_prismarine": "#3b6c63", "sea_lantern": "#c9e7d8",
    "packed_ice": "#86aef2", "blue_ice": "#5787df", "snow_block": "#f1f5f8",
    "calcite": "#e3ded1", "smooth_basalt": "#4d4b50",
    "amethyst_block": "#8e65c5", "budding_amethyst": "#9b70cf",
    "end_stone": "#d6d49d", "end_stone_bricks": "#c9c58a",
    "purpur_block": "#aa7eaa", "purpur_pillar": "#a46fa3",
    "obsidian": "#2f2545", "netherrack": "#773d3e", "blackstone": "#342f34",
    "magma_block": "#9b3e21", "farmland": "#76513a", "dirt_path": "#a98759",
    "lava": "#e56a1c", "white_stained_glass": "#d9e5e8",
}


def _rgb(value: str) -> RGB:
    value = value.lstrip("#")
    if len(value) == 3:
        value = "".join(ch * 2 for ch in value)
    if len(value) != 6:
        raise ValueError(f"Invalid hex colour {value!r}")
    return tuple(int(value[i:i+2], 16) for i in (0, 2, 4))  # type: ignore[return-value]


def _color(entry: PaletteEntry) -> RGB:
    if entry.color:
        return _rgb(entry.color)
    name = entry.namespaced_block().split(":", 1)[1]
    if name in DEFAULT_COLORS:
        return _rgb(DEFAULT_COLORS[name])
    h = 2166136261
    for ch in name:
        h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return 75 + h % 115, 75 + (h >> 8) % 115, 75 + (h >> 16) % 115


def _shade(color: RGB, multiplier: float) -> RGB:
    return tuple(max(0, min(255, round(c * multiplier))) for c in color)  # type: ignore[return-value]


@dataclass
class Raster:
    width: int
    height: int
    background: RGB = (232, 239, 248)

    def __post_init__(self) -> None:
        self.pixels = bytearray(self.width * self.height * 3)
        for i in range(0, len(self.pixels), 3):
            self.pixels[i:i+3] = bytes(self.background)

    def pixel(self, x: int, y: int, color: RGB) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            offset = (y * self.width + x) * 3
            self.pixels[offset:offset+3] = bytes(color)

    def polygon(self, points: list[tuple[float, float]], color: RGB) -> None:
        low = max(0, math.floor(min(y for _, y in points)))
        high = min(self.height - 1, math.ceil(max(y for _, y in points)))
        for py in range(low, high + 1):
            sy = py + 0.5
            hits: list[float] = []
            for i, (x1, y1) in enumerate(points):
                x2, y2 = points[(i + 1) % len(points)]
                if y1 == y2:
                    continue
                if (y1 <= sy < y2) or (y2 <= sy < y1):
                    t = (sy - y1) / (y2 - y1)
                    hits.append(x1 + t * (x2 - x1))
            hits.sort()
            for i in range(0, len(hits) - 1, 2):
                start = max(0, math.ceil(hits[i]))
                end = min(self.width - 1, math.floor(hits[i + 1]))
                for px in range(start, end + 1):
                    self.pixel(px, py, color)

    def save_png(self, path: str | Path) -> None:
        scanlines = bytearray()
        stride = self.width * 3
        for y in range(self.height):
            scanlines.append(0)
            start = y * stride
            scanlines.extend(self.pixels[start:start + stride])

        def chunk(kind: bytes, data: bytes) -> bytes:
            return (
                struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
            )

        data = bytearray(b"\x89PNG\r\n\x1a\n")
        data += chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 2, 0, 0, 0))
        data += chunk(b"IDAT", zlib.compress(bytes(scanlines), 9))
        data += chunk(b"IEND", b"")
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)


def _rotate(pos: Pos, turns: int) -> Pos:
    x, y, z = pos
    return (
        (x, y, z),
        (-z, y, x),
        (-x, y, -z),
        (z, y, -x),
    )[turns % 4]


def render_isometric(
    structure: Structure,
    path: str | Path,
    *,
    rotation: int = 0,
    tile: int = 18,
    margin: int = 30,
    highlight_detached: bool = True,
) -> None:
    if not structure.blocks:
        Raster(320, 200).save_png(path)
        return

    blocks = {_rotate(pos, rotation): symbol for pos, symbol in structure.blocks.items()}
    detached: set[Pos] = set()
    if highlight_detached:
        components = connected_components(structure)
        if len(components) > 1:
            detached = {
                _rotate(pos, rotation)
                for component in components[1:]
                for pos in component
            }
    half = tile / 2
    height = tile

    def project(p: tuple[float, float, float]) -> tuple[float, float]:
        x, y, z = p
        return (x - z) * tile, (x + z) * half - y * height

    raw_faces: list[tuple[float, list[tuple[float, float]], RGB]] = []
    for (x, y, z), symbol in blocks.items():
        base = (232, 63, 74) if (x, y, z) in detached else _color(structure.palette[symbol])
        depth = x + z - y * 0.03
        if (x, y + 1, z) not in blocks:
            raw_faces.append((depth - 0.2, [
                project((x, y + 1, z)), project((x + 1, y + 1, z)),
                project((x + 1, y + 1, z + 1)), project((x, y + 1, z + 1)),
            ], _shade(base, 1.12)))
        if (x + 1, y, z) not in blocks:
            raw_faces.append((depth, [
                project((x + 1, y, z)), project((x + 1, y + 1, z)),
                project((x + 1, y + 1, z + 1)), project((x + 1, y, z + 1)),
            ], _shade(base, 0.88)))
        if (x, y, z + 1) not in blocks:
            raw_faces.append((depth + 0.1, [
                project((x, y, z + 1)), project((x + 1, y, z + 1)),
                project((x + 1, y + 1, z + 1)), project((x, y + 1, z + 1)),
            ], _shade(base, 0.72)))

    all_points = [point for _, points, _ in raw_faces for point in points]
    min_x = min(x for x, _ in all_points)
    max_x = max(x for x, _ in all_points)
    min_y = min(y for _, y in all_points)
    max_y = max(y for _, y in all_points)
    width = max(120, math.ceil(max_x - min_x) + margin * 2)
    canvas_height = max(120, math.ceil(max_y - min_y) + margin * 2)
    raster = Raster(width, canvas_height)

    dx = margin - min_x
    dy = margin - min_y
    for _, points, color in sorted(raw_faces, key=lambda face: face[0]):
        raster.polygon([(x + dx, y + dy) for x, y in points], color)

    # Draw origin crosshair so anchor mistakes are visible in previews.
    ox, oy = project(_rotate(structure.origin, rotation))
    cx, cy = round(ox + dx), round(oy + dy)
    for d in range(-6, 7):
        raster.pixel(cx + d, cy, (210, 40, 40))
        raster.pixel(cx, cy + d, (210, 40, 40))

    raster.save_png(path)


def render_turntable(
    structure: Structure,
    directory: str | Path,
    *,
    tile: int = 18,
    highlight_detached: bool = True,
) -> list[Path]:
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    outputs: list[Path] = []
    for rotation in range(4):
        target = directory / f"view_{rotation}.png"
        render_isometric(
            structure,
            target,
            rotation=rotation,
            tile=tile,
            highlight_detached=highlight_detached,
        )
        outputs.append(target)
    return outputs
