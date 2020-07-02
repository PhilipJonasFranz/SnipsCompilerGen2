package Imm.ASM.Util.Operands;

public class RegOperand extends Operand {

	public enum REGISTER {
		R0, R1, R2, R3, 
		R4, R5, R6, R7, 
		R8, R9, R10, FP, 
		R12, SP, LR, PC;
	}
	
	public REGISTER reg;
	
	public RegOperand(REGISTER reg) {
		this.reg = reg;
	}
	
	public RegOperand(int reg) {
		this.reg = toReg(reg);
	}
	
	public static REGISTER toReg(int reg0) {
		REGISTER reg;
		if (reg0 == 11) reg = REGISTER.FP;
		else if (reg0 == 13) reg = REGISTER.SP;
		else if (reg0 == 14) reg = REGISTER.LR;
		else if (reg0 == 15) reg = REGISTER.PC;
		else reg = REGISTER.valueOf("R" + reg0);
		return reg;
	}

	public String toString() {
		return this.reg.toString().toLowerCase();
	}
	
	public static REGISTER convertStringToReg(String reg) {
		if (reg.equals("sp")) return REGISTER.SP;
		else if (reg.equals("lr")) return REGISTER.LR;
		else if (reg.equals("fp")) return REGISTER.FP;
		else if (reg.equals("pc")) return REGISTER.PC;
		else if (reg.equals("ex")) return REGISTER.R12;
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
					
					return RegOperand.toReg(regNum);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
	}
	
}
