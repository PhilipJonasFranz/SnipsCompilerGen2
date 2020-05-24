package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AST.Expression.RegisterAtom;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNRegisterAtom extends AsNExpression {

			/* --- METHODS --- */
	public static AsNRegisterAtom cast(RegisterAtom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNRegisterAtom atom = new AsNRegisterAtom();
		a.castedNode = atom;
		
		r.free(0);
		
		/* Make sure only primitives can be in an atom */
		assert(a.getType() instanceof PRIMITIVE);
		
		atom.instructions.add(new ASMMov(new RegOperand(target), new RegOperand(a.reg)));
		
		return atom;
	}
	
}
