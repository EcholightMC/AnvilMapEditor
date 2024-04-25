package com.github.hapily04.anvilmapeditor.commands.data;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.commands.Command;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.NBTCompoundArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import me.nullicorn.nedit.type.NBTCompound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;

class CreateSubcommand extends Command {

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component DATA_CREATED = MINI_MESSAGE.deserialize(
			PREFIX + "<green>Successfully added the data!");

	public CreateSubcommand() {
		super("create");
	}

	@Override
	protected void executes(CommandSender sender, CommandArguments args) {
		AnvilMapEditor plugin = AnvilMapEditor.getInstance();
		DataManager dataManager = plugin.getDataManager();
		Player player = (Player) sender; // defined in DataCommand.class
		String key = (String) args.get("key");
		NBTCompound nbtCompound = (NBTCompound) args.get("nbtdata");
		assert key != null;
		dataManager.addData(player.getWorld(), key, nbtCompound);
		player.sendMessage(DATA_CREATED);
	}

	@Override
	protected Argument<?>[] arguments() {
		Argument<String> keyArgument = new StringArgument("key");
		NBTCompoundArgument<NBTCompound> nbtCompoundArgument = new NBTCompoundArgument<>("nbtdata");
		return new Argument[]{keyArgument, nbtCompoundArgument};
	}

	@Override
	protected String[] aliases() {
		return new String[]{"set", "add"};
	}

}

