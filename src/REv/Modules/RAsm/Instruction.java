package REv.Modules.RAsm;

/**
 * A simple class that stores an assembly instruction and the 
 * line where the instructions is located.
 */
public class Instruction {
	
			/* ---< FIELDS >--- */
	String instruction;
	
	int line;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Instruction(String instruction, int line) {
		this.instruction = instruction.trim();
		
		/* Instruction at line 0 in file is displayed at line index 1 */
		this.line = line + 1;
	}
	
	
			/* ---< METHODS >--- */
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
