package Imm.ASM.Directive;

import Imm.ASM.ASMInstruction;

public class ASMDirective extends ASMInstruction {

	public String hardCode;
	
			/* ---< CONSTRUCTORS >--- */
	public ASMDirective(String hardCode) {
		this.hardCode = hardCode;
	}

	
			/* ---< METHODS >--- */
	/**
	 * Create the ASM memonic representation of this instruction.
	 */
	public String build() {
		return this.hardCode;
	}
	
	public int getRequiredCPUCycles() {
		return 0;
	}
	
} 
