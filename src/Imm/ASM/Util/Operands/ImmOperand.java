package Imm.ASM.Util.Operands;

public class ImmOperand extends Operand {

	public int value;
	
	public PatchableImmOperand patchable;
	
	public ImmOperand(int value) {
		this.value = value;
	}
	
	public ImmOperand(int value, PatchableImmOperand patchable) {
		this.value = value;
		this.patchable = patchable;
	}

	public String toString() {
		return "#" + this.value;
	}

	public ImmOperand clone() {
		return new ImmOperand(this.value, this.patchable);
	}

	public boolean equals(Operand operand) {
		return operand instanceof ImmOperand && ((ImmOperand) operand).value == this.value;
	}
	
}
