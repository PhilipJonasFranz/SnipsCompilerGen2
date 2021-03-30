package Imm.AST.Expression.Boolean;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

public class Ternary extends Expression {
	
			/* ---< FIELDS >--- */
	public Expression condition;
	
	public Expression leftOperand;
	
	public Expression rightOperand;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Ternary(Expression condition, Expression left, Expression right, Source source) {
		super(source);
		this.condition = condition;
		this.leftOperand = left;
		this.rightOperand = right;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Ternary");
		
		if (rec) {
			this.condition.print(d + this.printDepthStep, rec);
			this.leftOperand.print(d + this.printDepthStep, rec);
			this.rightOperand.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkTernary(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.condition.setContext(context);
		this.leftOperand.setContext(context);
		this.rightOperand.setContext(context);
	}

	public Expression clone() {
		return new Ternary(this.condition.clone(), this.leftOperand.clone(), this.rightOperand.clone(), this.getSource().clone());
	}

} 
