package CGen;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Statement.Declaration;

public class MemoryMap {

			/* --- NESTED --- */
	public class MemoryCell {
		
		private Declaration declaration;
		
		public MemoryCell(Declaration dec) {
			this.declaration = dec;
		}
		
		public Declaration getDeclaration() {
			return this.declaration;
		}
	}
	
	
			/* --- FIELDS --- */
	private List<MemoryCell> map = new ArrayList();

	/**
	* Wether new declarations have been pushed on the stack. Only used by AsNFunction to determine
	* the registers to save/restore.
	*/
	public boolean newDecsOnStack = false;


			/* --- METHODS --- */
	public List getMemory() {
		return this.map;
	}
	
	public boolean declarationLoaded(Declaration dec) {
		return this.map.stream().filter(x -> x.equals(dec)).count() > 0;
	}
	
	public void push(Declaration...dec) {
		for (Declaration dec0 : dec) this.map.add(new MemoryCell(dec0));
		this.newDecsOnStack = true;
	}
	
	public void print() {
		System.out.println("\n---- MEMORY START ----");
		for (int i = 0; i < this.map.size(); i++) {
			MemoryCell x = this.map.get(i);
			x.declaration.print(4, true);
		}
		System.out.println("---- MEMORY END ----\n");
	}
	
}
