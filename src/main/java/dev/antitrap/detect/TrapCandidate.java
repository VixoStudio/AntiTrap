package dev.antitrap.detect;

public final class TrapCandidate {
	public int x;
	public int y;
	public int z;
	public TrapCategory category;
	public DangerLevel level = DangerLevel.LOW;
	public int clusterSize;
	public boolean onWalkLine;
	public boolean facesPath;
	public boolean tightSpace;
	public boolean obstructs;
	public boolean dropper;
	public boolean inDropperCluster;

	public void set(int x, int y, int z, TrapCategory category) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.category = category;
		this.level = DangerLevel.LOW;
		this.clusterSize = 1;
		this.onWalkLine = false;
		this.facesPath = false;
		this.tightSpace = false;
		this.obstructs = false;
		this.dropper = false;
		this.inDropperCluster = false;
	}

	public long packedPos() {
		return BlockPosPack.pack(x, y, z);
	}
}
