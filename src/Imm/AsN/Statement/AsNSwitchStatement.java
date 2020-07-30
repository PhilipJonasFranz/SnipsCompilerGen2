package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
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
import Imm.AST.Expression.Expression;
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
	
		ASMLabel end = new ASMLabel(LabelGen.getLabel());
		
		ASMLabel next = new ASMLabel(LabelGen.getLabel());
		
		for (CaseStatement cs : s.cases) {
			sw.evaluateCondition(cs.condition, r, map, st, next);
			
			/* Add body */
			sw.addBody(cs, r, map, st);
			
			/* Branch to switch end */
			sw.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOp(end)));
			
			/* Add jump to next case */
			sw.instructions.add(next);
			
			/* Next element in chain */
			next = new ASMLabel(LabelGen.getLabel());
		}
		
		/* Add default body */
		sw.addBody(s.defaultStatement, r, map, st);
		
		/* Add end jump */
		sw.instructions.add(end);
		
		sw.freeDecs(r, s);
		return sw;
	}
	
	public void evaluateCondition(Expression condition, RegSet r, MemoryMap map, StackSet st, ASMLabel next) throws CGEN_EXC {
		/* Cast condition */
		AsNExpression expr = AsNExpression.cast(condition, r, map, st);
		
		if (expr instanceof AsNCmp) {
			/* Top Comparison */
			AsNCmp com = (AsNCmp) expr;
			
			COND neg = com.neg;
			
			/* Remove two conditional mov instrutions */
			com.instructions.remove(com.instructions.size() - 1);
			com.instructions.remove(com.instructions.size() - 1);
			
			/* Evaluate Condition */
			this.instructions.addAll(com.getInstructions());
			
			/* Condition was false, skip body */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOp(next)));
		}
		else {
			/* Default condition evaluation */
			this.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			this.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(1)));
			
			/* Condition was false, jump to else */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOp(next)));
		}
	}
	
} 
