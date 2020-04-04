package CGen;

import java.util.Stack;

import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.Declaration;

public class StackSet {

			/* --- NESTED --- */
	public enum CONTENT_TYPE {
		REGISTER, DECLARATION;
	}
	

	public class StackCell {
		
		private CONTENT_TYPE contentType;
		
		private REGISTER reg;
		
		private Declaration declaration;
		
		public StackCell(REGISTER reg) {
			this.reg = reg;
			this.contentType = CONTENT_TYPE.REGISTER;
		}
		
		public StackCell(Declaration dec) {
			this.declaration = dec;
			this.contentType = CONTENT_TYPE.DECLARATION;
		}
		
		public CONTENT_TYPE getType() {
			return this.contentType;
		}
		
		public REGISTER getReg() {
			return this.reg;
		}
		
		public Declaration getDeclaration() {
			return this.declaration;
		}
	}
	
	
			/* --- FIELDS --- */
	private Stack<StackCell> stack = new Stack();
	
	private Stack<Integer> scopes = new Stack();

	/**
	* Wether new declarations have been pushed on the stack. Only used by AsNFunction to determine
	* the registers to save/restore.
	*/
	public boolean newDecsOnStack = false;


			/* --- METHODS --- */
	public Stack getStack() {
		return this.stack;
	}
	
	public void push(Declaration...dec) {
		for (Declaration dec0 : dec) this.stack.push(new StackCell(dec0));
		this.newDecsOnStack = true;
	}
	
	public void push(REGISTER...reg) {
		for (REGISTER reg0 : reg) this.stack.push(new StackCell(reg0));
	}
	
	public void pop() {
		this.stack.pop();
	}
	
	public void popXCells(int x) {
		for (int i = 0; i < x; i++) {
			this.stack.pop();
		}
	}
	
	public void popXWords(int x) {
		int words = 0;
		while (words < x) {
			if (this.stack.peek().contentType == CONTENT_TYPE.REGISTER) {
				words++;
			}
			else {
				words += this.stack.peek().declaration.type.wordsize();
			}
			this.stack.pop();
		}
	}
	
	public void print() {
		System.out.println("\n---- STACK TOP ----");
		for (int i = this.stack.size() - 1; i >= 0; i--) {
			StackCell x = this.stack.get(i);
			System.out.println(x.contentType.toString() + ": ");
			if (x.contentType == CONTENT_TYPE.DECLARATION) {
				x.declaration.print(4, true);
			}
			else System.out.println("    " + x.reg.toString());
		}
		System.out.println("---- STACK BASE ----\n");
	}
	
	/**
	 * Finds the offset to a parameter passed in the stack. Returns -1 if given declaration
	 * is not a parameter.
	 */
	public int getParameterByteOffset(Declaration dec) {
		int x = 0;
		int off = 0;
		boolean foundHook = false;
		while (true) {
			if (stack.get(x).contentType == CONTENT_TYPE.REGISTER && stack.get(x).reg == REGISTER.LR) {
				if (!foundHook) return -1;
				else return off;
			}
			else if (stack.get(x).contentType == CONTENT_TYPE.DECLARATION && stack.get(x).declaration.equals(dec)) {
				off = 0;
				foundHook = true;
			}
			else off += 4;
			x++;
		}
	}
	
	/**
	 * Finds the offset to a local variable in the stack.
	 */
	public int getDeclarationInStackByteOffset(Declaration dec) {
		int off = 0;
		for (int i = 0; i < stack.size(); i++) {
			if (stack.get(i).contentType == CONTENT_TYPE.REGISTER) 
				off = 4;
			else if (stack.get(i).contentType == CONTENT_TYPE.DECLARATION) {
				if (stack.get(i).declaration.equals(dec)) break;
				off += (stack.get(i).declaration.type.wordsize() * 4);
			}
		}
		
		return off;
	}
	
	/**
	 * Open a new scope, f.e. for the body of a compound statement.
	 */
	public void openScope() {
		this.scopes.push(this.stack.size());
	}
	
	/**
	 * Pop all stack cells from the stack that were pushed within the scope or body of
	 * a compound statement. Returns the amount of bytes the stack was reset. This amount
	 * has to be added onto the stack pointer to reset the stack to the correct size.
	 */
	public int closeScope() {
		int target = this.scopes.pop();
		int add = 0;
		while (this.stack.size() != target) {
			this.stack.pop();
			add++;
		}
		
		return add * 4;
	}
	
}
