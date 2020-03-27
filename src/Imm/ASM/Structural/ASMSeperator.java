package Imm.ASM.Structural;

import Imm.ASM.ASMInstruction;
import Snips.CompilerDriver;

public class ASMSeperator extends ASMInstruction {

	public ASMSeperator() {
	
	}

	public String build() {
		return CompilerDriver.printDepth;
	}
	
}
