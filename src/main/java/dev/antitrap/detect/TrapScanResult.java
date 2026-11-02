package dev.antitrap.detect;

import java.util.ArrayList;
import java.util.List;

public final class TrapScanResult {
	public final List<TrapCandidate> highlights = new ArrayList<>(32);
	public DangerLevel nearest = DangerLevel.NONE;

	public void clear() {
		highlights.clear();
		nearest = DangerLevel.NONE;
	}

	public int count() {
		return highlights.size();
	}

	public void add(TrapCandidate candidate) {
		highlights.add(candidate);
		nearest = nearest.raise(candidate.level);
	}

	public void refreshNearest() {
		nearest = DangerLevel.NONE;
		for (int i = 0, n = highlights.size(); i < n; i++) {
			nearest = nearest.raise(highlights.get(i).level);
		}
	}
}
