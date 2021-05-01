package Imm.ASM.Structural.Label;

import Imm.ASM.ASMInstruction;

public class ASMLabel extends ASMInstruction {

	public String name;
	
	public boolean isFunctionLabel = false;
	
	public ASMLabel(String name) {
		this.name = name;
	}
	
	public ASMLabel(String name, boolean functionLabel) {
		this.name = name;
		this.isFunctionLabel = functionLabel;
	}
	
	public String build() {
		return this.name + ": ";
	}
	
	public int getRequiredCPUCycles() {
		return 0;
	}
	
} 
