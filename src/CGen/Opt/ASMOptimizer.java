package CGen.Opt;

import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AsN.AsNBody;

public class ASMOptimizer {

	public ASMOptimizer() {
		
	}
	
	public void optimize(AsNBody body) {
		for (int i = 1; i < body.instructions.size(); i++) {
			if (body.instructions.get(i - 1) instanceof ASMMove && body.instructions.get(i) instanceof ASMMove) {
				ASMMove m0 = (ASMMove) body.instructions.get(i - 1);
				ASMMove m1 = (ASMMove) body.instructions.get(i);
				
				if (m1.origin instanceof RegOperand) {
					if (((RegOperand) m1.origin).reg == ((RegOperand) m0.target).reg) {
						m1.origin = m0.origin;
						body.instructions.remove(i - 1);
						i--;
					}
				}
			}
		}
	}
	
}
