package Imm.AsN.Statement;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Expression.AsNExpression;

public class AsNDeclaration extends AsNStatement {

	public static AsNDeclaration cast(Declaration d, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNDeclaration dec = new AsNDeclaration();
		
		dec.instructions.addAll(AsNExpression.cast(d.value, r, st).getInstructions());
		
		int free = r.findFree();
		if (free != -1) {
			dec.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(0)));
			r.getReg(free).setDeclaration(d);
		}
		else {
			throw new CGEN_EXCEPTION(d.getSource(), "RegStack Overflow!");
		}
		
		return dec;
	}
	
}
