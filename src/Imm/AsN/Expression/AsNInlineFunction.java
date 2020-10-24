package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Exc.CTX_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
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
		
		try {
			funcCast = AsNFunction.cast(i.inlineFunction, new RegSet(), map, new StackSet());
		} catch (CTX_EXC e) {
			throw new CGEN_EXC(i.getSource(), "Failed to cast inline function: " + e.getMessage());
		}
		
		LabelUtil.funcPrefix = currentPrefix;
		
		AsNBody.instructionAppenix.addAll(funcCast.getInstructions());
		
		/* Create return address in R10, used to return from sys jump */
		ifunc.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.PC), new ImmOp(8)));
		
		/* Construct label name for function lambda target with provided provisos */
		String label = "lambda_" + i.inlineFunction.path.build();
		
		/* Branch to the lambda target of the function with a sys jump to obtain the address */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(new ASMLabel(label)));
		branch.optFlags.add(OPT_FLAG.SYS_JMP);
		ifunc.instructions.add(branch);
		
		/* Reset R10 to 0, for possible addressing calculation optimizations */
		ifunc.instructions.add(new ASMMov(new RegOp(REG.R10), new ImmOp(0)));
		
		return ifunc;
	}
	
} 
