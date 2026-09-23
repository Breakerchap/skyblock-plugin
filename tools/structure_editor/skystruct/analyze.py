from __future__ import annotations

from collections import Counter, deque
from dataclasses import asdict, dataclass
from statistics import pstdev
from typing import Any

from .model import NEIGHBORS_6, Pos, Structure, add_pos


@dataclass
class ComponentReport:
    size: int
    minimum: Pos
    maximum: Pos
    sample: Pos


@dataclass
class Analysis:
    name: str
    block_count: int
    bounds: tuple[Pos, Pos] | None
    size: Pos
    palette_used: dict[str, int]
    component_count: int
    detached_blocks: int
    singleton_components: int
    top_height_stddev: float
    footprint_fill_ratio: float
    components: list[ComponentReport]
    warnings: list[str]

    def to_json(self) -> dict[str, Any]:
        return asdict(self)


def connected_components(structure: Structure) -> list[set[Pos]]:
    remaining = set(structure.blocks)
    result: list[set[Pos]] = []
    while remaining:
        start = remaining.pop()
        queue = deque([start])
        component = {start}
        while queue:
            pos = queue.popleft()
            for delta in NEIGHBORS_6:
                nxt = add_pos(pos, delta)
                if nxt in remaining:
                    remaining.remove(nxt)
                    component.add(nxt)
                    queue.append(nxt)
        result.append(component)
    result.sort(key=len, reverse=True)
    return result


def analyze(structure: Structure) -> Analysis:
    components = connected_components(structure)
    palette_used = Counter(structure.blocks.values())

    reports: list[ComponentReport] = []
    for component in components[:20]:
        xs = [p[0] for p in component]
        ys = [p[1] for p in component]
        zs = [p[2] for p in component]
        reports.append(ComponentReport(
            size=len(component),
            minimum=(min(xs), min(ys), min(zs)),
            maximum=(max(xs), max(ys), max(zs)),
            sample=min(component),
        ))

    top: dict[tuple[int, int], int] = {}
    for x, y, z in structure.blocks:
        top[(x, z)] = max(top.get((x, z), y), y)

    sigma = pstdev(top.values()) if len(top) > 1 else 0.0
    fill_ratio = 0.0
    if top:
        xs = [x for x, _ in top]
        zs = [z for _, z in top]
        area = (max(xs) - min(xs) + 1) * (max(zs) - min(zs) + 1)
        fill_ratio = len(top) / area if area else 0.0

    detached = sum(len(c) for c in components[1:]) if components else 0
    singletons = sum(1 for c in components if len(c) == 1)
    warnings: list[str] = []

    if len(components) > 1:
        warnings.append(
            f"{len(components)-1} detached component(s) contain {detached} block(s)."
        )
    if singletons:
        warnings.append(
            f"{singletons} singleton block(s) detected; likely accidental floaters unless intentional."
        )
    if len(top) >= 40 and sigma < 0.75:
        warnings.append(
            f"Top surface is very flat (height sigma {sigma:.2f}); add vertical variation."
        )
    if len(top) >= 40 and fill_ratio > 0.88:
        warnings.append(
            f"Footprint fills {fill_ratio:.0%} of its bounding rectangle; silhouette may be boxy."
        )

    column_parts: dict[tuple[int, int], list[tuple[int, str]]] = {}
    for (x, y, z), symbol in structure.blocks.items():
        column_parts.setdefault((x, z), []).append((y, symbol))
    columns = {
        pos: tuple(sorted(stack))
        for pos, stack in column_parts.items()
    }
    if len(columns) >= 50:
        repeated = Counter(columns.values()).most_common(1)[0][1]
        if repeated / len(columns) > 0.35:
            warnings.append(
                f"{repeated}/{len(columns)} columns are exactly identical; terrain may look formulaic."
            )

    return Analysis(
        name=structure.name,
        block_count=len(structure.blocks),
        bounds=structure.bounds(),
        size=structure.size(),
        palette_used=dict(sorted(palette_used.items())),
        component_count=len(components),
        detached_blocks=detached,
        singleton_components=singletons,
        top_height_stddev=round(sigma, 3),
        footprint_fill_ratio=round(fill_ratio, 4),
        components=reports,
        warnings=warnings,
    )


def format_report(report: Analysis) -> str:
    lines = [
        f"Structure: {report.name}",
        f"Blocks: {report.block_count}",
        f"Bounds: {report.bounds}",
        f"Size: {report.size}",
        f"Components: {report.component_count}",
        f"Detached blocks: {report.detached_blocks}",
        f"Top height sigma: {report.top_height_stddev:.3f}",
        f"Footprint fill: {report.footprint_fill_ratio:.1%}",
        "Palette:",
    ]
    for symbol, count in sorted(report.palette_used.items(), key=lambda x: (-x[1], x[0])):
        lines.append(f"  {symbol}: {count}")
    if report.components:
        lines.append("Largest components:")
        for i, component in enumerate(report.components[:8], 1):
            lines.append(
                f"  {i}. {component.size} blocks, {component.minimum} -> {component.maximum}, "
                f"sample {component.sample}"
            )
    if report.warnings:
        lines.append("Warnings:")
        lines.extend(f"  - {warning}" for warning in report.warnings)
    else:
        lines.append("Warnings: none")
    return "\n".join(lines)
