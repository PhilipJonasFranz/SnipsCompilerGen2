package Imm.ASM.Util.Operands;

import Exc.SNIPS_EXC;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Shift;

public class VRegOp extends RegOp {
	
	public REG reg;
	
	public Shift shift;
	
	public VRegOp(REG reg) {
		super(reg);
		if (reg.toInt() < 16) throw new SNIPS_EXC("Found " + reg + " in VRegOp constructor call!");
	}
	
	public VRegOp(int reg) {
		super(REG.toVReg(reg));
	}

} 
