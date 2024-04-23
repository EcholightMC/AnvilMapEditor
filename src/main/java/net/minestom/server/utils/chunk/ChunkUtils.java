package net.minestom.server.utils.chunk;

public class ChunkUtils {

	private ChunkUtils() {}

	/**
	 * Gets the chunk index of chunk coordinates.
	 * <p>
	 * Used when you want to store a chunk somewhere without using a reference to the whole object
	 * (as this can lead to memory leaks).
	 *
	 * @param chunkX the chunk X
	 * @param chunkZ the chunk Z
	 * @return a number storing the chunk X and Z
	 */
	public static long getChunkIndex(int chunkX, int chunkZ) {
		return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
	}

	/**
	 * Gets the block index of a position.
	 *
	 * @param x the block X
	 * @param y the block Y
	 * @param z the block Z
	 * @return an index which can be used to store and retrieve later data linked to a block position
	 */
	public static int getBlockIndex(int x, int y, int z) {
		x = x % 16; // chunk size x
		z = z % 16; // chunk size z

		int index = x & 0xF; // 4 bits
		if (y > 0) {
			index |= (y << 4) & 0x07FFFFF0; // 23 bits (24th bit is always 0 because y is positive)
		} else {
			index |= ((-y) << 4) & 0x7FFFFF0; // Make positive and use 23 bits
			index |= 1 << 27; // Set negative sign at 24th bit
		}
		index |= (z << 28) & 0xF0000000; // 4 bits
		return index;
	}

}
