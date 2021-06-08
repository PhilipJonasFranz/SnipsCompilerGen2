package Imm.AsN.Statement.Lhs;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Lhs.LhsId;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AsN.Statement.AsNStatement;
import Res.Const;

public class AsNLhsId extends AsNStatement {

	/**
	 * Casting an lhs will cause the value stored in R0 or on the stack to be stored at the target
	 * location specified by the lhs.
	 */
	public static AsNLhsId cast(LhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNLhsId id = null;
		
		if (lhs instanceof SimpleLhsId) {
			id = AsNSimpleLhsId.cast((SimpleLhsId) lhs, r, map, st);
		}
		else if (lhs instanceof ArraySelectLhsId) {
			id = AsNArraySelectLhsId.cast((ArraySelectLhsId) lhs, r, map, st);
		}
		else if (lhs instanceof PointerLhsId) {
			id = AsNPointerLhsId.cast((PointerLhsId) lhs, r, map, st);
		}
		else if (lhs instanceof StructSelectLhsId) {
			id = AsNStructSelectLhsId.cast((StructSelectLhsId) lhs, r, map, st);
		}
		else throw new CGEN_EXC(lhs.getSource(), Const.NO_INJECTION_CAST_AVAILABLE, lhs.getClass().getName());

		return id;
	}
	
} 
