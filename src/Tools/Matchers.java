package Tools;

import Exc.CGEN_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Statement.Declaration;

public class Matchers {

	/**
	 * Checks if in the given subtree defined by the given statement, has an address reference via address of
	 * is made to a variable with given origin declaration. If so, return true.
	 */
	public static boolean hasAddressReference(SyntaxElement s, Declaration dec) throws CGEN_EXC {
		return !s.visit(x -> {
			if (x instanceof AddressOf) {
				AddressOf aof = (AddressOf) x;
				if (aof.expression instanceof IDRef) 
					return (((IDRef) aof.expression).origin.equals(dec));
				else if (aof.expression instanceof IDRefWriteback) 
					return (((IDRefWriteback) aof.expression).idRef.origin.equals(dec));
				else if (aof.expression instanceof StructSelect) 
					/* Struct will be on the stack anyway */
					return true;
				else if (aof.expression instanceof StructureInit) 
					return false;
				else if (aof.expression instanceof ArraySelect) {
					return (((ArraySelect) aof.expression).idRef.origin.equals(dec));
				}
			}
			
			return false;
		}).isEmpty();
	}
	
	public boolean hasRefTo(SyntaxElement s, Declaration dec) {
		return !(s.visit(x -> {
			if (x instanceof IDRef) {
				IDRef ref = (IDRef) x;
				if (ref.origin.equals(dec)) return true;
			}
			return false;
		}).isEmpty());
	}
	
}
