package Imm.AsN.Expression;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNAtom extends AsNExpression {

	public static AsNAtom cast(Atom a, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		AsNAtom atom = new AsNAtom();
		a.castedNode = atom;
		
		/* Int Literal, move directley into R0 */
		if (a.type instanceof INT) {
			atom.instructions.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(((INT) a.type).value)));
			r.getReg(0).setExpression(a);
		}
		else throw new CGEN_EXCEPTION(a.getSource(), "No injection cast for atom type supported: " + a.type.typeString());
		
		return atom;
	}
	
}
