# BlockGens changelog

## Unreleased

## 1.0.0 - 2026-10-05

BlockGens is the successor of atsBlockGenerator 2.4.3. The changes below are relative to it.

### Added

- Each generator can set what an explosion does to the generator block and to the generated block, under
  `explosion.generator` and `explosion.generated`: `prevent` (immune), `drop` (dropped like a regular block, with the
  explosion's yield as the chance) or `remove` (destroyed without a drop). Defaults: `drop` for the generator, `remove`
  for the generated block. A generated block destroyed by an explosion comes back after the delay.
- The `crafting` section of a generator is optional. Without it the generator has no recipe and can only be given with
  the command, e.g. for servers that sell generators in a shop.
- Pistons cannot move generators or generated blocks, and mobs cannot change them (endermen, withers, silverfish).
- While a generated block re-spawns, nothing can be put in its spot: no placed blocks, no liquids from buckets, no
  blocks moved by pistons or landing there. Before, the re-spawn replaced whatever was there.
- `/bg give <player> <generator> [amount]` gives generators to another player and works from the console, e.g. for
  shop plugins. The amount is 1 by default and at most 2304. Permission `blockgens.give`.
- `/bg reload` loads the config again without restarting the server. An invalid config is not applied and the command
  says what is wrong with it. Permission `blockgens.reload`.

### Changed

- Renamed from atsBlockGenerator to BlockGens. The plugin folder is `plugins/BlockGens`, the command is `/blockgens`
  with the `/bg` alias (`/blockgenerator` and `/generator` are gone), and the permissions are `blockgens.get`,
  `blockgens.give` and `blockgens.reload` (all part of `blockgens.*`), given to operators by default.
- Requires Paper (or a fork such as Purpur) 26.2 or newer and Java 25. Spigot is no longer supported.
- Names and lore in `config.yml` use the [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/) format,
  e.g. `<dark_purple>Creates food!` instead of `&5Creates food!`. Names are no longer italic and cannot be changed with
  an anvil.
- A generator block cannot use a material affected by gravity, such as sand. Such a config fails to load.
- The recipe of a generator is named after the generator in the config instead of its display name.
- Commands are handled by Lamp. A name containing spaces can be given in double quotes, e.g.
  `/bg generator "Stone generator"`, the help list shows a description for each subcommand, and messages about an
  unknown generator, generated block or drop item are shown in red.
- The plugin is licensed under the Apache License 2.0. Redistributions have to keep the `NOTICE` file, which the jar
  carries as `META-INF/NOTICE-block-gens`.

### Removed

- The server no longer downloads the `org.jetbrains:annotations` library when loading the plugin.

### Fixed

- Placed generators survive a server restart. They are stored in the chunk data of the world.
- A generated block that did not re-spawn, because the server stopped before the delay passed, comes back when its
  chunk is loaded.
- Breaking a generator while its generated block was re-spawning no longer brings the generated block back in the air.
- A regular block placed where a destroyed generator used to be no longer acts as that generator and no longer drops
  the generator item when broken.
- A generator without a `crafting` section no longer stops the whole plugin from loading.
- A generator placed from a stack of more than one item works as a generator, not as a regular block.
- A block put above a generator while the generated block is re-spawning is broken as a regular block instead of
  dropping items from the drop table.
- A generator placed where another plugin, such as a region protection, does not allow it no longer replaces the
  block above it and is no longer stored as a generator.
- Where another plugin, such as a region protection, does not allow breaking blocks, breaking a generator no longer
  drops it, and breaking a generated block no longer gives drops.
- A generator placed under a block no longer destroys that block (and the contents of a chest). The generator waits
  and starts once the block above is gone - broken, burnt, exploded, decayed or otherwise destroyed.
- Items given with a command no longer disappear when the inventory is full - they are dropped at the player's feet.
- An unknown or missing material in the config, also in a crafting mapping, stops the plugin with a message naming the
  material and where it is. The same goes for a drop that is not defined in `items`, and for a drop without a positive
  `weight` or with a `count` below 1. Before, an item failed with an unreadable error and a crafting ingredient was skipped
  without a word.
- Recipes are removed when the plugin is disabled, so enabling it again does not register them twice.
- Drop items keep the name and lore set in `items`. A drop without `count` drops one item instead of nothing.
