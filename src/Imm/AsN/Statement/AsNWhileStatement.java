package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.AST.Expression.Atom;
import Imm.AST.Statement.WhileStatement;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.BOOL;

public class AsNWhileStatement extends AsNConditionalCompoundStatement {

	public static AsNWhileStatement cast(WhileStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNWhileStatement w = new AsNWhileStatement();
		a.castedNode = w;
		
		/* Generate labels for targets within this loop and set them to the casted node */
		w.continueJump = new ASMLabel(LabelUtil.getLabel());
		
		ASMLabel whileStart = new ASMLabel(LabelUtil.getLabel());
		w.instructions.add(whileStart);

		w.breakJump = new ASMLabel(LabelUtil.getLabel());
		
		boolean injectConditionCheck = true;
		
		if (a.condition instanceof Atom) {
			Atom at = (Atom) a.condition;
			if (at.getType() instanceof BOOL) {
				boolean value = (boolean) at.getType().value;
				if (value) injectConditionCheck = false;
			}
		}
		
		if (injectConditionCheck) {
			COND cond =	injectConditionEvaluation(w, AsNExpression.cast(a.condition, r, map, st));
			
			/* Condition was false, no else, skip body */
			w.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(cond), new LabelOp(w.breakJump)));
		}
		
		/* Add Body */
		w.addBody(a, r, map, st);
		
		/* Add jump for continue statements to use as target */
		w.instructions.add(w.continueJump);
		
		/* Branch to loop start */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(whileStart));
		branch.optFlags.add(OPT_FLAG.LOOP_BRANCH);
		w.instructions.add(branch);
		
		w.instructions.add(w.breakJump);
		
		if (!w.instructions.isEmpty()) 
			w.instructions.get(0).comment = new ASMComment("Evaluate condition");
		
		w.freeDecs(r, a);
		return w;
	}
	
} 
