package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Stack.ASMPopStack;
import Imm.ASM.Stack.ASMPushStack;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BinaryExpression;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AsN.Expression.AsNExpression;
import Imm.TYPE.PRIMITIVES.INT;

public abstract class AsNBinaryExpression extends AsNExpression {

	public AsNBinaryExpression() {
		
	}
	
	public static AsNBinaryExpression cast(Expression e, RegSet r) throws CGEN_EXCEPTION {
		/* Relay to Expression type */
		if (e instanceof Add) {
			return AsNAdd.cast((Add) e, r);
		}
		else if (e instanceof Sub) {
			return AsNSub.cast((Sub) e, r);
		}
		else if (e instanceof Mul) {
			return AsNMult.cast((Mul) e, r);
		}
		else if (e instanceof Compare) {
			return AsNCompare.cast((Compare) e, r);
		}
		else if (e instanceof Lsl) {
			return AsNLsl.cast((Lsl) e, r);
		}
		else if (e instanceof Lsr) {
			return AsNLsr.cast((Lsr) e, r);
		}
		else throw new CGEN_EXCEPTION(e.getSource(), "No cast available for " + e.getClass().getName());
	}
	
	protected void generateLoaderCode(AsNBinaryExpression m, BinaryExpression b, RegSet r, Solver solver, ASMInstruction inject) throws CGEN_EXCEPTION {
		/* Total Atomic Loading */
		if (b.left() instanceof Atom && b.right() instanceof Atom) {
			m.atomicPrecalc(b, solver);
		}
		else {
			/* Partial Atomic Loading Left */
			if (b.left() instanceof Atom) {
				m.loadRight(b, 2, r);
				m.instructions.add(new ASMMove(new RegOperand(1), new ImmOperand(((INT) ((Atom) b.left()).type).value)));
			}
			/* Partial Atomic Loading Right */
			else if (b.right() instanceof Atom) {
				m.loadLeft(b, 1, r);
				m.instructions.add(new ASMMove(new RegOperand(2), new ImmOperand(((INT) ((Atom) b.right()).type).value)));
			}
			else {
				m.instructions.addAll(AsNExpression.cast(b.left(), r).getInstructions());
				m.instructions.add(new ASMPushStack(new RegOperand(REGISTER.R0)));
				r.regs [0].free();
				
				m.instructions.addAll(AsNExpression.cast(b.right(), r).getInstructions());
				
				m.instructions.add(new ASMMove(new RegOperand(2), new RegOperand(0)));
				r.copy(0, 2);
				
				m.instructions.add(new ASMPopStack(new RegOperand(REGISTER.R1)));
			}
			
			/* Inject calculation into loader code */
			m.instructions.add(inject);
			
			/* Clean up Reg Set */
			r.regs [0].setExpression(b);
			r.regs [1].free();
			r.regs [2].free();
		}
	}
	
	protected void loadLeft(BinaryExpression b, int target, RegSet r) throws CGEN_EXCEPTION {
		this.instructions.addAll(AsNExpression.cast(b.left(), r).getInstructions());
		if (target != 0) {
			this.instructions.add(new ASMMove(new RegOperand(target), new RegOperand(0)));
			r.copy(0, target);
		}
	}
	
	protected void loadRight(BinaryExpression b, int target, RegSet r) throws CGEN_EXCEPTION {
		this.instructions.addAll(AsNExpression.cast(b.right(), r).getInstructions());
		if (target != 0) {
			this.instructions.add(new ASMMove(new RegOperand(target), new RegOperand(0)));
			r.copy(0, target);
		}
	}
	
	/**
	 * Solve the binary expression for two given operands.
	 */
	public interface Solver {
		public int solve(int a, int b);
	}
	
	/**
	 * Precalculate this expression since both operands are immediates.
	 */
	protected void atomicPrecalc(BinaryExpression b, Solver s) {
		if (b.left() instanceof Atom && b.right() instanceof Atom) {
			Atom l0 = (Atom) b.left(), r0 = (Atom) b.right();
			if (l0.type instanceof INT && r0.type instanceof INT) {
				INT i0 = (INT) l0.type, i1 = (INT) r0.type;
				this.instructions.add(new ASMMove(new RegOperand(0), new ImmOperand(s.solve(i0.value, i1.value))));
			}
		}
	}
	
}
