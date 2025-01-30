package com.github.hapily04.anvilmapeditor.commands;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.commands.data.DataManager;
import com.github.hapily04.anvilmapeditor.util.FileUtils;
import com.google.common.io.Files;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.PolarChunk;
import net.hollowcube.polar.PolarSection;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;
import static com.github.hapily04.anvilmapeditor.commands.data.DataManager.NBT_DATA_KEY;

@CommandName("convertpolar")
public class ConvertPolarCommand extends Command {

	private static final String OUTPUT_DIRECTORY_NAME = "polar_output";

	private static final File OUTPUT_DIRECTORY = FileUtils.defendFile(
			new File(Bukkit.getServer().getWorldContainer(), OUTPUT_DIRECTORY_NAME), true);

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component WORLD_MUST_BE_UNLOADED = MINI_MESSAGE.deserialize(
			PREFIX + "<red>The provided world must be unloaded to be converted to polar.");
	/*private static final Component UNLOADING_UNUSED_CHUNKS = MINI_MESSAGE.deserialize(
			PREFIX + "<grey>Unloading unused chunks...");
	private static final Component UNUSED_CHUNKS_SUCCESS = MINI_MESSAGE.deserialize(
			PREFIX + "<green>Successfully unloaded unused chunks!");
	private static final Component UNUSED_CHUNKS_ERROR = MINI_MESSAGE.deserialize(
			PREFIX + "<red>An error occurred while attempting to unload unused chunks.");*/
	private static final Component CONVERTING = MINI_MESSAGE.deserialize(PREFIX + "<grey>Beginning conversion process...");
	private static final Component CONVERTED = MINI_MESSAGE.deserialize(PREFIX + "<green>Successfully converted to Polar!");

	private final Plugin plugin;

	public ConvertPolarCommand(String commandName) {
		super(commandName);
		plugin = JavaPlugin.getPlugin(AnvilMapEditor.class);
	}

	@Override
	protected void executes(CommandSender sender, CommandArguments args) {
		Player player = (Player) sender;
		String map = EditCommand.getMap(args);
		if (map == null) {
			player.sendMessage(EditCommand.INVALID_MAP);
			return;
		}
		File worldFolder = new File(EditCommand.INPUT_DIRECTORY, map);
		if (!worldFolder.exists()) {
			player.sendMessage(EditCommand.INVALID_MAP);
			return;
		}
		if (Bukkit.getWorld(EditCommand.INPUT_DIRECTORY_NAME + '/' + map) != null) {
			player.sendMessage(WORLD_MUST_BE_UNLOADED);
			return;
		}
		/*player.sendMessage(UNLOADING_UNUSED_CHUNKS);
		if (UnloadCache.unloadChunks(map)) {
			player.sendMessage(UNUSED_CHUNKS_SUCCESS);
		} else {
			player.sendMessage(UNUSED_CHUNKS_ERROR);
		}*/
		player.sendMessage(CONVERTING);
		convertToPolar(worldFolder, player);
	}

	private void convertToPolar(File worldFolder, Player player) {
		BukkitScheduler scheduler = Bukkit.getScheduler();
		scheduler.runTaskAsynchronously(plugin, () -> { // safe because it only schedules once saveWorldData is done
			String worldName = worldFolder.getName();
			String polarFileName = worldName + ".polar";
			try {
				PolarWorld polarWorld = AnvilPolar.anvilToPolar(worldFolder.toPath());
				File dataFile = new File(worldFolder, DataManager.DATA_FILE_NAME);
				NBTCompound dataCompound = NBTReader.readFile(dataFile);
				NBTList dataList = dataCompound.getList(NBT_DATA_KEY);
				Collection<PolarChunk> chunks = polarWorld.chunks();
				cleanPolarChunks(chunks, 1);
				if (dataList != null) {
					for (Object o : dataList) {
						NBTCompound compound = (NBTCompound) o;
						if (compound.containsKey("Biome")) {
							String biomeNamespaceID = compound.getCompound("Biome").getString("Name");
							for (PolarChunk polarChunk : chunks) {
								for (PolarSection section : polarChunk.sections()) {
									String[] biomes = section.biomePalette();
									Arrays.fill(biomes, biomeNamespaceID);
								}
							}
						}
					}
				}
				File outputFolder = FileUtils.defendFile(new File(OUTPUT_DIRECTORY, worldName), true);
				File outputFile = FileUtils.defendFile(new File(outputFolder, polarFileName));
				File outputDataFile = FileUtils.defendFile(new File(outputFolder, DataManager.DATA_FILE_NAME));
				Files.copy(dataFile, outputDataFile);
				try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
					fileOutputStream.write(PolarWriter.write(polarWorld));
				}
				safeMessage(player, CONVERTED);
			} catch (IOException e) {
				safeMessage(player, MINI_MESSAGE.deserialize(
						PREFIX + "<red>An error occurred while trying to convert " + polarFileName));
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Saves only chunks that are populated with at least 1 block + a radius of `bufferChunks` blank chunks around.
	 *
	 * @param chunks the chunks to query
	 * @param bufferChunks the radius value for how many chunks should be loaded around the occupied chunks.
	 *                        <b>A minimum value of 1 is recommended to prevent visual artifacts</b>
	 */
	public static void cleanPolarChunks(Collection<PolarChunk> chunks, int bufferChunks) {
		List<PolarChunk> validChunks = new ArrayList<>(); // chunks that aren't blank
		List<PolarChunk> blankChunks = new ArrayList<>();
		for (PolarChunk polarChunk : chunks) {
			boolean isBlank = true;
			for (PolarSection section : polarChunk.sections()) {
				String[] blockPalette = section.blockPalette();
				if (blockPalette.length != 1 || !blockPalette[0].equals("air")) {
					isBlank = false;
					validChunks.add(polarChunk);
					break;
				}
			}
			if (isBlank) blankChunks.add(polarChunk);//instanceContainer.loadChunk(polarChunk.x(), polarChunk.z());
		}
		for (PolarChunk blankChunk : blankChunks) {
			if (hasValidNeighboringChunk(blankChunk, blankChunks, bufferChunks)) validChunks.add(blankChunk); // needs to be loaded to fix culling
		}
		chunks.removeIf(chunk -> !validChunks.contains(chunk)); // remove all chunks that aren't valid & aren't part of buffer
	}

	private static boolean hasValidNeighboringChunk(PolarChunk chunk, List<PolarChunk> blankChunks, int bufferChunks) {
		int chunkX = chunk.x();
		int chunkZ = chunk.z();
		if (!isBlankChunkAt(chunkX, chunkZ+bufferChunks, blankChunks)) return true; // north
		if (!isBlankChunkAt(chunkX, chunkZ-bufferChunks, blankChunks)) return true; // south
		if (!isBlankChunkAt(chunkX-bufferChunks, chunkZ, blankChunks)) return true; // east
		return !isBlankChunkAt(chunkX+bufferChunks, chunkZ, blankChunks); // west
	}

	private static boolean isBlankChunkAt(int x, int z, List<PolarChunk> blankChunks) {
		return blankChunks.stream().anyMatch(chunk -> chunk.x() == x && chunk.z() == z);
	}

	private void safeMessage(Player player, Component message) {
		if (player == null) return;
		Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message)); // running it on the main thread
	}

	@Override
	protected Argument<?>[] arguments() {
		ArgumentSuggestions<CommandSender> mapSuggestions = ArgumentSuggestions.strings(info -> EditCommand.getMapInputs(true));
		Argument<String> map = new StringArgument("map").includeSuggestions(mapSuggestions).instance();
		return new Argument[]{map};
	}

	@Override
	public Predicate<CommandSender> requirement() {
		return commandSender -> commandSender instanceof Player;
	}

	@Override
	public String permission() {
		return "anvilmapeditor.edit";
	}

}
