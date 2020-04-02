package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Boolean.AsNCmp;

public class AsNSwitchStatement extends AsNConditionalCompoundStatement {

	public static AsNSwitchStatement cast(SwitchStatement s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNSwitchStatement sw = new AsNSwitchStatement();
		s.castedNode = sw;
		
		/* Capsule expressions in compare statements */
		s.cases.stream().forEach(x -> x.condition = new Compare(x.condition, s.condition, COMPARATOR.EQUAL, x.getSource()));
	
		ASMLabel end = new ASMLabel(LabelGen.getLabel());
		
		ASMLabel next = new ASMLabel(LabelGen.getLabel());
		
		for (CaseStatement cs : s.cases) {
			sw.evaluateCondition(cs.condition, r, st, next);
			
			/* Open scope for case body */
			st.openScope();
			
			for (Statement s0 : cs.body) 
				sw.instructions.addAll(AsNStatement.cast(s0, r, st).getInstructions());
			
			/* Pop Case body scope */
			sw.popDeclarationScope(cs, r, st);
			
			/* Branch to switch end */
			sw.instructions.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(end)));
			
			/* Add jump to next case */
			sw.instructions.add(next);
			
			/* Next element in chain */
			next = new ASMLabel(LabelGen.getLabel());
		}
		
		/* Handle Default Statement */
		st.openScope();
		
		for (Statement s0 : s.defaultStatement.body) 
			sw.instructions.addAll(AsNStatement.cast(s0, r, st).getInstructions());
		
		sw.popDeclarationScope(s.defaultStatement, r, st);
		
		/* Add end jump */
		sw.instructions.add(end);
		
		return sw;
	}
	
	public void evaluateCondition(Expression condition, RegSet r, StackSet st, ASMLabel next) throws CGEN_EXCEPTION {
		/* Cast condition */
		AsNExpression expr = AsNExpression.cast(condition, r, st);
		
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
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(neg), new LabelOperand(next)));
		}
		else {
			/* Default condition evaluation */
			this.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to true */
			this.instructions.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(1)));
			
			/* Condition was false, jump to else */
			this.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.NE), new LabelOperand(next)));
		}
	}
	
}
