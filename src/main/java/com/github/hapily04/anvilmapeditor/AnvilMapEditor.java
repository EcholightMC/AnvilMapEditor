package com.github.hapily04.anvilmapeditor;

import com.github.hapily04.anvilmapeditor.commands.Command;
import com.github.hapily04.anvilmapeditor.commands.data.DataManager;
import com.github.hapily04.anvilmapeditor.listeners.ChatListener;
import com.github.hapily04.anvilmapeditor.listeners.SpawnListener;
import com.github.hapily04.anvilmapeditor.session.SessionManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import me.nullicorn.nedit.SNBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minestom.server.MinecraftServer;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public final class AnvilMapEditor extends JavaPlugin {

    public static final String PREFIX = "<yellow><b><obf>|<reset> ";

    private static AnvilMapEditor instance;

    private DataManager dataManager;
    private SessionManager sessionManager;

    @Override
    public void onLoad() {
        MinecraftServer.init(); // required for polar conversion to work
        CommandAPIBukkitConfig config = new CommandAPIBukkitConfig(this).initializeNBTAPI(NBTCompound.class, o -> {
            CompoundTag compoundTag = (CompoundTag) o;
			try {
				return SNBTReader.readCompound(new SnbtPrinterTagVisitor().visit(compoundTag));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
        CommandAPI.onLoad(config);
    }

    @Override
    public void onEnable() {
        instance = this;
        dataManager = new DataManager(this);
        sessionManager = new SessionManager(this, dataManager);
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(sessionManager, this);
        pluginManager.registerEvents(new ChatListener(), this);
        pluginManager.registerEvents(new SpawnListener(this), this);
        CommandAPI.onEnable();
        Command.registerAllCommands(getFile(), "com.github.hapily04.anvilmapeditor.commands", getLogger());
        SessionManager.applyGameRules(getSpawnWorld());
    }

    @Override
    public void onDisable() {
		Set<World> worlds = new HashSet<>(dataManager.getWorlds());
        for (World world : worlds) {
            dataManager.saveExternalData(world, true, false);
        }
        CommandAPI.onDisable();
    }

    public World getSpawnWorld() {
        return getServer().getWorld("ame_lobby");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public static AnvilMapEditor getInstance() {
        return instance;
    }

}
