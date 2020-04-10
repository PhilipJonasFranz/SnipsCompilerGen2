package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Lhs.ElementSelectLhsId;
import Imm.AST.Lhs.LhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AsN.Statement.AsNStatement;

public class AsNLhsId extends AsNStatement {

	/**
	 * Casting an lhs will cause the value stored in R0 or on the stack to be stored at the target
	 * location specified by the lhs.
	 */
	public static AsNLhsId cast(LhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNLhsId id = null;
		
		if (lhs instanceof SimpleLhsId) {
			id = AsNSimpleLhsId.cast((SimpleLhsId) lhs, r, map, st);
		}
		else if (lhs instanceof ElementSelectLhsId) {
			id = AsNElementSelectLhsId.cast((ElementSelectLhsId) lhs, r, map, st);
		}
		else throw new CGEN_EXCEPTION(lhs.getSource(), "No injection cast available for " + lhs.getClass().getName());
	
		lhs.castedNode = id;
		return id;
	}
	
}
