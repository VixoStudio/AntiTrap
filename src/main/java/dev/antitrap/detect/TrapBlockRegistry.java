package dev.antitrap.detect;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Extensible lookup of blocks that can participate in a trap.
 * Results are cached per {@link Block} so palette checks stay cheap.
 */
public final class TrapBlockRegistry {
	private final List<TrapBlockRule> rules = new ArrayList<>();
	private final Reference2ObjectOpenHashMap<Block, TrapCategory> byBlock = new Reference2ObjectOpenHashMap<>();
	private final ReferenceOpenHashSet<Block> notTrap = new ReferenceOpenHashSet<>();

	public void register(TrapBlockRule rule) {
		rules.add(rule);
		byBlock.clear();
		notTrap.clear();
	}

	public void register(TrapCategory category, java.util.function.Predicate<BlockState> predicate) {
		register(new TrapBlockRule(category, predicate));
	}

	public boolean couldBeTrap(BlockState state) {
		if (state == null || state.isAir()) {
			return false;
		}
		Block block = state.getBlock();
		if (notTrap.contains(block)) {
			return false;
		}
		if (byBlock.containsKey(block)) {
			return true;
		}
		return classify(state) != null;
	}

	public TrapCategory classify(BlockState state) {
		if (state == null || state.isAir()) {
			return null;
		}
		Block block = state.getBlock();
		TrapCategory cached = byBlock.get(block);
		if (cached != null) {
			return cached;
		}
		if (notTrap.contains(block)) {
			return null;
		}
		for (int i = 0, n = rules.size(); i < n; i++) {
			TrapBlockRule rule = rules.get(i);
			if (rule.matches(state)) {
				byBlock.put(block, rule.category());
				return rule.category();
			}
		}
		notTrap.add(block);
		return null;
	}

	public boolean isTrapBlock(BlockState state) {
		return classify(state) != null;
	}

	public static boolean isDropper(BlockState state) {
		return state.getBlock() instanceof DropperBlock;
	}

	public static boolean isBlockEntityCategory(TrapCategory category) {
		return category == TrapCategory.SIGNS
				|| category == TrapCategory.DISPENSERS
				|| category == TrapCategory.CONTAINERS;
	}

	public boolean couldBeSectionTrap(BlockState state) {
		TrapCategory category = classify(state);
		return category != null && !isBlockEntityCategory(category);
	}
}
