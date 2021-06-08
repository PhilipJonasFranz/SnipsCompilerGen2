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
import Imm.AST.Statement.DoWhileStatement;
import Imm.AsN.Expression.AsNExpression;

public class AsNDoWhileStatement extends AsNConditionalCompoundStatement {

	public static AsNDoWhileStatement cast(DoWhileStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNDoWhileStatement w = new AsNDoWhileStatement().pushCreatorStack(a);

		/* End of while loop */
		w.breakJump = new ASMLabel(LabelUtil.getLabel());
		
		/* Create jump as target for continue statements */
		w.continueJump = new ASMLabel(LabelUtil.getLabel());
		
		/* Loop Entrypoint */
		ASMLabel whileStart = new ASMLabel(LabelUtil.getLabel());
		whileStart.optFlags.add(OPT_FLAG.LOOP_HEAD);
		w.instructions.add(whileStart);
		
		/* Add Body */
		w.addBody(a, r, map, st);

		/* Add jump for continue statements to use as target */
		w.instructions.add(w.continueJump);

		COND cond = injectConditionEvaluation(w, AsNExpression.cast(a.condition, r, map, st), a.condition);

		if (cond != COND.NO) {
			/* Condition was false, jump to end */
			w.instructions.add(new ASMBranch(BRANCH_TYPE.B, cond, new LabelOp(w.breakJump)));
		}
		
		/* Branch to loop start */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(whileStart));
		branch.optFlags.add(OPT_FLAG.LOOP_BRANCH);
		w.instructions.add(branch);
		
		w.instructions.add(w.breakJump);
		return w.popCreatorStack();
	}
	
} 
