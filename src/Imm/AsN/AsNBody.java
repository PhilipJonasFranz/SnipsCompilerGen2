package Imm.AsN;

import java.util.List;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMLabel;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

public class AsNBody extends AsNNode {

	public static AsNBody cast(Program p) throws CGEN_EXCEPTION {
		AsNBody body = new AsNBody();
		p.castedNode = body;
		
		body.instructions.add(new ASMComment("--" + CompilerDriver.file.getName()));
		
		ASMSectionAnnotation data = new ASMSectionAnnotation(SECTION.DATA);
		body.instructions.add(data);
		
		int globals = 0;
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Declaration) {
				globals++;
			}
		}
		
		if (globals == 0) body.instructions.remove(body.instructions.size() - 1);
		
		ASMSectionAnnotation text = new ASMSectionAnnotation(SECTION.TEXT);
		if (globals > 0) body.instructions.add(text);
		
		/* Branch to main Function */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOperand());
		if (!(p.programElements.get(0) instanceof Function && ((Function) p.programElements.get(0)).functionName.equals("main"))) {
			body.instructions.add(branch);
		}
		
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function) {
				List<ASMInstruction> ins = AsNFunction.cast((Function) s, new RegSet(), new StackSet()).getInstructions();
				
				/* Patch Branch to Main Function */
				if (((Function) s).functionName.equals("main")) {
					((LabelOperand) branch.target).patch((ASMLabel) ins.get(0));
				}
				
				body.instructions.addAll(ins);
			}
			
			body.instructions.add(new ASMSeperator());
		}
		
		return body;
	}
	
}
