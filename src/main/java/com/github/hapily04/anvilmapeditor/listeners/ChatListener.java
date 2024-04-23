package com.github.hapily04.anvilmapeditor.listeners;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class ChatListener implements Listener {

	private final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	@EventHandler
	private void onChat(AsyncChatEvent event) {
		event.renderer(new ChatRenderer() {
			@Override
			public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName,
											 @NotNull Component message, @NotNull Audience viewer) {
				TagResolver displayName = Placeholder.component("displayname", sourceDisplayName);
				TagResolver msg = Placeholder.component("message", message);
				return MINI_MESSAGE.deserialize("<white><displayname><reset> <dark_grey>» <grey><message>",
						displayName, msg);
			}
		});
	}

	@EventHandler
	private void onJoin(PlayerJoinEvent event) {
		TagResolver displayName = Placeholder.component("displayname", event.getPlayer().displayName());
		event.joinMessage(MINI_MESSAGE.deserialize("<green>» <white><displayname>", displayName));
	}

	@EventHandler
	private void onQuit(PlayerQuitEvent event) {
		TagResolver displayName = Placeholder.component("displayname", event.getPlayer().displayName());
		event.quitMessage(MINI_MESSAGE.deserialize("<red>« <white><displayname>", displayName));
	}

}
