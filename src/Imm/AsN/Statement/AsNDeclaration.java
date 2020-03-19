package Imm.AsN.Statement;

import CGen.RegSet;
import Imm.AST.Statement.Declaration;

public class AsNDeclaration extends AsNStatement {

	public AsNDeclaration() {
		
	}

	public static AsNDeclaration cast(Declaration d, RegSet r) {
		AsNDeclaration dec = new AsNDeclaration();
		
		dec.build();
		return dec;
	}
	
	protected void build() {
		
	}
	
}
