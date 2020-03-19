package Imm.AsN;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
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
	public static AsNFunction cast(Function f, RegSet r) throws CGEN_EXCEPTION {
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
		
		function.instructions.add(new ASMBranch(BRANCH_TYPE.BX, new RegOperand(REGISTER.LR)));
		
		return function;
	}

}
