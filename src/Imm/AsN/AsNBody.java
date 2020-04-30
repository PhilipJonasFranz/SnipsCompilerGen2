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
import Imm.ASM.Memory.ASMLdrLabel;
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
				ASMDataLabel dataEntry = new ASMDataLabel(dec.path.build(), new MemoryWordOperand(dec.value));
				body.instructions.add(dataEntry);
				
				/* Create address reference instruction for .text section */
				ASMDataLabel reference = new ASMDataLabel(LabelGen.mapToAddressName(dec.path.build()), new MemoryWordRefOperand(dataEntry));
				globalVarReferences.add(reference);
				
				/* Add declaration to global memory */
				map.add(dec, reference);
				globals = true;
			}
		}
		
		if (CompilerDriver.heap_referenced) {
			globals = true;
			
			Declaration heap = CompilerDriver.HEAP_START;
			
			/* Create instruction for .data Section */
			ASMDataLabel dataEntry = new ASMDataLabel(heap.path.build(), new MemoryWordOperand(heap.value));
			body.instructions.add(dataEntry);
			
			/* Create address reference instruction for .text section */
			ASMDataLabel reference = new ASMDataLabel(LabelGen.mapToAddressName(heap.path.build()), new MemoryWordRefOperand(dataEntry));
			globalVarReferences.add(reference);
			
			/* Add declaration to global memory */
			map.add(heap, reference);
		}
		
		/* No globals, remove .data annotation */
		if (!globals) body.instructions.remove(data);
		
		
		/* Add .text annotation if other sections exist */
		if (globals) {
			body.instructions.add(new ASMSeperator());
			body.instructions.add(new ASMSectionAnnotation(SECTION.TEXT));
		}
		
		
		/* Branch to main Function if main function is not first function, patch target later */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOperand());
		if (!(p.programElements.get(0) instanceof Function && ((Function) p.programElements.get(0)).path.build().equals("main"))) {
			body.instructions.add(branch);
		}
		
		
		/* Cast program elements */
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function) {
				List<ASMInstruction> ins = AsNFunction.cast((Function) s, new RegSet(), map, new StackSet()).getInstructions();
				
				/* Patch Branch to Main Function */
				if (((Function) s).path.build().equals("main")) 
					((LabelOperand) branch.target).patch((ASMLabel) ins.get(0));
				
				body.instructions.addAll(ins);
				body.instructions.add(new ASMSeperator());
			}
			else if (s instanceof Comment) {
				body.instructions.addAll(AsNComment.cast((Comment) s, null, map, null).getInstructions());
			}
		}
		
		/* Manage and create Literal Pools */
		List<ASMLdrLabel> buffer = new ArrayList();
		for (int i = 0; i < body.instructions.size(); i++) {
			/* Collect ASMLdrLabel instructions in buffer, insert them after next bx instruction */
			if (body.instructions.get(i) instanceof ASMLdrLabel) {
				ASMLdrLabel load = (ASMLdrLabel) body.instructions.get(i);
				buffer.add(load);
			}
			else if (body.instructions.get(i) instanceof ASMBranch) {
				ASMBranch b = (ASMBranch) body.instructions.get(i);
				
				// TODO: Implement Filter to only flush buffer after 4k of text
				
				if (b.type == BRANCH_TYPE.BX) {
					/* Flush buffer here */
					if (!buffer.isEmpty()) {
						/* Create a new prefix for this literal pool */
						String prefix = LabelGen.literalPoolPrefix();
						
						List<String> added = new ArrayList();
						
						for (ASMLdrLabel label : buffer) {
							/* Apply prefix to load label */
							label.prefix = prefix;
							
							/* Get label referenced by load instruction and clone it */
							ASMDataLabel l0 = map.resolve(label.dec).clone();
							
							/* Apply prefix to label name */
							if (l0.name.startsWith(".")) l0.name = l0.name.substring(1);
							l0.name = prefix + l0.name;
							
							if (added.contains(l0.name)) continue;
							else added.add(l0.name);
							
							/* Inject label clone at target position */
							body.instructions.add(i + 1, l0);
						}
						
						buffer.clear();
					}
				}
			}
		}
		
		/* Main function not present */
		if (((LabelOperand) branch.target).label == null) 
			body.instructions.remove(branch);
		
		return body;
	}
	
}
