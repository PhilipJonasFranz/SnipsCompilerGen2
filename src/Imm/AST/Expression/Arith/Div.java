package Imm.AST.Expression.Arith;

import java.util.List;

import Imm.AST.Expression.Expression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Div extends BinaryExpression {
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Div(Expression left, Expression right, Source source) {
		super(left, right, Operator.DIV, source);
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
}
