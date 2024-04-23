package com.github.hapily04.anvilmapeditor.listeners;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitScheduler;

public class SpawnListener implements Listener {

	private static final Component GET_STARTED = MiniMessage.miniMessage().deserialize(
			"<grey>Do <gold>/edit <map> <grey>to begin!");

	private final AnvilMapEditor plugin;

	public SpawnListener(AnvilMapEditor plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	private void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		BukkitScheduler scheduler = Bukkit.getScheduler();
		scheduler.runTaskLater(plugin, () -> {
			player.teleport(plugin.getSpawnWorld().getSpawnLocation());
			player.setGameMode(GameMode.ADVENTURE);
			player.getInventory().clear();
		}, 10L);
		scheduler.runTaskTimer(plugin, () -> {
			if (player.getWorld().equals(plugin.getSpawnWorld())) player.sendActionBar(GET_STARTED);
		}, 0L, 40L);
	}

	@EventHandler
	private void onDamage(EntityDamageEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	private void onMove(PlayerMoveEvent event) {
		Location from = event.getFrom();
		double fromX = from.getX();
		double fromY = from.getY();
		double fromZ = from.getZ();
		Location to = event.getTo();
		double toX = to.getX();
		double toY = to.getY();
		double toZ = to.getZ();
		if (fromX == toX && fromY == toY && fromZ == toZ) return;
		if (event.getPlayer().getWorld().equals(plugin.getSpawnWorld())) event.setCancelled(true);
	}

	@EventHandler
	private void onHunger(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}

}
