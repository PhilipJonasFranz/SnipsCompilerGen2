package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.Arith.UnaryExpression;

public abstract class AsNUnaryExpression extends AsNExpression {

	/**
	 * Solve the binary expression for two given operands.
	 */
	public interface UnarySolver {
		public int solve(int a, int b);
	}
	
	
	public static AsNUnaryExpression cast(Expression e, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to Expression type */
		throw new CGEN_EXCEPTION(e.getSource(), "No injection cast available for " + e.getClass().getName());
	}
	
		/* --- OPERAND LOADING --- */
	protected void generateLoaderCode(AsNUnaryExpression m, UnaryExpression u, RegSet r, StackSet st, UnarySolver solver, ASMInstruction inject) throws CGEN_EXCEPTION {
		
	}
	
	ERROR
	
}
