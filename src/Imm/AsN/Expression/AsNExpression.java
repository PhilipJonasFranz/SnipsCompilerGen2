package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.Arith.BinaryExpression;
import Imm.AST.Expression.Arith.UnaryExpression;
import Imm.AsN.AsNNode;

public abstract class AsNExpression extends AsNNode {

	public AsNExpression() {
		
	}
	
	public static AsNExpression cast(Expression e, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to Expression type */
		AsNExpression node = null;
		
		if (e instanceof BinaryExpression) {
			node = AsNBinaryExpression.cast((BinaryExpression) e, r, st);
		}
		else if (e instanceof UnaryExpression) {
			node = AsNUnaryExpression.cast((UnaryExpression) e, r, st);
		}
		else if (e instanceof InlineCall) {
			node = AsNInlineCall.cast((InlineCall) e, r, st);
		}
		else if (e instanceof IDRef) {
			node = AsNIdRef.cast((IDRef) e, r, st, 0);
		}
		else if (e instanceof Atom) {
			node = AsNAtom.cast((Atom) e, r, st); 
		}
		else throw new CGEN_EXCEPTION(e.getSource(), "No injection cast available for " + e.getClass().getName());
	
		e.castedNode = node;
		return node;
	}
	
}
