package Imm.AsN.Expression;

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
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BinaryExpression;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AsN.Expression.Arith.AsNAdd;
import Imm.AsN.Expression.Arith.AsNCmp;
import Imm.AsN.Expression.Arith.AsNLsl;
import Imm.AsN.Expression.Arith.AsNLsr;
import Imm.AsN.Expression.Arith.AsNMult;
import Imm.AsN.Expression.Arith.AsNSub;
import Imm.TYPE.PRIMITIVES.INT;

public abstract class AsNBinaryExpression extends AsNExpression {

	/**
	 * Solve the binary expression for two given operands.
	 */
	public interface BinarySolver {
		public int solve(int a, int b);
	}
	
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
			return AsNCmp.cast((Compare) e, r);
		}
		else if (e instanceof Lsl) {
			return AsNLsl.cast((Lsl) e, r);
		}
		else if (e instanceof Lsr) {
			return AsNLsr.cast((Lsr) e, r);
		}
		else throw new CGEN_EXCEPTION(e.getSource(), "No injection cast available for " + e.getClass().getName());
	}
	
		/* --- OPERAND LOADING --- */
	protected void generateLoaderCode(AsNBinaryExpression m, BinaryExpression b, RegSet r, BinarySolver solver, ASMInstruction inject) throws CGEN_EXCEPTION {
		/* Total Atomic Loading */
		if (b.left() instanceof Atom && b.right() instanceof Atom) {
			m.atomicPrecalc(b, solver);
		}
		else {
			this.clearOperandRegs(r);
			
			/* Partial Atomic Loading Left */
			if (b.left() instanceof Atom) {
				m.loadRightOperand(b, 2, r);
				m.instructions.add(new ASMMove(new RegOperand(1), new ImmOperand(((INT) ((Atom) b.left()).type).value)));
			}
			/* Partial Atomic Loading Right */
			else if (b.right() instanceof Atom) {
				m.loadLeftOperand(b, 1, r);
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
	
	protected void loadLeftOperand(BinaryExpression b, int target, RegSet r) throws CGEN_EXCEPTION {
		this.loadOperand(b.left(), target, r);
	}
	
	protected void loadRightOperand(BinaryExpression b, int target, RegSet r) throws CGEN_EXCEPTION {
		this.loadOperand(b.right(), target, r);
	}
	
	protected void loadOperand(Expression e, int target, RegSet r) throws CGEN_EXCEPTION {
		/* Operand is ID Reference and can be loaded directley into the target register, 
		 * 		no need for intermidiate result in R0 */
		if (e instanceof IDRef) {
			this.instructions.addAll(AsNIdRef.cast((IDRef) e, r, target).getInstructions());
		}
		else {
			this.instructions.addAll(AsNExpression.cast(e, r).getInstructions());
			if (target != 0) {
				this.instructions.add(new ASMMove(new RegOperand(target), new RegOperand(0)));
				r.copy(0, target);
			}
		}
		
		r.regs [target].setExpression(e);
	}
	
	/**
	 * Precalculate this expression since both operands are immediates.
	 */
	protected void atomicPrecalc(BinaryExpression b, BinarySolver s) {
		if (b.left() instanceof Atom && b.right() instanceof Atom) {
			Atom l0 = (Atom) b.left(), r0 = (Atom) b.right();
			if (l0.type instanceof INT && r0.type instanceof INT) {
				INT i0 = (INT) l0.type, i1 = (INT) r0.type;
				this.instructions.add(new ASMMove(new RegOperand(0), new ImmOperand(s.solve(i0.value, i1.value))));
			}
		}
	}
	
		/* --- REGISTER CLEARING --- */
	/**
	 * Clear R0, R1, R2 using {@link #clearReg(RegSet, int)}
	 * @param r The current RegSet
	 */
	protected void clearOperandRegs(RegSet r) {
		this.clearReg(r, 0);
		this.clearReg(r, 1);
		this.clearReg(r, 2);
	}
	
	/**
	 * Clear given reg under the current RegSet by searching for a free reg and copying the value
	 * into it. Clears the given reg in the RegSet.
	 * @param r The current RegSet
	 * @param reg The Register to clear
	 */
	protected void clearReg(RegSet r, int reg) {
		if (!r.regs [reg].isFree()) {
			int free = r.findFree();
			this.instructions.add(new ASMMove(new RegOperand(free), new RegOperand(reg)));
			r.copy(reg, free);
			r.regs [reg].free();
		}
	}
	
}
