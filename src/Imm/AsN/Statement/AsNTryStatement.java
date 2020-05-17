package Imm.AsN.Statement;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Statement.TryStatement;

public class AsNTryStatement extends AsNCompoundStatement {

	public static AsNTryStatement cast(TryStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		AsNTryStatement tr0 = new AsNTryStatement();
		
		
		
		return tr0;
	}
	
}
