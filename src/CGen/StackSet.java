package CGen;

import java.util.Stack;

import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Declaration;

public class StackSet {

	private Stack<StackCell> stack = new Stack();
	
	public enum CONTENT_TYPE {
		REGISTER, DECLARATION, EXPRESSION;
	}
	
	public class StackCell {
		
		private CONTENT_TYPE contentType;
		
		private REGISTER reg;
		
		private Declaration declaration;
		
		public Expression expression;
		
		public StackCell reference;
		
		/* Used as marker */
		public StackCell(REGISTER reg) {
			this.reg = reg;
			this.contentType = CONTENT_TYPE.REGISTER;
		}
		
		public StackCell(Declaration dec) {
			this.declaration = dec;
			this.contentType = CONTENT_TYPE.DECLARATION;
		}
		
		public StackCell(Expression e) {
			this.expression = e;
			this.contentType = CONTENT_TYPE.EXPRESSION;
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
		
		public Expression getExpression() {
			return this.expression;
		}
		
		public void setReference(StackCell reference) {
			this.reference = reference;
		}
		
	}
	
	public StackSet() {
		
	}
	
	public Stack getStack() {
		return this.stack;
	}
	
	public void push(Declaration...dec) {
		for (Declaration dec0 : dec) this.stack.push(new StackCell(dec0));
	}
	
	public void push(Expression...e) {
		for (Expression e0 : e) this.stack.push(new StackCell(e0));
	}
	
	public void push(REGISTER...reg) {
		for (REGISTER reg0 : reg) this.stack.push(new StackCell(reg0));
	}
	
	public void pop() {
		this.stack.pop();
	}
	
	public void pop(int x) {
		for (int i = 0; i < x; i++) {
			this.stack.pop();
		}
	}
	
	public void print() {
		System.out.println("Stack Layout:");
		for (int i = this.stack.size() - 1; i >= 0; i--) {
			StackCell x = this.stack.get(i);
			System.out.println(x.contentType.toString() + ": ");
			if (x.contentType == CONTENT_TYPE.DECLARATION) {
				x.declaration.print(4, true);
			}
			else if (x.contentType == CONTENT_TYPE.EXPRESSION) {
				x.expression.print(4, true);
			}
			else System.out.println("    " + x.reg.toString());
		}
	}
	
}
