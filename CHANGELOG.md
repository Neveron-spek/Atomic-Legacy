# Changelog

## 0.2.0-alpha — first release as Atomic Legacy

First public test build under the new name. The project continues the earlier port (mod id `reinhardtshbm`), so **worlds from earlier builds are not compatible** — start a new world.

### Changed
- Renamed to **Atomic Legacy (HBM's NTM Port)**; mod id `atomiclegacy`, package `com.atomiclegacy`.
- Old `hbmConfig` / `hbmRecipes` files with the old id are migrated automatically on load.

### Ported and aligned with HBM's NTM 1.7.10 (commit 936c60c)
- Radiation: chunk radiation, contamination sources, crater biomes, uranium ore radon, Geiger counter readouts.
- Worldgen: ores, structures and their biome rules, Soyuz capsule, oil deposits; mobs with guns in polluted chunks.
- Mobs: creepers, crabs, FBI, RAD beast, UFO, chopper, BOT Prime, Mask Man, glyphids and hives; old AI with the original path finder.
- Machines: heat exchangers, boilers, ICF laser, breeding reactor, legacy furnaces and batteries, custom machines, recipe selector screens.
- Weapons: B92, power-armor weapons, ammo and casing bags, debug guns.
- Armor: 3D suits on the player, enchantment glint, no9 flashlight, player cosmetics.
- Interfaces: slot layouts of 165 machines, gauges, tank rendering, tooltips, buttons.
- Recipes: about 2,230 crafting/smelting recipes checked against the original; non-original recipes removed.
- Item renders: original renderers for machines, bedrock ores, plants and many blocks.

### Known deviations from the original
See [`AGENTS.md`](AGENTS.md): radiation is kept when returning from the End, anthrax mutation uses the intended weighted output, legacy batteries connect to cables, a few rendering and platform limits.

### Known issues
- Very little has been tested in game yet; expect bugs.
- Some 1.21 limitations (off-hand, recipe patterns with an empty row).
