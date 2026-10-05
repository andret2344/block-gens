package eu.andret.blockgens.config;

/**
 * What happens to a generator block or a generated block caught in an explosion.
 */
public enum ExplosionMode {
	/**
	 * The block is immune to explosions.
	 */
	PREVENT,
	/**
	 * The block is destroyed and drops like a regular block, with the explosion's yield as the chance.
	 */
	DROP,
	/**
	 * The block is destroyed and drops nothing.
	 */
	REMOVE
}
