package Imm.AsN.Expression;

import CGen.RegSet;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.ImmediateOperand;
import Imm.ASM.Util.RegisterOperand;
import Imm.ASM.Util.RegisterOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNAtom extends AsNExpression {

	public AsNAtom() {
		
	}
	
	public static AsNAtom cast(Atom a, RegSet r) {
		AsNAtom atom = new AsNAtom();
		
		if (a.type instanceof INT) {
			atom.instructions.add(new ASMMove(new RegisterOperand(REGISTER.R0), new ImmediateOperand(((INT) a.type).value)));
			r.regs [0].setAtomic(a);
		}
		
		return atom;
	}
	
}
