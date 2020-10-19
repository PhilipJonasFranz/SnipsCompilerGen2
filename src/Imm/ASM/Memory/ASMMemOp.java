package Imm.ASM.Memory;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.Operand;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public abstract class ASMMemOp extends ASMInstruction {

			/* --- FIELDS --- */
	/** The target of the memory operation, or the origin when loading */
	public RegOp target;
	
	public Operand op0, op1;
	
	/* Set to true when op1 is reg Operand and is supposed to be subtracted from base */
	public boolean subFromBase = false;
	
	
			/* --- CONSTRUCTORS --- */
	/** Example Usage: ldr/str r0, [r1, #2] */
	public ASMMemOp(RegOp target, Operand op0, Operand op1) {
		this.target = target;
		this.op0 = op0;
		this.op1 = op1;
	}

	
			/* --- METHODS --- */
	/**
	 * Builds assembly instruction corresponding to the memory operation.
	 * @param operation The type of the operation f.E ldr/str.
	 */
	public String build(String operation) {
		String s = CompilerDriver.printDepth + operation + " " + target.toString();
		
		/* Build with second operand */
		if (op1 != null) 
			s += ", [" + this.op0.toString() + ", " + ((this.subFromBase)? "-" : "") + this.op1.toString() + "]";
		
		/* Build with only one operand */
		else s += ", [" + this.op0.toString() + "]";
		
		return s;
	}
	
} 
