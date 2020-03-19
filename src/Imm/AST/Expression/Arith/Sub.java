package Imm.AST.Expression.Arith;

import java.util.List;

import Imm.AST.Expression.Expression;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class Sub extends BinaryExpression {
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Sub(Expression left, Expression right, Source source) {
		super(left, right, Operator.SUB, source);
	}

	@Override
	public void print(int d, boolean rec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> buildProgram(int pad) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
