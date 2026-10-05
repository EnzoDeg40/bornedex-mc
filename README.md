# Bornedex

A NeoForge mod for Minecraft 1.21.1 about surveying: place levelling benchmarks on walls, then use a theodolite to engrave their elevation.

## Features

- **Levelling Benchmark**: a small plate that attaches to the side of a solid block (walls only). Can be waterlogged. Rename it in an anvil to give it a name, shown above the plate when you look at it; a named benchmark keeps its name when broken (the elevation is never kept).
- **Theodolite**: a tripod-mounted instrument placed on the ground. Right-click it to target the nearest unengraved benchmark within 10 blocks and in line of sight. The scope aims at it, fires a particle laser and engraves the benchmark's elevation relative to the dimension's sea level. Powering it with redstone does the same as a right-click, once each time the signal turns on.
- **Erasing an elevation**: hold right-click on an engraved benchmark with a vanilla brush for about 2.5 seconds to erase its elevation (uses 1 durability), so it can be surveyed again.
- **FTB Quests chapter**: a Bornedex quest chapter (English and French) is installed into `config/ftbquests/quests` on first launch only. Existing quest files are never overwritten; in a pack that already has quests, the chapter's texts are merged into its existing lang files. Set `installDefaultQuests = false` in `config/bornedex-startup.toml` to disable the install, or delete `config/bornedex/quests_installed` to run it again.

## Recipes

| Item | Recipe |
|------|--------|
| Levelling Benchmark | 1 copper ingot surrounded by 4 iron nuggets (plus shape) |
| Theodolite | 1 spyglass on top of 2 sticks |

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x

Optional:

- FTB Quests (plus FTB Library and FTB Teams): the quest chapter is installed only when FTB Quests is present

## Development

```bash
./gradlew runClient
```

```bash
./gradlew build
```

The built jar ends up in `build/libs/`.

## Releasing

1. Bump `mod_version` in `gradle.properties` and add a matching `## [x.y.z]` section to `CHANGELOG.md`.
2. Commit, then push a `v`-prefixed tag, e.g. `git tag v0.1.0 && git push origin v0.1.0`.
3. The `Release` workflow publishes the jar to CurseForge, Modrinth (if `modrinth_project_id` is set) and GitHub Releases.

Versions containing `alpha` or `beta` are published with that release type. Running `./gradlew publishMods` locally without `CURSEFORGE_TOKEN` is a dry run (output in `build/publishMods/`).

## License

[MIT](LICENSE)
