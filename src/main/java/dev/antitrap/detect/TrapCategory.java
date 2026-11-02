package dev.antitrap.detect;

public enum TrapCategory {
	TRIPWIRE("tripwire"),
	SIGNS("signs"),
	DOORS("doors"),
	TRAPDOORS("trapdoors"),
	DISPENSERS("dispensers"),
	PISTONS("pistons"),
	REDSTONE("redstone"),
	CONTAINERS("containers"),
	HAZARDS("hazards");

	public final String id;

	TrapCategory(String id) {
		this.id = id;
	}

	public static TrapCategory byId(String id) {
		for (TrapCategory category : values()) {
			if (category.id.equals(id)) {
				return category;
			}
		}
		return null;
	}
}
