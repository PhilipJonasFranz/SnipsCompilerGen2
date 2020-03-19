package Imm.AsN.Expression;

import CGen.RegSet;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.Arith.AsNAddition;
import Imm.AsN.Expression.Arith.AsNMultiplication;
import Imm.AsN.Expression.Arith.AsNSubtraction;

public abstract class AsNExpression extends AsNNode {

	public AsNExpression() {
		
	}
	
	public static AsNExpression cast(Expression e, RegSet r) {
		/* Relay to Expression type */
		if (e instanceof Add) {
			return AsNAddition.cast((Add) e, r);
		}
		else if (e instanceof Sub) {
			return AsNSubtraction.cast((Sub) e, r);
		}
		else if (e instanceof Mul) {
			return AsNMultiplication.cast((Mul) e, r);
		}
		else if (e instanceof IDRef) {
			return AsNIdRef.cast((IDRef) e, r);
		}
		else if (e instanceof Atom) {
			return AsNAtom.cast((Atom) e, r); 
		}
		else return null;
	}
	
}
