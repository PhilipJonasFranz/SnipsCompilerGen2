package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Processing.Arith.ASMVMov;
import Imm.AST.Expression.RegisterAtom;

public class AsNRegisterAtom extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNRegisterAtom cast(RegisterAtom a, RegSet r, MemoryMap map, StackSet st, int target) {
		AsNRegisterAtom atom = new AsNRegisterAtom();
		atom.pushOnCreatorStack(a);
		a.castedNode = atom;
		
		r.free(0);
		
		/* Simply move the requested register into R0 */
		if (a.reg.toInt() < 16)
			atom.instructions.add(new ASMMov(new RegOp(target), new RegOp(a.reg)));
		else 
			atom.instructions.add(new ASMVMov(new RegOp(target), new VRegOp(a.reg)));
		
		atom.registerMetric();
		return atom;
	}
	
} 
