package Imm.ASM.VFP.Memory;

import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public class ASMVStr extends ASMStr {

			/* ---< CONSTRUCTORS >--- */
	public ASMVStr(RegOp target, Operand op0, Operand op1) {
		super(target, op0, op1);
	}
	
	public ASMVStr(RegOp target, Operand op0) {
		super(target, op0, null);
	}
	
			/* ---< METHODS >--- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	public String build() {
		return super.build("vstr");
	}
	
} 
