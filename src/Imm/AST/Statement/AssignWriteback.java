package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
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

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkAssignWriteback(this);
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.reference.setContext(context);
	}

	public Statement clone() {
		return new AssignWriteback(this.reference.clone(), this.getSource().clone());
	}

} 
