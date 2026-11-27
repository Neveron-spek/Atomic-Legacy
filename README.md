# Atomic Legacy — HBM's Nuclear Tech Port

**Unofficial** port of **HBM's Nuclear Tech Mod** (Minecraft 1.7.10, by **HbmMods**) to **Minecraft 1.21.1 / NeoForge 21.1**.

The goal is a faithful port: original textures, models, sounds, texts, interfaces, recipes, values and behaviour, with only the technical changes the new platform requires. Agreed deviations from the original are listed in [`AGENTS.md`](AGENTS.md).

> **Status: alpha.** Most content is ported and checked by automated audits, but very little has been tested in game yet. Use a separate world, keep backups, and expect bugs. Worlds made with builds before the mod id change (`reinhardtshbm` → `atomiclegacy`) are not compatible.

## Credits

- **Original mod:** [HBM's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT) by **HbmMods** and its contributors. Almost all content — textures, models, sounds, texts and game mechanics — is theirs. Reference commit: `936c60c4c07dc10aef81f75d800ad04282f08e62`.
- **Origin of this port:** the project started as a fork of [Reinhardt's HBM NeoPort](https://github.com/SakurakoujiSaika1926/Reinhardt-s-HBM-NeoPort) by **SakurakoujiSaika1926** (commit `b9b49ea`). Since then the code has been largely rewritten: about 80 % of the Java code is new, about 16 % of the base's files were reworked, and about 4 % remain unchanged.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.250 or newer
- Java 21

Optional integrations: Create, Immersive Engineering, Applied Energistics 2, TACZ.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/atomiclegacy-<version>.jar`.

## Reporting bugs

Open an issue at https://github.com/Neveron-spek/Atomic-Legacy/issues and include the Minecraft and NeoForge versions, the list of other mods, steps to reproduce and, for crashes, the file from `crash-reports/`.

## License

This project is free software under the **GNU General Public License v3** (see [`LICENSE`](LICENSE)). It includes code from HBM's Nuclear Tech Mod, licensed under the GNU LGPL v3 (see [`LICENSE.LESSER`](LICENSE.LESSER)). Modified versions must stay under the GPL and keep the credits above.
