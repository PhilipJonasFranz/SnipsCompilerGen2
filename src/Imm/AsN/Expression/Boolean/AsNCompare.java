package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AsN.AsNBody;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNNFoldExpression;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.NULL;

public class AsNCompare extends AsNNFoldExpression {

			/* ---< FIELDS >--- */
	public COND trueC, neg;
	
	
			/* ---< METHODS >--- */
	/**
	 * Compare both operands based on the set Comparator. Move #1 in into R0 if the
	 * expression is true, #0 if not.
	 */
	public static AsNCompare cast(Compare c, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNCompare cmp = new AsNCompare();
		cmp.pushOnCreatorStack(c);
		c.castedNode = cmp;
		
		if (c.operands.size() > 2) throw new SNIPS_EXC("N-Operand Chains are not supported!");
		
		/* Clear only R0, R1 since R2 is not needed */
		r.free(0, 1);
		
		if (c.operands.get(1) instanceof Atom && !(((Atom) c.operands.get(1)).getType() instanceof NULL)) {
			cmp.instructions.addAll(AsNExpression.cast(c.operands.get(0), r, map, st).getInstructions());
			
			TYPE t = ((Atom) c.operands.get(1)).getType();
			
			if (Integer.parseInt(t.sourceCodeRepresentation()) < 255) {
				cmp.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(Integer.parseInt(t.sourceCodeRepresentation()))));
			}
			else {
				AsNBody.literalManager.loadValue(cmp, Integer.parseInt(t.sourceCodeRepresentation()), 1);
				cmp.instructions.add(new ASMCmp(new RegOp(REG.R0), new RegOp(REG.R1)));
			}
		}
		else {
			/* Generate Loader code that places the operands in R0 and R1 */
			cmp.generatePrimitiveLoaderCode(cmp, c, c.operands.get(0), c.operands.get(1), r, map, st, 0, 1);
			
			cmp.instructions.add(new ASMCmp(new RegOp(REG.R0), new RegOp(REG.R1)));
		}
	
		cmp.trueC = COND.toCondition(c.comparator);
		cmp.neg = cmp.trueC.negate();
		
		/* Move #1 into R0 when condition is true with comparator of c */
		cmp.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), cmp.trueC));
		
		/* Move #0 into R0 when condition is false with negated operator of c */
		cmp.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), cmp.neg));
	
		r.free(0, 1);
		
		cmp.registerMetric();
		return cmp;
	}

	public ASMInstruction buildInjector() {
		throw new SNIPS_EXC("No injector available for 'Cmp'!");
	}
	
} 
