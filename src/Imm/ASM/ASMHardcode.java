package Imm.ASM;

import Snips.CompilerDriver;

public class ASMHardcode extends ASMInstruction {

	public String hardCode;
	
			/* ---< CONSTRUCTORS >--- */
	public ASMHardcode(String hardCode) {
		this.hardCode = hardCode;
	}

	
			/* ---< METHODS >--- */
	/**
	 * Create the ASM memonic representation of this instruction.
	 */
	public String build() {
		return CompilerDriver.printDepth + this.hardCode;
	}
	
} 
