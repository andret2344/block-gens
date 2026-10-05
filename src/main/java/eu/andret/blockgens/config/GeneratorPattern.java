package eu.andret.blockgens.config;

import eu.andret.blockgens.util.RandomCollection;
import org.bukkit.inventory.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record GeneratorPattern(@NotNull String name,
		long delay,
		@NotNull NamedItem generatorItem,
		@NotNull NamedItem generatedItem,
		@NotNull RandomCollection<NamedItem> dropItems,
		@NotNull ExplosionMode generatorExplosion,
		@NotNull ExplosionMode generatedExplosion,
		@Nullable ShapedRecipe recipe) {
}
