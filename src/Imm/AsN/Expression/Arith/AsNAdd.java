package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Arith.Add;
import Imm.AsN.Expression.AsNBinaryExpression;

public class AsNAdd extends AsNBinaryExpression {

			/* ---< METHODS >--- */
	public static AsNAdd cast(Add a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNAdd add = new AsNAdd();
		
		add.generateLoaderCode(add, a, r, map, st, (x, y) -> x + y, 
			new ASMAdd(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
		
		return add;
	}
	
} 
