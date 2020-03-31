package Imm.ASM.Processing;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOperand;

public abstract class ASMUnaryData extends ASMInstruction {

	public RegOperand target;
	
	public Operand op0;
	
	public ASMUnaryData(RegOperand target, Operand op0) {
		this.target = target;
		this.op0 = op0;
	}
	
	public ASMUnaryData(RegOperand target, Operand op0, Cond cond) {
		super(cond);
		this.target = target;
		this.op0 = op0;
	}
	
	public abstract String build();

}
