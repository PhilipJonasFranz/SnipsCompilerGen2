package CGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.AST.Statement.Declaration;

public class MemoryMap {

			/* --- NESTED --- */
	public class MemoryCell {
		
		public int wordSize;
		
		private Declaration declaration;
		
		public MemoryCell(Declaration dec) {
			this.declaration = dec;
			this.wordSize = dec.type.wordsize();
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
	
	/**
	 * Check wether given declaration is loaded in the memory map.
	 */
	public boolean declarationLoaded(Declaration dec) {
		return this.map.stream().filter(x -> x.declaration.equals(dec)).count() > 0;
	}
	
	/**
	 * Get the Data Label associated with this declaration.
	 */
	public ASMDataLabel resolve(Declaration dec) {
		return this.cellMapping.get(dec);
	}
	
	public void free(Declaration dec) {
		this.cellMapping.remove(dec);
		for (int i = 0; i < this.map.size(); i++) {
			if (this.map.get(i).declaration != null && this.map.get(i).declaration.equals(dec)) {
				this.map.remove(i);
				return;
			}
		}
	}
	
	public int getByteOffset(Declaration dec) {
		int offset = 0;
		for (int i = 0; i < this.map.size(); i++) {
			if (this.map.get(i).declaration != null && this.map.get(i).declaration.equals(dec)) {
				break;
			}
			else offset += this.map.get(i).wordSize;
		}
		
		return offset * 4;
	}
	
	/**
	 * Add the given declaration to the memory map, 
	 * @param dec
	 * @param dataLabel
	 */
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
