package dev.antitrap.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Player-sized walking volume in front of the look direction.
 * Side blocks that never intersect this volume are ignored.
 */
public final class PathCorridor {
	public double originX;
	public double originY;
	public double originZ;
	public double dirX;
	public double dirZ;
	public double rightX;
	public double rightZ;
	public double depth;
	public double halfWidth;
	public double height;
	public AABB volume = new AABB(0, 0, 0, 0, 0, 0);
	public AABB walkLine = new AABB(0, 0, 0, 0, 0, 0);

	public void update(Vec3 origin, Vec3 look, double playerWidth, double playerHeight, double analysisDepth) {
		originX = origin.x;
		originY = origin.y;
		originZ = origin.z;
		double hx = look.x;
		double hz = look.z;
		double len = Math.sqrt(hx * hx + hz * hz);
		if (len < 1.0E-4) {
			hx = 0;
			hz = 1;
			len = 1;
		}
		dirX = hx / len;
		dirZ = hz / len;
		rightX = -dirZ;
		rightZ = dirX;
		depth = analysisDepth;
		halfWidth = (playerWidth * 0.5) + 0.12;
		height = playerHeight;
		double endX = originX + dirX * depth;
		double endZ = originZ + dirZ * depth;
		volume = new AABB(
				Math.min(originX, endX) - halfWidth,
				originY,
				Math.min(originZ, endZ) - halfWidth,
				Math.max(originX, endX) + halfWidth,
				originY + height,
				Math.max(originZ, endZ) + halfWidth
		);
		double lineHalf = Math.max(0.18, playerWidth * 0.28);
		walkLine = new AABB(
				Math.min(originX, endX) - lineHalf,
				originY + 0.05,
				Math.min(originZ, endZ) - lineHalf,
				Math.max(originX, endX) + lineHalf,
				originY + height - 0.05,
				Math.max(originZ, endZ) + lineHalf
		);
	}

	public boolean intersectsBlock(int x, int y, int z) {
		return volume.intersects(x, y, z, x + 1, y + 1, z + 1);
	}

	public boolean onWalkLine(int x, int y, int z) {
		return walkLine.intersects(x, y, z, x + 1, y + 1, z + 1);
	}

	public double along(int x, int z) {
		double dx = (x + 0.5) - originX;
		double dz = (z + 0.5) - originZ;
		return dx * dirX + dz * dirZ;
	}

	public boolean isAhead(int x, int z) {
		return along(x, z) >= -0.35 && along(x, z) <= depth + 0.35;
	}

	public int minX() {
		return Mth.floor(volume.minX);
	}

	public int minY() {
		return Mth.floor(volume.minY);
	}

	public int minZ() {
		return Mth.floor(volume.minZ);
	}

	public int maxX() {
		return Mth.floor(volume.maxX);
	}

	public int maxY() {
		return Mth.floor(volume.maxY);
	}

	public int maxZ() {
		return Mth.floor(volume.maxZ);
	}

	public Vec3 sideSample(int x, int y, int z, double sign) {
		return new Vec3(x + 0.5 + rightX * sign, y + 0.5, z + 0.5 + rightZ * sign);
	}

	public BlockPos.MutableBlockPos sidePos(BlockPos.MutableBlockPos cursor, int x, int y, int z, double sign) {
		return cursor.set(
				Mth.floor(x + 0.5 + rightX * sign),
				y,
				Mth.floor(z + 0.5 + rightZ * sign)
		);
	}
}
