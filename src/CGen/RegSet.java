package CGen;

import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

public class RegSet {

			/* ---< NESTED >--- */
	/** Used to identify the state of a register */
	private enum STATUS {
		USED, FREE, RESERVED;
	}
	
	/**
	 * A reg capsuled a status field, and a declaration statement. When a reg is free,
	 * the declaration is set to null. If its used, the declaration will contain the variable
	 * currently stored in the register. If the reg is reserved, it will never contain anything.
	 */
	public class Reg {
		
				/* ---< FIELDS >--- */
		/** The status of the Register */
		private STATUS status = STATUS.FREE;
		
		/** The declaration that is currently in the register. */
		public Declaration declaration;
		
				/* ---< METHODS >--- */
		/** Free the register, set the declaration to null and set the status to free */
		public void free() {
			this.declaration = null;
			this.status = STATUS.FREE;
		}
		
		/** Set the declaration of the reg to the given one and set the status to used. */
		public void setDeclaration(Declaration value) {
			this.declaration = value;
			this.status = STATUS.USED;
		}
		
		/** Print out the register status and contents. */
		public void print() {
			CompilerDriver.outs.println("    Status: " + this.status.toString());
			if (this.status == STATUS.USED) 
				this.declaration.print(4, true);
		}
		
		/** Return wether the reg status is free. */
		public boolean isFree() {
			return this.status == STATUS.FREE;
		}
		
	}
	
	
			/* ---< FIELDS >--- */
	/** The register array. */
	private Reg [] regs = new Reg [16]; 
	
	
			/* ---< CONSTRUCTORS >--- */
	/** Create a new RegSet object, initialize the regs, set regs 11-15 to reserved. */
	public RegSet() {
		for (int i = 0; i < regs.length; i++) regs [i] = new Reg();
		for (int i = 10; i < regs.length; i++) regs [i].status = STATUS.RESERVED;
	}
	
	/** Returns the {@link num} reg. */
	public Reg getReg(int num) {
		return this.regs [num];
	}
	
	/**
	 * Find the lowest register number that has the status free.
	 * If no register is free, -1 is returned.
	 * @return The number of this register.
	 */
	public int findFree() {
		for (int i = 3; i < 11; i++) 
			if (regs [i].status == STATUS.FREE) return i;
		
		return -1;
	}
	
	/**
	 * Copy the content and the status from the first to the second register.
	 */
	public void copy(int from, int to) {
		this.regs [to].status = this.regs [from].status;
		this.regs [to].declaration = this.regs [from].declaration;
	}
	
	/** 
	 * Prints out the reg set and all of its registers via Reg.print().
	 */
	public void print() {
		CompilerDriver.outs.println("RegSet State:");
		for (int i = 0; i < regs.length; i++) {
			CompilerDriver.outs.println("R" + i);
			this.regs [i].print();
		}
	}
	
	/** Check wether given declaration is loaded in any register. */
	public boolean declarationLoaded(Declaration dec) {
		for (int i = 3; i < regs.length; i++) 
			if (regs [i].declaration != null && regs [i].declaration.equals(dec)) return true;
		
		return false;
	}
	
	/** 
	 * Get the register number where given declaration is loaded. Returns -1 if the declaration is not loaded. 
	 */
	public int declarationRegLocation(Declaration dec) {
		for (int i = 3; i < regs.length; i++) 
			if (regs [i].declaration != null && regs [i].declaration.equals(dec)) return i;
		
		return -1;
	}
	
	/** Free all given regs */
	public void free(int...regs) {
		for (int r : regs) this.regs [r].free();
	}
	
} 
