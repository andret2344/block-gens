# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

BlockGens (successor of atsBlockGenerator) - a Paper plugin. Players craft a "generator" block; placing it spawns a
"generated" block above it. Breaking the generated block drops a weighted-random item and re-spawns the block after
`delay` ticks. Everything (generators, blocks, items, recipes, drops, explosion modes) is defined in
`src/main/resources/config.yml`. Paper-only on purpose: Spigot is not supported, Paper API is fine to use.

## Commands

```sh
./gradlew build                      # compile, test, coverage check, shadowJar -> build/libs/BlockGens-<version>.jar
./gradlew test                       # TestNG tests, then jacoco coverage verification (min 80%)
./gradlew test --tests eu.andret.blockgens.config.ConfigLoaderTest -x jacocoTestCoverageVerification
./gradlew test --tests "eu.andret.blockgens.BlockGensListenerTest.placeGenerator" -x jacocoTestCoverageVerification
```

- `test` is `finalizedBy` `jacocoTestCoverageVerification`, so running a subset of tests fails the 80% coverage rule
  unless that task is excluded with `-x`.
- Java toolchain, Paper API and all dependency versions live in `gradle/libs.versions.toml`. Project `version`,
  `group`, `artifact` and `minecraftVersions` (what releases are marked as supporting) live in `gradle.properties`;
  `${version}` is expanded into `plugin.yml`. MockBukkit (`mockbukkit-v26.2`) must match the Paper API version.
- The `jar` task is disabled on purpose: the shadow jar is the only jar. There is no Maven publishing.

## Architecture

Packages under `eu.andret.blockgens`: the root holds the plugin, its command and its listener; `config` the loader
and what it produces (`GeneratorPattern`, `NamedItem`, `ExplosionMode`); `storage` the persistence of placed
generators; `command` the Lamp argument types and condition; `util` generic helpers.

- `BlockGensPlugin` - entry point. `applyPatterns` swaps the pattern list and the registered recipes in one go; it is
  used on enable, by `reload` (which parses the file into a fresh `YamlConfiguration` first, so an invalid config -
  YAML or content - throws and changes nothing) and on disable (removes the recipes). On enable: saves default config,
  loads `GeneratorPattern`s via
  `ConfigLoader`, registers the listener and runs `restore` on every already loaded chunk (they never fire
  `ChunkLoadEvent` for the plugin), registers commands with Lamp, starts bStats. Holds the pattern list (mutable, tests
  add patterns to it), the `GeneratorStore` and a `Map<Block, Integer>` of pending re-spawn task IDs, keyed by the
  **generator** block (not the generated one), in memory only. The task removes its own entry when it runs, so an
  entry means "re-spawn pending".
- `ConfigLoader` - parses `config.yml`. `blocks` and `items` define named `ItemStack`s (material, MiniMessage `name`
  set as the item name - not the custom name, so no italics and no anvil renaming - and MiniMessage `lore`).
  `generators` reference them (`blocks.generator`, `blocks.generated`, `drops.<item>` with `weight`/`count`) plus
  `delay`, optional `crafting` and optional `explosion.generator`/`explosion.generated` (`ExplosionMode`). Generator
  and generated must be blocks, the generator must not have gravity. The recipe key is the sanitized generator name.
  It takes the config as a parameter and builds `GeneratorPattern.recipe` but never registers it - registering is the
  plugin's job. Config errors throw `IllegalArgumentException` with the config path in the message and stop the plugin
  from loading.
- `GeneratorStore` - placed generators live in the chunk's `PersistentDataContainer`, one entry per generator:
  key `<plugin namespace>:generator/<x>/<y>/<z>`, value = pattern name. The namespace is the plugin name, so renaming
  the plugin orphans every placed generator.
- `BlockGensListener` - runtime behaviour. A stored entry only counts when the block's material still equals the
  pattern's generator material (`findPattern`); a "generated block" is a block above such a generator with the
  generated material and no pending re-spawn (`findGeneratedPattern`) - during a re-spawn anything in that spot was
  put there by a player and must break as a regular block. Generator items are matched with `isSimilar`, never
  `equals` (which compares the amount). Entries are not cleaned up when a generator disappears outside our
  events (WorldEdit), so this check is what stops a plain block in that spot from dropping the generator item; placing
  a non-generator block also clears the entry. The spot above a generator is reserved (`isReserved`), whatever is in
  it: placing blocks, emptying buckets, pistons moving blocks or their head into it and `EntityChangeBlockEvent` there
  are cancelled, so the re-spawn never overwrites anything. The generated block is only ever put into an empty spot
  (`fill`: `isAir()` and no pending re-spawn): a generator placed under a block waits, and the removal events of the
  block above (break, burn, decay, fade, Paper's `BlockDestroyEvent`, explosions) call `fillLater`, which checks one
  tick later because they fire before the block is gone. Anything else is caught by `restore` on chunk load. For a retracting piston `getDirection()` is where the
  piston faces; the pulled blocks move the opposite way. Every handler of a cancellable event ignores cancelled events, so region
  protections keep working: `place` runs at `MONITOR`, the break handlers - which cancel the event themselves to stop
  the vanilla drop - at `HIGH`, after protections cancelling at `NORMAL` or lower; cancelling a placement ourselves
  happens at normal priority (`placeOnReserved`). Handlers: place, two `BlockBreakEvent`s (generated: cancel,
  clear, re-spawn, drop in survival/adventure; generator: drop the item, remove), piston extend/retract and
  `EntityChangeBlockEvent` cancelled for generators and reserved spots, entity/block explosions take both out of the
  block list and apply the pattern's `ExplosionMode` (drop chance = explosion yield), `ChunkLoadEvent` -> `restore`
  (puts back an `AIR` generated block unless a re-spawn is pending, forgets entries whose block no longer matches,
  keeps entries of patterns missing from the config).
- `RandomCollection` (`util`) - weighted random pick used for drops.
- `command/` - Lamp (`revxrsal.commands`) command `/blockgens` (alias `bg`) is registered in code, not in `plugin.yml`.
  `GeneratorItem`/`GeneratedItem`/`DropItem` are thin wrapper records so Lamp can resolve three different argument
  types; all three are parsed by the generic `NamedItemParameterType`, wired in `BlockGensPlugin#setUpCommand`.
  Lamp needs `-parameters` (set in `build.gradle.kts`). Lamp runs a command that matched only the start of the input,
  so `PlaceholderCondition` stops the `@CommandPlaceholder` help from swallowing the errors of the subcommands.
  `give` takes a `CommandSender` (the console, for shop plugins), a `Player` argument and an optional amount
  (`@Default("1") @Range`, capped at a full inventory). Items are given as copies (`asQuantity`; `addItem` may change
  the amount of the stack it gets) and what does not fit is dropped at the player's feet in stacks of at most the max
  stack size.
  MockBukkit's `addItem` also fills the armour slots, so a "full inventory" test must fill all of `getContents()`.
- bStats and Lamp are shaded and relocated under `eu.andret.blockgens.*` by `shadowJar`. bStats refuses to start
  unrelocated, so tests run with `bstats.relocatecheck=false`.

## Tests

- TestNG + AssertJ + MockBukkit, `// given` / `// when` / `// then` structure. No Mockito.
- Tests extend `helper/PluginTest`, which starts `MockBukkit.mock()`, loads the real plugin with the shipped
  `config.yml` and adds a world before every test. `placeGenerator` sets up a generator with its generated block.
- Events are built by hand and fired with `server.getPluginManager().callEvent`; block breaking goes through the
  test's own `breakBlock` (MockBukkit's `simulateBlockBreak` and `assertSaid` are deprecated for removal).
- The scheduler only runs on `server.getScheduler().performTicks(n)`. MockBukkit worlds start at Y=0.
- `ConfigLoaderTest` loads other configs with `plugin.getConfig().loadFromString(...)` and a new `ConfigLoader`;
  generator names must differ from the shipped ones, whose recipes are already registered.

## Conventions

- Tabs for indentation, LF line endings (`.gitattributes`), no wildcard imports, no `var`, `final` on parameters and
  locals, `@NotNull`/`@Nullable` from `org.jetbrains.annotations` on everything.
- User-facing changes go under `## Unreleased` in `CHANGELOG.md` (`org.jetbrains.changelog` format); release notes
  are extracted from it.
- Apache 2.0: the jar's `META-INF` carries `LICENSE`/`NOTICE` renamed with the `-block-gens` suffix so shaded
  libraries' files do not overwrite them.
- Build script reads project properties through `project.group`/`project.version` and
  `providers.gradleProperty(...)`, never `project.properties[...]` (deprecated, fails in Gradle 10).
- CI is GitHub Actions. `build.yml` builds every push and PR and uploads the JaCoCo XML report
  (`build/reports/jacoco/test/jacocoTestReport.xml`) to Codecov with the `CODECOV_TOKEN` secret. Releasing takes
  two workflows - releases are never made by pushing tags:
  - `release.yml` is run by hand from the Actions tab on `main` with a `version` input. It sets the version in
    `gradle.properties`, builds and tests, runs `patchChangelog` (fails when "Unreleased" is empty), commits both files
    as `Released <version>.` by github-actions, and creates a **draft** GitHub release of that commit with the jar.
  - `publish.yml` runs when that draft is published (which creates the `v<version>` tag). It downloads the jar from the
    release and uploads that same file, with the release description as the changelog, to Modrinth (`mc-publish`,
    project ID `WZxrLw1e` in the workflow, `MODRINTH_TOKEN` secret) and Hangar (`hangarPublish` in `build.gradle.kts`
    with `-PhangarJar`, project `andret2344/BlockGens`, `HANGAR_API_TOKEN` secret).

  So user-facing changes only go under "Unreleased" - never bump the version or add version sections by hand. Each
  upload is skipped without its secret; a token is passed only to the step that needs it. A version with a `-suffix`
  is a pre-release (Modrinth beta, Hangar "Snapshot" channel).
