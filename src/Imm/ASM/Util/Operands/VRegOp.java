package Imm.ASM.Util.Operands;

import Imm.ASM.Util.REG;
import Imm.ASM.Util.Shift;

public class VRegOp extends RegOp {
	
	public REG reg;
	
	public Shift shift;
	
	public VRegOp(REG reg) {
		super(reg);
	}
	
	public VRegOp(int reg) {
		super(REG.toVReg(reg));
	}

} 
