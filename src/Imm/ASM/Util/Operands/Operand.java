package Imm.ASM.Util.Operands;

public abstract class Operand {
	
	public abstract String toString();
	
	public abstract Operand clone();
	
	public abstract boolean equals(Operand operand);
	
}
