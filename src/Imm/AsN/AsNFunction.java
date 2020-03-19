package Imm.AsN;

import CGen.RegSet;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.AST.Function;
import Imm.AST.Statement.Statement;
import Imm.AsN.Statement.AsNStatement;

public class AsNFunction extends AsNNode {

	Function source;
	
	public AsNFunction() {
		
	}
	
	/**
	 * Casts given syntax element based on the given reg set to a asm function node. 
	 */
	public static AsNFunction cast(Function f, RegSet r) {
		AsNFunction function = new AsNFunction();
		function.source = f;
		
		function.instructions.add(new ASMSectionAnnotation(SECTION.GLOBAL, function.source.functionName));
		function.instructions.add(new ASMLabel(function.source.functionName));
		
		/* Set Params in Registers */
		for (int i = 0; i < f.parameters.size(); i++) {
			r.regs [i].setDeclaration(f.parameters.get(i));
		}
		
		for (Statement s : f.statements) {
			function.instructions.addAll(AsNStatement.cast(s, r).getInstructions());
		}
		
		return function;
	}

}
