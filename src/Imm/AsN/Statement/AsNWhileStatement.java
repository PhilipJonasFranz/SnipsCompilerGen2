package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.AST.Statement.WhileStatement;
import Imm.AsN.Expression.AsNExpression;

public class AsNWhileStatement extends AsNConditionalCompoundStatement {

	public static AsNWhileStatement cast(WhileStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNWhileStatement w = new AsNWhileStatement().pushCreatorStack(a);

		/* Generate labels for targets within this loop and set them to the casted node */
		w.continueJump = LabelUtil.getLabel();
		
		ASMLabel whileStart = LabelUtil.getLabel();
		w.instructions.add(whileStart.flag(OPT_FLAG.LOOP_HEAD));

		w.breakJump = LabelUtil.getLabel();
		
		COND cond =	injectConditionEvaluation(w, AsNExpression.cast(a.condition, r, map, st), a.condition);

		/* Skip body */
		if (cond != COND.NO) w.instructions.add(new ASMBranch(BRANCH_TYPE.B, cond, new LabelOp(w.breakJump)));
		
		/* Add Body */
		w.addBody(a, r, map, st);
		
		/* Add jump for continue statements to use as target */
		w.instructions.add(w.continueJump);
		
		/* Branch to loop start */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(whileStart));
		w.instructions.add(branch.flag(OPT_FLAG.LOOP_BRANCH));

		w.instructions.add(w.breakJump);
		
		if (!w.instructions.isEmpty()) 
			w.instructions.get(0).com("Evaluate condition");
		
		w.freeDecs(r, a);
		return w.popCreatorStack();
	}
	
} 
