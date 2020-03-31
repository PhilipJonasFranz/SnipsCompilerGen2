package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.Arith.UnaryExpression;
import Imm.AST.Expression.Boolean.Not;
import Imm.AsN.Expression.Boolean.AsNNot;

public abstract class AsNUnaryExpression extends AsNExpression {

			/* --- NESTED --- */
	/**
	 * Solve the binary expression for two given operands.
	 */
	public interface UnarySolver {
		public int solve(int a, int b);
	}
	
	
			/* --- METHODS --- */
	public static AsNUnaryExpression cast(UnaryExpression u, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNUnaryExpression node = null;
		
		if (u instanceof Not) {
			node = AsNNot.cast((Not) u, r, st);
		}
		else throw new CGEN_EXCEPTION(u.getSource(), "No injection cast available for " + u.getClass().getName());
		
		u.castedNode = node;
		return node;
	}
	
}
