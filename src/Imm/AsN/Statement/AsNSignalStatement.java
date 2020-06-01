package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AsN.AsNFunction;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.AsNStructureInit;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNSignalStatement extends AsNStatement {

	public static AsNSignalStatement cast(SignalStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNSignalStatement sig = new AsNSignalStatement();
		
		STRUCT excType = (STRUCT) s.exceptionInit.getType();
		
		/* Load Exception */
		sig.instructions.addAll(AsNStructureInit.cast(s.exceptionInit, r, map, st).getInstructions());
	
		/* Move Struct ID into R12 to signal a thrown exception */
		ASMMov signal = new ASMMov(new RegOperand(REGISTER.R12), new ImmOperand(excType.typedef.SID));
		signal.comment = new ASMComment("Signal thrown exception");
		sig.instructions.add(signal);
		
		/* Move word size of thrown exception into r0 to be used in the copy loop */
		ASMMov mov = new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(s.exceptionInit.getType().wordsize() * 4));
		mov.optFlags.add(OPT_FLAG.WRITEBACK);
		sig.instructions.add(mov);
		
		/* Add the branch to the watchpoint */
		injectWatchpointBranch(sig, s.watchpoint, null);
		
		return sig;
	}
	
	public static void injectWatchpointBranch(AsNNode node, SyntaxElement watchpoint, Cond cond) {
		ASMLabel escape = null;
		
		/* Branch to escape target */
		if (watchpoint instanceof Function) {
			Function f = (Function) watchpoint;
			
			/* Function does not signal, meaning all exceptions must be caught */
			if (!f.signals) return;
			else {
				/* Branch directley to function end, exception is not watched */
				escape = ((AsNFunction) f.castedNode).copyLoopEscape;
			}
		}
		else if (watchpoint instanceof TryStatement) {
			/* Branch to try statement watchpoint */
			escape = ((AsNTryStatement) ((TryStatement) watchpoint).castedNode).watchpointLabel;
		}
		else throw new SNIPS_EXCEPTION("Unknown watchpoint type " + watchpoint.getClass().getName() + ", " + watchpoint.getSource().getSourceMarker());
	
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, cond, new LabelOperand(escape));
		branch.comment = new ASMComment("Exception thrown, branch to escape target");
		
		node.instructions.add(branch);
	}
	
}
