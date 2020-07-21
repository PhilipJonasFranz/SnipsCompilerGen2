package Imm.ASM.Util.Operands;

public class PatchableImmOp extends Operand {

	public enum PATCH_DIR {
		UP, DOWN;
	}
	
	public PATCH_DIR dir;
	
	public int value;
	
	public int patchedValue;
	
	public PatchableImmOp(PATCH_DIR dir, int value) {
		this.dir = dir;
		this.value = value;
		this.patchedValue = value;
	}

	public String toString() {
		return "#" + this.patchedValue;
	}
	
	public void patch(int off) {
		if (this.dir == PATCH_DIR.UP) {
			this.patchedValue = this.value + off;
		}
		else this.patchedValue = this.value - off;
	}

	public PatchableImmOp clone() {
		PatchableImmOp p = new PatchableImmOp(this.dir, this.value);
		p.patchedValue = this.patchedValue;
		return p;
	}

	public boolean equals(Operand operand) {
		if (!(operand instanceof PatchableImmOp)) return false;
		else {
			PatchableImmOp op = (PatchableImmOp) operand;
			return op.dir == this.dir && op.value == this.value;
		}
	}
	
}
