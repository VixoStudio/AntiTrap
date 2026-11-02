package dev.antitrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import dev.antitrap.command.AntiTrapCommands;
import dev.antitrap.config.AntiTrapConfig;
import dev.antitrap.detect.DefaultTrapRules;
import dev.antitrap.detect.TrapBlockRegistry;
import dev.antitrap.input.AntiTrapKeybinds;
import dev.antitrap.render.TrapOutlineRenderer;
import dev.antitrap.scan.TrapScanner;

public final class AntiTrapMod implements ClientModInitializer {
	public static final String MOD_ID = "antitraps";
	public static final Logger LOGGER = LoggerFactory.getLogger("AntiTrap");

	private static AntiTrapMod instance;
	private static volatile boolean active = true;

	private final TrapBlockRegistry blockRegistry = new TrapBlockRegistry();
	private final AntiTrapConfig config = new AntiTrapConfig();
	private final TrapScanner scanner = new TrapScanner();
	private final TrapOutlineRenderer renderer = new TrapOutlineRenderer();

	public static AntiTrapMod get() {
		return instance;
	}

	public static TrapBlockRegistry blocks() {
		return instance.blockRegistry;
	}

	public static AntiTrapConfig config() {
		return instance.config;
	}

	public static TrapScanner scanner() {
		return instance.scanner;
	}

	public static boolean isActive() {
		return active;
	}

	public static void setEnabled(boolean enabled) {
		active = enabled;
		AntiTrapConfig cfg = config();
		cfg.enabled = enabled;
		cfg.save();
		if (!enabled) {
			scanner().clear();
			instance.renderer.shutdown();
		} else {
			scanner().forceRescan();
		}
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		config.load();
		active = config.enabled;
		DefaultTrapRules.register(blockRegistry);
		AntiTrapKeybinds.register();
		AntiTrapCommands.register();
		renderer.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			AntiTrapKeybinds.tick(client);
			if (!active) {
				return;
			}
			scanner.tick(client, config, blockRegistry);
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());

		LOGGER.info("AntiTrap ready. R toggle, K settings.");
	}
}
