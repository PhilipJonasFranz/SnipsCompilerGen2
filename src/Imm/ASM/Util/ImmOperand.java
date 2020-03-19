package Imm.ASM.Util;

public class ImmOperand extends Operand {

	public int value;
	
	public ImmOperand(int value) {
		this.value = value;
	}

	public String toString() {
		return "#" + this.value;
	}
	
}
