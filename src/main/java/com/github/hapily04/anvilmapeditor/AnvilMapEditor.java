package com.github.hapily04.anvilmapeditor;

import com.github.hapily04.anvilmapeditor.commands.Command;
import com.github.hapily04.anvilmapeditor.listeners.ChatListener;
import com.github.hapily04.anvilmapeditor.listeners.SpawnListener;
import com.github.hapily04.anvilmapeditor.session.SessionManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AnvilMapEditor extends JavaPlugin {

    public static final String PREFIX = "<yellow><b><obf>|<reset> ";

    private static AnvilMapEditor instance;

    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        instance = this;
        sessionManager = new SessionManager(this);
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(sessionManager, this);
        pluginManager.registerEvents(new ChatListener(), this);
        pluginManager.registerEvents(new SpawnListener(this), this);
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
        Command.registerAllCommands(getFile(), "com.github.hapily04.anvilmapeditor.commands", getLogger());
        SessionManager.applyGameRules(getSpawnWorld());
    }

    public World getSpawnWorld() {
        return getServer().getWorld("ame_lobby");
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public static AnvilMapEditor getInstance() {
        return instance;
    }

}
