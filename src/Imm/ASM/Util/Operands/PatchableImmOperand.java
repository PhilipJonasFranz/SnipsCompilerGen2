package Imm.ASM.Util.Operands;

public class PatchableImmOperand extends Operand {

	public enum PATCH_DIR {
		UP, DOWN;
	}
	
	public PATCH_DIR dir;
	
	public int value;
	
	public int patchedValue;
	
	public PatchableImmOperand(PATCH_DIR dir, int value) {
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

	public PatchableImmOperand clone() {
		PatchableImmOperand p = new PatchableImmOperand(this.dir, this.value);
		p.patchedValue = this.patchedValue;
		return p;
	}

	public boolean equals(Operand operand) {
		if (!(operand instanceof PatchableImmOperand)) return false;
		else {
			PatchableImmOperand op = (PatchableImmOperand) operand;
			return op.dir == this.dir && op.value == this.value;
		}
	}
	
}
