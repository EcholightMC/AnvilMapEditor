package com.github.hapily04.anvilmapeditor.commands.data;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.commands.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;
import static com.github.hapily04.anvilmapeditor.commands.data.DataManager.NBT_DATA_KEY;

class GetSubcommand extends Command {

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component DATA_NOT_FOUND = MINI_MESSAGE.deserialize(
			PREFIX + "<red>No data was found for the given key.");
	private static final Component ERORR_PRINTING = MINI_MESSAGE.deserialize(
			PREFIX + "<red>There was an error while trying to print the found data.");

	public GetSubcommand() {
		super("get");
	}

	@Override
	protected void executes(CommandSender sender, CommandArguments args) {
		AnvilMapEditor plugin = AnvilMapEditor.getInstance();
		DataManager dataManager = plugin.getDataManager();
		Player player = (Player) sender; // defined in DataCommand.class
		World world = player.getWorld();
		String key = (String) args.get("key");
		NBTList dataList = dataManager.getOrCreateDataList(world, NBT_DATA_KEY);
		NBTCompound compound = null;
		if (key == null) {
			compound = new NBTCompound();
			compound.put(NBT_DATA_KEY, dataList);
			sendPrettyCompound(player, compound);
		} else {
			for (Object o : dataList) {
				NBTCompound nbtCompound = (NBTCompound) o;
				if (nbtCompound.containsKey(key)) {
					compound = nbtCompound;
					break;
				}
			}
			if (compound == null) {
				player.sendMessage(DATA_NOT_FOUND);
				return;
			}
			sendPrettyCompound(player, compound);
		}
	}

	@SuppressWarnings("CallToPrintStackTrace")
	private void sendPrettyCompound(Player player, NBTCompound compound) {
		try {
			TextComponentTagVisitor textComponentTagVisitor = new TextComponentTagVisitor("");
			net.minecraft.network.chat.Component component = textComponentTagVisitor.visit(
					TagParser.parseTag(compound.toString()));
			HolderLookup.Provider provider = HolderLookup.Provider.create(
					MinecraftServer.getDefaultRegistryAccess().listRegistries());
			Component prettyData = GsonComponentSerializer.gson().deserialize(
					net.minecraft.network.chat.Component.Serializer.toJson(component, provider));
			player.sendMessage(prettyData);
		} catch (CommandSyntaxException e) {
			player.sendMessage(ERORR_PRINTING);
			e.printStackTrace();
		}
	}

	@Override
	protected Argument<?>[] arguments() {
		Argument<String> keyArgument = new StringArgument("key").setOptional(true);
		return new Argument[]{keyArgument};
	}


}
