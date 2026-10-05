# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).
Each release section is used as-is for the CurseForge, Modrinth and GitHub release notes.

## [0.4.0]

- The theodolite can be triggered by redstone: each time it receives a signal, it surveys the nearest unengraved benchmark, exactly like a right-click.

## [0.3.0]

- Benchmarks can be named by renaming them in an anvil. The name appears above the plate, like a name tag, while you look at it.
- Breaking a named benchmark drops it with its name; the elevation is never kept.
- "Christening" advancement and matching FTB quest (after "Your first benchmark") for the first benchmark renamed in an anvil.

## [0.2.0]

- Brushing an engraved benchmark with a vanilla brush (about 2.5 seconds) erases its elevation, so it can be surveyed again.
- "Clean Slate" advancement and matching FTB quest for the first erased elevation.

## [0.1.1]

- Improved the theodolite model: the tripod legs now meet cleanly under the scope instead of crossing and sticking out.

## [0.1.0]

Initial release for Minecraft 1.21.1 (NeoForge).

- Levelling Benchmark: a wall-mounted plate (waterloggable) that displays its elevation once surveyed.
- Theodolite: a tripod instrument that aims at the nearest unengraved benchmark within 10 blocks, in line of sight, and engraves its elevation relative to the dimension's sea level.
- "Levelled!" advancement for the first survey.
- Optional FTB Quests chapter (English and French), installed once on first launch; can be disabled in `config/bornedex-startup.toml`.
