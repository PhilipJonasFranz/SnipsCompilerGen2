package REv.Modules.RAsm;

public class Instruction {
	
	String instruction;
	
	int line;
	
	public Instruction(String instruction, int line) {
		this.instruction = instruction.trim();
		this.line = line + 1;
	}
	
	public int getLine() {
		return this.line;
	}
	
	public String getInstruction() {
		return this.instruction;
	}
	
	public void setInstruction(String newInstr) {
		this.instruction = newInstr.trim();
	}
	
	public boolean isEmpty() {
		return this.instruction.equals("");
	}
	
}
