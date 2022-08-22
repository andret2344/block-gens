# atsBlockGenerator

The plugin allows to craft the generator block, that will generate another (or the same - you decide!) block above
itself as in the infinite loop with certain delay.

## Details

You can define any amount of own generators in the `config.yml` file. The generator's name **has to** be unique in a
plugin. You are able to configure the generatorItem block as a `ItemStack`, so you can set it's material, name and lore
as you wish. The same situation is with the blocks that are generated above the generators, you have to set the material
and can (but don't have to) set the name and lore. You can also define own crafting for each generatorItem (crafting
recipes **have to** be unique in the whole plugin!), and define the regen delay set in ticks (20 ticks = 1s, 100 ticks =
5s).

An example config that includes 2 different generators is present as a default config.
