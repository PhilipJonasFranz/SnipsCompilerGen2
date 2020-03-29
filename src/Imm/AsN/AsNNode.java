package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Processing.ASMMov;
import Imm.ASM.Stack.ASMStrStack;
import Imm.ASM.Stack.ASMMemOp.MEM_OP;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.SyntaxElement;

public abstract class AsNNode {

	public List<ASMInstruction> instructions = new ArrayList();
	
	
	/**
	 * Casts given syntax element based on the given reg set to a asm node. 
	 */
	public static AsNNode cast(SyntaxElement s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		return null;
	}
	
	/**
	 * Returns all generated ASM Instructions in order. 
	 */
	public List<ASMInstruction> getInstructions() {
		return this.instructions;
	}
	
	/**
	 * Clear given reg under the current RegSet by searching for a free reg and copying the value
	 * into it. If no reg is free, copy to stack. Clears the given reg in the RegSet.
	 * @param r The current RegSet
	 * @param regs The Register to clear
	 */
	protected void clearReg(RegSet r, StackSet st, int...regs) {
		for (int reg : regs) {
			if (!r.getReg(reg).isFree()) {
				int free = r.findFree();
				
				if (free == -1) {
					this.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(reg), new RegOperand(REGISTER.SP), 
						new PatchableImmOperand(PATCH_DIR.DOWN, -4)));
					st.push(r.getReg(reg).declaration);
				}
				else {
					this.instructions.add(new ASMMov(new RegOperand(free), new RegOperand(reg)));
					r.copy(reg, free);
				}
				
				r.free(reg);
			}
		}
	}
	
}
