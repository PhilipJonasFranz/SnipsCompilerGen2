package Imm.AsN.Statement;

import CGen.RegSet;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Return;
import Imm.AsN.AsNNode;

public abstract class AsNStatement extends AsNNode {

	public AsNStatement() {
		
	}
	
	public static AsNStatement cast(SyntaxElement s, RegSet r) {
		/* Relay to statement type */
		if (s instanceof Return) {
			return AsNReturn.cast((Return) s, r); 
		}
		else return null;
	}
	
}
