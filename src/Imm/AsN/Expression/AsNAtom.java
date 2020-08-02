package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Atom;
import Imm.AsN.AsNBody;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.NULL;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;

public class AsNAtom extends AsNExpression {

			/* --- METHODS --- */
	public static AsNAtom cast(Atom a, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNAtom atom = new AsNAtom();
		a.castedNode = atom;
		
		r.free(0);
		
		/* Make sure only primitives can be in an atom */
		assert(a.getType() instanceof PRIMITIVE || a.getType() instanceof NULL);
		
		/* a is placeholder -> (placeholder type != null || value != null) */
		assert !a.isPlaceholder || (a.placeholderType != null || a.getType().value != null) : "Atom is placeholder but has no placeholder type, " + a.getSource().getSourceMarker() + "(" + CompilerDriver.inputFile.getPath() + ")";
		
		/* 
		 * When atom is a placeholder, it can be potentially larger than 1 word.
		 * Because of this, we need to handle it specially here. Atom placeholders
		 * can only be allowed at three locations: Structure Inits, Function Call
		 * Parameters, and Declarations. These three locations need special CTX
		 * rules, and work with a placeholder atom together.
		 */
		if (a.isPlaceholder && a.placeholderType != null) {
			TYPE t = a.placeholderType;
			
			/* Absolute placeholder */
			if (a.getType() instanceof VOID) {
				/* Make space on stack */
				if (t.wordsize() > 1) {
					atom.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(t.wordsize() * 4)));
					
					for (int i = 0; i < t.wordsize(); i++)
						st.push(REG.R0);
				}
			}
			else {
				if (t.wordsize() > 1) {
					int regs = 0;
					
					for (int i = 0; i < t.wordsize(); i++) {
						/* Load value via literal manager, directley or via label */
						AsNBody.literalManager.loadValue(atom, Integer.parseInt(a.getType().sourceCodeRepresentation()), regs);
						regs++;
						
						/* If group size is 3, push them on the stack */
						if (regs == 3) {
							AsNStructureInit.flush(regs, atom);
							regs = 0;
						}
						
						st.push(REG.R0);
					}
					
					AsNStructureInit.flush(regs, atom);
				}
				else {
					/* Load value via literal manager, directley or via label */
					AsNBody.literalManager.loadValue(atom, Integer.parseInt(a.getType().sourceCodeRepresentation()), target);
				}
			}
		}
		else {
			/* Load value of null pointer */
			if (a.getType() instanceof NULL) {
				/* Load value from memory */
				ASMDataLabel nullPtr = map.resolve(CompilerDriver.NULL_PTR);
					
				/* Load memory address */
				ASMLdrLabel ins = new ASMLdrLabel(new RegOp(target), new LabelOp(nullPtr), CompilerDriver.NULL_PTR);
				ins.comment = new ASMComment("Load null address");
				atom.instructions.add(ins);
			}
			else {
				/* Load value via literal manager, directley or via label */
				AsNBody.literalManager.loadValue(atom, Integer.parseInt(a.getType().sourceCodeRepresentation()), target);
			}
		}
		
		return atom;
	}
	
} 
