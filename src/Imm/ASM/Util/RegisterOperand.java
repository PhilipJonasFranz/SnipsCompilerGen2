package Imm.ASM.Util;

public class RegisterOperand extends Operand {

	public enum REGISTER {
		R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14, R15,
		FP, SP, LR, PC
	}
	
	public REGISTER reg;
	
	public RegisterOperand(REGISTER reg) {
		if (reg == REGISTER.R11) this.reg = REGISTER.FP;
		else if (reg == REGISTER.R13) this.reg = REGISTER.SP;
		else if (reg == REGISTER.R14) this.reg = REGISTER.LR;
		else if (reg == REGISTER.R15) this.reg = REGISTER.PC;
		else this.reg = reg;
	}
	
	public RegisterOperand(int reg) {
		if (reg == 11) this.reg = REGISTER.FP;
		else if (reg == 13) this.reg = REGISTER.SP;
		else if (reg == 14) this.reg = REGISTER.LR;
		else if (reg == 15) this.reg = REGISTER.PC;
		else if (reg == 0) this.reg = REGISTER.R0;
		else if (reg == 1) this.reg = REGISTER.R1;
		else if (reg == 2) this.reg = REGISTER.R2;
		else if (reg == 3) this.reg = REGISTER.R3;
		else if (reg == 4) this.reg = REGISTER.R4;
		else if (reg == 5) this.reg = REGISTER.R5;
		else if (reg == 6) this.reg = REGISTER.R6;
		else if (reg == 7) this.reg = REGISTER.R7;
		else if (reg == 8) this.reg = REGISTER.R8;
		else if (reg == 9) this.reg = REGISTER.R9;
		else if (reg == 10) this.reg = REGISTER.R10;
	}

	public String toString() {
		return this.reg.toString().toLowerCase();
	}
	
}
