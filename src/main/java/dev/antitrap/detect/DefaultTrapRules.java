package dev.antitrap.detect;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;

public final class DefaultTrapRules {
	private DefaultTrapRules() {
	}

	public static void register(TrapBlockRegistry registry) {
		registry.register(TrapCategory.TRIPWIRE, state -> {
			Block block = state.getBlock();
			return block instanceof TripWireBlock || block instanceof TripWireHookBlock;
		});
		registry.register(TrapCategory.SIGNS, state -> state.is(BlockTags.ALL_SIGNS));
		registry.register(TrapCategory.DOORS, state -> state.is(BlockTags.DOORS) || state.is(BlockTags.FENCE_GATES));
		registry.register(TrapCategory.TRAPDOORS, state -> state.is(BlockTags.TRAPDOORS));
		registry.register(TrapCategory.DISPENSERS, state -> {
			Block block = state.getBlock();
			return block instanceof DropperBlock || block instanceof DispenserBlock || state.is(Blocks.CRAFTER);
		});
		registry.register(TrapCategory.PISTONS, state ->
				state.is(Blocks.PISTON)
						|| state.is(Blocks.STICKY_PISTON)
						|| state.is(Blocks.PISTON_HEAD)
						|| state.is(Blocks.MOVING_PISTON));
		registry.register(TrapCategory.REDSTONE, state -> {
			Block block = state.getBlock();
			return block instanceof ObserverBlock
					|| block instanceof TntBlock
					|| block instanceof HopperBlock
					|| block instanceof ButtonBlock
					|| block instanceof BasePressurePlateBlock
					|| block instanceof LeverBlock
					|| block instanceof RedstoneTorchBlock
					|| block instanceof LightningRodBlock
					|| block instanceof CopperBulbBlock
					|| block instanceof RedstoneLampBlock
					|| state.is(Blocks.REDSTONE_BLOCK)
					|| state.is(Blocks.REDSTONE_WIRE)
					|| state.is(Blocks.REPEATER)
					|| state.is(Blocks.COMPARATOR)
					|| state.is(Blocks.TARGET)
					|| state.is(Blocks.DAYLIGHT_DETECTOR)
					|| state.is(Blocks.LECTERN)
					|| state.is(Blocks.POWERED_RAIL)
					|| state.is(Blocks.SCULK_SENSOR)
					|| state.is(Blocks.CALIBRATED_SCULK_SENSOR)
					|| state.is(Blocks.SCULK_SHRIEKER);
		});
		registry.register(TrapCategory.CONTAINERS, state ->
				state.is(Blocks.TRAPPED_CHEST)
						|| state.is(Blocks.CHEST)
						|| state.is(Blocks.BARREL)
						|| state.is(Blocks.HOPPER)
						|| state.is(BlockTags.SHULKER_BOXES)
						|| state.is(Blocks.SPAWNER)
						|| state.is(Blocks.TRIAL_SPAWNER));
		registry.register(TrapCategory.HAZARDS, state -> {
			Block block = state.getBlock();
			return block instanceof BaseFireBlock
					|| block instanceof InfestedBlock
					|| state.is(Blocks.COBWEB)
					|| state.is(Blocks.POWDER_SNOW)
					|| state.is(Blocks.MAGMA_BLOCK)
					|| state.is(Blocks.SOUL_SAND)
					|| state.is(Blocks.CAMPFIRE)
					|| state.is(Blocks.SOUL_CAMPFIRE)
					|| state.is(Blocks.POINTED_DRIPSTONE)
					|| state.is(Blocks.SCAFFOLDING)
					|| state.is(Blocks.SLIME_BLOCK)
					|| state.is(Blocks.HONEY_BLOCK)
					|| state.is(Blocks.NOTE_BLOCK)
					|| state.is(Blocks.BIG_DRIPLEAF)
					|| state.is(Blocks.SCULK_CATALYST)
					|| state.is(Blocks.SWEET_BERRY_BUSH)
					|| state.is(Blocks.DETECTOR_RAIL)
					|| state.is(Blocks.ACTIVATOR_RAIL)
					|| state.is(Blocks.WITHER_ROSE);
		});
	}
}
