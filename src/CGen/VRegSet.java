package CGen;

import CGen.RegSet.STATUS;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

public class VRegSet {

	public class Reg {
		
		STATUS status = STATUS.FREE;
		
		public Declaration declaration;
		
		public void free() {
			this.declaration = null;
			this.status = STATUS.FREE;
		}
		
		public void setDeclaration(Declaration value) {
			this.declaration = value;
			this.status = STATUS.USED;
		}
		
		public void print() {
			CompilerDriver.outs.println("    Status: " + this.status.toString());
			if (this.status == STATUS.USED) 
				this.declaration.print(4, true);
		}
		
		public boolean isFree() {
			return this.status == STATUS.FREE;
		}
		
	}
	
	Reg [] regs = new Reg [32]; 
	
	public VRegSet() {
		for (int i = 0; i < regs.length; i++) regs [i] = new Reg();
	}
	
	public Reg getReg(int num) {
		return this.regs [num];
	}
	
	public int findFree() {
		for (int i = 3; i < regs.length; i++) 
			if (regs [i].status == STATUS.FREE) return i;
		
		return -1;
	}
	
	public void copy(int from, int to) {
		this.regs [to].status = this.regs [from].status;
		this.regs [to].declaration = this.regs [from].declaration;
	}
	
	public void print() {
		CompilerDriver.outs.println("VRegSet State:");
		for (int i = 0; i < regs.length; i++) {
			CompilerDriver.outs.println("S" + i);
			this.regs [i].print();
		}
	}
	
	public boolean declarationLoaded(Declaration dec) {
		for (int i = 3; i < regs.length; i++) 
			if (regs [i].declaration != null && regs [i].declaration.equals(dec)) return true;
		
		return false;
	}
	
	public int declarationRegLocation(Declaration dec) {
		for (int i = 3; i < regs.length; i++) 
			if (regs [i].declaration != null && regs [i].declaration.equals(dec)) return i;
		
		return -1;
	}
	
	public void free(int...regs) {
		for (int r : regs) this.regs [r].free();
	}
	
} 
