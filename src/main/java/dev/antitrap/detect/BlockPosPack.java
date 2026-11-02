package dev.antitrap.detect;

public final class BlockPosPack {
	private BlockPosPack() {
	}

	public static long pack(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
	}
}
