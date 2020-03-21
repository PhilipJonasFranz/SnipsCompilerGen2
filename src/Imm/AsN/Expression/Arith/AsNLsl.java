package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNLsl extends AsNBinaryExpression {

	public AsNLsl() {
		
	}
	
	public static AsNLsl cast(Lsl l, RegSet r) throws CGEN_EXCEPTION {
		AsNLsl lsl = new AsNLsl();
		
		lsl.generateLoaderCode(lsl, l, r, (x, y) -> x << y, 
				new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			
		return lsl;
	}
	
}
