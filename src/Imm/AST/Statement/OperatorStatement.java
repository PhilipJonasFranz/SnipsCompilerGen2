package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.OperatorExpression;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Tools.ASTNodeVisitor;
import Util.Source;

/**
 * Describes an operation that is applied to two or more operands.
 */
public class OperatorStatement extends Statement {	
	
			/* ---< FIELDS >--- */
	public OperatorExpression expression;

	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public OperatorStatement(OperatorExpression expression, Source source) {
		super(source);
		
		this.expression = expression;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		this.expression.print(d, rec);
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkOperatorStatement(this);
		
		ctx.popTrace();
		return t;
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.expression.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.expression.setContext(context);
	}

	public Statement opt(ASTOptimizer s) throws OPT0_EXC {
		return s.optOperatorStatement(this);
	}

	public List<String> codePrint(int d) {
		return Collections.singletonList(this.expression.codePrint());
	}

	public Statement clone() {
		return new OperatorStatement((OperatorExpression) this.expression.clone(), this.getSource().clone());
	}
	
} 
