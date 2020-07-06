package Imm.ASM;

import Imm.ASM.Util.Cond;
import Snips.CompilerDriver;

public class ASMHardcode extends ASMInstruction {

	public String hardCode;
	
			/* --- CONSTRUCTORS --- */
	public ASMHardcode(String hardCode) {
		this.hardCode = hardCode;
	}
	
	/**
	 * Constructor for instruction with conditional.
	 */
	public ASMHardcode(Cond cond) {
		this.cond = cond;
	}
	
	
			/* --- METHODS --- */
	/**
	 * Create the ASM memonic representation of this instruction.
	 * @return
	 */
	public String build() {
		return CompilerDriver.printDepth + this.hardCode;
	}
	
}
