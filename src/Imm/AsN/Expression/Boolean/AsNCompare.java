package Imm.AsN.Expression.Boolean;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.Util.REG;
import Imm.ASM.VFP.Processing.Logic.ASMVCmp;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AsN.AsNBody;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNNFoldExpression;
import Imm.TYPE.PRIMITIVES.NULL;
import Imm.TYPE.TYPE;
import Util.FBin;

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

		boolean isVFP = c.operands.stream().anyMatch(x -> x.getType().isFloat());
		
		if (c.operands.size() > 2) throw new SNIPS_EXC("N-Operand Chains are not supported!");
		
		/* Clear only R0, R1 since R2 is not needed */
		if (isVFP) r.getVRegSet().free(0, 1);
		else r.free(0, 1);
		
		if (c.operands.get(1) instanceof Atom && !(c.operands.get(1).getType() instanceof NULL)) {
			cmp.instructions.addAll(AsNExpression.cast(c.operands.get(0), r, map, st).getInstructions());
			
			TYPE t = c.operands.get(1).getType();
			int value = Integer.parseInt(t.toPrimitive().sourceCodeRepresentation());
			
			if (value < 255) {
				if (isVFP) {
					AsNBody.literalManager.loadValue(cmp, FBin.toDecimal(FBin.toFBin(value)), 1, true, "" + (float) value);
					cmp.instructions.add(new ASMVCmp(new VRegOp(REG.S0), new VRegOp(REG.S1)));
				}
				else cmp.instructions.add(new ASMCmp(new RegOp(REG.R0), new ImmOp(value)));
			}
			else {
				AsNBody.literalManager.loadValue(cmp, value, 1, t.isFloat(), t.value.toString());
				cmp.instructions.add(new ASMCmp(new RegOp(REG.R0), new RegOp(REG.R1)));
				
				if (isVFP) cmp.instructions.add(new ASMVCmp(new VRegOp(REG.S0), new VRegOp(REG.S1)));
				else cmp.instructions.add(new ASMCmp(new RegOp(REG.R0), new RegOp(REG.R1)));
			}
		}
		else {
			/* Generate Loader code that places the operands in R0 and R1 */
			cmp.generatePrimitiveLoaderCode(cmp, c.operands.get(0), c.operands.get(1), r, map, st, 0, 1, isVFP);
			
			if (isVFP) cmp.instructions.add(new ASMVCmp(new VRegOp(REG.S0), new VRegOp(REG.S1)));
			else cmp.instructions.add(new ASMCmp(new RegOp(REG.R0), new RegOp(REG.R1)));
		}
	
		cmp.trueC = COND.toCondition(c.comparator);
		cmp.neg = cmp.trueC.negate();
		
		/* Move #1 into R0 when condition is true with comparator of c */
		cmp.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), cmp.trueC));
		
		/* Move #0 into R0 when condition is false with negated operator of c */
		cmp.instructions.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), cmp.neg));
	
		if (isVFP) r.getVRegSet().free(0, 1);
		else r.free(0, 1);
		
		cmp.registerMetric();
		return cmp;
	}
	
} 
