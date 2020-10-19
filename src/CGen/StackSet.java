package CGen;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import Exc.CGEN_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Res.Const;
import Util.Pair;

public class StackSet {

			/* ---< NESTED >--- */
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
		private CONTENT_TYPE type;
		
		/** The stored register. */
		private REG reg;
		
		/** The stored declaration */
		private Declaration declaration;
		
		/** Create a new stack cell, set the type to register and set given register. */
		public StackCell(REG reg) {
			this.reg = reg;
			this.type = CONTENT_TYPE.REGISTER;
		}
		
		/** Create a new stack cell, set the type to declaration and set given declaration. */
		public StackCell(Declaration dec) {
			this.declaration = dec;
			this.type = CONTENT_TYPE.DECLARATION;
		}
		
		/** Returns the Content type of this stack cell. */
		public CONTENT_TYPE getType() {
			return this.type;
		}
		
		/** Returns the register stored in this stack cell. */
		public REG getReg() {
			return this.reg;
		}
		
		/** Returns the declaration stored in this stack cell. */
		public Declaration getDeclaration() {
			return this.declaration;
		}
		
	}
	
	
			/* ---< FIELDS >--- */
	/** The stack that houses the stack cells. */
	private Stack<StackCell> stack = new Stack();
	
	/** The stack that contains the scope sizes. See {@link #closeScope()} for more information. */
	private Stack<Pair<Integer, CompoundStatement>> scopes = new Stack();

	/**
	* Wether new declarations have been pushed on the stack. Only used by AsNFunction to determine
	* the registers to save/restore.
	*/
	public boolean newDecsOnStack = false;


			/* ---< METHODS >--- */
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
	public void push(REG...reg) {
		for (REG reg0 : reg) {
			this.stack.push(new StackCell(reg0));
			if (reg0 == REG.RX)
				this.newDecsOnStack = true;
		}
	}
	
	/** Pop a stack cell from the stack top. */
	public void pop() {
		this.stack.pop();
	}
	
	/** 
	 * Pops the x first memory cells from the top of the stack. 
	 * Returns the sum of the word sizes of the popped cells.
	 */
	public int popXCells(int x) throws CGEN_EXC {
		int bytes = 0;
		for (int i = 0; i < x; i++) {
			if (stack.isEmpty())
				throw new CGEN_EXC(Const.UNABLE_TO_POP_X_WORDS, x, i);
			
			StackCell c = this.stack.pop();
			
			if (c.type == CONTENT_TYPE.REGISTER) bytes += 4;
			else bytes += c.declaration.getType().wordsize();
		}
		
		return bytes;
	}
	
	/** 
	 * Pop given amount of words from the stack. Throws an CGEN_EXCEPTION if not exactly x words can be popped.
	 * This will mostly be caused by an internal compilation logic error.
	 */
	public void popXWords(int x) throws CGEN_EXC {
		int words = 0;
		while (words < x) {
			if (this.stack.peek().type == CONTENT_TYPE.REGISTER) words++;
			else words += this.stack.peek().declaration.getType().wordsize();
			
			this.stack.pop();
		}
		
		if (words != x) 
			throw new CGEN_EXC(Const.UNABLE_TO_POP_X_WORDS, x, words);
	}
	
	/** Prints out the stack layout and the contents of the stack cells. */
	public void print() {
		System.out.println("\n---- STACK TOP ----");
		
		for (int i = this.stack.size() - 1; i >= 0; i--) {
			StackCell x = this.stack.get(i);
			
			System.out.println(x.type.toString() + ": ");
			
			if (x.type == CONTENT_TYPE.DECLARATION) 
				x.declaration.print(4, true);
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
			if (stack.get(x).type == CONTENT_TYPE.REGISTER && stack.get(x).reg == REG.LR) {
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
		
		boolean regs = false;
		
		/* Check if LR or FP regs were pushed */
		for (int i = 0; i < stack.size(); i++) 
			if (stack.get(i).type == CONTENT_TYPE.REGISTER && stack.get(i).reg == REG.FP || stack.get(i).reg == REG.LR) 
				regs = true;
		
		boolean hook = false;
		for (int i = 0; i < stack.size(); i++) {
			if (stack.get(i).type == CONTENT_TYPE.REGISTER) 
				if (stack.get(i).reg == REG.FP || stack.get(i).reg == REG.LR) {
					hook = true;
					off = 4;
				}
				else off += 4;
			else if (stack.get(i).type == CONTENT_TYPE.DECLARATION) {
				if (stack.get(i).declaration.equals(dec)) {
					/* Hook was not found and there is a reg section */
					if (!hook && regs) return -1;
					else return off;
				}
				off += (stack.get(i).declaration.getType().wordsize() * 4);
			}
		}
		
		return -1;
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
		int target = this.stack.size();
		
		if (close) target = this.scopes.pop().getFirst();
		else {
			for (int i = this.scopes.size() - 1; i >= 0; i--) {
				target = this.scopes.get(i).getFirst();
				if (this.scopes.get(i).getSecond().equals(cs)) break;
			}
		}
		
		int add = 0;
		if (close) {
			while (this.stack.size() > target) {
				StackCell c = this.stack.pop();
				if (c.type == CONTENT_TYPE.REGISTER) add++;
				else add += c.declaration.getType().wordsize();
			}
		}
		else {
			Stack<StackCell> st0 = new Stack();
			while (this.stack.size() > target) {
				StackCell c = this.stack.pop();
				if (c.type == CONTENT_TYPE.REGISTER) add++;
				else add += c.declaration.getType().wordsize();
				st0.push(c);
			}
			
			while (!st0.isEmpty()) 
				this.stack.push(st0.pop());
		}
		
		return add * 4;
	}
	
	/**
	 * Returns the distance in bytes from the FP/LR base to the newest pushed
	 * SP register. Used for try/watch construct. Returns the distance in bytes or -1.
	 */
	public int getHighestSPBackupOffset() {
		int off = 4;
		List<Integer> occurences = new ArrayList();
		
		for (int i = 0; i < stack.size(); i++) {
			if (stack.get(i).type == CONTENT_TYPE.REGISTER) 
				if (stack.get(i).reg == REG.FP || stack.get(i).reg == REG.LR) off = 4;
				else if (stack.get(i).reg == REG.SP) occurences.add(off);
				else off += 4;
			else if (stack.get(i).type == CONTENT_TYPE.DECLARATION) 
				off += (stack.get(i).declaration.getType().wordsize() * 4);
		}
		
		if (occurences.isEmpty()) return -1;
		else return occurences.get(occurences.size() - 1);
	}
	
	/**
	 * Returns the total amount of words on the stack starting after the FP/LR backup.
	 */
	public int getFrameSize() {
		int off = 0;
		for (StackCell c : this.stack) {
			if (c.getType() == CONTENT_TYPE.REGISTER) {
				if (c.getReg() == REG.LR || c.getReg() == REG.FP) off = 0;
				else off += 4;
			}
			else off += c.getDeclaration().getType().wordsize();
		}
		
		return off;
	}
	
	public Stack<StackCell> getStack() {
		return this.stack;
	}
	
} 
