package dev.antitrap.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import dev.antitrap.config.AntiTrapConfig;
import dev.antitrap.detect.DangerLevel;
import dev.antitrap.detect.TrapBlockRegistry;
import dev.antitrap.detect.TrapCandidate;
import dev.antitrap.detect.TrapCategory;
import dev.antitrap.detect.TrapScanResult;

/**
 * Droppers and signs are collected from chunk block-entities (same path).
 * Terrain traps use palette skip so stone/air sections are never walked.
 */
public final class TrapScanner {
	private static final int MAX_HIGHLIGHTS = 8192;
	private static final int SCAN_INTERVAL_TICKS = 10;

	private final TrapScanResult result = new TrapScanResult();
	private final PathCorridor corridor = new PathCorridor();
	private final DropperClusters dropperClusters = new DropperClusters();
	private final LongOpenHashSet seen = new LongOpenHashSet(1024);
	private final List<TrapCandidate> pool = new ArrayList<>(512);

	private int poolIndex;
	private int lastScanTick = Integer.MIN_VALUE;
	private int lastBlockX = Integer.MIN_VALUE;
	private int lastBlockY;
	private int lastBlockZ;
	private int lastRadius = -1;
	private boolean lastTrapPlus;
	private int generation;

	public TrapScanResult result() {
		return result;
	}

	public int generation() {
		return generation;
	}

	public void clear() {
		result.clear();
		lastBlockX = Integer.MIN_VALUE;
		generation++;
	}

	public void forceRescan() {
		lastBlockX = Integer.MIN_VALUE;
		lastScanTick = Integer.MIN_VALUE;
	}

	public void tick(Minecraft client, AntiTrapConfig config, TrapBlockRegistry registry) {
		LocalPlayer player = client.player;
		if (!(client.level instanceof ClientLevel level) || player == null) {
			return;
		}
		if (!config.entityScanEnabled() && !config.terrainScanEnabled()) {
			if (!result.highlights.isEmpty()) {
				result.clear();
				generation++;
			}
			return;
		}
		int tick = player.tickCount;
		if (!needsRescan(player, tick, config)) {
			return;
		}
		scan(player, level, config, registry);
		remember(player, tick, config);
	}

	private boolean needsRescan(LocalPlayer player, int tick, AntiTrapConfig config) {
		if (config.scanRadius != lastRadius || config.trapPlus != lastTrapPlus || lastBlockX == Integer.MIN_VALUE) {
			return true;
		}
		int dx = player.getBlockX() - lastBlockX;
		int dy = player.getBlockY() - lastBlockY;
		int dz = player.getBlockZ() - lastBlockZ;
		if (dx * dx + dy * dy + dz * dz >= 4) {
			return true;
		}
		return tick - lastScanTick >= SCAN_INTERVAL_TICKS;
	}

	private void remember(LocalPlayer player, int tick, AntiTrapConfig config) {
		lastScanTick = tick;
		lastBlockX = player.getBlockX();
		lastBlockY = player.getBlockY();
		lastBlockZ = player.getBlockZ();
		lastRadius = config.scanRadius;
		lastTrapPlus = config.trapPlus;
	}

	private void scan(LocalPlayer player, ClientLevel level, AntiTrapConfig config, TrapBlockRegistry registry) {
		result.clear();
		seen.clear();
		poolIndex = 0;

		Vec3 look = player.getViewVector(1f);
		corridor.update(player.position(), look, player.getBbWidth(), player.getBbHeight(), config.analysisDepth);

		int radius = config.scanRadius;
		int radiusSq = radius * radius;
		int px = player.getBlockX();
		int py = player.getBlockY();
		int pz = player.getBlockZ();
		int minY = Math.max(level.getMinY(), py - radius);
		int maxY = Math.min(level.getMaxY(), py + radius);
		if (config.trapPlus) {
			maxY = Math.min(maxY, py - AntiTrapConfig.TRAP_PLUS_BELOW);
			if (maxY < minY) {
				generation++;
				return;
			}
		}
		int minCx = px - radius >> 4;
		int maxCx = px + radius >> 4;
		int minCz = pz - radius >> 4;
		int maxCz = pz + radius >> 4;

		ClientChunkCache chunks = level.getChunkSource();
		for (int cx = minCx; cx <= maxCx && result.count() < MAX_HIGHLIGHTS; cx++) {
			int chunkMinX = cx << 4;
			int nearestX = Mth.clamp(px, chunkMinX, chunkMinX + 15);
			int dx = nearestX - px;
			int dx2 = dx * dx;
			if (dx2 > radiusSq) {
				continue;
			}
			for (int cz = minCz; cz <= maxCz && result.count() < MAX_HIGHLIGHTS; cz++) {
				int chunkMinZ = cz << 4;
				int nearestZ = Mth.clamp(pz, chunkMinZ, chunkMinZ + 15);
				int dz = nearestZ - pz;
				if (dx2 + dz * dz > radiusSq) {
					continue;
				}
				LevelChunk chunk = chunks.getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				if (config.entityScanEnabled()) {
					scanBlockEntities(chunk, config, registry, px, py, pz, radiusSq, maxY);
				}
				if (result.count() >= MAX_HIGHLIGHTS) {
					break;
				}
				if (config.terrainScanEnabled()) {
					scanTerrain(chunk, config, registry, px, py, pz, radiusSq, minY, maxY);
				}
			}
		}

		dropperClusters.mark(result.highlights);
		generation++;
	}

	private void scanBlockEntities(
			LevelChunk chunk,
			AntiTrapConfig config,
			TrapBlockRegistry registry,
			int px,
			int py,
			int pz,
			int radiusSq,
			int maxY
	) {
		Map<BlockPos, BlockEntity> entities = chunk.getBlockEntities();
		if (entities.isEmpty()) {
			return;
		}
		for (BlockEntity be : entities.values()) {
			BlockPos pos = be.getBlockPos();
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			if (y > maxY) {
				continue;
			}
			int dx = x - px;
			int dy = y - py;
			int dz = z - pz;
			if (dx * dx + dy * dy + dz * dz > radiusSq) {
				continue;
			}
			boolean dropper = be instanceof DropperBlockEntity;
			boolean dispenser = !dropper && be instanceof DispenserBlockEntity;
			boolean sign = be instanceof SignBlockEntity;
			if (dropper || dispenser) {
				if (!config.isCategoryEnabled(TrapCategory.DISPENSERS)) {
					continue;
				}
				accept(be.getBlockState(), TrapCategory.DISPENSERS, x, y, z, dropper);
			} else if (sign) {
				if (!config.isCategoryEnabled(TrapCategory.SIGNS)) {
					continue;
				}
				accept(be.getBlockState(), TrapCategory.SIGNS, x, y, z, false);
			} else {
				BlockState state = be.getBlockState();
				TrapCategory category = registry.classify(state);
				if (category == null || !config.isCategoryEnabled(category) || !TrapBlockRegistry.isBlockEntityCategory(category)) {
					continue;
				}
				accept(state, category, x, y, z, false);
			}
			if (result.count() >= MAX_HIGHLIGHTS) {
				return;
			}
		}
	}

	private void scanTerrain(
			LevelChunk chunk,
			AntiTrapConfig config,
			TrapBlockRegistry registry,
			int px,
			int py,
			int pz,
			int radiusSq,
			int minY,
			int maxY
	) {
		LevelChunkSection[] sections = chunk.getSections();
		int originX = chunk.getPos().x << 4;
		int originZ = chunk.getPos().z << 4;
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section == null || section.hasOnlyAir()) {
				continue;
			}
			int originY = chunk.getSectionYFromSectionIndex(i) << 4;
			if (originY + 15 < minY || originY > maxY) {
				continue;
			}
			if (!section.maybeHas(registry::couldBeSectionTrap)) {
				continue;
			}
			for (int ly = 0; ly < 16; ly++) {
				int y = originY + ly;
				if (y < minY || y > maxY) {
					continue;
				}
				int dy = y - py;
				int dy2 = dy * dy;
				if (dy2 > radiusSq) {
					continue;
				}
				for (int lz = 0; lz < 16; lz++) {
					int z = originZ + lz;
					int dz = z - pz;
					int dyz = dy2 + dz * dz;
					if (dyz > radiusSq) {
						continue;
					}
					for (int lx = 0; lx < 16; lx++) {
						int x = originX + lx;
						if ((x - px) * (x - px) + dyz > radiusSq) {
							continue;
						}
						BlockState state = section.getBlockState(lx, ly, lz);
						if (state.isAir()) {
							continue;
						}
						TrapCategory category = registry.classify(state);
						if (category == null || TrapBlockRegistry.isBlockEntityCategory(category) || !config.isCategoryEnabled(category)) {
							continue;
						}
						accept(state, category, x, y, z, false);
						if (result.count() >= MAX_HIGHLIGHTS) {
							return;
						}
					}
				}
			}
		}
	}

	private void accept(BlockState state, TrapCategory category, int x, int y, int z, boolean dropper) {
		long key = pack(x, y, z);
		if (!seen.add(key)) {
			return;
		}
		TrapCandidate candidate = nextCandidate();
		candidate.set(x, y, z, category);
		candidate.dropper = dropper || TrapBlockRegistry.isDropper(state);
		candidate.onWalkLine = corridor.onWalkLine(x, y, z);
		if (state.hasProperty(BlockStateProperties.FACING)) {
			Direction facing = state.getValue(BlockStateProperties.FACING);
			candidate.facesPath = corridor.onWalkLine(
					x + facing.getStepX(),
					y + facing.getStepY(),
					z + facing.getStepZ()
			);
		}
		if (candidate.onWalkLine && (category == TrapCategory.TRIPWIRE || category == TrapCategory.REDSTONE || category == TrapCategory.HAZARDS)) {
			candidate.level = DangerLevel.HIGH;
		} else if (candidate.facesPath && (category == TrapCategory.PISTONS || category == TrapCategory.DISPENSERS)) {
			candidate.level = candidate.onWalkLine ? DangerLevel.HIGH : DangerLevel.MEDIUM;
		} else if (candidate.onWalkLine) {
			candidate.level = DangerLevel.MEDIUM;
		} else {
			candidate.level = DangerLevel.LOW;
		}
		result.add(candidate);
	}

	private TrapCandidate nextCandidate() {
		if (poolIndex < pool.size()) {
			return pool.get(poolIndex++);
		}
		TrapCandidate created = new TrapCandidate();
		pool.add(created);
		poolIndex++;
		return created;
	}

	private static long pack(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
	}
}
