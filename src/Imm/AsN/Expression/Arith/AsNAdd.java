package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMAdd;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Arith.Add;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNAdd extends AsNBinaryExpression {

	public AsNAdd() {
		
	}
	
	public static AsNAdd cast(Add a, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNAdd add = new AsNAdd();
		
		add.generateLoaderCode(add, a, r, st, (x, y) -> x + y, 
			new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
		
		return add;
	}
	
}
