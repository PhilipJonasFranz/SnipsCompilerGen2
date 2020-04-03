package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNLsr extends AsNBinaryExpression {

	public static AsNLsr cast(Lsr l, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNLsr lsr = new AsNLsr();
		
		lsr.generateLoaderCode(lsr, l, r, map, st, (x, y) -> x >> y, 
				new ASMLsr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		
		return lsr;
	}
	
}
