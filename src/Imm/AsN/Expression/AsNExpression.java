package Imm.AsN.Expression;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.Arith.AsNAdd;
import Imm.AsN.Expression.Arith.AsNMult;
import Imm.AsN.Expression.Arith.AsNSub;

public abstract class AsNExpression extends AsNNode {

	public AsNExpression() {
		
	}
	
	public static AsNExpression cast(Expression e, RegSet r) throws CGEN_EXCEPTION {
		/* Relay to Expression type */
		if (e instanceof Add) {
			return AsNAdd.cast((Add) e, r);
		}
		else if (e instanceof Sub) {
			return AsNSub.cast((Sub) e, r);
		}
		else if (e instanceof Mul) {
			return AsNMult.cast((Mul) e, r);
		}
		else if (e instanceof IDRef) {
			return AsNIdRef.cast((IDRef) e, r);
		}
		else if (e instanceof Atom) {
			return AsNAtom.cast((Atom) e, r); 
		}
		else throw new CGEN_EXCEPTION(e.getSource(), "No cast available for " + e.getClass().getName());
	}
	
}
