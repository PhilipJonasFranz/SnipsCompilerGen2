package Imm.ASM.Util.Operands.Memory;

public class MemoryWordOperand extends MemoryOperand {

	public int value;
	
	public MemoryWordOperand(int value) {
		this.value = value;
	}
	
	public String toString() {
		return ".word " + this.value;
	}
	
}
