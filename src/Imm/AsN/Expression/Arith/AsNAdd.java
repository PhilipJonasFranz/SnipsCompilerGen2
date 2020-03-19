package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.ImmOperand;
import Imm.ASM.Util.RegOperand;
import Imm.ASM.Util.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Arith.Add;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public class AsNAdd extends AsNExpression {

	public AsNAdd() {
		
	}
	
	public static AsNAdd cast(Add a, RegSet r) throws CGEN_EXCEPTION {
		AsNAdd add = new AsNAdd();
		
		/* Process left and right operand */
		
		/* Both operands are immediates, precalculate */
		if (a.left() instanceof Atom && a.right() instanceof Atom) {
			Atom l0 = (Atom) a.left(), r0 = (Atom) a.right();
			if (l0.type instanceof INT && r0.type instanceof INT) {
				INT i0 = (INT) l0.type, i1 = (INT) r0.type;
				add.instructions.add(new ASMMove(new RegOperand(0), new ImmOperand(i0.value + i1.value)));
			}
		}
		else {
			if (a.left() instanceof Atom) {
				add.loadRight(a, 2, r);
				add.loadLeft(a, 1, r);
			}
			else if (a.right() instanceof Atom) {
				add.loadLeft(a, 1, r);
				add.loadRight(a, 2, r);
			}
			else {
				add.instructions.addAll(AsNExpression.cast(a.left(), r).getInstructions());
				add.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				r.regs [0].free();
				
				add.instructions.addAll(AsNExpression.cast(a.right(), r).getInstructions());
				
				add.instructions.add(new ASMMove(new RegOperand(2), new RegOperand(0)));
				r.copy(0, 2);
				
				add.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
			}
			
			add.instructions.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			r.regs [0].setExpression(a);
			r.regs [1].free();
			r.regs [2].free();
		}
		
		return add;
	}
	
	private void loadLeft(Add a, int target, RegSet r) throws CGEN_EXCEPTION {
		this.instructions.addAll(AsNExpression.cast(a.left(), r).getInstructions());
		this.instructions.add(new ASMMove(new RegOperand(target), new RegOperand(0)));
		r.copy(0, target);
	}
	
	private void loadRight(Add a, int target, RegSet r) throws CGEN_EXCEPTION {
		this.instructions.addAll(AsNExpression.cast(a.right(), r).getInstructions());
		this.instructions.add(new ASMMove(new RegOperand(target), new RegOperand(0)));
		r.copy(0, target);
	}
	
}
