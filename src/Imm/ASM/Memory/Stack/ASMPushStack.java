package Imm.ASM.Memory.Stack;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ASMPushStack extends ASMInstruction {

			/* ---< FIELDS >--- */
	/** The list of operands that are pushed in the order of the list. */
	public List<RegOp>operands;
	
	public ASMPopStack popCounterpart;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ASMPushStack(RegOp...operands) {
		this.operands = Arrays.stream(operands).collect(Collectors.toList());
	}
	
	
			/* ---< METHODS >--- */
	public String build() {
		String s = CompilerDriver.printDepth + "push" + 
					((this.cond != null)? this.cond.getCondPostfix() : "" ) + " ";

		s += operands.stream().map(RegOp::toString).collect(Collectors.joining(", ", "{ ", " }"));

		return s;
	}
	
	public int getRequiredCPUCycles() {
		return this.operands.size() + 2; // +N +(n-1)S +I +S
	}
	
} 
