package dev.antitrap.detect;

import java.util.function.Predicate;

import net.minecraft.world.level.block.state.BlockState;

public record TrapBlockRule(TrapCategory category, Predicate<BlockState> predicate) {
	public boolean matches(BlockState state) {
		return predicate.test(state);
	}
}
