package dev.antitrap.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import dev.antitrap.AntiTrapMod;
import dev.antitrap.detect.TrapCategory;

public final class AntiTrapConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static final int MIN_RADIUS = 16;
	public static final int MAX_RADIUS = 256;
	public static final int DEFAULT_RADIUS = 192;
	public static final int TRAP_PLUS_BELOW = 2;

	public boolean enabled = true;
	public boolean showHud = false;
	public boolean trapPlus = false;
	public int scanRadius = DEFAULT_RADIUS;
	public int analysisDepth = 3;
	public int configVersion = 4;
	public float outlineIntensity = 0.85f;
	public String colorLow = "#7CFFB2";
	public String colorMedium = "#FFC44D";
	public String colorHigh = "#FF5A6A";
	public Map<String, Boolean> categories = defaultCategories();

	private transient int packedLow = 0xFF7CFFB2;
	private transient int packedMedium = 0xFFFFC44D;
	private transient int packedHigh = 0xFFFF5A6A;

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("antitraps.json");
	}

	public void load() {
		Path file = path();
		Path legacy = FabricLoader.getInstance().getConfigDir().resolve("antitrap.json");
		if (!Files.isRegularFile(file) && Files.isRegularFile(legacy)) {
			file = legacy;
		}
		if (Files.isRegularFile(file)) {
			try {
				AntiTrapConfig loaded = GSON.fromJson(Files.readString(file), AntiTrapConfig.class);
				if (loaded != null) {
					copyFrom(loaded);
				}
			} catch (Exception e) {
				AntiTrapMod.LOGGER.warn("Failed to read {}, using defaults", file, e);
			}
		}
		sanitize();
		save();
	}

	public void save() {
		sanitize();
		try {
			Path file = path();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this));
		} catch (IOException e) {
			AntiTrapMod.LOGGER.warn("Failed to write AntiTrap config", e);
		}
	}

	public boolean isCategoryEnabled(TrapCategory category) {
		Boolean value = categories.get(category.id);
		return value == null || value;
	}

	public boolean entityScanEnabled() {
		return isCategoryEnabled(TrapCategory.SIGNS)
				|| isCategoryEnabled(TrapCategory.DISPENSERS)
				|| isCategoryEnabled(TrapCategory.CONTAINERS);
	}

	public boolean terrainScanEnabled() {
		return isCategoryEnabled(TrapCategory.TRIPWIRE)
				|| isCategoryEnabled(TrapCategory.DOORS)
				|| isCategoryEnabled(TrapCategory.TRAPDOORS)
				|| isCategoryEnabled(TrapCategory.PISTONS)
				|| isCategoryEnabled(TrapCategory.REDSTONE)
				|| isCategoryEnabled(TrapCategory.HAZARDS);
	}

	public int colorFor(dev.antitrap.detect.DangerLevel level) {
		return switch (level) {
			case HIGH -> packedHigh;
			case MEDIUM -> packedMedium;
			default -> packedLow;
		};
	}

	public float outlineAlpha() {
		return Math.max(0.15f, Math.min(1f, outlineIntensity));
	}

	private void copyFrom(AntiTrapConfig other) {
		this.enabled = other.enabled;
		this.showHud = false;
		this.trapPlus = other.trapPlus;
		int radius = other.configVersion < 2 ? 50 : other.scanRadius;
		if (other.configVersion < 4 && radius <= 64) {
			radius = DEFAULT_RADIUS;
		}
		this.scanRadius = radius;
		this.analysisDepth = other.analysisDepth;
		this.outlineIntensity = other.outlineIntensity;
		this.configVersion = 4;
		this.colorLow = migrateColor(other.colorLow, "#6BCB77", "#7CFFB2");
		this.colorMedium = migrateColor(other.colorMedium, "#FFD93D", "#FFC44D");
		this.colorHigh = migrateColor(other.colorHigh, "#FF6B6B", "#FF5A6A");
		if (other.categories != null) {
			for (Map.Entry<String, Boolean> entry : other.categories.entrySet()) {
				this.categories.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private void sanitize() {
		scanRadius = clamp(scanRadius, MIN_RADIUS, MAX_RADIUS);
		analysisDepth = 3;
		outlineIntensity = Math.max(0.2f, Math.min(1f, outlineIntensity));
		for (TrapCategory category : TrapCategory.values()) {
			categories.putIfAbsent(category.id, true);
		}
		packedLow = parseColor(colorLow, 0xFF7CFFB2);
		packedMedium = parseColor(colorMedium, 0xFFFFC44D);
		packedHigh = parseColor(colorHigh, 0xFFFF5A6A);
	}

	private static Map<String, Boolean> defaultCategories() {
		Map<String, Boolean> map = new LinkedHashMap<>();
		for (TrapCategory category : TrapCategory.values()) {
			map.put(category.id, true);
		}
		return map;
	}

	private static String migrateColor(String current, String oldDefault, String nextDefault) {
		if (current == null || current.equalsIgnoreCase(oldDefault)) {
			return nextDefault;
		}
		return current;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public static int parseColor(String hex, int fallback) {
		if (hex == null) {
			return fallback;
		}
		String cleaned = hex.trim();
		if (cleaned.startsWith("#")) {
			cleaned = cleaned.substring(1);
		}
		try {
			int rgb = Integer.parseUnsignedInt(cleaned, 16);
			if (cleaned.length() <= 6) {
				return 0xFF000000 | rgb;
			}
			return rgb;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
