package Imm.AsN.Expression;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Exc.CTEX_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.ASM.Util.Operands.Memory.MemoryWordOp;
import Imm.ASM.Util.Operands.Memory.MemoryWordRefOp;
import Imm.AST.Expression.InlineFunction;
import Imm.AsN.AsNBody;
import Imm.AsN.AsNFunction;

public class AsNInlineFunction extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNInlineFunction cast(InlineFunction i, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNInlineFunction ifunc = new AsNInlineFunction();
		i.castedNode = ifunc;
		
		AsNFunction funcCast;
		
		String currentPrefix = LabelUtil.funcPrefix;
		int currentUID = LabelUtil.funcUID;
		
		try {
			funcCast = AsNFunction.cast(i.inlineFunction, new RegSet(), map, new StackSet());
		} catch (CTEX_EXC e) {
			throw new CGEN_EXC(i.getSource(), "Failed to cast inline function: " + e.getMessage());
		}
		
		LabelUtil.funcPrefix = currentPrefix;
		LabelUtil.funcUID = currentUID;
		
		List<ASMInstruction> dataBlock = new ArrayList();
		
		for (String funcLabel : funcCast.generatedLabels) {
			ASMDataLabel entry = new ASMDataLabel(funcLabel, new MemoryWordOp(0));
			MemoryWordRefOp parent = new MemoryWordRefOp(entry);
			
			ASMDataLabel funcEntry = new ASMDataLabel("lambda_" + funcLabel, parent);
			dataBlock.add(funcEntry);
		}
		
		AsNBody.addToTranslationUnit(dataBlock, i.getSource(), SECTION.DATA);
		AsNBody.addToTranslationUnit(funcCast.getInstructions(), i.getSource(), SECTION.TEXT);
		
		/* Construct label name for function lambda target with provided provisos */
		String label = "lambda_" + i.inlineFunction.path.build() + ((i.inlineFunction.requireUIDInLabel)? "@" + i.inlineFunction.UID : "");
		ASMDataLabel entry = new ASMDataLabel(label, new MemoryWordOp(0));
		
		LabelOp operand = new LabelOp(entry);
		ifunc.instructions.add(new ASMLdrLabel(new RegOp(REG.R0), operand, null));
		ifunc.instructions.add(new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R0)));
		
		return ifunc;
	}
	
} 
