package com.github.hapily04.anvilmapeditor.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class ItemBuilder {

	protected final ItemStack itemStack;
	protected final ItemMeta itemMeta;
	protected final MiniMessage miniMessage;

	/**
	 * Creates a new {@link ItemBuilder} from the given {@link Material} & {@link MiniMessage} component serializer.
	 *
	 * @param material the material designated to the item to be built by this builder
	 * @param miniMessage the serializer to be used for component creation from string input
	 */
	public ItemBuilder(@NotNull Material material, @NotNull MiniMessage miniMessage) {
		Objects.requireNonNull(material);
		Objects.requireNonNull(miniMessage);
		itemStack = new ItemStack(material);
		itemMeta = itemStack.getItemMeta();
		this.miniMessage = miniMessage;
	}

	/**
	 * Creates a new {@link ItemBuilder} from the given {@link Material} & default {@link MiniMessage} component serializer.
	 *
	 * @param material the material designated to the item to be built by this builder
	 */
	public ItemBuilder(@NotNull Material material) {
		this(material, MiniMessage.miniMessage());
	}

	/**
	 * Sets the component name of the item being built by this {@link ItemBuilder} using the specified/default
	 * {@link MiniMessage} component serializer.
	 *
	 * @param name the name of the item in MiniMessage formatting to be deserialized into a
	 * {@link net.kyori.adventure.text.Component} using the specified/default {@link MiniMessage} component serializer
	 * @return the {@link ItemBuilder} being modified
	 */
	public ItemBuilder named(@NotNull String name) {
		Objects.requireNonNull(name);
		itemMeta.displayName(miniMessage.deserialize("<!italic>" + name));
		return this;
	}

	/**
	 * Sets the lore of the item being built by this {@link ItemBuilder} using the specified/default
	 * {@link MiniMessage} component serializer.
	 *
	 * @param lines the lines of the lore in MiniMessage formatting to be deserialized into a list of
	 * {@link net.kyori.adventure.text.Component}s using the specified/default {@link MiniMessage} component serializer
	 * @return the {@link ItemBuilder} being modified
	 */
	public ItemBuilder withLore(@NotNull String... lines) {
		Objects.requireNonNull(lines);
		itemMeta.lore(Arrays.stream(lines).map((line) -> miniMessage.deserialize("<!italic>" + line)).toList());
		return this;
	}

	/**
	 * Makes the item being built by this {@link ItemBuilder} unbreakable.
	 *
	 * @return the {@link ItemBuilder} being modified
	 */
	public ItemBuilder unbreakable() {
		itemMeta.setUnbreakable(true);
		return this;
	}

	/**
	 * Sets the model data id of the item being built by this {@link ItemBuilder}.
	 *
	 * @param data the model data id
	 * @return the {@link ItemBuilder} being modified
	 */
	public ItemBuilder withModelData(int data) {
		itemMeta.setCustomModelData(data);
		return this;
	}

	/**
	 * Hides the specified flags on the item being built by this {@link ItemBuilder}.
	 *
	 * @param flags the {@link ItemFlag}s to be hidden from the player
	 * @return the {@link ItemBuilder} being modified
	 */
	public ItemBuilder withHiddenFlags(@NotNull ItemFlag... flags) {
		Objects.requireNonNull(flags);
		itemMeta.addItemFlags(flags);
		return this;
	}

	/**
	 * Adds the specified {@link Enchantment} of the specified level to the item being built by this {@link ItemBuilder}
	 *
	 * @param enchant the enchantment
	 * @param level the level of the {@link Enchantment}
	 * @return the {@link ItemBuilder} being modified
	 */
	public ItemBuilder withEnchant(@NotNull Enchantment enchant, int level) {
		Objects.requireNonNull(enchant);
		itemMeta.addEnchant(enchant, level, true);
		return this;
	}

	/**
	 * Modifies the specified {@link Attribute} of the item being built by this {@link ItemBuilder}.
	 *
	 * @param attribute the attribute being modified
	 * @param name a custom id of sorts
	 * @param operation the operation being performed on the attribute
	 * @param amount the amount you want the operation to perform
	 * @param slot the optional slot in which this attribute will be enabled in
	 * @return the {@link ItemBuilder} being modified
	 */
	public ItemBuilder modifyAttribute(@NotNull Attribute attribute, @NotNull String name, @NotNull AttributeModifier.Operation operation,
									   double amount, @Nullable EquipmentSlot slot) {
		Objects.requireNonNull(attribute);
		Objects.requireNonNull(name);
		Objects.requireNonNull(operation);
		AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), name, amount, operation, slot);
		itemMeta.addAttributeModifier(attribute, modifier);
		return this;
	}

	/**
	 * Saves data on the item being built by this {@link ItemBuilder}
	 *
	 * @param key the key that the data is being saved to
	 * @param type the type of the data being saved on the item being built by this {@link ItemBuilder}
	 * @param data the actual data being saved
	 * @return the {@link ItemBuilder} being modified
	 * @param <T> the generic type of the data being set
	 */
	public <T> ItemBuilder withCustomData(@NotNull NamespacedKey key, @NotNull PersistentDataType<?, T> type,
										 @NotNull T data) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(type);
		Objects.requireNonNull(data);
		itemMeta.getPersistentDataContainer().set(key, type, data);
		return this;
	}

	/**
	 * Compiles all of the item modifications on the item being built by this {@link ItemBuilder} into an {@link Item}
	 * that you can use to get the effective {@link ItemStack}.
	 *
	 * @return an {@link Item}
	 * @see Item#getItem()
	 * @see	Item#getOriginalItem()
	 */
	public Item build() {
		itemStack.setItemMeta(itemMeta);
		return new Item(itemStack);
	}

	public static class Item {

		private final ItemStack itemStack;

		private Item(ItemStack itemStack) {
			this.itemStack = itemStack;
		}

		/**
		 * Retrieves a copy of the original {@link ItemStack} <br>
		 * This method should almost always be the one being called as it allows for safe modification later on.
		 *
		 * @return a copy of the original {@link ItemStack}
		 */
		public ItemStack getItem() {
			return itemStack.clone();
		}

		/**
		 * Retrieves the exact original {@link ItemStack} built by the {@link ItemBuilder} <br>
		 *
		 * @return the exact original {@link ItemStack}
		 * @see Item#getItem()
		 */
		public ItemStack getOriginalItem() {
			return itemStack;
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass") // effectively using the ItemStack's equals
		public boolean equals(Object obj) {
			return itemStack.equals(obj);
		}

	}

}
