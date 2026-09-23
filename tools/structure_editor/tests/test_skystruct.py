from __future__ import annotations

import gzip
import json
from pathlib import Path
import tempfile
import unittest

from skystruct.analyze import analyze
from skystruct.model import PaletteEntry, Structure
from skystruct.nbt import export_structure
from skystruct.ops import organic_island, rotate_y
from skystruct.render import render_turntable


class SkyStructTests(unittest.TestCase):
    def basic(self) -> Structure:
        return Structure(
            "Test",
            {
                "g": PaletteEntry("minecraft:grass_block", "#6f9f45"),
                "d": PaletteEntry("minecraft:dirt", "#8a6545"),
                "s": PaletteEntry("minecraft:stone", "#818181"),
                "l": PaletteEntry("minecraft:oak_log", "#73532f", {"axis": "y"}),
            },
        )

    def test_json_round_trip_preserves_negative_coordinates_and_origin(self) -> None:
        structure = self.basic()
        structure.origin = (2, 0, -3)
        structure.set((-4, -2, 6), "s")
        structure.set((0, 0, 0), "g")
        structure.entities.append({"id": "minecraft:goat", "pos": [1, 1, 1]})

        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "test.skystruct.json"
            structure.save(path)
            loaded = Structure.load(path)

        self.assertEqual(structure.blocks, loaded.blocks)
        self.assertEqual(structure.origin, loaded.origin)
        self.assertEqual(structure.entities, loaded.entities)

    def test_analysis_finds_detached_blocks(self) -> None:
        structure = self.basic()
        structure.set((0, 0, 0), "s")
        structure.set((1, 0, 0), "s")
        structure.set((20, 20, 20), "s")
        report = analyze(structure)
        self.assertEqual(2, report.component_count)
        self.assertEqual(1, report.detached_blocks)
        self.assertTrue(report.warnings)

    def test_organic_island_is_seeded_connected_and_not_flat(self) -> None:
        a = self.basic()
        b = self.basic()
        kwargs = dict(
            radius=10,
            top_y=0,
            surface="g",
            soil="d",
            stone="s",
            seed=1337,
            fullness=0.70,
        )
        organic_island(a, **kwargs)
        organic_island(b, **kwargs)
        self.assertEqual(a.blocks, b.blocks)

        report = analyze(a)
        self.assertEqual(1, report.component_count)
        self.assertGreater(report.top_height_stddev, 0.4)
        self.assertLess(report.footprint_fill_ratio, 0.9)

    def test_rotation_moves_geometry_and_common_block_states(self) -> None:
        structure = Structure(
            "Rotate",
            {
                "f": PaletteEntry("minecraft:oak_stairs", "#997044", {"facing": "north", "half": "bottom"}),
                "l": PaletteEntry("minecraft:oak_log", "#73532f", {"axis": "x"}),
            },
        )
        structure.origin = (0, 0, 0)
        structure.set((0, 0, -2), "f")
        structure.set((2, 0, 0), "l")
        structure.entities.append({"id": "minecraft:goat", "pos": [1, 0, 0]})
        structure.markers.append({"kind": "spawn", "pos": [0, 0, -1]})

        rotate_y(structure, 1)

        self.assertEqual("f", structure.get((2, 0, 0)))
        self.assertEqual("l", structure.get((0, 0, 2)))
        self.assertEqual("east", structure.palette["f"].state["facing"])
        self.assertEqual("z", structure.palette["l"].state["axis"])
        self.assertEqual([0, 0, 1], structure.entities[0]["pos"])
        self.assertEqual([1, 0, 0], structure.markers[0]["pos"])

    def test_nbt_export_is_gzip_structure_and_tracks_anchor_offset(self) -> None:
        structure = self.basic()
        structure.origin = (0, 0, 0)
        structure.set((-3, -2, -1), "s")
        structure.set((2, 1, 4), "g")

        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp) / "structure.nbt"
            sidecar = export_structure(structure, output)
            raw = gzip.decompress(output.read_bytes())
            placement = json.loads(output.with_suffix(".placement.json").read_text())

        self.assertEqual([-3, -2, -1], sidecar["placement_offset"])
        self.assertEqual([-3, -2, -1], placement["placement_offset"])
        self.assertIn(b"palette", raw)
        self.assertIn(b"blocks", raw)
        self.assertIn(b"DataVersion", raw)
        self.assertIn(b"minecraft:grass_block", raw)

    def test_renderer_produces_four_real_pngs(self) -> None:
        structure = self.basic()
        structure.fill_box((-2, -2, -2), (2, 0, 2), "s")
        structure.fill_box((-1, 1, -1), (1, 2, 1), "g")

        with tempfile.TemporaryDirectory() as tmp:
            paths = render_turntable(structure, tmp, tile=8)
            self.assertEqual(4, len(paths))
            for path in paths:
                data = path.read_bytes()
                self.assertTrue(data.startswith(b"\x89PNG\r\n\x1a\n"))
                self.assertGreater(len(data), 100)


if __name__ == "__main__":
    unittest.main()
