package dev.antitrap.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import dev.antitrap.AntiTrapMod;
import dev.antitrap.detect.TrapCategory;

public final class AntiTrapConfigScreen extends Screen {
	private static final int PANEL = 320;
	private final Screen parent;
	private final Button[] categoryButtons = new Button[TrapCategory.values().length];
	private int blocksLabelY = 128;

	public AntiTrapConfigScreen(Screen parent) {
		super(Component.translatable("screen.antitrap.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		AntiTrapConfig config = AntiTrapMod.config();
		int cx = this.width / 2;
		int left = cx - PANEL / 2;
		int y = 36;

		addRenderableWidget(Button.builder(modLabel(config.enabled), button -> {
			boolean next = !AntiTrapMod.config().enabled;
			AntiTrapMod.setEnabled(next);
			button.setMessage(modLabel(next));
		}).bounds(left, y, PANEL, 20).build());

		y += 24;
		addRenderableWidget(new ConfigSlider(left, y, PANEL, 20, config.scanRadius, AntiTrapConfig.MIN_RADIUS, AntiTrapConfig.MAX_RADIUS, value -> {
			config.scanRadius = value;
			config.save();
			AntiTrapMod.scanner().forceRescan();
		}));

		y += 24;
		addRenderableWidget(new IntensitySlider(left, y, PANEL, 20, config.outlineIntensity, value -> {
			config.outlineIntensity = value;
			config.save();
		}));

		y += 24;
		addRenderableWidget(Button.builder(trapPlusLabel(config.trapPlus), button -> {
			config.trapPlus = !config.trapPlus;
			button.setMessage(trapPlusLabel(config.trapPlus));
			config.save();
			AntiTrapMod.scanner().forceRescan();
		}).bounds(left, y, PANEL, 20).build());

		y += 28;
		this.blocksLabelY = y;
		y += 12;
		int half = (PANEL - 8) / 2;
		addRenderableWidget(Button.builder(Component.translatable("screen.antitrap.allOn"), button -> setAllCategories(true))
				.bounds(left, y, half, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.antitrap.allOff"), button -> setAllCategories(false))
				.bounds(left + half + 8, y, half, 20).build());

		y += 26;
		TrapCategory[] categories = TrapCategory.values();
		for (int i = 0; i < categories.length; i++) {
			TrapCategory category = categories[i];
			int bx = left + (i % 2) * (half + 8);
			int by = y + (i / 2) * 22;
			categoryButtons[i] = addRenderableWidget(Button.builder(categoryLabel(category, config.isCategoryEnabled(category)), button -> {
				boolean next = !config.isCategoryEnabled(category);
				config.categories.put(category.id, next);
				button.setMessage(categoryLabel(category, next));
				config.save();
				AntiTrapMod.scanner().forceRescan();
			}).bounds(bx, by, half, 20).build());
		}

		addRenderableWidget(Button.builder(Component.translatable("screen.antitrap.done"), button -> onClose())
				.bounds(cx - 100, this.height - 28, 200, 20)
				.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("screen.antitrap.blocks"), this.width / 2, this.blocksLabelY, 0xFFC9C2B6);
	}

	@Override
	public void onClose() {
		AntiTrapMod.config().save();
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	private void setAllCategories(boolean enabled) {
		AntiTrapConfig config = AntiTrapMod.config();
		TrapCategory[] categories = TrapCategory.values();
		for (int i = 0; i < categories.length; i++) {
			config.categories.put(categories[i].id, enabled);
			if (categoryButtons[i] != null) {
				categoryButtons[i].setMessage(categoryLabel(categories[i], enabled));
			}
		}
		config.save();
		AntiTrapMod.scanner().forceRescan();
	}

	private static Component trapPlusLabel(boolean on) {
		return Component.translatable("screen.antitrap.trapPlus").append(Component.literal(": ")).append(onOff(on));
	}

	private static Component modLabel(boolean on) {
		return Component.translatable("screen.antitrap.mod").append(Component.literal(": ")).append(onOff(on));
	}

	private static Component categoryLabel(TrapCategory category, boolean on) {
		return Component.translatable("antitrap.category." + category.id)
				.append(Component.literal(": "))
				.append(onOff(on));
	}

	private static Component onOff(boolean on) {
		return Component.translatable(on ? "screen.antitrap.on" : "screen.antitrap.off");
	}

	private static final class ConfigSlider extends AbstractSliderButton {
		private final int min;
		private final int max;
		private final IntConsumer consumer;

		private ConfigSlider(int x, int y, int width, int height, int current, int min, int max, IntConsumer consumer) {
			super(x, y, width, height, Component.empty(), (current - min) / (double) (max - min));
			this.min = min;
			this.max = max;
			this.consumer = consumer;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			int mapped = min + Mth.floor(this.value * (max - min) + 0.5);
			setMessage(Component.translatable("screen.antitrap.radius", mapped));
		}

		@Override
		protected void applyValue() {
			int mapped = min + Mth.floor(this.value * (max - min) + 0.5);
			consumer.accept(mapped);
		}
	}

	private static final class IntensitySlider extends AbstractSliderButton {
		private final FloatConsumer consumer;

		private IntensitySlider(int x, int y, int width, int height, float current, FloatConsumer consumer) {
			super(x, y, width, height, Component.empty(), Mth.clamp((current - 0.2f) / 0.8f, 0f, 1f));
			this.consumer = consumer;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			int pct = Mth.floor((0.2f + (float) this.value * 0.8f) * 100f);
			setMessage(Component.translatable("screen.antitrap.intensity", pct));
		}

		@Override
		protected void applyValue() {
			consumer.accept(0.2f + (float) this.value * 0.8f);
		}
	}

	@FunctionalInterface
	private interface IntConsumer {
		void accept(int value);
	}

	@FunctionalInterface
	private interface FloatConsumer {
		void accept(float value);
	}
}
