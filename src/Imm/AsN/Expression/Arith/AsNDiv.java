package Imm.AsN.Expression.Arith;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.Util.REG;
import Imm.ASM.VFP.Processing.Arith.ASMVDiv;
import Imm.AST.Expression.Arith.Div;
import Imm.AsN.Expression.AsNInlineCall;
import Imm.AsN.Expression.AsNNFoldExpression;

public class AsNDiv extends AsNNFoldExpression {

			/* ---< METHODS >--- */
	public static AsNDiv cast(Div d, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNDiv div = new AsNDiv();
		div.pushOnCreatorStack(d);
		d.castedNode = div;

		if (d.placeholderCall != null) {
			/* Not a float division, inline call calls the __op_div function */
			div.instructions.addAll(AsNInlineCall.cast(d.placeholderCall, r, map, st).getInstructions());
		}
		else {
			/* Float division using the vdiv instruction */
			div.evalExpression(div, d, r, map, st);
		}

		div.registerMetric();
		return div;
	}

	public ASMInstruction buildVInjector() {
		return new ASMVDiv(new VRegOp(REG.S0), new VRegOp(REG.S1), new VRegOp(REG.S2));
	}
	
} 
