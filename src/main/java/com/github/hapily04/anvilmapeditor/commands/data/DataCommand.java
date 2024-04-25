package com.github.hapily04.anvilmapeditor.commands.data;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.commands.Command;
import com.github.hapily04.anvilmapeditor.commands.CommandName;
import dev.jorel.commandapi.executors.CommandArguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;

@CommandName("data")
public class DataCommand extends Command {

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component USAGE = MINI_MESSAGE.deserialize(
			PREFIX + "<dark_grey>/data <yellow>(create/add/set) <grey><key> <nbtdata> <dark_grey>- " +
					"<grey>Creates miscellaneous data without a physical point. Overrides data if present.\n" +
				  PREFIX + "<dark_grey>/data <yellow>physical <grey><location> <block> <size> <key> <nbtdata> " +
					"<dark_grey>- <grey>Creates a physical data point entity.\n" +
	              PREFIX + "<dark_grey>/data <yellow>delete <grey>[<key>] <dark_grey>- <grey>Deletes data of the " +
					"provided key. If no key is provided, it attempts to find the physical data point that you're looking at.\n" +
				  PREFIX + "<dark_grey>/data <yellow>get <grey> [<key>] <dark_grey>- <grey>Prints the nbt data of the " +
					"provided key. If no key is provided, it prints all of the data.");

	public DataCommand(String commandName) {
		super(commandName);
	}

	@Override
	protected void executes(CommandSender sender, CommandArguments args) {
		sender.sendMessage(USAGE);
	}

	@Override
	protected Command[] subcommands() {
		return new Command[]{new CreateSubcommand(), new PhysicalSubcommand(), new DeleteSubcommand(), new GetSubcommand()};
	}

	@Override
	public Predicate<CommandSender> requirement() {
		World spawnWorld = AnvilMapEditor.getInstance().getSpawnWorld();
		return commandSender -> commandSender instanceof Player player && player.getWorld() != spawnWorld;
	}

}
