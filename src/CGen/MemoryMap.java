package CGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

/**
 * The memory map is used to lay out the static .data memory and get the memory offsets of the
 * variables store in it.
 */
public class MemoryMap {

			/* ---< NESTED >--- */
	/**
	 * Memory cell to describe an entry in the memory cell. The cells can have
	 * different word sizes based on the declaration they capsule.
	 */
	public class MemoryCell {
		
				/* ---< FIELDS >--- */
		/** The declaration that this cell houses */
		private Declaration declaration;
		
		
				/* ---< CONSTRUCTORS >--- */
		/** Default constructor */
		public MemoryCell(Declaration dec) {
			this.declaration = dec;
		}
		
		
				/* ---< METHODS >--- */
		/** Returns the stored declaration */
		public Declaration getDeclaration() {
			return this.declaration;
		}
		
	}
	
	
			/* ---< FIELDS >--- */
	/** The map that houses all active memory cells. */
	private List<MemoryCell> map = new ArrayList();

	/**
	 * This hash map stores the pairs of declarations stored in the data memory and their
	 * corresponding data label.
	 */
	private HashMap<Declaration, ASMDataLabel> cellMapping = new HashMap();

	
			/* ---< METHODS >--- */
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
	
	/**
	 * Add the given declaration to the memory map and wraps it in a new memory cell.
	 * @param dec The declaration to add.
	 * @param dataLabel The data label linked to the declaration.
	 */
	public void add(Declaration dec, ASMDataLabel dataLabel) {
		this.map.add(new MemoryCell(dec));
		this.cellMapping.put(dec, dataLabel);
	}
	
	/** Prints out the memory layout. */
	public void print() {
		CompilerDriver.outs.println("\n----< MEMORY START ----");
		for (int i = 0; i < this.map.size(); i++) {
			MemoryCell x = this.map.get(i);
			x.declaration.print(4, true);
		}
		CompilerDriver.outs.println("----< MEMORY END >----\n");
	}
	
	/**
	 * Returns the map in the form of a list of memory cells.
	 * The order of the memory cells corresponds to the final
	 * memory layout, where the first memory cell lies at the
	 * start of the memory section.
	 */
	public List<MemoryCell> getMap() {
		return this.map;
	}
	
} 
