package com.github.hapily04.anvilmapeditor.commands.data;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.commands.Command;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;

class DeleteSubcommand extends Command {

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component DATA_NOT_FOUND_KEY = MINI_MESSAGE.deserialize(
			PREFIX + "<red>No data was found for the key (or none was provided and you aren't looking at an entity).");
	private static final Component DATA_DELETED = MINI_MESSAGE.deserialize(
			PREFIX + "<green>Successfully deleted the data!");

	public DeleteSubcommand() {
		super("delete");
	}

	@Override
	protected void executes(CommandSender sender, CommandArguments args) {
		AnvilMapEditor plugin = AnvilMapEditor.getInstance();
		DataManager dataManager = plugin.getDataManager();
		Player player = (Player) sender; // defined in DataCommand.class
		World world = player.getWorld();
		String key = (String) args.get("key");
		if (key == null) {
			Entity targetEntity = player.getTargetEntity(20);
			if (targetEntity != null) {
				if (targetEntity.hasMetadata("Key")) {
					key = targetEntity.getMetadata("Key").get(0).asString();
					targetEntity.remove();
				}
			}
		}
		if (!dataManager.hasData(world, key)) { // catches if key is null
			player.sendMessage(DATA_NOT_FOUND_KEY);
			return;
		}
		dataManager.removeEntityData(world, key); // attempts to remove any entity data, won't cause problems if there isn't an entity
		dataManager.deleteData(world, key);
		dataManager.clearEntities(world);
		dataManager.loadEntities(world);
		player.sendMessage(DATA_DELETED);
	}

	@Override
	protected Argument<?>[] arguments() {
		Argument<String> keyArgument = new StringArgument("key").setOptional(true);
		return new Argument[]{keyArgument};
	}


}
