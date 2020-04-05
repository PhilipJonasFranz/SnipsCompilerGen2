package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AST.Expression.Atom;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNAtom extends AsNExpression {

			/* --- METHODS --- */
	public static AsNAtom cast(Atom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNAtom atom = new AsNAtom();
		a.castedNode = atom;
		
		atom.clearReg(r, st, 0);
		
		/* Make sure only primitives can be in an atom */
		assert(a.type instanceof PRIMITIVE);
		
		atom.instructions.add(new ASMMov(new RegOperand(target), new ImmOperand(Integer.parseInt(a.type.sourceCodeRepresentation()))));
		
		return atom;
	}
	
}
