package com.github.hapily04.anvilmapeditor.session.chunk;

import com.github.hapily04.anvilmapeditor.commands.EditCommand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UnloadCache {

	private static final Map<String, Set<ChunkPos>> unloadCache = new HashMap<>();

	public static void setChunks(String worldName, Set<ChunkPos> chunks) {
		unloadCache.put(worldName, chunks);
	}

	/*public static boolean unloadChunks(String worldName) {
		File worldFolder = new File(EditCommand.INPUT_DIRECTORY, worldName);
		if (!worldFolder.exists()) return false;
		File regionFileDirectory = new File(worldFolder, "region");
		for (ChunkPos chunkPos : unloadCache.get(worldName)) {
			Path regionPath = new File(regionFileDirectory,
					"r." + chunkPos.getRegionX() + "." + chunkPos.getRegionZ() + ".mca").toPath();
			try (RegionFile regionFile = new RegionFile(regionPath, regionFileDirectory.toPath(), true)) {
				regionFile.clear(new net.minecraft.world.level.ChunkPos(chunkPos.x, chunkPos.z));
				regionFile.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}*/

}
