package Imm.AsN.Expression;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNAtom extends AsNExpression {

	public AsNAtom() {
		
	}
	
	public static AsNAtom cast(Atom a, RegSet r) throws CGEN_EXCEPTION {
		AsNAtom atom = new AsNAtom();
		
		if (a.type instanceof INT) {
			atom.instructions.add(new ASMMove(new RegOperand(REGISTER.R0), new ImmOperand(((INT) a.type).value)));
			r.regs [0].setExpression(a);
		}
		else throw new CGEN_EXCEPTION(a.getSource(), "No cast for atom type supported: " + a.type.typeString());
		
		return atom;
	}
	
}
