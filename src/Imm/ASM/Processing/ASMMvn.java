package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMMvn extends ASMInstruction {

	public RegOperand target;
	
	public Operand origin;
	
	public ASMMvn(RegOperand target, Operand origin) {
		this.target = target;
		this.origin = origin;
	}
	
	public ASMMvn(RegOperand target, Operand origin, Cond cond) {
		super(cond);
		this.target = target;
		this.origin = origin;
	}
	
	public String build() {
		return CompilerDriver.printDepth + "mvn" + ((this.cond != null)? this.cond.getCondPostfix() : "" ) + " " + this.target.toString() + ", " + this.origin.toString();
	}

}
