package Imm.ASM.Util.Operands;

import Imm.ASM.Util.REG;
import Imm.ASM.Util.Shift;

public class RegOp extends Operand {
	
	public REG reg;
	
	public Shift shift;
	
	public RegOp(REG reg) {
		this.reg = reg;
	}
	
	public RegOp(int reg) {
		this.reg = REG.toReg(reg);
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
					
					return REG.toReg(regNum);
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
