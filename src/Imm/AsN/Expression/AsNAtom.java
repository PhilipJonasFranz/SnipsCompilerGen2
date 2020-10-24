package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.Atom;
import Imm.AsN.AsNBody;
import Imm.TYPE.PRIMITIVES.NULL;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Snips.CompilerDriver;

public class AsNAtom extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNAtom cast(Atom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNAtom atom = new AsNAtom();
		a.castedNode = atom;
		
		r.free(0);
		
		/* Make sure only primitives can be in an atom */
		assert(a.getType() instanceof PRIMITIVE || a.getType() instanceof NULL);
		
		/* Load value of null pointer */
		if (a.getType() instanceof NULL) {
			/* Load value from memory */
			ASMDataLabel nullPtr = map.resolve(CompilerDriver.NULL_PTR);
				
			/* Load memory address */
			ASMLdrLabel ins = new ASMLdrLabel(new RegOp(target), new LabelOp(nullPtr), CompilerDriver.NULL_PTR);
			ins.comment = new ASMComment("Load null address");
			atom.instructions.add(ins);
		}
		else 
			/* Load value via literal manager, directley or via label */
			AsNBody.literalManager.loadValue(atom, Integer.parseInt(a.getType().sourceCodeRepresentation()), target);
		
		return atom;
	}
	
} 
