package CGen;

import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Declaration;

public class RegSet {

	public enum STATUS {
		USED, FREE, RESERVED;
	}
	
	public class RegState {
		
		public STATUS status = STATUS.FREE;
		
		public Declaration value;
		
		public Expression expr;
		
		public RegState() {
			
		}
		
		public void free() {
			this.value = null;
			this.expr = null;
			this.status = STATUS.FREE;
		}
		
		public void setDeclaration(Declaration value) {
			this.value = value;
			this.status = STATUS.USED;
		}
		
		public void setExpression(Expression e) {
			this.expr = e;
			this.value = null;
			this.status = STATUS.USED;
		}
		
		public void print() {
			System.out.println("    Status: " + this.status.toString());
			if (this.status == STATUS.USED) {
				if (this.value != null) this.value.print(4, true);
				else if (this.expr != null) this.expr.print(4, true);
			}
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
	
	public int findFree() {
		for (int i = 3; i < 11; i++) {
			if (regs [i].status == STATUS.FREE) {
				return i;
			}
		}
		return -1;
	}
	
	public void copy(int from, int to) {
		this.regs [to].status = this.regs [from].status;
		// TODO Needs cloning
		this.regs [to].value = this.regs [from].value;
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
			if (regs [i].value != null && regs [i].value.equals(dec)) return true;
		}
		return false;
	}
	
	public boolean expressionLoaded(Expression e) {
		for (int i = 0; i < regs.length; i++) {
			if (regs [i].expr != null && regs [i].expr.equals(e)) return true;
		}
		return false;
	}
	
	public int declarationRegLocation(Declaration dec) {
		for (int i = 0; i < regs.length; i++) {
			if (regs [i].value != null && regs [i].value.equals(dec)) return i;
		}
		return -1;
	}
	
	public int ExpressionRegLocation(Expression e) {
		for (int i = 0; i < regs.length; i++) {
			if (regs [i].expr != null && regs [i].expr.equals(e)) return i;
		}
		return -1;
	}
	
	public void free(int...regs) {
		for (int r : regs) this.regs [r].free();
	}
	
}
