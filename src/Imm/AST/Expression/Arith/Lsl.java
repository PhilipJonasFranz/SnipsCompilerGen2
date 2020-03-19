package Imm.AST.Expression.Arith;

import java.util.List;

import Imm.AST.Expression.Expression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Lsl extends BinaryExpression {
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Lsl(Expression left, Expression right, Source source) {
		super(left, right, Operator.LSL, source);
	}

	public List<String> buildProgram(int pad) {
		return null;
	}
	
}
