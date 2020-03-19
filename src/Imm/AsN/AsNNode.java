package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import Imm.ASM.ASMInstruction;
import Imm.AST.SyntaxElement;

public abstract class AsNNode {

	public List<ASMInstruction> instructions = new ArrayList();
	
	public AsNNode() {
		
	}
	
	/**
	 * Casts given syntax element based on the given reg set to a asm node. 
	 */
	public static AsNNode cast(SyntaxElement s, RegSet r) {
		return null;
	}
	
	/**
	 * Returns all generated ASM Instructions in order. 
	 */
	public List<ASMInstruction> getInstructions() {
		return this.instructions;
	}
	
}
