package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.Arith.UnaryExpression;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.Not;
import Imm.AsN.Expression.Arith.AsNUnaryMinus;
import Imm.AsN.Expression.Boolean.AsNNot;

public abstract class AsNUnaryExpression extends AsNExpression {

			/* --- METHODS --- */
	public static AsNUnaryExpression cast(UnaryExpression u, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNUnaryExpression node = null;
		
		if (u instanceof Not) {
			node = AsNNot.cast((Not) u, r, st);
		}
		else if (u instanceof UnaryMinus) {
			node = AsNUnaryMinus.cast((UnaryMinus) u, r, st);
		}
		else throw new CGEN_EXCEPTION(u.getSource(), "No injection cast available for " + u.getClass().getName());
		
		u.castedNode = node;
		return node;
	}
	
}
