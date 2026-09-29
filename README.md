# Bornedex

A NeoForge mod for Minecraft 1.21.1 about surveying: place levelling benchmarks on walls, then use a theodolite to engrave their elevation.

## Features

- **Levelling Benchmark**: a small plate that attaches to the side of a solid block (walls only). Can be waterlogged.
- **Theodolite**: a tripod-mounted instrument placed on the ground. Right-click it to target the nearest unengraved benchmark within 10 blocks and in line of sight. The scope aims at it, fires a particle laser and engraves the benchmark's elevation relative to the dimension's sea level.
- **FTB Quests chapter**: a Bornedex quest chapter (English and French) is installed into `config/ftbquests/quests` on first launch only. Existing quest files are never overwritten; in a pack that already has quests, the chapter's texts are merged into its existing lang files. Set `installDefaultQuests = false` in `config/bornedex-startup.toml` to disable the install, or delete `config/bornedex/quests_installed` to run it again.

## Recipes

| Item | Recipe |
|------|--------|
| Levelling Benchmark | 1 copper ingot surrounded by 4 iron nuggets (plus shape) |
| Theodolite | 1 spyglass on top of 2 sticks |

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- FTB Quests (plus FTB Library and FTB Teams)

## Development

```bash
./gradlew runClient
```

```bash
./gradlew build
```

The built jar ends up in `build/libs/`.

## License

All Rights Reserved.
