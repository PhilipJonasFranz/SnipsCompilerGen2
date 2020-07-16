package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.PatchableImmOperand;
import Imm.ASM.Util.Operands.PatchableImmOperand.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.SyntaxElement;

public abstract class AsNNode {

			/* --- NESTED --- */
	public enum MODIFIER {
		
		SHARED, RESTRICTED, EXCLUSIVE;
		
	}
	
	
			/* --- FIELDS --- */
	public List<ASMInstruction> instructions = new ArrayList();
	
	
			/* --- METHODS --- */
	/**
	 * Casts given syntax element based on the given reg set to a asm node. 
	 */
	public static AsNNode cast(SyntaxElement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
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
	public void clearReg(RegSet r, StackSet st, int...regs) {
		for (int reg : regs) {
			if (!r.getReg(reg).isFree()) {
				int free = r.findFree();
				
				if (free == -1) {
					this.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(reg), new RegOperand(REGISTER.SP), 
						new PatchableImmOperand(PATCH_DIR.DOWN, -4)));
					st.push(r.getReg(reg).declaration);
				}
				else {
					ASMMov mov = new ASMMov(new RegOperand(free), new RegOperand(reg));
					
					/* Mark for optimizer to prevent double crossing optimization */
					mov.optFlags.add(OPT_FLAG.FUNC_CLEAN);
					
					this.instructions.add(mov);
					r.copy(reg, free);
				}
				
				r.free(reg);
			}
		}
	}
	
}
