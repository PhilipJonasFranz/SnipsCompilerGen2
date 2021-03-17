package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class AssignWriteback extends Statement {

			/* ---< NESTED >--- */
	public enum WRITEBACK {
		INCR, DECR;
	}

	
			/* ---< FIELDS >--- */
	public Expression reference;
	
	
			/* ---< CONSTRUCTORS >--- */
	public AssignWriteback(Expression value, Source source) {
		super(source);
		this.reference = value;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Assign Writeback");
		if (rec) this.reference.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkAssignWriteback(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.reference.setContext(context);
	}

	public Statement clone() {
		return new AssignWriteback(this.reference.clone(), this.getSource().clone());
	}

} 
