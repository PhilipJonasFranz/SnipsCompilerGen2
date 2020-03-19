package Imm.AsN.Statement;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.RegOperand;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Expression.AsNExpression;

public class AsNDeclaration extends AsNStatement {

	public AsNDeclaration() {
		
	}

	public static AsNDeclaration cast(Declaration d, RegSet r) throws CGEN_EXCEPTION {
		AsNDeclaration dec = new AsNDeclaration();
		
		dec.instructions.addAll(AsNExpression.cast(d.value, r).getInstructions());
		
		int free = r.findFree();
		if (free != -1) {
			dec.instructions.add(new ASMMove(new RegOperand(free), new RegOperand(0)));
			r.regs [free].setDeclaration(d);
		}
		else {
			throw new CGEN_EXCEPTION(d.getSource(), "RegStack Overflow!");
		}
		
		return dec;
	}
	
}
