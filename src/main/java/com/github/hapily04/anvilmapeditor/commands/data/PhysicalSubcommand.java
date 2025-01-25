package com.github.hapily04.anvilmapeditor.commands.data;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.commands.Command;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BlockStateArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.NBTCompoundArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import me.nullicorn.nedit.type.NBTCompound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;

class PhysicalSubcommand extends Command {

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component KEY_ALREADY_USED = MINI_MESSAGE.deserialize(
			PREFIX + "<red>The provided key is already under use.");
	private static final Component DATA_ENTITY_CREATED = MINI_MESSAGE.deserialize(
			PREFIX + "<green>Successfully created a data entity at your location!");

	public PhysicalSubcommand() {
		super("physical");
	}

	@Override
	protected void executes(CommandSender sender, CommandArguments args) {
		AnvilMapEditor plugin = AnvilMapEditor.getInstance();
		DataManager dataManager = plugin.getDataManager();
		Player player = (Player) sender; // defined in DataCommand.class
		Location loc = (Location) args.get("location");
		BlockData blockData = (BlockData) args.get("block");
		Float size = (Float) args.get("size");
		String key = (String) args.get("key");
		NBTCompound data = (NBTCompound) args.get("nbtdata");
		World world = player.getWorld();
		assert key != null;
		if (dataManager.hasData(world, key)) {
			player.sendMessage(KEY_ALREADY_USED);
			return;
		}
		NBTCompound entityCompound = new NBTCompound();
		assert loc != null;
		entityCompound.put("x", (float) loc.getX());
		entityCompound.put("y", (float) loc.getY());
		entityCompound.put("z", (float) loc.getZ());
		assert blockData != null;
		entityCompound.put("Material", blockData.getMaterial().toString());
		assert size != null;
		entityCompound.put("Size", size);
		entityCompound.put("Key", key);
		dataManager.addEntityData(world, entityCompound);
		dataManager.addData(world, key, data);
		dataManager.clearEntities(world);
		dataManager.loadEntities(world);
		player.sendMessage(DATA_ENTITY_CREATED);
	}

	@Override
	protected Argument<?>[] arguments() {
		Argument<Location> locationArgument = new LocationArgument("location");
		Argument<BlockData> blockArgument = new BlockStateArgument("block");
		Argument<Float> sizeArgument = new FloatArgument("size", 0.1f);
		Argument<String> keyArgument = new StringArgument("key");
		NBTCompoundArgument<NBTCompound> nbtCompoundArgument = new NBTCompoundArgument<>("nbtdata");
		return new Argument[]{locationArgument, blockArgument, sizeArgument, keyArgument, nbtCompoundArgument};
	}


}
