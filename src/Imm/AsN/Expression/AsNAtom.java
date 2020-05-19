package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.Atom;
import Imm.AsN.AsNBody;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class AsNAtom extends AsNExpression {

			/* --- METHODS --- */
	public static AsNAtom cast(Atom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNAtom atom = new AsNAtom();
		a.castedNode = atom;
		
		r.free(0);
		
		/* Make sure only primitives can be in an atom */
		assert(a.getType() instanceof PRIMITIVE);
		
		/* Load value via literal manager, directley or via label */
		AsNBody.literalManager.loadValue(atom, Integer.parseInt(a.getType().sourceCodeRepresentation()), target);
		
		return atom;
	}
	
}
