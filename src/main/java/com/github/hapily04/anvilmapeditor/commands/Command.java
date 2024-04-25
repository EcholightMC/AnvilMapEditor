package com.github.hapily04.anvilmapeditor.commands;

import com.github.hapily04.anvilmapeditor.util.FileUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

public abstract class Command extends CommandAPICommand {

	public Command(String commandName) {
		super(commandName);
		executes((sender, args) -> {
			executes(sender, args);
		});
		runIfNotNull(arguments(), this::withArguments);
		runIfNotNull(optionalArguments(), this::withOptionalArguments);
		runIfNotNull(permission(), this::withPermission);
		runIfNotNull(withoutPermission(), this::withoutPermission);
		runIfNotNull(aliases(), this::withAliases);
		runIfNotNull(fullDescription(), this::withFullDescription);
		runIfNotNull(shortDescription(), this::withShortDescription);
		//withHelp(shortDescription(), fullDescription()); can't support
		runIfNotNull(requirement(), this::withRequirement);
		runIfNotNull(subcommands(), this::withSubcommands);
		runIfNotNull(usage(), this::withUsage);
		//override();
	}

	protected abstract void executes(CommandSender sender, CommandArguments args);

	protected Argument<?>[] arguments() {
		return null;
	}

	protected Argument<?>[] optionalArguments() {
		return null;
	}

	protected String permission() {
		return null;
	}

	protected String withoutPermission() {
		return null;
	}

	protected String[] aliases() {
		return null;
	}

	protected String fullDescription() {
		return null;
	}

	protected String shortDescription() {
		return null;
	}

	protected Predicate<CommandSender> requirement() {
		return null;
	}

	protected CommandAPICommand[] subcommands() {
		return null;
	}

	protected String usage() {
		return null;
	}

	@Override
	public final void register() { // make sure no subclasses override
		CommandAPI.unregister(getName(), true);
		super.register();
	}

	@SuppressWarnings("CallToPrintStackTrace")
	public static void registerAllCommands(File jarFile, String pkg, Logger logger) {
		try {
			Class<Command>[] commandClasses = FileUtils.getClasses(pkg,
					Command.class, jarFile);
			for (Class<Command> command : commandClasses) {
				CommandName commandNameAnnotation = command.getAnnotation(CommandName.class);
				if (commandNameAnnotation == null) {
					//logger.severe("No CommandName annotation found for '" + command.getName() + "'."); // do nothing because subcommands might exist
					continue;
				}
				Command cmd = command.getConstructor(String.class).newInstance(commandNameAnnotation.value());
				cmd.register();
			}
		} catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
				 InstantiationException | IllegalAccessException e) {
			logger.severe("Unable to register commands: ");
			e.printStackTrace();
		}
	}

	private <T> void runIfNotNull(T object, Consumer<T> biConsumer) {
		if (object != null) biConsumer.accept(object);
	}

}
