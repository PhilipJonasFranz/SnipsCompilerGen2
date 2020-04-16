package CGen;

import java.util.Stack;

import Exc.CGEN_EXCEPTION;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Util.Pair;
import lombok.Getter;

public class StackSet {

			/* --- NESTED --- */
	/** Used to identify the contents of a stack cell. */
	public enum CONTENT_TYPE {
		REGISTER, DECLARATION;
	}
	
	/**
	 * A stack cell capsules a content type, and a register or a declaration. The wordsize of the stack cell
	 * is 1 if the content type is a register, and the word size of the type of the declaration if the content
	 * type is a declaration.
	 */
	public class StackCell {
		
		/** The content type of the cell. */
		@Getter
		private CONTENT_TYPE type;
		
		/** The stored register. */
		@Getter
		private REGISTER reg;
		
		/** The stored declaration */
		@Getter
		private Declaration declaration;
		
		/** Create a new stack cell, set the type to register and set given register. */
		public StackCell(REGISTER reg) {
			this.reg = reg;
			this.type = CONTENT_TYPE.REGISTER;
		}
		
		/** Create a new stack cell, set the type to declaration and set given declaration. */
		public StackCell(Declaration dec) {
			this.declaration = dec;
			this.type = CONTENT_TYPE.DECLARATION;
		}
	}
	
	
			/* --- FIELDS --- */
	@Getter
	/** The stack that houses the stack cells. */
	private Stack<StackCell> stack = new Stack();
	
	/** The stack that contains the scope sizes. See {@link #closeScope()} for more information. */
	private Stack<Pair<Integer, CompoundStatement>> scopes = new Stack();

	/**
	* Wether new declarations have been pushed on the stack. Only used by AsNFunction to determine
	* the registers to save/restore.
	*/
	public boolean newDecsOnStack = false;


			/* --- METHODS --- */
	/** Push all given declarations on the stack and wrap them in stack cells. 
	 * The first given declaration will end up on the bottom of the newly pushed stack section.
	 */
	public void push(Declaration...dec) {
		for (Declaration dec0 : dec) this.stack.push(new StackCell(dec0));
		this.newDecsOnStack = true;
	}
	
	/** Push all given registers on the stack and wrap them in stack cells. 
	 * The first given register will end up on the bottom of the newly pushed stack section.
	 */
	public void push(REGISTER...reg) {
		for (REGISTER reg0 : reg) this.stack.push(new StackCell(reg0));
	}
	
	/** Pop a stack cell from the stack top. */
	public void pop() {
		this.stack.pop();
	}
	
	/** Pop x cells from the top of the stack */
	public void popXCells(int x) {
		for (int i = 0; i < x; i++) {
			this.stack.pop();
		}
	}
	
	/** 
	 * Pop given amount of words from the stack. Throws an CGEN_EXCEPTION if not exactly x words can be popped.
	 * This will mostly be caused by an internal compilation logic error.
	 */
	public void popXWords(int x) throws CGEN_EXCEPTION {
		int words = 0;
		while (words < x) {
			if (this.stack.peek().type == CONTENT_TYPE.REGISTER) {
				words++;
			}
			else {
				words += this.stack.peek().declaration.getType().wordsize();
			}
			this.stack.pop();
		}
		
		if (words != x) {
			throw new CGEN_EXCEPTION("Unable to pop " + x + " Words from the stack, could only pop " + words);
		}
	}
	
	/** Prints out the stack layout and the contents of the stack cells. */
	public void print() {
		System.out.println("\n---- STACK TOP ----");
		for (int i = this.stack.size() - 1; i >= 0; i--) {
			StackCell x = this.stack.get(i);
			System.out.println(x.type.toString() + ": ");
			if (x.type == CONTENT_TYPE.DECLARATION) {
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
		int off = 0;
		boolean foundHook = false;
		for (int x = 0; x < stack.size(); x++) {
			if (stack.get(x).type == CONTENT_TYPE.REGISTER && stack.get(x).reg == REGISTER.LR) {
				if (!foundHook) return -1;
				else return off;
			}
			else if (stack.get(x).type == CONTENT_TYPE.DECLARATION && stack.get(x).declaration.equals(dec)) {
				off = 0;
				foundHook = true;
			}
			else {
				if (stack.get(x).type == CONTENT_TYPE.DECLARATION) off += stack.get(x).declaration.getType().wordsize() * 4;
				else off += 4;
			}
		}
		return -1;
	}
	
	/**
	 * Finds the offset to a local variable in the stack.
	 */
	public int getDeclarationInStackByteOffset(Declaration dec) {
		int off = 4;
		for (int i = 0; i < stack.size(); i++) {
			if (stack.get(i).type == CONTENT_TYPE.REGISTER) 
				if (stack.get(i).reg == REGISTER.FP || stack.get(i).reg == REGISTER.LR) off = 4;
				else off += 4;
			else if (stack.get(i).type == CONTENT_TYPE.DECLARATION) {
				if (stack.get(i).declaration.equals(dec)) break;
				off += (stack.get(i).declaration.getType().wordsize() * 4);
			}
		}
		
		return off;
	}
	
	/**
	 * Open a new scope, f.e. for the body of a compound statement. Also pushes the current stack size
	 * on the scopes stack, so that when popping this scope, the amount of words to be added to the 
	 * sp can be determined.
	 */
	public void openScope(CompoundStatement cs) {
		this.scopes.push(new Pair<Integer, CompoundStatement>(this.stack.size(), cs));
	}
	
	/**
	 * Pop all stack cells from the stack that were pushed within the scope or body of
	 * a compound statement. Returns the amount of bytes the stack was reset. This amount
	 * has to be added onto the stack pointer to reset the stack to the correct size.
	 */
	public int closeScope(CompoundStatement cs, boolean close) {
		int target = 0;
		
		if (close) {
			target = this.scopes.pop().getFirst();
		}
		else {
			for (int i = this.scopes.size() - 1; i >= 0; i--) {
				target = this.scopes.get(i).getFirst();
				if (this.scopes.get(i).getSecond().equals(cs)) break;
			}
		}
		
		int add = 0;
		if (close) {
			while (this.stack.size() != target) {
				this.stack.pop();
				add++;
			}
		}
		else {
			Stack<StackCell> st0 = new Stack();
			while (this.stack.size() != target) {
				st0.push(this.stack.pop());
				add++;
			}
			
			while (!st0.isEmpty()) {
				this.stack.push(st0.pop());
			}
		}
		
		return add * 4;
	}
	
}
