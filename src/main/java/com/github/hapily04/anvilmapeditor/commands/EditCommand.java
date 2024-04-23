package com.github.hapily04.anvilmapeditor.commands;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.util.FileUtils;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.function.Predicate;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;

@CommandName("edit")
public class EditCommand extends Command {

	private static final File WORLD_CONTAINER = AnvilMapEditor.getInstance().getServer().getWorldContainer();
	private static final String INPUT_DIRECTORY_NAME = "anvil_input";
	private static final File INPUT_DIRECTORY = FileUtils.defendFile(new File(WORLD_CONTAINER, INPUT_DIRECTORY_NAME), true);

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component INVALID_MAP = MINI_MESSAGE.deserialize(
			PREFIX + "<red>You need to provide a valid map to begin.");
	private static final Component ALREADY_EDITING = MINI_MESSAGE.deserialize(
			PREFIX + "<red>You are already editing the provided map.");
	private static final Component LOADING_WORLD = MINI_MESSAGE.deserialize(
			PREFIX + "<grey>Loading world...");
	private static final Component WORLD_FOUND = MINI_MESSAGE.deserialize(
			PREFIX + "<green>World already loaded!");
	private static final Component EDITING_STARTED = MINI_MESSAGE.deserialize(
			PREFIX + "<green>Edit session has started!");

	private final AnvilMapEditor plugin;

	public EditCommand(String commandName) {
		super(commandName);
		plugin = AnvilMapEditor.getInstance();
	}

	@Override
	protected void executes(CommandSender sender, CommandArguments args) {
		Player player = (Player) sender;
		String map = getMap(args);
		if (map == null) {
			player.sendMessage(INVALID_MAP);
			return;
		}
		World world = Bukkit.getWorld(map);
		if (world == null) {
			player.sendMessage(LOADING_WORLD);
			world = WorldCreator.name(INPUT_DIRECTORY_NAME + '/' + map).createWorld();
		} else {
			player.sendMessage(WORLD_FOUND);
		}
		if (plugin.getSessionManager().createEditSession(player, world, new File(INPUT_DIRECTORY, map))) {
			player.sendMessage(EDITING_STARTED);
		} else {
			player.sendMessage(ALREADY_EDITING);
		}
	}

	@Override
	protected Argument<?>[] arguments() {
		ArgumentSuggestions<CommandSender> mapSuggestions = ArgumentSuggestions.strings(getMapInputs(true));
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

	private @Nullable String getMap(CommandArguments args) {
		String map = (String) args.get("map");
		if (map == null) return null;
		for (String mapInput : getMapInputs(false)) {
			if (mapInput.equalsIgnoreCase(map)) return mapInput;
		}
		return null;
	}

	private String[] getMapInputs(boolean suggestions) {
		File[] files = INPUT_DIRECTORY.listFiles();
		if (files == null) return new String[]{};
		String[] mapNames = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isDirectory()) continue;
			if (suggestions) {
				mapNames[i] = file.getName().toLowerCase(Locale.ENGLISH);
			} else {
				mapNames[i] = file.getName();
			}
		}
		return mapNames;
	}

}
