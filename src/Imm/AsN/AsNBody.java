package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.Memory.MemoryWordOperand;
import Imm.ASM.Util.Operands.Memory.MemoryWordRefOperand;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Statement.AsNComment;
import Snips.CompilerDriver;

public class AsNBody extends AsNNode {

			/* --- METHODS --- */
	public static AsNBody cast(Program p) throws CGEN_EXCEPTION {
		AsNBody body = new AsNBody();
		p.castedNode = body;
		
		MemoryMap map = new MemoryMap();
		
		/* File name comment */
		body.instructions.add(new ASMComment("--" + CompilerDriver.file.getName()));
		
		
		/* Create .data section annotation */
		ASMSectionAnnotation data = new ASMSectionAnnotation(SECTION.DATA);
		body.instructions.add(data);
		
		/* Count global variables */
		boolean globals = false;
		List<ASMInstruction> globalVarReferences = new ArrayList();
		
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Declaration) {
				Declaration dec = (Declaration) s;
				
				/* Create instruction for .data Section */
				ASMDataLabel dataEntry = new ASMDataLabel(dec.fieldName, new MemoryWordOperand(dec.value));
				body.instructions.add(dataEntry);
				
				/* Create address reference instruction for .text section */
				ASMDataLabel reference = new ASMDataLabel(LabelGen.mapToAddressName(dec.fieldName), new MemoryWordRefOperand(dataEntry));
				globalVarReferences.add(reference);
				
				/* Add declaration to global memory */
				map.add(dec, reference);
				globals = true;
			}
		}
		
		/* No globals, remove .data annotation */
		if (!globals) body.instructions.remove(body.instructions.size() - 1);
		
		
		/* Add .text annotation if other sections exist */
		if (globals) {
			body.instructions.add(new ASMSeperator());
			body.instructions.add(new ASMSectionAnnotation(SECTION.TEXT));
		}
		
		
		/* Branch to main Function if main function is not first function, patch target later */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOperand());
		if (!(p.programElements.get(0) instanceof Function && ((Function) p.programElements.get(0)).functionName.equals("main"))) {
			body.instructions.add(branch);
		}
		
		
		/* Cast program elements */
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function) {
				List<ASMInstruction> ins = AsNFunction.cast((Function) s, new RegSet(), map, new StackSet()).getInstructions();
				
				/* Patch Branch to Main Function */
				if (((Function) s).functionName.equals("main")) 
					((LabelOperand) branch.target).patch((ASMLabel) ins.get(0));
				
				body.instructions.addAll(ins);
				body.instructions.add(new ASMSeperator());
			}
			else if (s instanceof Comment) {
				body.instructions.addAll(AsNComment.cast((Comment) s, null, map, null).getInstructions());
			}
		}
		
		
		/* Main function not present */
		if (((LabelOperand) branch.target).label == null) 
			body.instructions.remove(branch);
		
		body.instructions.addAll(globalVarReferences);
		
		return body;
	}
	
}
