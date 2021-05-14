package Imm.ASM.VFP.Memory.Stack;

import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;

public class ASMVStrStack extends ASMStrStack {

			/* ---< CONSTRUCTORS >--- */
	public ASMVStrStack(MEM_OP memOp, RegOp target, RegOp op0, Operand op1) {
		super(memOp, target, op0, op1);
	}
	
	
			/* ---< METHODS >--- */
	/**
	 * Calls build of ASMMemOp with class as parameter.
	 */
	public String build() {
		return super.build("vstr");
	}
	
} 
