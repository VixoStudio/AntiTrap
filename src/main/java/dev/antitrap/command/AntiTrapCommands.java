package dev.antitrap.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import dev.antitrap.AntiTrapMod;
import dev.antitrap.config.AntiTrapConfig;
import dev.antitrap.detect.TrapCategory;

public final class AntiTrapCommands {
	private AntiTrapCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("antitraps")
						.executes(ctx -> {
							boolean next = !AntiTrapMod.config().enabled;
							AntiTrapMod.setEnabled(next);
							ctx.getSource().sendFeedback(Component.translatable(
									next ? "antitrap.status.enabled" : "antitrap.status.disabled"
							));
							return 1;
						})
						.then(ClientCommandManager.literal("on").executes(ctx -> setEnabled(ctx, true)))
						.then(ClientCommandManager.literal("off").executes(ctx -> setEnabled(ctx, false)))
						.then(ClientCommandManager.literal("menu").executes(ctx -> {
							net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
							client.execute(() -> client.setScreen(new dev.antitrap.config.AntiTrapConfigScreen(client.screen)));
							return 1;
						}))
						.then(ClientCommandManager.literal("reload").executes(ctx -> {
							AntiTrapMod.config().load();
							AntiTrapMod.scanner().forceRescan();
							ctx.getSource().sendFeedback(Component.literal("AntiTraps config reloaded"));
							return 1;
						}))
						.then(ClientCommandManager.literal("radius")
								.then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(AntiTrapConfig.MIN_RADIUS, AntiTrapConfig.MAX_RADIUS))
										.executes(ctx -> {
											AntiTrapConfig config = AntiTrapMod.config();
											config.scanRadius = IntegerArgumentType.getInteger(ctx, "blocks");
											config.save();
											AntiTrapMod.scanner().forceRescan();
											ctx.getSource().sendFeedback(Component.literal("Scan radius: " + config.scanRadius));
											return 1;
										})))
						.then(ClientCommandManager.literal("category")
								.then(ClientCommandManager.argument("id", StringArgumentType.word())
										.then(ClientCommandManager.literal("on").executes(ctx -> setCategory(ctx, true)))
										.then(ClientCommandManager.literal("off").executes(ctx -> setCategory(ctx, false)))))
		));
	}

	private static int setEnabled(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx, boolean value) {
		AntiTrapMod.setEnabled(value);
		ctx.getSource().sendFeedback(Component.translatable(value ? "antitrap.status.enabled" : "antitrap.status.disabled"));
		return 1;
	}

	private static int setCategory(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx, boolean value) {
		String id = StringArgumentType.getString(ctx, "id");
		TrapCategory category = TrapCategory.byId(id);
		if (category == null) {
			ctx.getSource().sendError(Component.literal("Unknown category: " + id));
			return 0;
		}
		AntiTrapMod.config().categories.put(id, value);
		AntiTrapMod.config().save();
		AntiTrapMod.scanner().forceRescan();
		ctx.getSource().sendFeedback(Component.literal(id + ": " + (value ? "on" : "off")));
		return 1;
	}
}
