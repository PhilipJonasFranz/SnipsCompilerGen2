package CGen;

import Imm.AST.Statement.Declaration;

public class RegSet {

	public enum STATUS {
		USED, FREE, RESERVED;
	}
	
	public class RegState {
		
		public STATUS status = STATUS.FREE;
		
		public Declaration declaration;
		
		public RegState() {
			
		}
		
		public void free() {
			this.declaration = null;
			this.status = STATUS.FREE;
		}
		
		public void setDeclaration(Declaration value) {
			this.declaration = value;
			this.status = STATUS.USED;
		}
		
		public void print() {
			System.out.println("    Status: " + this.status.toString());
			if (this.status == STATUS.USED) 
				this.declaration.print(4, true);
		}
		
		public boolean isFree() {
			return this.status == STATUS.FREE;
		}
		
	}
	
	private RegState [] regs = new RegState [16]; 
	
	public RegSet() {
		for (int i = 0; i < regs.length; i++) regs [i] = new RegState();
		for (int i = 11; i < regs.length; i++) regs [i].status = STATUS.RESERVED;
	}
	
	public RegState getReg(int num) {
		return this.regs [num];
	}
	
	/**
	 * Find the first register that has the status free.
	 * @return The number of this register.
	 */
	public int findFree() {
		for (int i = 3; i < 11; i++) {
			if (regs [i].status == STATUS.FREE) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Copy the content and the status from the first to the second register.
	 */
	public void copy(int from, int to) {
		this.regs [to].status = this.regs [from].status;
		this.regs [to].declaration = this.regs [from].declaration;
	}
	
	public void print() {
		System.out.println("RegSet State:");
		for (int i = 0; i < regs.length; i++) {
			System.out.println("R" + i);
			this.regs [i].print();
		}
	}
	
	public boolean declarationLoaded(Declaration dec) {
		for (int i = 0; i < regs.length; i++) {
			if (regs [i].declaration != null && regs [i].declaration.equals(dec)) return true;
		}
		return false;
	}
	
	public int declarationRegLocation(Declaration dec) {
		for (int i = 0; i < regs.length; i++) {
			if (regs [i].declaration != null && regs [i].declaration.equals(dec)) return i;
		}
		return -1;
	}
	
	public void free(int...regs) {
		for (int r : regs) this.regs [r].free();
	}
	
}
