package com.github.hapily04.anvilmapeditor.session;

import com.github.hapily04.anvilmapeditor.AnvilMapEditor;
import com.github.hapily04.anvilmapeditor.util.FileUtils;
import com.github.hapily04.anvilmapeditor.util.ItemBuilder;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static com.github.hapily04.anvilmapeditor.AnvilMapEditor.PREFIX;

public class SessionManager implements Listener {


	private static final ItemBuilder.Item EXIT_ITEM = new ItemBuilder(Material.STRUCTURE_VOID)
														 .named("<red>Exit")
														 .withLore("<grey>Saves the world, unloads the world, and teleports you back to the lobby.")
														 .build();
	private static final ItemBuilder.Item CONVERT_ITEM = new ItemBuilder(Material.COMMAND_BLOCK)
														 .named("<gradient:yellow:aqua>Convert to Polar")
														 .withLore("<grey>Saves the world & outputs a polar world in the output folder.")
														 .build();

	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private static final Component SAVING_WORLD = MINI_MESSAGE.deserialize(PREFIX + "<grey>Saving the anvil world...");
	private static final Component WORLD_SAVED = MINI_MESSAGE.deserialize(PREFIX + "<green>Anvil world saved!");
	private static final Component CONVERTING = MINI_MESSAGE.deserialize(PREFIX + "<grey>Beginning conversion process...");
	private static final Component CONVERTED = MINI_MESSAGE.deserialize(PREFIX + "<green>Successfully converted to Polar!");
	private static final Component EXITED_SESSION = MINI_MESSAGE.deserialize(PREFIX + "<grey>Exited edit session.");

	private static final Set<ItemBuilder.Item> UNTOUCHED_ITEMS = Set.of(EXIT_ITEM, CONVERT_ITEM);

	private final Set<EditSession> editSessions = new HashSet<>();

	private final AnvilMapEditor plugin;

	private final File worldContainer;
	private final File outputDirectory;

	public SessionManager(AnvilMapEditor plugin) {
		this.plugin = plugin;
		worldContainer = plugin.getServer().getWorldContainer();
		outputDirectory = FileUtils.defendFile(new File(worldContainer, "polar_output"), true);
	}

	@SuppressWarnings("ClassEscapesDefinedScope") // we want it to be package private for the constructor
	@Nullable
	public EditSession getEditSession(UUID uuid) {
		for (EditSession editSession : editSessions) {
			if (editSession.uuid().equals(uuid)) return editSession;
		}
		return null;
	}

	/**
	 * @param player who
	 * @param world where
	 * @param worldPath the internal file location of the world
	 * @return false if the requested world is already being edited by the player
	 */
	public boolean createEditSession(Player player, World world, File worldPath) {
		EditSession currentEditSession = getEditSession(player.getUniqueId());
		if (currentEditSession != null) {
			if (currentEditSession.editingWorld().equals(world)) return false;
			editSessions.remove(currentEditSession);
		}
		applyGameRules(world);
		EditSession editSession = new EditSession(player.getUniqueId(), world, worldPath);
		editSessions.add(editSession);
		player.teleport(world.getSpawnLocation());
		player.setGameMode(GameMode.CREATIVE);
		player.setFlying(true);
		setItems(player);
		player.sendActionBar(Component.empty());
		playTeleportEffects(player);
		return true;
	}

	public void exitSession(Player player) {
		EditSession currentEditSession = getEditSession(player.getUniqueId());
		if (currentEditSession == null) return;
		player.teleport(plugin.getSpawnWorld().getSpawnLocation());
		player.getInventory().clear();
		player.setGameMode(GameMode.ADVENTURE);
		playTeleportEffects(player);
		player.sendMessage(SAVING_WORLD);
		Bukkit.unloadWorld(currentEditSession.editingWorld(), true);
		player.sendMessage(WORLD_SAVED);
		editSessions.remove(currentEditSession);
		player.sendMessage(EXITED_SESSION);
	}

	public static void applyGameRules(World world) {
		world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
		world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
		world.setGameRule(GameRule.DO_INSOMNIA, false);
		world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
		world.setGameRule(GameRule.DISABLE_RAIDS, true);
		world.setGameRule(GameRule.DO_FIRE_TICK, false);
		world.setGameRule(GameRule.BLOCK_EXPLOSION_DROP_DECAY, false);
		world.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);
		world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
		world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		world.setGameRule(GameRule.MOB_GRIEFING, false);
		world.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
		world.setGameRule(GameRule.DO_MOB_LOOT, false);
		world.setGameRule(GameRule.DO_TILE_DROPS, false);
		world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
		world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
		world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
		world.setGameRule(GameRule.KEEP_INVENTORY, true);
		world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
		world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
	}

	private void setItems(Player player) {
		player.getInventory().setItem(7, CONVERT_ITEM.getItem());
		player.getInventory().setItem(8, EXIT_ITEM.getItem());
	}

	private void playTeleportEffects(Player player) {
		Location location = player.getLocation();
		player.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.75f, 0.85f);
		player.spawnParticle(Particle.END_ROD, location, 20, 0.25, 0.25, 0.25, 0.1);
	}

	private void convertToPolar(EditSession editSession) {
		MiniMessage miniMessage = MiniMessage.miniMessage();
		Player player = Bukkit.getPlayer(editSession.uuid());
		BukkitScheduler scheduler = Bukkit.getScheduler();
		scheduler.runTaskAsynchronously(plugin, () -> {
			safeMessage(player, SAVING_WORLD);
			scheduler.runTask(plugin, () -> editSession.editingWorld().save());
			safeMessage(player, WORLD_SAVED);
			safeMessage(player, CONVERTING);
			File worldFolder = editSession.worldFolder();
			String polarFileName = worldFolder.getName() + ".polar";
			try {
				PolarWorld polarWorld = AnvilPolar.anvilToPolar(worldFolder.toPath());
				File outputFile = FileUtils.defendFile(new File(outputDirectory, polarFileName));
				// todo going to have to make a folder instead of 1 file for the whole world for miscellaneous data.
				// todo also add this file to the anvil world folder
				try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
					fileOutputStream.write(PolarWriter.write(polarWorld));
				}
			} catch (IOException e) {
				safeMessage(player, miniMessage.deserialize(
						PREFIX + "<red>An error occurred while trying to convert " + polarFileName));
				throw new RuntimeException(e);
			}
			safeMessage(player, CONVERTED);
		});
	}

	private void safeMessage(Player player, Component message) {
		if (player == null) return;
		Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message)); // running it on the main thread
	}

	@EventHandler
	private void onDrop(PlayerDropItemEvent event) {
		Item itemDrop = event.getItemDrop();
		for (ItemBuilder.Item item : UNTOUCHED_ITEMS) {
			ItemStack originalItem = item.getOriginalItem();
			if (itemDrop.getItemStack().isSimilar(originalItem)) {
				itemDrop.setItemStack(ItemStack.empty());
				event.setCancelled(true);
				setItems(event.getPlayer());
				return;
			}
		}
		event.setCancelled(true);
	}

	@EventHandler
	private void onInteract(PlayerInteractEvent event) {
		if (event.getHand() == EquipmentSlot.OFF_HAND) return;
		if (event.getAction().isLeftClick()) return;
		if (!event.hasItem()) return;
		ItemStack item = event.getItem();
		assert item != null; // checked if the event is item related already
		Player player = event.getPlayer();
		event.setCancelled(true);
		if (item.isSimilar(EXIT_ITEM.getOriginalItem())) exitSession(player);
		else if (item.isSimilar(CONVERT_ITEM.getOriginalItem())) {
			Material convertItemMaterial = CONVERT_ITEM.getOriginalItem().getType();
			if (player.hasCooldown(convertItemMaterial)) return;
			EditSession editSession = getEditSession(player.getUniqueId());
			if (editSession == null) return;
			player.setCooldown(convertItemMaterial, 100);
			convertToPolar(editSession);
		}
	}

	@EventHandler
	private void onOffHand(PlayerSwapHandItemsEvent event) {
		for (ItemBuilder.Item item : UNTOUCHED_ITEMS) {
			ItemStack originalItem = item.getOriginalItem();
			if (event.getMainHandItem().isSimilar(originalItem) || event.getOffHandItem().isSimilar(originalItem)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	private void onInventoryClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		if (player.getWorld() != plugin.getSpawnWorld()) setItems(player);
	}

	@EventHandler
	private void onInventoryClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		ItemStack slotItem = event.getCurrentItem();
		if (slotItem == null) return;
		ItemStack cursorItem = event.getCursor();
		for (ItemBuilder.Item item : UNTOUCHED_ITEMS) {
			ItemStack originalItem = item.getOriginalItem();
			if (slotItem.isSimilar(originalItem)) {
				event.setCancelled(true);
				setItems(player);
				return;
			} else if (cursorItem.isSimilar(originalItem)) {
				event.setCancelled(true);
				setItems(player);
				return;
			}
		}
	}

	@EventHandler
	private void onInventoryMove(InventoryMoveItemEvent event) {
		ItemStack itemStack = event.getItem();
		for (ItemBuilder.Item item : UNTOUCHED_ITEMS) {
			ItemStack originalItem = item.getOriginalItem();
			if (itemStack.isSimilar(originalItem)) {
				event.setCancelled(true);
				return;
			}
		}
	}

}
