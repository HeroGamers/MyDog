# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MyDog is a Bukkit plugin that extends tamed wolves ("dogs") with names, levels/XP, stats, teleport-follow, revival, trading and attack/defend modes. It compiles against `paper-api` 26.3 but **must keep running on Spigot**: use only Bukkit/Spigot API. Where Paper has deprecated a String-based method that Spigot still needs (`setCustomName(String)`, `getDisplayName()`, `getDescription()`), keep the call in one place with `@SuppressWarnings("deprecation")` and a comment. Vault is an optional soft dependency (economy only).

## Build

```bash
nix develop                    # devshell with JDK 25 + Maven (flake.nix); or prefix commands with `nix develop -c`
mvn clean package              # default goal; runs the tests and builds target/MyDog-<version>.jar
mvn test -Dtest=LegacyTextTest # a single test class (append #method for one test)
```

- Java 25 (`maven.compiler.release`). CI (`.github/workflows/build.yml`) builds on JDK 25 for every push and PR. Pushing a tag `v<pom version>` (e.g. `v0.3.0`) publishes a GitHub release with the jar.
- Tests only cover code that runs without a server: migrations, `LegacyText`, `LevelTable`, and `MessageKeysTest` (every message key used in code must exist in the bundled `messages.yml`). Anything touching `Registry`, entities or the scheduler can only be checked on a real server.
- Adventure (MiniMessage + legacy serializer) and bStats are shaded and relocated to `dk.fido2603.mydog.libs.*`. Never pass Adventure objects to Bukkit/Paper API, and never call Adventure methods on Bukkit objects (e.g. `Sound#key()`): after relocation those are different classes. Text is turned into legacy `§` strings with `Messages.legacy` before it reaches Bukkit.
- Only `plugin.yml` is Maven-filtered (for `${project.version}`).

## Architecture

All code is under `dk.fido2603.mydog`. `MyDog` (the `JavaPlugin`) only wires things together. Dependencies are passed through constructors; there are no static getters. Settings are passed as a `Supplier<Settings>`, so `/mydog reload` takes effect everywhere just by swapping one object.

- **`config/`**: `Settings` is an immutable record tree read from `config.yml` once per (re)load, with validation that logs bad values and falls back to defaults. `LevelTable` holds the levels. To add a setting, add it to the bundled `src/main/resources/config.yml` (with a comment) and to `Settings`. `MyDog.loadSettings` writes missing keys into existing configs, except inside `levels.table` and `levels.experience-per-kill`, which admins may shorten.
- **`migration/`**: `VersionedYaml` loads a file with a version key (`config-version`, `messages-version`, `data-version`), backs it up as `<file>.v<n>.bak` and chains `Migration`s up to the current version. To change the shape of a file, bump its version constant (`MyDog.CONFIG_VERSION`, `Messages.MESSAGES_VERSION`, `YamlDogRepository.DATA_VERSION`) and add a `Migration` from the previous version; don't change how old versions are read. `ConfigV1ToV2` and `DogsV1ToV2` upgrade files from MyDog 0.2.x, and also move customized messages to `messages.yml`, converting them with `LegacyText` (`&` codes to MiniMessage).
- **`message/Messages`**: reads `messages.yml` (MiniMessage; the bundled file supplies defaults for missing keys). Use `messages.send(sender, "key", tags...)` with the `Messages.dog(dog)` and `Messages.text(...)` placeholder helpers. A message can be a list of lines, and `''` sends nothing. No user-visible text belongs in Java code.
- **`objects/Dog`**: a plain mutable data object, keyed by the wolf entity's UUID, plus a per-owner integer **identifier** that players use in commands. It keeps the old getter names because `MyDogAPI` exposes it to other plugins.
- **`storage/`**: `DogRepository` / `YamlDogRepository`. All dogs live in memory. `markChanged` flags a change, and saving snapshots the data on the main thread, then writes `dogs.yml` atomically on a single background thread (autosave every `saving.autosave-minutes`, or on every change with `saving.instant`). Never write `dogs.yml` any other way.
- **`service/`**:
  - `DogService` owns every change to a dog and keeps the data, the wolf's nametag and its level-based health/damage in sync. New dog behavior goes here, not in listeners or commands.
  - `TreatService` makes dog treats (a bone tagged `mydog:treat` with the breed or personality they give; always recognize items by that tag, never by name) and re-registers their recipes from `breeds.treats` on every reload. `config/TreatKind` (breed or personality) holds all wolf variant registry lookups, behind one `@SuppressWarnings` since Paper deprecates the Spigot registry API. A dog's look is stored in `dogs.yml` under `appearance` and reapplied on revive.
  - `TradeService` holds pending trades. `TeleportService` brings pets to their owner onto a safe spot; experimental mode only defers the teleport.
  - `Permissions.has` lets operators and the console do everything. `Economy` wraps Vault and is looked up on each use: MyDog loads at `STARTUP`, before most economy plugins register.
- **`listener/`** turn events into service calls: taming, breeding and deaths (`TamingListener`); right-click petting, dye, food and name tag (`InteractListener`); protection, lifesteal, kill XP via `EntityDeathEvent#getDamageSource` and low-health warnings (`CombatListener`); entity load/unload, player teleport and portals (`WorldListener`).
- **Dog arguments** in commands are `<id>` (one of your own dogs) or `<owner>:<id>` (a dog shared with you, or any dog for admins with `mydog.admin.others`). Resolve them with `SubCommand.dog/livingDog/livingDogs` and pick `Access.OWNER` (giving a dog away, IDs, levels) or `Access.SHARED` (looking after it: co-owners may too). The dog's wolf always belongs to its main owner and only follows them.
- **`command/`**: `CommandRouter` handles `/mydog` (aliases `md`, `dog`, `dogs`); each subcommand is its own `SubCommand` class. The router checks the permission, player-only and `isAvailable()` before `execute`, and turns a thrown `CommandException(messageKey)` or `UsageException` into a message. To add a subcommand: write the class, register it in `MyDog.registerCommands`, add `command.help.<name>` and its messages to `messages.yml`, and add its permission to `plugin.yml` and the README's permission list.
- **`api/`** is the public API (`MyDogApi`, `DogInfo`), registered in Bukkit's services manager on enable. `service/MyDogApiService` implements it on top of `DogService`. Its signatures may only use Java, Bukkit and `api` types (`ApiSurfaceTest` checks this): never internal classes, and never Adventure, which is relocated in the jar. Keep it backwards compatible. `MyDogAPI` is the old, deprecated API.
- **`task/`**: `DistanceTask` (every 10 s, brings back dogs that wandered off) and `AttackModeTask` (every 2 s, points attack-mode dogs at monsters near their owner).
