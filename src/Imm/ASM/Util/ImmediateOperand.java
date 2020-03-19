package Imm.ASM.Util;

public class ImmediateOperand extends Operand {

	public int value;
	
	public ImmediateOperand(int value) {
		this.value = value;
	}

	public String toString() {
		return "#" + this.value;
	}
	
}
