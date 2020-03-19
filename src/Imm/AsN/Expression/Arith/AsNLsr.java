package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.Lsr;

public class AsNLsr extends AsNBinaryExpression {

	public AsNLsr() {
		
	}
	
	public static AsNLsr cast(Lsr l, RegSet r) throws CGEN_EXCEPTION {
		AsNLsr lsr = new AsNLsr();
		
		lsr.generateLoaderCode(lsr, l, r, (x, y) -> x >> y, 
				new ASMLsr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		
		return lsr;
	}
	
}
