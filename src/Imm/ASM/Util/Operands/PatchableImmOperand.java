package Imm.ASM.Util.Operands;

public class PatchableImmOperand extends Operand {

	public enum PATCH_DIR {
		UP, DOWN;
	}
	
	public PATCH_DIR dir;
	
	public int value;
	
	public PatchableImmOperand(PATCH_DIR dir, int value) {
		this.dir = dir;
		this.value = value;
	}

	public String toString() {
		return "#" + this.value;
	}
	
	public int patch(int off) {
		if (this.dir == PATCH_DIR.UP) {
			this.value += off;
		}
		else this.value -= off;
		
		return this.value;
	}

	public PatchableImmOperand clone() {
		return new PatchableImmOperand(this.dir, this.value);
	}
	
}
