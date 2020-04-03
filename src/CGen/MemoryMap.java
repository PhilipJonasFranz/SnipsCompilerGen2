package CGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Imm.ASM.Structural.Label.ASMDataLabel;
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

	private HashMap<Declaration, ASMDataLabel> cellMapping = new HashMap();

	
			/* --- METHODS --- */
	public List getMemory() {
		return this.map;
	}
	
	public boolean declarationLoaded(Declaration dec) {
		return this.map.stream().filter(x -> x.declaration.equals(dec)).count() > 0;
	}
	
	public ASMDataLabel resolve(Declaration dec) {
		return this.cellMapping.get(dec);
	}
	
	public void add(Declaration dec, ASMDataLabel dataLabel) {
		this.map.add(new MemoryCell(dec));
		this.cellMapping.put(dec, dataLabel);
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
