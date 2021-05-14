package Imm.AsN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import CGen.RegSet;
import CGen.StackSet;
import CGen.VRegSet;
import Exc.SNIPS_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Memory.Stack.ASMStrStack;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.VRegOp;
import Imm.ASM.VFP.Processing.Arith.ASMVMov;
import Imm.AST.SyntaxElement;
import Util.Pair;

public abstract class AsNNode {

	/* 
	 * Tracks which AsNNode is creating instructions at the moment. 
	 * Instructions will set their creator to the current top of the stack
	 * when they are created.
	 */
	public static Stack<AsNNode> creatorStack = new Stack();
	
	/* AsNNode, Amount of Commits to this Node Type, Instruction Size, Cycles */
	public static HashMap<String, Pair<Integer, Pair<Double, Double>>> metricsMap = new HashMap();
	
	
			/* ---< FIELDS >--- */
	/**
	 * The ASM-Instructions that represent the cast of this node.
	 */
	public List<ASMInstruction> instructions = new ArrayList();
	
	/**
	 * The AST-Node that that this AsNNode was casted from.
	 */
	public SyntaxElement castedNode;
	
	
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
	public void clearReg(RegSet r, StackSet st, boolean isVFP, int...regs) {
		if (isVFP) {
			VRegSet v = r.getVRegSet();
			
			for (int reg : regs) {
				if (!v.getReg(reg).isFree()) {
					int free = v.findFree();
					
					if (free == -1) {
						this.instructions.add(new ASMStrStack(MEM_OP.PRE_WRITEBACK, new VRegOp(reg), new RegOp(REG.SP), 
							new PatchableImmOp(PATCH_DIR.DOWN, -4)));
						st.push(v.getReg(reg).declaration);
					}
					else {
						ASMVMov mov = new ASMVMov(new VRegOp(free), new VRegOp(reg));
						
						/* Mark for optimizer to prevent double crossing optimization */
						mov.optFlags.add(OPT_FLAG.FUNC_CLEAN);
						
						this.instructions.add(mov);
						v.copy(reg, free);
					}
					
					v.free(reg);
				}
			}
		}
		else {
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
	}
	
	/**
	 * Push this AsNNode on the stack and set the casted node of
	 * this node to the given syntax element.
	 * @param s The AST-Node this AsNNode is casting.
	 */
	public void pushOnCreatorStack(SyntaxElement s) {
		creatorStack.push(this);
		this.castedNode = s;
	}
	
	/**
	 * Pop this node from the creator-stack and register its metrics
	 * in the {@link #metricsMap}.
	 */
	public void registerMetric() {
		if (creatorStack.isEmpty()) throw new SNIPS_EXC("Attempted to pop from empty creator stack!");
		else if (!creatorStack.peek().equals(this)) throw new SNIPS_EXC("Creator stack is not lined up!");
		
		creatorStack.pop();
		
		String key = this.getClass().getSimpleName();
		
		if (!metricsMap.containsKey(key)) 
			metricsMap.put(key, new Pair<Integer, Pair<Double, Double>>(0, new Pair<>(0.0, 0.0)));
		
		Pair<Integer, Pair<Double, Double>> pair = metricsMap.get(key);
		
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
