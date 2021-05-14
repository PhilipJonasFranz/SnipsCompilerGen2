package Imm.ASM.Memory.Stack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMPopStack extends ASMInstruction {

			/* ---< FIELDS >--- */
	/** The list of operands that are popped in the order of the list. */
	public List<RegOp> operands;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ASMPopStack(RegOp...operands) {
		this.operands = Arrays.stream(operands).collect(Collectors.toList());
	}
	
	
			/* ---< METHODS >--- */
	public String build() {
		String s = CompilerDriver.printDepth + "pop" + 
					((this.cond != null)? this.cond.getCondPostfix() : "" ) + 
					" { ";
		for (int i = 0; i < operands.size(); i++) {
			s += operands.get(i).toString();
			if (i < operands.size() - 1) s += ", ";
		}
		s += " }";
		return s;
	}
	
	public ASMPopStack clone() {
		ASMPopStack pop = new ASMPopStack();
		for (RegOp op : this.operands) pop.operands.add(op.clone());
		return pop;
	}
	
	public int getRequiredCPUCycles() {
		if (this.operands.stream().anyMatch(x -> x.reg == REG.PC)) {
			return this.operands.size() + 4; // +N +(n-1)S +I +N +2S
		}
		else return this.operands.size() + 2; // +N +(n-1)S +I +S
	}
	
} 
