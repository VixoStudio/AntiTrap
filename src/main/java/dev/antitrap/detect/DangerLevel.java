package dev.antitrap.detect;

public enum DangerLevel {
	NONE(0),
	LOW(1),
	MEDIUM(2),
	HIGH(3);

	public final int rank;

	DangerLevel(int rank) {
		this.rank = rank;
	}

	public DangerLevel raise(DangerLevel other) {
		return other.rank > rank ? other : this;
	}
}
