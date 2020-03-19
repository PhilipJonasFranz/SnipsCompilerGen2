package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMMove;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
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
