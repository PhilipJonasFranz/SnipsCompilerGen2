package Imm.ASM.VFP.Memory.Stack;

import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.VRegOp;
import Snips.CompilerDriver;

public class ASMVPushStack extends ASMPushStack {

	
			/* ---< CONSTRUCTORS >--- */
	public ASMVPushStack(VRegOp...operands) {
		super(operands);
	}
	
	
			/* ---< METHODS >--- */
	public String build() {
		String s = CompilerDriver.printDepth + "vpush" + 
					((this.cond != null)? this.cond.getCondPostfix() : "" ) + 
					" { ";
		for (int i = 0; i < operands.size(); i++) {
			s += operands.get(i).toString();
			if (i < operands.size() - 1) s += ", ";
		}
		s += " }";
		return s;
	}
	
} 
