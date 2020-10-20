package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Boolean.AsNCmp;

public class AsNSwitchStatement extends AsNConditionalCompoundStatement {

	public static AsNSwitchStatement cast(SwitchStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNSwitchStatement sw = new AsNSwitchStatement();
		s.castedNode = sw;
		
		/* Capsule expressions in compare statements */
		s.cases.stream().forEach(x -> x.condition = new Compare(x.condition, s.condition, COMPARATOR.EQUAL, x.getSource()));
	
		ASMLabel end = new ASMLabel(LabelUtil.getLabel());
		
		for (CaseStatement cs : s.cases) {
			ASMLabel next = new ASMLabel(LabelUtil.getLabel());
			
			/* Cast condition */
			AsNExpression expr = AsNExpression.cast(cs.condition, r, map, st);
			
			COND cond = COND.EQ;
			
			if (expr instanceof AsNCmp) {
				/* Top Comparison */
				AsNCmp com = (AsNCmp) expr;
				
				cond = com.neg;
				
				/* Remove two conditional mov instrutions */
				com.instructions.remove(com.instructions.size() - 1);
				com.instructions.remove(com.instructions.size() - 1);
				
				/* Evaluate Condition */
				sw.instructions.addAll(com.getInstructions());
			}
			else {
				/* Default condition evaluation */
				sw.instructions.addAll(expr.getInstructions());
				
				/* Check if expression was evaluated to true */
				sw.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
			}
			
			/* Condition was false, skip body */
			sw.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(cond), new LabelOp(next)));
			
			/* Add body */
			sw.addBody(cs, r, map, st);
			
			/* Branch to switch end */
			sw.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(end)));
			
			/* Add jump to next case */
			sw.instructions.add(next);
		}
		
		/* Add default body */
		sw.addBody(s.defaultStatement, r, map, st);
		
		/* Add end jump */
		sw.instructions.add(end);
		
		sw.freeDecs(r, s);
		return sw;
	}
	
} 
