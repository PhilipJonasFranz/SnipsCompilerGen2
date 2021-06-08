package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Exc.CTEX_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.Memory.MemoryWordOp;
import Imm.AST.Expression.InlineFunction;
import Imm.AsN.AsNBody;
import Imm.AsN.AsNFunction;

public class AsNInlineFunction extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNInlineFunction cast(InlineFunction i, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNInlineFunction ifunc = new AsNInlineFunction().pushCreatorStack(i);

		AsNFunction funcCast;
		
		String currentPrefix = LabelUtil.funcPrefix;
		int currentUID = LabelUtil.funcUID;
		
		try {
			funcCast = AsNFunction.cast(i.inlineFunction, map);
		} catch (CTEX_EXC e) {
			throw new CGEN_EXC(i.getSource(), "Failed to cast inline function: " + e.getMessage());
		}
		
		LabelUtil.funcPrefix = currentPrefix;
		LabelUtil.funcUID = currentUID;
		
		AsNBody.addToTranslationUnit(funcCast.getInstructions(), i.getSource(), SECTION.TEXT);
		
		/* Construct label name for function lambda target with provided provisos */
		String label = i.inlineFunction.path + ((i.inlineFunction.requireUIDInLabel)? "_" + i.inlineFunction.UID : "");
		ASMDataLabel entry = new ASMDataLabel(label, new MemoryWordOp(0));
		
		LabelOp operand = new LabelOp(entry);
		ifunc.instructions.add(new ASMLdrLabel(new RegOp(REG.R0), operand, null));

		return ifunc.popCreatorStack();
	}
	
} 
