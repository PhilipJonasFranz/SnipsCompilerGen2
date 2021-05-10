package Imm.ASM.VFP.Memory.Stack;

import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Util.Operands.VRegOp;
import Snips.CompilerDriver;

public class ASMVPopStack extends ASMPopStack {
	
			/* ---< CONSTRUCTORS >--- */
	public ASMVPopStack(VRegOp...operands) {
		super(operands);
	}
	
	
			/* ---< METHODS >--- */
	public String build() {
		String s = CompilerDriver.printDepth + "vpop" + 
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
