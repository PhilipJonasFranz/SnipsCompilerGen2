package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.Util.REG;
import Imm.ASM.VFP.Memory.Stack.ASMVPushStack;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TempAtom;
import Imm.AsN.AsNNode;
import Imm.TYPE.COMPOSIT.STRUCT;
import Snips.CompilerDriver;

import java.util.List;

public class AsNStructureInit extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNStructureInit cast(StructureInit s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNStructureInit init = new AsNStructureInit().pushCreatorStack(s);

		r.free(0, 1, 2);
		r.getVRegSet().free(0, 1, 2);
		
		/* Check for special case, where entire struct is initialized with absolute placeholder */
		if (s.elements.size() == 1 && s.elements.get(0) instanceof TempAtom a) {
			if (a.base == null) {
				/* Size of the memory section that the placeholder covers in words */
				int size = s.structType.wordsize();
				
				/* One word is covered by the SID */
				if (!CompilerDriver.disableStructSIDHeaders) size--;
				
				/* Make space on stack */
				init.instructions.add(new ASMSub(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(size * 4)));
				
				st.pushDummies(size);
				
				if (!CompilerDriver.disableStructSIDHeaders) {
					/* Load SID header */
					s.structType.getTypedef().loadSIDInReg(init, REG.R0, s.structType.proviso);
					init.instructions.add(new ASMPushStack(new RegOp(REG.R0)));
					
					/* Push dummy for SID header */
					st.pushDummy();
				}

				return init.popCreatorStack();
			}
		}
		
		structureInit(init, s.elements, (STRUCT) s.getType(), s.isTopLevelExpression, s.hasCoveredParam, r, map, st);

		return init.popCreatorStack();
	}

	/**
	 * Flags the given push instruction with the STRUCT_INIT OPT_FLAG
	 * and returns the instruction.
	 */
	public static ASMPushStack flagInit(ASMPushStack push) {
		return push.flag(OPT_FLAG.STRUCT_INIT);
	}

	/**
	 * Loads the element in reverse order on the stack, so the first element in the list will end up on the top 
	 * of the stack.
	 */
	public static void structureInit(AsNNode node, List<Expression> elements, STRUCT struct, boolean isTopLevel, boolean coveredParam, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Compute all elements, push them push them with dummy value on the stack */
		int regs = 0;
		
		boolean isVFP = elements.get(elements.size() - 1).getType().isFloat();
		
		for (int i = elements.size() - 1; i >= 0; i--) {
			/* If elements are multiple atoms after another, the push can be grouped together in max three */
			if (elements.get(i) instanceof Atom atom) {
				/* If group size is 3, push them on the stack */
				if (regs == 3 || elements.get(i).getType().isFloat() != isVFP) {
					flush(regs, node, isVFP);
					regs = 0;
				}
				
				/* Load atom directley in destination */
				node.instructions.addAll(AsNAtom.cast(atom, r, map, st, regs).getInstructions());
				regs++;
				
				st.pushDummy();
			}
			else if (elements.get(i) instanceof TempAtom atom) {
				if (atom.getType().wordsize() > 1 || atom.getType().isFloat() != isVFP) {
					flush(regs, node, isVFP);
					regs = 0;
				}
				
				node.instructions.addAll(AsNTempAtom.cast(atom, r, map, st, regs).getInstructions());
				
				if (atom.getType().wordsize() == 1) regs++;
			}
			else {
				/* Flush all atoms to clear regs */
				flush(regs, node, isVFP);
				regs = 0;
				
				node.instructions.addAll(AsNExpression.cast(elements.get(i), r, map, st).getInstructions());
			
				/* Push on stack, push R0 on stack, AsNDeclaration will pop the R0s and replace it with the declaration */
				if (!elements.get(i).getType().isStackType()) {
					if (isVFP) node.instructions.add(flagInit(new ASMVPushStack(new VRegOp(REG.S0))));
					else node.instructions.add(flagInit(new ASMPushStack(new RegOp(REG.R0))));
					st.pushDummy();
				}
			}
			
			isVFP = elements.get(i).getType().isFloat();
		}
		
		/* 
		 * When optimizing, the last push statement can be removed. This results in a 
		 * performance improvement, but bigger file size. When this piece of code is 
		 * not executed, its possible that the SID is part of one push. This means that
		 * it can not be optimized for performance as well, but the file size is smaller.
		 */
		if (isTopLevel && !CompilerDriver.optimizeFileSize) {
			flush(regs, node, isVFP);
			regs = 0;
		}
		
		/* Delete pushed SID for first param if param is covered */
		if (coveredParam && !CompilerDriver.disableStructSIDHeaders) {
			node.instructions.add(new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(4)));
			st.pop();
		}
		
		if (!CompilerDriver.disableStructSIDHeaders && struct != null) {
			/* Load SID header */
			struct.getTypedef().loadSIDInReg(node, new RegOp(regs).reg, struct.proviso);
			
			/* Push dummy for SID header */
			st.pushDummy();
			regs++;
			
			isVFP = false;
		}
		
		/* Flush remaining atoms */
		flush(regs, node, isVFP);
	}
	
	/**
	 * Shared with AsNIDRef.<br>
	 * Flush {@link #regs} on the stack. The flushed regs can be R0, R1, R2, based on regs.
	 * F. e. if regs is 1, only R0 is flushed. If regs equals 3, R2, R1, R0 are flushed.<br>
	 * The push order is so that f.E. R2 would end up at a higher address in the stack than R0.<br>
	 * Requires that regs is between 0 and 3.
	 */
	public static void flush(int regs, AsNNode node, boolean isVFP) {
		if (isVFP) {
			if (regs > 0) {
				if (regs == 3) node.instructions.add(flagInit(new ASMVPushStack(new VRegOp(REG.S2), new VRegOp(REG.S1), new VRegOp(REG.S0))));
				else if (regs == 2) node.instructions.add(flagInit(new ASMVPushStack(new VRegOp(REG.S1), new VRegOp(REG.S0))));
				else node.instructions.add(flagInit(new ASMVPushStack(new VRegOp(REG.S0))));
			}
		}
		else {
			if (regs > 0) {
				if (regs == 3) node.instructions.add(flagInit(new ASMPushStack(new RegOp(REG.R2), new RegOp(REG.R1), new RegOp(REG.R0))));
				else if (regs == 2) node.instructions.add(flagInit(new ASMPushStack(new RegOp(REG.R1), new RegOp(REG.R0))));
				else node.instructions.add(flagInit(new ASMPushStack(new RegOp(REG.R0))));
			}
		}
	}
	
} 
