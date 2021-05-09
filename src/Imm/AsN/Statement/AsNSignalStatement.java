package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AsN.AsNFunction;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.COMPOSIT.STRUCT;
import Res.Const;

public class AsNSignalStatement extends AsNStatement {

	public static AsNSignalStatement cast(SignalStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNSignalStatement sig = new AsNSignalStatement();
		sig.pushOnCreatorStack(s);
		s.castedNode = sig;
		
		STRUCT excType = (STRUCT) s.exceptionBuilder.getType();
		
		/* Load Exception */
		sig.instructions.addAll(AsNExpression.cast(s.exceptionBuilder, r, map, st).getInstructions());
	
		/* Move Struct ID into R12 to signal a thrown exception */
		excType.getTypedef().loadSIDInReg(sig, REG.R12, excType.proviso);
		
		/* Move word size of thrown exception into r0 to be used in the copy loop */
		ASMMov mov = new ASMMov(new RegOp(REG.R0), new ImmOp(s.exceptionBuilder.getType().wordsize() * 4));
		mov.optFlags.add(OPT_FLAG.WRITEBACK);
		sig.instructions.add(mov);
		
		/* Add the branch to the watchpoint */
		injectWatchpointBranch(sig, s.watchpoint, null);
		
		sig.registerMetric();
		return sig;
	}
	
	public static void injectWatchpointBranch(AsNNode node, SyntaxElement watchpoint, COND cond) {
		ASMLabel escape = null;
		
		/* Branch to escape target */
		if (watchpoint instanceof Function) {
			Function f = (Function) watchpoint;
			
			/* Function does not signal, meaning all exceptions must be caught */
			if (!f.signals()) return;
			else {
				/* Branch directley to function end, exception is not watched */
				escape = ((AsNFunction) f.castedNode).copyLoopEscape;
			}
		}
		else if (watchpoint instanceof TryStatement) {
			/* Branch to try statement watchpoint */
			escape = ((AsNTryStatement) ((TryStatement) watchpoint).castedNode).watchpointLabel;
		}
		else throw new SNIPS_EXC(Const.UNKNOWN_WATCHPOINT_TYPE, watchpoint.getClass().getName(), watchpoint.getSource().getSourceMarker());
	
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, cond, new LabelOp(escape));
		branch.comment = new ASMComment("Exception thrown, branch to escape target");
		
		node.instructions.add(branch);
	}
	
} 
