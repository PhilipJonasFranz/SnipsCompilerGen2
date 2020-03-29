package Imm.ASM.Util.Operands;

public class RegOperand extends Operand {

	public enum REGISTER {
		R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, FP, R12, SP, LR, PC;
	}
	
	public REGISTER reg;
	
	public RegOperand(REGISTER reg) {
		this.reg = reg;
	}
	
	public RegOperand(int reg) {
		if (reg == 11) this.reg = REGISTER.FP;
		else if (reg == 13) this.reg = REGISTER.SP;
		else if (reg == 14) this.reg = REGISTER.LR;
		else if (reg == 15) this.reg = REGISTER.PC;
		else this.reg = REGISTER.valueOf("R" + reg);
	}

	public String toString() {
		return this.reg.toString().toLowerCase();
	}
	
}
