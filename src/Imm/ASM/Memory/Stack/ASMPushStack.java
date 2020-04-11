package Imm.ASM.Memory.Stack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMPushStack extends ASMInstruction {

			/* --- FIELDS --- */
	/** The list of operands that are pushed in the order of the list. */
	public List<RegOperand>operands;
	
	
			/* --- CONSTRUCTORS --- */
	public ASMPushStack(RegOperand...operands) {
		this.operands = Arrays.stream(operands).collect(Collectors.toList());
	}
	
	public ASMPushStack(Cond cond, RegOperand...operands) {
		super(cond);
		this.operands = Arrays.stream(operands).collect(Collectors.toList());
	}

	
			/* --- METHODS --- */
	public String build() {
		String s = CompilerDriver.printDepth + "push" + 
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
