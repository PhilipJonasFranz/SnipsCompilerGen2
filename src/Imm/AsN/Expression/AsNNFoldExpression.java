package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Memory.Stack.ASMVPopStack;
import Imm.ASM.VFP.Memory.Stack.ASMVPushStack;
import Imm.ASM.VFP.Processing.Arith.ASMVMov;
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
import Imm.AsN.Expression.Arith.AsNMul;
import Imm.AsN.Expression.Arith.AsNSub;
import Imm.AsN.Expression.Boolean.AsNAnd;
import Imm.AsN.Expression.Boolean.AsNCompare;
import Imm.AsN.Expression.Boolean.AsNOr;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Res.Const;

public abstract class AsNNFoldExpression extends AsNExpression {

			/* ---< NESTED >--- */
	/**
	 * Solve the binary expression for two given operands.
	 */
	public interface BinarySolver {
		public int solve(int a, int b);
	}
	

			/* ---< METHODS >--- */
	public static AsNNFoldExpression cast(Expression e, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to Expression type */
		AsNNFoldExpression node = null;
		
		if (e instanceof Add) {
			node = AsNAdd.cast((Add) e, r, map, st);
		}
		else if (e instanceof Sub) {
			node = AsNSub.cast((Sub) e, r, map, st);
		}
		else if (e instanceof Mul) {
			node = AsNMul.cast((Mul) e, r, map, st);
		}
		else if (e instanceof Compare) {
			node = AsNCompare.cast((Compare) e, r, map, st);
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
	
	public ASMInstruction buildInjector() {
		throw new SNIPS_EXC("No injector available for '" + this.getClass().getSimpleName() + "'!");
	}
	
	public ASMInstruction buildVInjector() {
		throw new SNIPS_EXC("No VFP injector available for '" + this.getClass().getSimpleName() + "'!");
	}

	
		/* --- OPERAND LOADING --- */
	protected void generatePrimitiveLoaderCode(AsNNFoldExpression m, NFoldExpression b, Expression e0, Expression e1, RegSet r, MemoryMap map, StackSet st, int target0, int target1, boolean isVFP) throws CGEN_EXC {
		
		/* Some assertions for debug purposes */
		if (e0 instanceof TypeCast) {
			assert(e0.getType().getCoreType() instanceof PRIMITIVE);
		}
		
		if (e1 instanceof TypeCast) {
			assert(e1.getType().getCoreType() instanceof PRIMITIVE);
		}
		
		/* If operands are TypeCasts, unrwrap expression from type cast */
		Expression left = (e0 instanceof TypeCast)? ((TypeCast) e0).expression : e0;
		Expression right = (e1 instanceof TypeCast)? ((TypeCast) e1).expression : e1;
		
		/* Load both operands directley */
		if (left instanceof IDRef && right instanceof IDRef) {
			m.instructions.addAll(AsNIDRef.cast((IDRef) left, r, map, st, target0).getInstructions());
			m.instructions.addAll(AsNIDRef.cast((IDRef) right, r, map, st, target1).getInstructions());
		}
		/* Load the right operand, then the left directley */
		else if (left instanceof IDRef) {
			m.instructions.addAll(AsNExpression.cast(right, r, map, st).getInstructions());
			if (target1 != 0) {
				if (isVFP) m.instructions.add(new ASMVMov(new VRegOp(target1), new VRegOp(0)));
				else m.instructions.add(new ASMMov(new RegOp(target1), new RegOp(0)));
				
				r.copy(0, target1);
			}
			
			m.instructions.addAll(AsNIDRef.cast((IDRef) left, r, map, st, target0).getInstructions());
		}
		/* Load the left operand, then the right directley */
		else if (right instanceof IDRef) {
			m.instructions.addAll(AsNExpression.cast(left, r, map, st).getInstructions());
			if (target0 != 0) {
				if (isVFP) m.instructions.add(new ASMVMov(new VRegOp(target0), new VRegOp(0)));
				else m.instructions.add(new ASMMov(new RegOp(target0), new RegOp(0)));
				
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
			if (right instanceof InlineCall) {
				if (isVFP) free = r.getVRegSet().findFree();
				else free = r.findFree();
			}
			
			if (isVFP) {
				if (free != -1) {
					m.instructions.add(new ASMVMov(new VRegOp(free), new VRegOp(REG.S0)));
					r.getVRegSet().getReg(free).setDeclaration(null);
				}
				else {
					m.instructions.add(new ASMVPushStack(new VRegOp(REG.S0)));
					st.pushDummy();
				}
			}
			else {
				if (free != -1) {
					m.instructions.add(new ASMMov(new RegOp(free), new RegOp(REG.R0)));
					r.getReg(free).setDeclaration(null);
				}
				else {
					m.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
					st.pushDummy();
				}
			}
			
			if (isVFP) r.getVRegSet().free(0);
			else r.free(0);
			
			/* Compute the right operand and move it to target location */
			m.instructions.addAll(AsNExpression.cast(right, r, map, st).getInstructions());
			
			/* Check if instructions were added, if not, this means that the operand is already loaded in the correct location */
			if (target1 != 0) {
				if (isVFP) {
					m.instructions.add(new ASMVMov(new VRegOp(target1), new VRegOp(0)));
					r.getVRegSet().copy(0, target1);
				}
				else {
					m.instructions.add(new ASMMov(new RegOp(target1), new RegOp(0)));
					r.copy(0, target1);
				}
			}
			
			if (free == -1) {
				/* Pop the left operand in the target register */
				if (isVFP) {
					m.instructions.add(new ASMVPopStack(new VRegOp(REG.S0)));
					m.instructions.add(new ASMVMov(new VRegOp(target0), new RegOp(REG.S0)));
				}
				else m.instructions.add(new ASMPopStack(new RegOp(target0)));
				st.pop();
			}
			else {
				if (isVFP) {
					r.getVRegSet().free(free);
					m.instructions.add(new ASMVMov(new VRegOp(target0), new VRegOp(free)));
				}
				else {
					r.free(free);
					m.instructions.add(new ASMMov(new RegOp(target0), new RegOp(free)));
				}
			}
		}
	}
	
	protected void evalExpression(AsNNFoldExpression m, NFoldExpression b, RegSet r, MemoryMap map, StackSet st, BinarySolver solver) throws CGEN_EXC {
		
		boolean isVFP = b.operands.stream().filter(x -> x.getType().isFloat()).count() > 0;
		
		this.clearReg(r, st, isVFP, 0, 1, 2);

		generateLoaderCode(m, b, b.operands.get(0), b.operands.get(1), r, map, st, solver, isVFP);
		
		/* Inject calculation into loader code */
		if (!isVFP) m.instructions.add(m.buildInjector());
		else m.instructions.add(m.buildVInjector());
		
		if (b.operands.size() > 2) {
			for (int i = 2; i < b.operands.size(); i++) {
				Expression op0 = b.operands.get(i);
				boolean requireClear = !(op0 instanceof IDRef || op0 instanceof Atom);
				
				int free = -1;
				
				/* 
				 * Expression is inline call, which means that push/pop most likely
				 * cannot be removed during optimizing. Attempt to move to other reg.
				 */
				if (op0 instanceof InlineCall) {
					if (isVFP) free = r.getVRegSet().findFree();
					else free = r.findFree();
				}
				
				if (requireClear) {
					if (isVFP) {
						if (free != -1) {
							m.instructions.add(new ASMVMov(new VRegOp(free), new VRegOp(REG.S0)));
							r.getVRegSet().getReg(free).setDeclaration(null);
						}
						else {
							m.instructions.add(new ASMVPushStack(new VRegOp(REG.S0)));
							st.pushDummy();
						}
					}
					else {
						if (free != -1) {
							m.instructions.add(new ASMMov(new RegOp(free), new RegOp(REG.R0)));
							r.getReg(free).setDeclaration(null);
						}
						else {
							m.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
							st.pushDummy();
						}
					}
				}
				else {
					if (isVFP) this.instructions.add(new ASMVMov(new VRegOp(REG.S1), new VRegOp(REG.S0)));
					else this.instructions.add(new ASMMov(new RegOp(REG.R1), new RegOp(REG.R0)));
				}
				
				if (isVFP) r.getVRegSet().free(2);
				else r.free(2);
				
				loadOperand(b.operands.get(i), 2, r, map, st, isVFP);
				
				if (requireClear) {
					if (free != -1) {
						/* Move back from original reg */
						if (isVFP) this.instructions.add(new ASMVMov(new VRegOp(REG.S1), new RegOp(free)));
						else this.instructions.add(new ASMMov(new RegOp(REG.R1), new RegOp(free)));
						r.free(free);
					}
					else {
						if (isVFP) this.instructions.add(new ASMVPopStack(new VRegOp(REG.S1)));
						else this.instructions.add(new ASMPopStack(new RegOp(REG.R1)));
						st.pop();
					}
				}
				
				/* Inject calculation into loader code */
				if (!isVFP) m.instructions.add(m.buildInjector());
				else m.instructions.add(m.buildVInjector());
			}
		}
		
		/* Clean up Reg Set */
		if (isVFP) r.getVRegSet().free(0, 1, 2);
		else r.free(0, 1, 2);
	}
	
	protected void generateLoaderCode(AsNNFoldExpression m, NFoldExpression b, Expression e0, Expression e1, RegSet r, MemoryMap map, StackSet st, BinarySolver solver, boolean isVFP) throws CGEN_EXC {
		/* Partial Atomic Loading Left */
		if (e0 instanceof Atom) {
			this.loadOperand(e1, 2, r, map, st, isVFP);
			AsNBody.literalManager.loadValue(this, Integer.parseInt(e0.getType().toPrimitive().sourceCodeRepresentation()), 1, isVFP, e0.getType().value.toString());
		}
		/* Partial Atomic Loading Right */
		else if (e1 instanceof Atom) {
			this.loadOperand(e0, 1, r, map, st, isVFP);
			AsNBody.literalManager.loadValue(this, Integer.parseInt(e1.getType().toPrimitive().sourceCodeRepresentation()), 2, isVFP, e1.getType().value.toString());
		}
		else m.generatePrimitiveLoaderCode(m, b, e0, e1, r, map, st, 1, 2, isVFP);
	}
	
	protected void loadOperand(Expression e, int target, RegSet r, MemoryMap map, StackSet st, boolean isVFP) throws CGEN_EXC {
		/* Operand is ID Reference and can be loaded directley into the target register, 
		 * 		no need for intermidiate result in R0 */
		if (e instanceof IDRef) {
			this.instructions.addAll(AsNIDRef.cast((IDRef) e, r, map, st, target).getInstructions());
		}
		else {
			this.instructions.addAll(AsNExpression.cast(e, r, map, st).getInstructions());
			if (target != 0) {
				if (isVFP) {
					this.instructions.add(new ASMVMov(new VRegOp(target), new VRegOp(REG.S0)));
					r.getVRegSet().copy(0, target);
				}
				else {
					this.instructions.add(new ASMMov(new RegOp(target), new RegOp(REG.R0)));
					r.copy(0, target);
				}
			}
		}
	}
	
} 
