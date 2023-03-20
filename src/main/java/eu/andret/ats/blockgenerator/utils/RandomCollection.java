/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Value
public class RandomCollection<E> {
	private static final Random RANDOM = new Random();

	@Getter(AccessLevel.NONE)
	Map<E, Double> elements = new HashMap<>();

	public void add(final E item, final double weight) {
		if (weight <= 0) {
			throw new IllegalArgumentException("Weight must be positive! Provided: " + weight);
		}
		elements.put(item, weight);
	}

	public E next() {
		// solution from https://stackoverflow.com/a/11926952/4675794
		return elements.entrySet().stream()
				.map(entry -> Map.entry(entry.getKey(), -Math.log(RANDOM.nextDouble()) / entry.getValue()))
				.min(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	public List<E> getItems() {
		return new ArrayList<>(elements.keySet());
	}

	public boolean isEmpty() {
		return elements.size() == 0;
	}
}
