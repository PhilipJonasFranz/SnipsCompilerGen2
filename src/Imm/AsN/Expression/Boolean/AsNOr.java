package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Or;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNOr extends AsNNFoldExpression {

			/* ---< METHODS >--- */
	public static AsNOr cast(Or o, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNOr or = new AsNOr();
		
		if (o.operands.size() > 2) throw new SNIPS_EXC("N-Operand Chains are not supported!");
		
		/* Clear only R0, R1 since R2 is not needed */
		r.free(0, 1);
		
		if (o.operands.get(0) instanceof Atom && o.operands.get(1) instanceof Atom) {
			int value0 = o.operands.get(0).getType().toInt();
			int value1 = o.operands.get(1).getType().toInt();
			
			or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp((value0 != 0 || value1 != 0)? 1 : 0)));
		}
		else if (o.operands.get(0) instanceof Atom) {
			int value = o.operands.get(0).getType().toInt();
			
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.operands.get(1), r, map, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), COND.NE));
			}
		}
		else if (o.operands.get(1) instanceof Atom) {
			int value = o.operands.get(1).getType().toInt();
			
			if (value == 1) {
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1)));
			}
			else {
				or.instructions.addAll(AsNExpression.cast(o.operands.get(0), r, map, st).getInstructions());
				
				or.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), COND.NE));
			}
		}
		else {
			/* Load Operands */
			or.generatePrimitiveLoaderCode(or, o, o.operands.get(0), o.operands.get(1), r, map, st, 0, 1);
			
			ASMOrr orr = new ASMOrr(new RegOp(REG.R0), new RegOp(REG.R0), new RegOp(REG.R1));
			orr.updateCondField();
			or.instructions.add(orr);
			
			or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), COND.NE));
			or.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), COND.EQ));
			
			return or;
		}
		
		return or;
	}

	public ASMInstruction buildInjector() {
		throw new SNIPS_EXC("No injector available for 'Or'!");
	}

} 
