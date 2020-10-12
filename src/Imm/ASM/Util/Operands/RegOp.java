package Imm.ASM.Util.Operands;

import Imm.ASM.Util.Shift;

public class RegOp extends Operand {

	public enum REG {
		R0, R1, R2, R3, 
		R4, R5, R6, R7, 
		R8, R9, R10, FP, 
		R12, SP, LR, PC,
		
		/* Special Reg, used to signal unbound data on stack. See AsNAddressOf. */
		RX;
	}
	
	public REG reg;
	
	public Shift shift;
	
	public RegOp(REG reg) {
		this.reg = reg;
	}
	
	public RegOp(REG reg, Shift shift) {
		this.reg = reg;
		this.shift = shift;
	}
	
	public RegOp(int reg) {
		this.reg = toReg(reg);
	}
	
	public static REG toReg(int reg0) {
		REG reg;
		if (reg0 == 11) reg = REG.FP;
		else if (reg0 == 13) reg = REG.SP;
		else if (reg0 == 14) reg = REG.LR;
		else if (reg0 == 15) reg = REG.PC;
		else reg = REG.valueOf("R" + reg0);
		return reg;
	}
	
	public static int toInt(REG reg) {
		if (reg == REG.R0) return 0;
		if (reg == REG.R1) return 1;
		if (reg == REG.R2) return 2;
		if (reg == REG.R3) return 3;
		if (reg == REG.R4) return 4;
		if (reg == REG.R5) return 5;
		if (reg == REG.R6) return 6;
		if (reg == REG.R7) return 7;
		if (reg == REG.R8) return 8;
		if (reg == REG.R9) return 9;
		if (reg == REG.R10) return 10;
		if (reg == REG.FP) return 11;
		if (reg == REG.R12) return 12;
		if (reg == REG.SP) return 13;
		if (reg == REG.LR) return 14;
		if (reg == REG.PC) return 15;
		return -1;
	}

	public String toString() {
		return this.reg.toString().toLowerCase() + ((this.shift != null)? this.shift.getShiftPostfix() : "");
	}
	
	public static REG convertStringToReg(String reg) {
		if (reg.equals("sp")) return REG.SP;
		else if (reg.equals("lr")) return REG.LR;
		else if (reg.equals("fp")) return REG.FP;
		else if (reg.equals("pc")) return REG.PC;
		else {
			if (reg.length() < 2) {
				return null;
			}
			else {
				String r0 = reg.substring(1);
				try {
					int regNum = Integer.parseInt(r0);
					
					if (regNum < 0 || regNum > 15) {
						return null;
					}
					
					return RegOp.toReg(regNum);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
	}

	public RegOp clone() {
		RegOp op = new RegOp(this.reg);
		if (this.shift != null) op.shift = this.shift.clone();
		return op;
	}

	public boolean equals(Operand operand) {
		if (!(operand instanceof RegOp)) return false;
		else {
			RegOp op = (RegOp) operand;
			if (this.shift != null) {
				if (op.shift == null) return false;
				else {
					if (op.shift.distance != this.shift.distance || op.shift.shift != this.shift.shift) return false;
				}
			}
			else if (this.shift != null) return false;
			
			return this.reg == op.reg;
		}
	}
	
} 
