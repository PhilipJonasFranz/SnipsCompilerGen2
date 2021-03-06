package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.And;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNAnd extends AsNNFoldExpression {

			/* ---< METHODS >--- */
	public static AsNAnd cast(And a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNAnd and = new AsNAnd();
		and.pushOnCreatorStack(a);
		a.castedNode = and;
		
		if (a.operands.size() > 2) throw new SNIPS_EXC("N-Operand Chains are not supported!");
		
		/* Clear only R0, R1 since R2 is not needed */
		r.free(0, 1);
		
		if (a.operands.get(0) instanceof Atom && a.operands.get(1) instanceof Atom) {
			int value0 = a.operands.get(0).getType().toInt();
			int value1 = a.operands.get(1).getType().toInt();
			
			and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp((value0 == 0 || value1 == 0)? 0 : 1)));
		}
		else if (a.operands.get(0) instanceof Atom) {
			int value = a.operands.get(0).getType().toInt();
			
			if (value == 0) {
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0)));
			}
			else {
				and.instructions.addAll(AsNExpression.cast(a.operands.get(1), r, map, st).getInstructions());
				
				and.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), COND.NE));
			}
		}
		else if (a.operands.get(1) instanceof Atom) {
			int value = a.operands.get(1).getType().toInt();
			
			if (value == 0) {
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0)));
			}
			else {
				and.instructions.addAll(AsNExpression.cast(a.operands.get(0), r, map, st).getInstructions());
				
				and.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
				
				/* Move #1 into R0 when expression is not 0 */
				and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), COND.NE));
			}
		}
		else {
			/* Load Operands */
			and.generatePrimitiveLoaderCode(and, a, a.operands.get(0), a.operands.get(1), r, map, st, 0, 1);
			
			/* Perform and */
			ASMAdd and0 = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(0));
			and0.updateCondField();
			and.instructions.add(and0);
			
			and.instructions.add(new ASMMov(new RegOp(REG.R1), new ImmOp(1), COND.NE));
			
			and.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(0)));
			
			and.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.R1), COND.NE));
			and.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), COND.EQ));
		}
		
		and.registerMetric();
		return and;
	}

	public ASMInstruction buildInjector() {
		throw new SNIPS_EXC("No injector available for 'And'!");
	}
	
} 
