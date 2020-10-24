package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.ConditionalCompoundStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.Boolean.AsNCmp;
import Res.Const;

public abstract class AsNConditionalCompoundStatement extends AsNCompoundStatement {

	public ASMLabel breakJump;
	
	public ASMLabel continueJump;
	
	public static AsNConditionalCompoundStatement cast(ConditionalCompoundStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNConditionalCompoundStatement node = null;
		
		if (s instanceof IfStatement) {
			node = AsNIfStatement.cast((IfStatement) s, r, map, st);
		}
		else if (s instanceof WhileStatement) {
			node = AsNWhileStatement.cast((WhileStatement) s, r, map, st);
		}
		else if (s instanceof DoWhileStatement) {
			node = AsNDoWhileStatement.cast((DoWhileStatement) s, r, map, st);
		}
		else if (s instanceof ForStatement) {
			node = AsNForStatement.cast((ForStatement) s, r, map, st);
		}
		else throw new CGEN_EXC(s.getSource(), Const.NO_INJECTION_CAST_AVAILABLE, s.getClass().getName());	
	
		s.castedNode = node;
		return node;
	}
	
	/**
	 * Adds the instruction of the casted expression to this node. If the expression
	 * is a AsNCmp, the last two instructions are removed to fit the desired pattern, 
	 * which is that the instructions of the expression end with an <br>
	 * <br>
	 * 		<code>cmp ...</code><br>
	 * <br>
	 * If the expression is not a AsNCmp, this instruction is added in manually. Returns
	 * the condition to check against. Per default EQ, but when the expression is a AsNCmp,
	 * the returned condition is the negation of the condition of this expression, since we
	 * want to check if the condition is false.
	 */
	public static COND injectConditionEvaluation(AsNNode node, AsNExpression expr) {
		COND cond = COND.EQ;
		
		if (expr instanceof AsNCmp) {
			AsNCmp com = (AsNCmp) expr;
			
			cond = com.neg;

			/* Remove two conditional mov instrutions */
			com.instructions.remove(com.instructions.size() - 1);
			com.instructions.remove(com.instructions.size() - 1);

			/* Evaluate Condition */
			node.instructions.addAll(com.getInstructions());
		}
		else {
			/* Evaluate Condition */
			node.instructions.addAll(expr.getInstructions());
			
			/* Check if expression was evaluated to false */
			node.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
		}
		
		return cond;
	}
	
} 
