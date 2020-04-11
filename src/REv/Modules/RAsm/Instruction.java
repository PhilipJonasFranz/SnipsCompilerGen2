package REv.Modules.RAsm;

import lombok.Getter;

public class Instruction {
	
			/* --- FIELDS --- */
	@Getter
	String instruction;
	
	@Getter
	int line;
	
	
			/* --- CONSTRUCTORS --- */
	public Instruction(String instruction, int line) {
		this.instruction = instruction.trim();
		this.line = line + 1;
	}
	
	
			/* --- METHODS --- */
	public void setInstruction(String newInstr) {
		this.instruction = newInstr.trim();
	}
	
	public boolean isEmpty() {
		return this.instruction.equals("");
	}
	
}
