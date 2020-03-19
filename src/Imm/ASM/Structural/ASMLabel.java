package Imm.ASM.Structural;

import Imm.ASM.ASMInstruction;

public class ASMLabel extends ASMInstruction {

	public String name;
	
	public ASMLabel(String name) {
		this.name = name;
	}
	
	public String build() {
		return this.name + ":";
	}
	
}
