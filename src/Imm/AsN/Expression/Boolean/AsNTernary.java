package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Statement.AsNConditionalCompoundStatement;

public class AsNTernary extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNTernary cast(Ternary t, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNTernary tern = new AsNTernary().pushCreatorStack(t);
		r.free(0, 1, 2);
		
		/* The Target that is branched to if the condition is false */
		ASMLabel loadFalse = new ASMLabel(LabelUtil.getLabel());
		
		/* The End Label Target */
		ASMLabel end = new ASMLabel(LabelUtil.getLabel());

		/* Cast condition */
		AsNExpression expr = AsNExpression.cast(t.condition, r, map, st);
		
		COND cond = AsNConditionalCompoundStatement.injectConditionEvaluation(tern, expr, t.condition);
		
		/* Condition was false, no else, skip first result */
		if (cond != COND.NO)
			tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, cond, new LabelOp(loadFalse)));
		
		/* Load true result */
		tern.instructions.addAll(AsNExpression.cast(t.left, r, map, st).getInstructions());
		
		/* Branch to end */
		tern.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(end)));
		
		if (cond != COND.NO) {
			/* False Target */
			tern.instructions.add(loadFalse);
			
			/* Load false result */
			tern.instructions.addAll(AsNExpression.cast(t.right, r, map, st).getInstructions());
		}
		
		/* Add end */
		tern.instructions.add(end);
		
		r.free(0, 1, 2);
		return tern.popCreatorStack();
	}
	
} 
