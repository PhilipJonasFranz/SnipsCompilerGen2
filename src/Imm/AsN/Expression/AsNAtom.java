package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Expression.Atom;
import Imm.AsN.AsNBody;
import Snips.CompilerDriver;

public class AsNAtom extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNAtom cast(Atom a, RegSet r, MemoryMap map, StackSet st, int target) {
		AsNAtom atom = new AsNAtom();
		atom.pushOnCreatorStack(a);
		a.castedNode = atom;
		
		if (a.getType().isFloat()) r.getVRegSet().free(0);
		else r.free(0);
		
		/* Make sure only primitives can be in an atom */
		assert(a.getType().isPrimitive() || a.getType().isNull());
		
		/* Load value of null pointer */
		if (a.getType().isNull()) {
			/* Load value from memory */
			ASMDataLabel nullPtr = map.resolve(CompilerDriver.NULL_PTR);
				
			/* Load memory address */
			ASMLdrLabel ins = new ASMLdrLabel(new RegOp(target), new LabelOp(nullPtr), CompilerDriver.NULL_PTR);
			ins.comment = new ASMComment("Load null address");
			atom.instructions.add(ins);
		}
		else {
			/* Load value via literal manager, directly or via label */
			int value = Integer.parseInt(a.getType().toPrimitive().sourceCodeRepresentation());	
			AsNBody.literalManager.loadValue(atom, value, target, a.getType().isFloat(), a.getType().value.toString());
		}
		
		atom.registerMetric();
		return atom;
	}
	
} 
