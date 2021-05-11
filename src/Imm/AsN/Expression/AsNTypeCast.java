package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Util.PRECISION;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Processing.Arith.ASMVCvt;
import Imm.ASM.VFP.Processing.Arith.ASMVMov;
import Imm.AST.Expression.TypeCast;

public class AsNTypeCast extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNTypeCast cast(TypeCast tc, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNTypeCast t = new AsNTypeCast();
		t.pushOnCreatorStack(tc);
		tc.castedNode = t;
		
		/* 
		 * Relay to capsuled expression for now, currently no datatype requires some kind of transformation,
		 * FLOAT -> INT, INT -> FLOAT in the future maybe.
		 */
		t.instructions.addAll(AsNExpression.cast(tc.expression, r, map, st).getInstructions());
		
		/* Cast from FLOAT to non-float */
		if (tc.expression.getType().isFloat() && !tc.castType.isFloat()) {
			t.instructions.add(new ASMVCvt(new VRegOp(REG.S0), new VRegOp(REG.S0), PRECISION.F32, PRECISION.S32));
			t.instructions.add(new ASMVMov(new RegOp(REG.R0), new VRegOp(REG.S0)));
		}
		
		/* Cast from non-float to FLOAT */
		if (!tc.expression.getType().isFloat() && tc.castType.isFloat()) {
			t.instructions.add(new ASMVMov(new VRegOp(REG.S0), new RegOp(REG.R0)));
			t.instructions.add(new ASMVCvt(new VRegOp(REG.S0), new VRegOp(REG.S0), PRECISION.S32, PRECISION.F32));
		}
		
		t.registerMetric();
		return t;
	}
	
} 
