package Imm.ASM.Util;

public enum REG {
	R0,  R1, R2,  R3, 
	R4,  R5, R6,  R7, 
	R8,  R9, R10, FP, 
	R12, SP, LR,  PC,
	
	/* Special Reg, used to signal unbound data on stack. See AsNAddressOf. */
	RX,
	
	S0,  S1,  S2,  S3, 
	S4,  S5,  S6,  S7, 
	S8,  S9,  S10, S11, 
	S12, S13, S14, S15, 
	S16, S17, S18, S19, 
	S20, S21, S22, S23, 
	S24, S25, S26, S27, 
	S28, S29, S30, S31;
	
	public static REG toReg(int reg0) {
		REG reg;
		if (reg0 == 11) reg = REG.FP;
		else if (reg0 == 13) reg = REG.SP;
		else if (reg0 == 14) reg = REG.LR;
		else if (reg0 == 15) reg = REG.PC;
		else if (reg0 < 16) reg = REG.valueOf("R" + reg0);
		else reg = REG.valueOf("S" + (reg0 - 16));
		return reg;
	}
	
	public static REG toVReg(int reg0) {
		REG reg = REG.valueOf("S" + reg0);
		return reg;
	}
	
	public int toInt() {
		if (this == REG.R0) return 0;
		if (this == REG.R1) return 1;
		if (this == REG.R2) return 2;
		if (this == REG.R3) return 3;
		if (this == REG.R4) return 4;
		if (this == REG.R5) return 5;
		if (this == REG.R6) return 6;
		if (this == REG.R7) return 7;
		if (this == REG.R8) return 8;
		if (this == REG.R9) return 9;
		if (this == REG.R10) return 10;
		if (this == REG.FP) return 11;
		if (this == REG.R12) return 12;
		if (this == REG.SP) return 13;
		if (this == REG.LR) return 14;
		if (this == REG.PC) return 15;
		
		if (this.toString().startsWith("S")) {
			String s = this.toString();
			return 16 + Integer.parseInt(s.substring(1));
		}
		
		return -1;
	}
	
	public boolean isSpecialReg() {
		return this == REG.R10 || this == REG.FP || this == REG.R12 ||
				this == REG.SP || this == REG.LR || this == REG.PC;
	}
	
	public boolean isOperandReg() {
		return this == REG.R0 || this == REG.R1 || this == REG.R2 ||
				this == REG.S0 || this == REG.S1 || this == REG.S2;
	}
	
}
