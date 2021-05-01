package Imm.AsN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import CGen.RegSet;
import CGen.StackSet;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Util.Pair;

public abstract class AsNNode {

	public static Stack<AsNNode> creatorStack = new Stack();
	
	/* AsNNode, Amount of Commits to this Node Type, Instruction Size, Cycles */
	public static HashMap<String, Pair<Integer, Pair<Integer, Integer>>> metricsMap = new HashMap();
	
	
			/* ---< NESTED >--- */
	public enum MODIFIER {
		
		STATIC, SHARED, RESTRICTED, EXCLUSIVE;
		
	}
	
	
			/* ---< FIELDS >--- */
	public List<ASMInstruction> instructions = new ArrayList();
	
	
			/* ---< METHODS >--- */
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
					this.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new RegOp(reg), new RegOp(REG.SP), 
						new PatchableImmOp(PATCH_DIR.DOWN, -4)));
					st.push(r.getReg(reg).declaration);
				}
				else {
					ASMMov mov = new ASMMov(new RegOp(free), new RegOp(reg));
					
					/* Mark for optimizer to prevent double crossing optimization */
					mov.optFlags.add(OPT_FLAG.FUNC_CLEAN);
					
					this.instructions.add(mov);
					r.copy(reg, free);
				}
				
				r.free(reg);
			}
		}
	}
	
	public void pushOnCreatorStack() {
		creatorStack.push(this);
	}
	
	public void registerMetric() {
		if (creatorStack.isEmpty()) throw new SNIPS_EXC("Attempted to pop from empty creator stack!");
		else if (!creatorStack.peek().equals(this)) throw new SNIPS_EXC("Creator stack is not lined up!");
		
		creatorStack.pop();
		
		String key = this.getClass().getSimpleName();
		
		if (!metricsMap.containsKey(key)) 
			metricsMap.put(key, new Pair<>(0, new Pair<>(0, 0)));
		
		Pair<Integer, Pair<Integer, Integer>> pair = metricsMap.get(key);
		
		pair.first++;
		
		int cycles = 0;
		int sum = 0;
		for (ASMInstruction ins : this.getInstructions()) {
			if (ins.creator.equals(this)) {
				cycles += ins.getRequiredCPUCycles();
				sum++;
			}
		}
		
		pair.second.first += sum;
		pair.second.second += cycles;
	}
	
} 
