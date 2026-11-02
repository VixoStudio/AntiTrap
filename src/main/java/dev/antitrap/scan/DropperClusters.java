package dev.antitrap.scan;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import dev.antitrap.detect.DangerLevel;
import dev.antitrap.detect.TrapCandidate;

/**
 * Groups nearby droppers. A dispenser cannon / lag machine / trap
 * almost never uses a single isolated dropper.
 */
public final class DropperClusters {
	private static final int LINK = 6;
	private final IntArrayList droppers = new IntArrayList(64);
	private int[] parent = new int[64];
	private int[] size = new int[64];

	public void mark(List<TrapCandidate> highlights) {
		droppers.clear();
		for (int i = 0, n = highlights.size(); i < n; i++) {
			if (highlights.get(i).dropper) {
				droppers.add(i);
			}
		}
		int count = droppers.size();
		if (count == 0) {
			return;
		}
		ensureUf(count);
		for (int i = 0; i < count; i++) {
			parent[i] = i;
			size[i] = 1;
		}
		for (int i = 0; i < count; i++) {
			TrapCandidate a = highlights.get(droppers.getInt(i));
			for (int j = i + 1; j < count; j++) {
				TrapCandidate b = highlights.get(droppers.getInt(j));
				if (linked(a, b)) {
					union(i, j);
				}
			}
		}
		for (int i = 0; i < count; i++) {
			int root = find(i);
			int cluster = size[root];
			if (cluster < 2) {
				continue;
			}
			TrapCandidate candidate = highlights.get(droppers.getInt(i));
			candidate.inDropperCluster = true;
			candidate.clusterSize = Math.max(candidate.clusterSize, cluster);
			if (cluster >= 4 || candidate.onWalkLine || candidate.facesPath) {
				candidate.level = DangerLevel.HIGH;
			} else {
				candidate.level = candidate.level.raise(DangerLevel.MEDIUM);
			}
		}
	}

	private static boolean linked(TrapCandidate a, TrapCandidate b) {
		int dx = Math.abs(a.x - b.x);
		if (dx > LINK) {
			return false;
		}
		int dy = Math.abs(a.y - b.y);
		if (dy > LINK) {
			return false;
		}
		int dz = Math.abs(a.z - b.z);
		return dz <= LINK;
	}

	private void ensureUf(int n) {
		if (parent.length < n) {
			int cap = Math.max(n, parent.length * 2);
			parent = new int[cap];
			size = new int[cap];
		}
	}

	private int find(int i) {
		int p = parent[i];
		if (p != i) {
			parent[i] = find(p);
		}
		return parent[i];
	}

	private void union(int a, int b) {
		int ra = find(a);
		int rb = find(b);
		if (ra == rb) {
			return;
		}
		if (size[ra] < size[rb]) {
			int tmp = ra;
			ra = rb;
			rb = tmp;
		}
		parent[rb] = ra;
		size[ra] += size[rb];
	}
}
