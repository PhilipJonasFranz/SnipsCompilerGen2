package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.Sub;

public class AsNSub extends AsNBinaryExpression {

	public AsNSub() {
		
	}
	
	public static AsNSub cast(Sub s, RegSet r) throws CGEN_EXCEPTION {
		AsNSub sub = new AsNSub();
	
		sub.generateLoaderCode(sub, s, r, (x, y) -> x - y, 
				new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		
		return sub;
	}
	
}
