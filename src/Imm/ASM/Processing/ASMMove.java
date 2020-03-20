package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Snips.CompilerDriver;

public class ASMMove extends ASMInstruction {

	public Operand target;
	
	public Operand origin;
	
	public ASMMove(Operand target, Operand origin) {
		this.target = target;
		this.origin = origin;
	}
	
	public ASMMove(Operand target, Operand origin, Cond cond) {
		super(cond);
		this.target = target;
		this.origin = origin;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "mov" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.origin.toString();
	}

}
