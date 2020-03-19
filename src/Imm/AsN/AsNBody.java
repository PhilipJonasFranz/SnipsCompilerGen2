package Imm.AsN;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

public class AsNBody extends AsNNode {

	List<AsNFunction> functions = new ArrayList();
	
	public AsNBody() {
		
	}
	
	/**
	 * Casts given program into an ASMBody.
	 */
	public static AsNBody cast(Program p) {
		AsNBody body = new AsNBody();
		
		body.instructions.add(new ASMComment("---" + CompilerDriver.file.getName()));
		
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
		body.instructions.add(text);
		
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function) {
				body.instructions.addAll(AsNFunction.cast((Function) s, new RegSet()).getInstructions());
			}
		}
		
		return body;
	}
	
}
