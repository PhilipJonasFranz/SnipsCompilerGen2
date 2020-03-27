package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMult;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.Mul;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNMult extends AsNBinaryExpression {

	public static AsNMult cast(Mul m, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNMult mul = new AsNMult();
		
		mul.generateLoaderCode(mul, m, r, st, (x, y) -> x * y, 
				new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		
		return mul;
	}
	
}
