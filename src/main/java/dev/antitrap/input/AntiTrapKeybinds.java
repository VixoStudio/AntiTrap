package dev.antitrap.input;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import dev.antitrap.AntiTrapMod;
import dev.antitrap.config.AntiTrapConfig;
import dev.antitrap.config.AntiTrapConfigScreen;

public final class AntiTrapKeybinds {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(AntiTrapMod.MOD_ID, "main")
	);

	public static KeyMapping toggle;
	public static KeyMapping menu;

	private AntiTrapKeybinds() {
	}

	public static void register() {
		toggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.antitrap.toggle",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_R,
				CATEGORY
		));
		menu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.antitrap.menu",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_K,
				CATEGORY
		));
	}

	public static void tick(Minecraft client) {
		while (toggle.consumeClick()) {
			AntiTrapConfig cfg = AntiTrapMod.config();
			boolean next = !cfg.enabled;
			AntiTrapMod.setEnabled(next);
			if (client.player != null) {
				client.player.displayClientMessage(
						Component.translatable(next ? "antitrap.status.enabled" : "antitrap.status.disabled"),
						true
				);
			}
		}
		while (menu.consumeClick()) {
			if (client.screen instanceof AntiTrapConfigScreen) {
				continue;
			}
			client.setScreen(new AntiTrapConfigScreen(client.screen));
		}
	}
}
