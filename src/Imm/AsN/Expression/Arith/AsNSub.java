package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMSub;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNSub extends AsNBinaryExpression {

	public AsNSub() {
		
	}
	
	public static AsNSub cast(Sub s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNSub sub = new AsNSub();
	
		sub.generateLoaderCode(sub, s, r, st, (x, y) -> x - y, 
				new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		
		return sub;
	}
	
}
