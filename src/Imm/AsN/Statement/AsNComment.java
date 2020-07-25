package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Structural.ASMComment;
import Imm.AST.Statement.Comment;

public class AsNComment extends AsNStatement {

	public static AsNComment cast(Comment a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNComment com = new AsNComment();
		
		com.instructions.add(new ASMComment(a.comment));
		
		return com;
	}
	
}
