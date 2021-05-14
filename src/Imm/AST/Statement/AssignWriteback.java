package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class AssignWriteback extends Statement {

			/* ---< NESTED >--- */
	public enum WRITEBACK {
		INCR, DECR
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
		CompilerDriver.outs.println(Util.pad(d) + "Assign Writeback");
		if (rec) this.reference.print(d + this.printDepthStep, true);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkAssignWriteback(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optAssignWriteback(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.reference.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.reference.setContext(context);
	}

	public Statement clone() {
		AssignWriteback awb = new AssignWriteback(this.reference.clone(), this.getSource().clone());
		awb.copyDirectivesFrom(this);
		return awb;
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		code.add(Util.pad(d) + this.reference.codePrint() + ";");
		return code;
	}

} 
