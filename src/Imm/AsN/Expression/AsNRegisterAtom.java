package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.RegisterAtom;

public class AsNRegisterAtom extends AsNExpression {

			/* --- METHODS --- */
	public static AsNRegisterAtom cast(RegisterAtom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNRegisterAtom atom = new AsNRegisterAtom();
		a.castedNode = atom;
		
		r.free(0);
		
		/* Simply move the requested register into R0 */
		atom.instructions.add(new ASMMov(new RegOp(target), new RegOp(a.reg)));
		
		return atom;
	}
	
}
