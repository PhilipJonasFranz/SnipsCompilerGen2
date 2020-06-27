package REv.Modules.RAsm;

public class Instruction {
	
			/* --- FIELDS --- */
	String instruction;
	
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
	
	public String getInstruction() {
		return this.instruction;
	}
	
	public int getLine() {
		return this.line;
	}
	
}
