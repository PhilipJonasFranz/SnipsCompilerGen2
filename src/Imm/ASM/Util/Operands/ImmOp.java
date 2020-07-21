package Imm.ASM.Util.Operands;

public class ImmOp extends Operand {

	public int value;
	
	public PatchableImmOp patchable;
	
	public ImmOp(int value) {
		this.value = value;
	}
	
	public ImmOp(int value, PatchableImmOp patchable) {
		this.value = value;
		this.patchable = patchable;
	}

	public String toString() {
		return "#" + this.value;
	}

	public ImmOp clone() {
		return new ImmOp(this.value, this.patchable);
	}

	public boolean equals(Operand operand) {
		return operand instanceof ImmOp && ((ImmOp) operand).value == this.value;
	}
	
}
