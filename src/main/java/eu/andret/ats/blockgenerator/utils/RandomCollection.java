/*
 * Copyright Andret Tools System (c) 2018-2023. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class RandomCollection<E> {
	private static final Random RANDOM = new Random();

	private final Map<E, Double> elements = new HashMap<>();

	public void add(@NotNull final E item, final double weight) {
		if (weight <= 0) {
			throw new IllegalArgumentException("Weight must be positive! Provided: " + weight);
		}
		elements.put(item, weight);
	}

	@Nullable
	public E next() {
		// solution from https://stackoverflow.com/a/11926952/4675794
		return elements.entrySet()
				.stream()
				.map(entry -> Map.entry(entry.getKey(), -Math.log(RANDOM.nextDouble()) / entry.getValue()))
				.min(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	@NotNull
	public List<E> getItems() {
		return new ArrayList<>(elements.keySet());
	}

	public boolean isEmpty() {
		return elements.size() == 0;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof final RandomCollection<?> that)) {
			return false;
		}
		return Objects.equals(elements, that.elements);
	}

	@Override
	public int hashCode() {
		return Objects.hash(elements);
	}

	@Override
	public String toString() {
		return "RandomCollection{" +
				"elements=" + elements +
				'}';
	}
}
