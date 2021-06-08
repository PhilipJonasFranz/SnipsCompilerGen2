package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Expression.UnaryExpression;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.Not;
import Imm.AsN.Expression.Arith.AsNBitNot;
import Imm.AsN.Expression.Arith.AsNUnaryMinus;
import Imm.AsN.Expression.Boolean.AsNNot;
import Res.Const;

public abstract class AsNUnaryExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNUnaryExpression cast(UnaryExpression u, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNUnaryExpression node = null;
		
		if (u instanceof Not) {
			node = AsNNot.cast((Not) u, r, map, st);
		}
		else if (u instanceof UnaryMinus) {
			node = AsNUnaryMinus.cast((UnaryMinus) u, r, map, st);
		}
		else if (u instanceof BitNot) {
			node = AsNBitNot.cast((BitNot) u, r, map, st);
		}
		else throw new CGEN_EXC(u.getSource(), Const.NO_INJECTION_CAST_AVAILABLE, u.getClass().getName());

		return node;
	}
	
} 
