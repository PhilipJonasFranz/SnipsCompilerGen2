package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.NFoldExpression;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BitAnd;
import Imm.AST.Expression.Arith.BitOr;
import Imm.AST.Expression.Arith.BitXor;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Boolean.And;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Or;
import Imm.AsN.AsNBody;
import Imm.AsN.Expression.Arith.AsNAdd;
import Imm.AsN.Expression.Arith.AsNBitAnd;
import Imm.AsN.Expression.Arith.AsNBitOr;
import Imm.AsN.Expression.Arith.AsNBitXor;
import Imm.AsN.Expression.Arith.AsNLsl;
import Imm.AsN.Expression.Arith.AsNLsr;
import Imm.AsN.Expression.Arith.AsNMult;
import Imm.AsN.Expression.Arith.AsNSub;
import Imm.AsN.Expression.Boolean.AsNAnd;
import Imm.AsN.Expression.Boolean.AsNCmp;
import Imm.AsN.Expression.Boolean.AsNOr;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Res.Const;

public abstract class AsNBinaryExpression extends AsNExpression {

			/* ---< NESTED >--- */
	/**
	 * Solve the binary expression for two given operands.
	 */
	public interface BinarySolver {
		public int solve(int a, int b);
	}
	

			/* ---< METHODS >--- */
	public static AsNBinaryExpression cast(Expression e, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to Expression type */
		AsNBinaryExpression node = null;
		
		if (e instanceof Add) {
			node = AsNAdd.cast((Add) e, r, map, st);
		}
		else if (e instanceof Sub) {
			node = AsNSub.cast((Sub) e, r, map, st);
		}
		else if (e instanceof Mul) {
			node = AsNMult.cast((Mul) e, r, map, st);
		}
		else if (e instanceof Compare) {
			node = AsNCmp.cast((Compare) e, r, map, st);
		}
		else if (e instanceof Lsl) {
			return AsNLsl.cast((Lsl) e, r, map, st);
		}
		else if (e instanceof Lsr) {
			node = AsNLsr.cast((Lsr) e, r, map, st);
		}
		else if (e instanceof And) {
			node = AsNAnd.cast((And) e, r, map, st);
		}
		else if (e instanceof Or) {
			node = AsNOr.cast((Or) e, r, map, st);
		}
		else if (e instanceof BitOr) {
			node = AsNBitOr.cast((BitOr) e, r, map, st);
		}
		else if (e instanceof BitAnd) {
			node = AsNBitAnd.cast((BitAnd) e, r, map, st);
		}
		else if (e instanceof BitXor) {
			node = AsNBitXor.cast((BitXor) e, r, map, st);
		}
		else throw new CGEN_EXC(e.getSource(), Const.NO_INJECTION_CAST_AVAILABLE, e.getClass().getName());
	
		e.castedNode = node;
		return node;
	}
	
	
		/* --- OPERAND LOADING --- */
	protected void generatePrimitiveLoaderCode(AsNBinaryExpression m, NFoldExpression b, RegSet r, MemoryMap map, StackSet st, int target0, int target1) throws CGEN_EXC {
		
		/* Some assertions for debug purposes */
		if (b.getLeft() instanceof TypeCast) {
			assert(b.getLeft().getType().getCoreType() instanceof PRIMITIVE);
		}
		
		if (b.getRight() instanceof TypeCast) {
			assert(b.getRight().getType().getCoreType() instanceof PRIMITIVE);
		}
		
		/* If operands are TypeCasts, unrwrap expression from type cast */
		Expression left = (b.getLeft() instanceof TypeCast)? ((TypeCast) b.getLeft()).expression : b.getLeft();
		Expression right = (b.getRight() instanceof TypeCast)? ((TypeCast) b.getRight()).expression : b.getRight();
		
		/* Load both operands directley */
		if (left instanceof IDRef && right instanceof IDRef) {
			m.instructions.addAll(AsNIDRef.cast((IDRef) left, r, map, st, target0).getInstructions());
			m.instructions.addAll(AsNIDRef.cast((IDRef) right, r, map, st, target1).getInstructions());
		}
		/* Load the right operand, then the left directley */
		else if (left instanceof IDRef) {
			m.instructions.addAll(AsNExpression.cast(right, r, map, st).getInstructions());
			if (target1 != 0) {
				m.instructions.add(new ASMMov(new RegOp(target1), new RegOp(0)));
				r.copy(0, target1);
			}
			
			m.instructions.addAll(AsNIDRef.cast((IDRef) left, r, map, st, target0).getInstructions());
		}
		/* Load the left operand, then the right directley */
		else if (right instanceof IDRef) {
			m.instructions.addAll(AsNExpression.cast(left, r, map, st).getInstructions());
			if (target0 != 0) {
				m.instructions.add(new ASMMov(new RegOp(target0), new RegOp(0)));
				r.copy(0, target0);
			}
			
			m.instructions.addAll(AsNIDRef.cast((IDRef) right, r, map, st, target1).getInstructions());
		}
		else {
			r.free(0, 1, 2);
			
			/* Compute left operand and push the result on the stack */
			m.instructions.addAll(AsNExpression.cast(left, r, map, st).getInstructions());
			
			int free = -1;
			
			/* 
			 * Expression is inline call, which means that push/pop most likely
			 * cannot be removed during optimizing. Attempt to move to other reg.
			 */
			if (right instanceof InlineCall) free = r.findFree();
			
			if (free != -1) {
				m.instructions.add(new ASMMov(new RegOp(free), new RegOp(REG.R0)));
				r.getReg(free).setDeclaration(null);
			}
			else {
				m.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
				st.pushDummy();
			}
			
			r.free(0);
			
			/* Compute the right operand and move it to target location */
			m.instructions.addAll(AsNExpression.cast(right, r, map, st).getInstructions());
			
			/* Check if instructions were added, if not, this means that the operand is already loaded in the correct location */
			if (target1 != 0) {
				m.instructions.add(new ASMMov(new RegOp(target1), new RegOp(0)));
				r.copy(0, target1);
			}
			
			if (free == -1) {
				/* Pop the left operand in the target register */
				m.instructions.add(new ASMPopStack(new RegOp(target0)));
				st.pop();
			}
			else {
				r.free(free);
				m.instructions.add(new ASMMov(new RegOp(target0), new RegOp(free)));
			}
		}
	}
	
	protected void generateLoaderCode(AsNBinaryExpression m, NFoldExpression b, RegSet r, MemoryMap map, StackSet st, BinarySolver solver, ASMInstruction inject) throws CGEN_EXC {
		/* Total Atomic Loading */
		if (b.getLeft() instanceof Atom && b.getRight() instanceof Atom) {
			m.atomicPrecalc(b, solver);
		}
		else {
			this.clearReg(r, st, 0, 1, 2);
			
			/* Partial Atomic Loading Left */
			if (b.getLeft() instanceof Atom) {
				this.loadOperand(b.getRight(), 2, r, map, st);
				AsNBody.literalManager.loadValue(this, Integer.parseInt(b.getLeft().getType().sourceCodeRepresentation()), 1);
			}
			/* Partial Atomic Loading Right */
			else if (b.getRight() instanceof Atom) {
				this.loadOperand(b.getLeft(), 1, r, map, st);
				AsNBody.literalManager.loadValue(this, Integer.parseInt(b.getRight().getType().sourceCodeRepresentation()), 2);
			}
			else m.generatePrimitiveLoaderCode(m, b, r, map, st, 1, 2);
			
			/* Inject calculation into loader code */
			m.instructions.add(inject);
			
			/* Clean up Reg Set */
			r.free(0, 1, 2);
		}
	}
	
	protected void loadOperand(Expression e, int target, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Operand is ID Reference and can be loaded directley into the target register, 
		 * 		no need for intermidiate result in R0 */
		if (e instanceof IDRef) {
			this.instructions.addAll(AsNIDRef.cast((IDRef) e, r, map, st, target).getInstructions());
		}
		else {
			this.instructions.addAll(AsNExpression.cast(e, r, map, st).getInstructions());
			if (target != 0) {
				this.instructions.add(new ASMMov(new RegOp(target), new RegOp(0)));
				r.copy(0, target);
			}
		}
	}
	
	/**
	 * Precalculate this expression since both operands are immediates.
	 */
	protected void atomicPrecalc(NFoldExpression b, BinarySolver s) {
		if (b.getLeft() instanceof Atom && b.getRight() instanceof Atom) {
			Atom l0 = (Atom) b.getLeft(), r0 = (Atom) b.getRight();
			if (l0.getType() instanceof INT && r0.getType() instanceof INT) {
				INT i0 = (INT) l0.getType(), i1 = (INT) r0.getType();
				AsNBody.literalManager.loadValue(this, s.solve(i0.value, i1.value), 0);
			}
		}
	}
	
} 
