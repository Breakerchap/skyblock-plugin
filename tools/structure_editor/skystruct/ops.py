from __future__ import annotations

from collections import deque
import math
import random

from .analyze import connected_components
from .model import NEIGHBORS_6, PaletteEntry, Pos, Structure


CARDINAL_2 = ((1, 0), (-1, 0), (0, 1), (0, -1))
DIAGONAL_2 = (
    (1, 0), (-1, 0), (0, 1), (0, -1),
    (1, 1), (1, -1), (-1, 1), (-1, -1),
)


def _edge_distance(cells: set[tuple[int, int]]) -> dict[tuple[int, int], int]:
    distance: dict[tuple[int, int], int] = {}
    queue: deque[tuple[int, int]] = deque()
    for cell in cells:
        x, z = cell
        if any((x + dx, z + dz) not in cells for dx, dz in CARDINAL_2):
            distance[cell] = 0
            queue.append(cell)
    while queue:
        x, z = queue.popleft()
        for dx, dz in CARDINAL_2:
            nxt = (x + dx, z + dz)
            if nxt in cells and nxt not in distance:
                distance[nxt] = distance[(x, z)] + 1
                queue.append(nxt)
    return distance


def grow_organic_footprint(
    radius: int,
    *,
    seed: int,
    fullness: float = 0.72,
    asymmetry: float = 0.28,
) -> set[tuple[int, int]]:
    if radius < 2:
        raise ValueError("radius must be >= 2")
    rng = random.Random(seed)
    target = max(12, round(math.pi * radius * radius * fullness))

    cells: set[tuple[int, int]] = {(0, 0)}
    frontier: set[tuple[int, int]] = set(CARDINAL_2)

    # Directional bias gives each seed a distinct silhouette rather than a fuzzy circle.
    angle = rng.random() * math.tau
    bias_x = math.cos(angle) * asymmetry
    bias_z = math.sin(angle) * asymmetry

    while frontier and len(cells) < target:
        candidates = []
        weights = []
        for x, z in frontier:
            distance = math.hypot(x, z)
            if distance > radius * (1.0 + asymmetry):
                continue
            adjacent = sum((x + dx, z + dz) in cells for dx, dz in CARDINAL_2)
            diagonal = sum((x + dx, z + dz) in cells for dx, dz in DIAGONAL_2)
            directional = 1.0 + (x * bias_x + z * bias_z) / max(radius, 1)
            radial = max(0.05, 1.0 - (distance / (radius * 1.15)) ** 2)
            weight = max(0.001, (0.5 + adjacent * 1.8 + diagonal * 0.15) * radial * directional)
            candidates.append((x, z))
            weights.append(weight)
        if not candidates:
            break
        chosen = rng.choices(candidates, weights=weights, k=1)[0]
        frontier.discard(chosen)
        cells.add(chosen)
        x, z = chosen
        for dx, dz in CARDINAL_2:
            nxt = (x + dx, z + dz)
            if nxt not in cells:
                frontier.add(nxt)

    # Two conservative smoothing passes: close obvious one-cell holes, but preserve bays.
    for _ in range(2):
        additions: set[tuple[int, int]] = set()
        removals: set[tuple[int, int]] = set()
        xs = [x for x, _ in cells]
        zs = [z for _, z in cells]
        for x in range(min(xs) - 1, max(xs) + 2):
            for z in range(min(zs) - 1, max(zs) + 2):
                neighbors = sum((x + dx, z + dz) in cells for dx, dz in DIAGONAL_2)
                if (x, z) not in cells and neighbors >= 6:
                    additions.add((x, z))
                elif (x, z) in cells and (x, z) != (0, 0) and neighbors <= 1:
                    removals.add((x, z))
        cells |= additions
        cells -= removals

    return cells


def organic_island(
    structure: Structure,
    *,
    radius: int,
    top_y: int,
    surface: str,
    soil: str,
    stone: str,
    seed: int,
    fullness: float = 0.72,
    min_depth: int = 2,
    max_depth: int = 10,
    hanging_spikes: float = 0.06,
    clear_existing: bool = False,
) -> int:
    for symbol in (surface, soil, stone):
        if symbol not in structure.palette:
            raise KeyError(f"Unknown palette symbol {symbol!r}")

    if clear_existing:
        structure.blocks.clear()

    rng = random.Random(seed)
    cells = grow_organic_footprint(radius, seed=seed, fullness=fullness)
    edge = _edge_distance(cells)
    placed = 0

    # Broad, correlated height variation from a few random ridges.
    ridges = [
        (
            rng.uniform(-radius * 0.5, radius * 0.5),
            rng.uniform(-radius * 0.5, radius * 0.5),
            rng.uniform(-1.4, 1.8),
            rng.uniform(radius * 0.35, radius * 0.8),
        )
        for _ in range(5)
    ]

    tops: dict[tuple[int, int], int] = {}
    for x, z in cells:
        height_delta = 0.0
        for rx, rz, amplitude, spread in ridges:
            d2 = (x - rx) ** 2 + (z - rz) ** 2
            height_delta += amplitude * math.exp(-d2 / max(1.0, 2 * spread * spread))
        # Edge noise is slightly stronger, giving broken lips instead of a neat plateau.
        edge_noise = rng.choice((-1, 0, 0, 0, 1)) if edge[(x, z)] <= 1 else rng.choice((0, 0, 0, 1))
        tops[(x, z)] = top_y + round(height_delta) + edge_noise

    # Relax top heights so adjacent columns rarely make ugly sheer steps.
    for _ in range(2):
        updated = dict(tops)
        for (x, z), y in tops.items():
            neighbors = [tops[(x + dx, z + dz)] for dx, dz in CARDINAL_2 if (x + dx, z + dz) in tops]
            if neighbors:
                mean = sum(neighbors) / len(neighbors)
                if y > mean + 2:
                    updated[(x, z)] = round(mean + 2)
                elif y < mean - 2:
                    updated[(x, z)] = round(mean - 2)
        tops = updated

    for (x, z), surface_y in tops.items():
        inward = edge[(x, z)]
        depth = min(
            max_depth,
            min_depth + round(inward * 0.75) + rng.randint(0, 2),
        )
        bottom = surface_y - depth

        structure.set((x, surface_y, z), surface)
        placed += 1
        for y in range(surface_y - 1, bottom - 1, -1):
            symbol = soil if y >= surface_y - min(2, depth) else stone
            structure.set((x, y, z), symbol)
            placed += 1

        # Sparse hanging teeth are attached to existing columns and therefore never float.
        if edge[(x, z)] == 0 and rng.random() < hanging_spikes:
            extra = rng.randint(1, 4)
            for y in range(bottom - 1, bottom - extra - 1, -1):
                structure.set((x, y, z), stone)
                placed += 1

    return placed


def prune_small_components(structure: Structure, *, keep_at_least: int = 4) -> int:
    components = connected_components(structure)
    removed = 0
    for component in components:
        if len(component) >= keep_at_least:
            continue
        for pos in component:
            structure.blocks.pop(pos, None)
            removed += 1
    return removed


def replace_random(
    structure: Structure,
    source: str,
    target: str,
    *,
    chance: float,
    seed: int,
    only_exposed: bool = False,
) -> int:
    if target not in structure.palette:
        raise KeyError(f"Unknown target symbol {target!r}")
    rng = random.Random(seed)
    changed = 0
    for pos in list(structure.positions(source)):
        if only_exposed:
            x, y, z = pos
            if all(
                (x + dx, y + dy, z + dz) in structure.blocks
                for dx, dy, dz in NEIGHBORS_6
            ):
                continue
        if rng.random() < chance:
            structure.set(pos, target)
            changed += 1
    return changed


def scatter_on_top(
    structure: Structure,
    symbol: str,
    *,
    count: int,
    seed: int,
    require_below: set[str] | None = None,
) -> int:
    if symbol not in structure.palette:
        raise KeyError(f"Unknown palette symbol {symbol!r}")
    top: dict[tuple[int, int], int] = {}
    for x, y, z in structure.blocks:
        top[(x, z)] = max(y, top.get((x, z), y))
    candidates: list[Pos] = []
    for (x, z), y in top.items():
        below_symbol = structure.get((x, y, z))
        if require_below and below_symbol not in require_below:
            continue
        candidates.append((x, y + 1, z))
    rng = random.Random(seed)
    rng.shuffle(candidates)
    placed = 0
    for pos in candidates[:count]:
        if structure.get(pos) == ".":
            structure.set(pos, symbol)
            placed += 1
    return placed


_CARDINAL_ROTATION = {
    "north": "east",
    "east": "south",
    "south": "west",
    "west": "north",
}

_RAIL_ROTATION = {
    "north_south": "east_west",
    "east_west": "north_south",
    "ascending_north": "ascending_east",
    "ascending_east": "ascending_south",
    "ascending_south": "ascending_west",
    "ascending_west": "ascending_north",
    "south_east": "south_west",
    "south_west": "north_west",
    "north_west": "north_east",
    "north_east": "south_east",
}


def _turn_direction(value: str, turns: int) -> str:
    result = value
    for _ in range(turns % 4):
        result = _CARDINAL_ROTATION.get(result, result)
    return result


def _turn_rail_shape(value: str, turns: int) -> str:
    result = value
    for _ in range(turns % 4):
        result = _RAIL_ROTATION.get(result, result)
    return result


def _rotated_entry(entry: PaletteEntry, turns: int) -> PaletteEntry:
    turns %= 4
    if not turns or not entry.state:
        return entry

    state = dict(entry.state)
    if "facing" in state:
        state["facing"] = _turn_direction(state["facing"], turns)
    if "axis" in state and turns % 2 and state["axis"] in {"x", "z"}:
        state["axis"] = "z" if state["axis"] == "x" else "x"
    if "rotation" in state:
        try:
            state["rotation"] = str((int(state["rotation"]) + 4 * turns) % 16)
        except ValueError:
            pass
    if "shape" in state:
        state["shape"] = _turn_rail_shape(state["shape"], turns)

    return PaletteEntry(entry.block, entry.color, state)


def rotate_y(structure: Structure, turns: int = 1) -> None:
    """Rotate the full authored structure clockwise around its logical origin."""
    turns %= 4
    if turns == 0:
        return

    ox, oy, oz = structure.origin

    def turn_point(pos: Pos) -> Pos:
        x, y, z = pos
        dx, dz = x - ox, z - oz
        for _ in range(turns):
            dx, dz = -dz, dx
        return ox + dx, y, oz + dz

    structure.blocks = {turn_point(pos): symbol for pos, symbol in structure.blocks.items()}
    structure.palette = {
        symbol: _rotated_entry(entry, turns)
        for symbol, entry in structure.palette.items()
    }

    for collection in (structure.entities, structure.markers):
        for item in collection:
            raw = item.get("pos")
            if isinstance(raw, list) and len(raw) == 3:
                turned = turn_point((int(raw[0]), int(raw[1]), int(raw[2])))
                item["pos"] = list(turned)
