package dev.antitrap.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.antitrap.detect.DangerLevel;
import dev.antitrap.detect.TrapCandidate;
import dev.antitrap.detect.TrapCategory;

/**
 * Ranks how dangerous a found block is. Every trap block in range is still highlighted.
 */
public final class TrapAnalyzer {
	public DangerLevel score(BlockGetter level, TrapCandidate candidate, int relatedCount) {
		candidate.clusterSize = relatedCount;
		DangerLevel levelRank = DangerLevel.LOW;

		if (candidate.onWalkLine && isTriggerOnPath(candidate.category)) {
			levelRank = DangerLevel.HIGH;
		}
		if (candidate.facesPath && (candidate.category == TrapCategory.PISTONS || candidate.category == TrapCategory.DISPENSERS)) {
			levelRank = candidate.onWalkLine || candidate.tightSpace ? DangerLevel.HIGH : DangerLevel.MEDIUM;
		}
		if (candidate.obstructs && candidate.tightSpace && (candidate.category == TrapCategory.TRAPDOORS || candidate.category == TrapCategory.DOORS || candidate.category == TrapCategory.PISTONS)) {
			levelRank = DangerLevel.HIGH;
		}
		if (candidate.category == TrapCategory.TRAPDOORS && isFallTrap(level, candidate)) {
			levelRank = DangerLevel.HIGH;
		}
		if (relatedCount >= 2 && (candidate.onWalkLine || candidate.tightSpace || candidate.facesPath)) {
			levelRank = levelRank.raise(DangerLevel.MEDIUM);
		}
		if (relatedCount >= 3 && candidate.onWalkLine) {
			levelRank = DangerLevel.HIGH;
		}
		if (candidate.inDropperCluster) {
			levelRank = candidate.clusterSize >= 4 ? DangerLevel.HIGH : levelRank.raise(DangerLevel.MEDIUM);
		} else if (candidate.dropper && candidate.facesPath) {
			levelRank = levelRank.raise(DangerLevel.MEDIUM);
		}
		return levelRank;
	}

	public boolean facesIntoPath(BlockState state, BlockPos pos, PathCorridor corridor) {
		if (!state.hasProperty(BlockStateProperties.FACING)) {
			return false;
		}
		Direction facing = state.getValue(BlockStateProperties.FACING);
		int fx = pos.getX() + facing.getStepX();
		int fy = pos.getY() + facing.getStepY();
		int fz = pos.getZ() + facing.getStepZ();
		return corridor.intersectsBlock(fx, fy, fz) || corridor.onWalkLine(fx, fy, fz);
	}

	public boolean hasCollision(BlockGetter level, BlockPos pos, BlockState state) {
		VoxelShape shape = state.getCollisionShape(level, pos);
		return !shape.isEmpty();
	}

	public boolean isTightSpace(BlockGetter level, BlockPos.MutableBlockPos cursor, PathCorridor corridor, int x, int y, int z) {
		boolean leftSolid = hasCollision(level, corridor.sidePos(cursor, x, y, z, -1.0), level.getBlockState(cursor));
		boolean rightSolid = hasCollision(level, corridor.sidePos(cursor, x, y, z, 1.0), level.getBlockState(cursor));
		return leftSolid && rightSolid;
	}

	private static boolean isTriggerOnPath(TrapCategory category) {
		return category == TrapCategory.TRIPWIRE || category == TrapCategory.REDSTONE;
	}

	private static boolean isFallTrap(BlockGetter level, TrapCandidate candidate) {
		BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos(candidate.x, candidate.y - 1, candidate.z);
		BlockState under = level.getBlockState(below);
		if (under.isAir() || level.getFluidState(below).is(Fluids.LAVA)) {
			return candidate.tightSpace || candidate.onWalkLine;
		}
		below.setY(candidate.y - 2);
		under = level.getBlockState(below);
		return (under.isAir() || level.getFluidState(below).is(Fluids.LAVA)) && (candidate.tightSpace || candidate.onWalkLine);
	}
}
